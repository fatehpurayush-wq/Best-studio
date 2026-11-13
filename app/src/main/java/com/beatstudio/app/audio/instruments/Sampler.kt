package com.beatstudio.app.audio.instruments

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.beatstudio.app.audio.Envelope
import com.beatstudio.app.domain.SamplerParams
import com.beatstudio.app.util.FileUtils
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteOrder
import kotlin.math.min
import kotlin.math.pow

data class SampleBuffer(
    val filePath: String,
    val samples: FloatArray, // Mono 44100Hz normalized -1..1
    val sampleRate: Int = 44100
)

class SamplerVoice {
    private var buffer: SampleBuffer? = null
    private var params = SamplerParams()
    private var position = 0.0
    private var rateRatio = 1.0
    private var startIdx = 0
    private var endIdx = 0
    private var velocity = 0.85f
    private val env = Envelope()

    val isActive: Boolean
        get() = env.isActive && buffer != null

    fun trigger(
        sampleBuffer: SampleBuffer,
        midiPitch: Int,
        vel: Float,
        samplerParams: SamplerParams,
        sampleRate: Float
    ) {
        val total = sampleBuffer.samples.size
        if (total <= 16) return
        buffer = sampleBuffer
        params = samplerParams
        velocity = vel.coerceIn(0.05f, 1f)

        val tStart = samplerParams.trimStart.coerceIn(0f, 0.95f)
        val tEnd = samplerParams.trimEnd.coerceIn(tStart + 0.02f, 1f)
        startIdx = (tStart * total).toInt().coerceIn(0, total - 2)
        endIdx = (tEnd * total).toInt().coerceIn(startIdx + 1, total - 1)

        val semitones = (midiPitch - samplerParams.rootMidiNote) + samplerParams.pitchSemitones
        rateRatio = 2.0.pow(semitones / 12.0) * (sampleBuffer.sampleRate.toDouble() / sampleRate)
        position = if (samplerParams.reverse) (endIdx - 1).toDouble() else startIdx.toDouble()

        env.configure(0.004f, 0.05f, 1.0f, 0.04f, sampleRate)
        env.trigger()
    }

    fun release() {
        env.release()
    }

    fun forceStop() {
        env.forceStop()
    }

    fun nextSample(): Float {
        val buf = buffer ?: return 0f
        val envVal = env.nextSample()
        if (!env.isActive) return 0f

        val idx0 = position.toInt().coerceIn(startIdx, endIdx)
        val idx1 = (idx0 + 1).coerceAtMost(endIdx)
        val frac = (position - idx0).toFloat().coerceIn(0f, 1f)
        val sample = buf.samples[idx0] * (1f - frac) + buf.samples[idx1] * frac

        if (params.reverse) {
            position -= rateRatio
            if (position <= startIdx) {
                if (params.loop) {
                    position = endIdx.toDouble()
                } else {
                    env.release()
                }
            }
        } else {
            position += rateRatio
            if (position >= endIdx) {
                if (params.loop) {
                    position = startIdx.toDouble()
                } else {
                    env.release()
                }
            }
        }

        return sample * envVal * velocity * 0.9f
    }
}

object SampleStore {
    private val cache = HashMap<String, SampleBuffer>()

    fun getCachedOrLoad(filePath: String?): SampleBuffer? {
        if (filePath.isNullOrBlank()) return null
        cache[filePath]?.let { return it }
        val file = File(filePath)
        if (!file.exists()) return null
        return loadRawFloatFile(file)?.also { cache[filePath] = it }
    }

    fun putSample(buffer: SampleBuffer) {
        cache[buffer.filePath] = buffer
    }

    fun saveMonoFloatSample(file: File, samples: FloatArray, sampleRate: Int = 44100) {
        DataOutputStream(BufferedOutputStream(FileOutputStream(file))).use { dos ->
            dos.writeInt(sampleRate)
            dos.writeInt(samples.size)
            for (s in samples) {
                dos.writeFloat(s)
            }
        }
        cache[file.absolutePath] = SampleBuffer(file.absolutePath, samples, sampleRate)
    }

    private fun loadRawFloatFile(file: File): SampleBuffer? {
        return try {
            DataInputStream(BufferedInputStream(FileInputStream(file))).use { dis ->
                val sr = dis.readInt()
                val len = dis.readInt()
                if (len <= 0 || len > 44100 * 120) return null
                val arr = FloatArray(len)
                for (i in 0 until len) arr[i] = dis.readFloat()
                SampleBuffer(file.absolutePath, arr, sr)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Decodes a picked audio Uri (WAV/MP3/AAC/M4A/OGG/FLAC) into internal 44100Hz mono float PCM.
     * Returns Result with SampleBuffer or clear Hinglish error.
     */
    fun importAudioUri(context: Context, uri: Uri, targetNamePrefix: String = "sample"): Result<SampleBuffer> {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        return try {
            extractor.setDataSource(context, uri, null)
            var audioTrackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = fmt
                    break
                }
            }
            if (audioTrackIndex < 0 || format == null) {
                return Result.failure(IllegalArgumentException("Yeh audio file supported nahi hai (koi audio track nahi mila)."))
            }

            extractor.selectTrack(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            val srcSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val srcChannels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 1

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val maxInputSamples = srcSampleRate * 25 // cap at 25 seconds for mobile memory safety
            val decodedMono = ArrayList<Float>(min(maxInputSamples, srcSampleRate * 6))
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            while (!outputDone && decodedMono.size < maxInputSamples) {
                if (!inputDone) {
                    val inIndex = codec.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        val inBuf = codec.getInputBuffer(inIndex)!!
                        val sampleSize = extractor.readSampleData(inBuf, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(info, 10_000)
                if (outIndex >= 0) {
                    val outBuf = codec.getOutputBuffer(outIndex)
                    if (outBuf != null && info.size > 0) {
                        outBuf.position(info.offset)
                        outBuf.limit(info.offset + info.size)
                        val shortBuf = outBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                        val frameCount = shortBuf.remaining() / srcChannels.coerceAtLeast(1)
                        for (f in 0 until frameCount) {
                            var sum = 0f
                            for (c in 0 until srcChannels.coerceAtLeast(1)) {
                                sum += shortBuf.get() / 32768f
                            }
                            decodedMono.add(sum / srcChannels.coerceAtLeast(1))
                            if (decodedMono.size >= maxInputSamples) break
                        }
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true
                    }
                }
            }

            if (decodedMono.isEmpty()) {
                return Result.failure(IllegalArgumentException("Audio decode nahi ho paya — file khali ya unsupported hai."))
            }

            val resampled = if (srcSampleRate == 44100) {
                decodedMono.toFloatArray()
            } else {
                resampleLinear(decodedMono, srcSampleRate, 44100)
            }

            val safeName = FileUtils.sanitizeFileName(targetNamePrefix) + "_" + System.currentTimeMillis() + ".pcm"
            val outFile = File(FileUtils.getSamplesDir(context), safeName)
            saveMonoFloatSample(outFile, resampled, 44100)
            Result.success(SampleBuffer(outFile.absolutePath, resampled, 44100))
        } catch (_: Exception) {
            Result.failure(IllegalArgumentException("Unsupported audio file! Kripya WAV, MP3, ya M4A file chunein."))
        } finally {
            runCatching { codec?.stop(); codec?.release() }
            runCatching { extractor.release() }
        }
    }

    private fun resampleLinear(input: List<Float>, srcRate: Int, dstRate: Int): FloatArray {
        val ratio = srcRate.toDouble() / dstRate.toDouble()
        val outLen = (input.size / ratio).toInt().coerceAtLeast(1)
        val out = FloatArray(outLen)
        for (i in 0 until outLen) {
            val pos = i * ratio
            val idx0 = pos.toInt().coerceIn(0, input.lastIndex)
            val idx1 = (idx0 + 1).coerceAtMost(input.lastIndex)
            val frac = (pos - idx0).toFloat()
            out[i] = input[idx0] * (1f - frac) + input[idx1] * frac
        }
        return out
    }
}
