package com.beatstudio.app.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.beatstudio.app.audio.instruments.SampleBuffer
import com.beatstudio.app.audio.instruments.SampleStore
import com.beatstudio.app.domain.MusicalScale
import com.beatstudio.app.util.FileUtils
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

class MicRecorder {
    private val isRecordingFlag = AtomicBoolean(false)
    private var recordThread: Thread? = null
    private val recordedSamples = ArrayList<Float>(44100 * 15)

    @Volatile var liveInputLevel: Float = 0f
        private set

    val isRecording: Boolean
        get() = isRecordingFlag.get()

    @SuppressLint("MissingPermission")
    fun startRecording(
        onError: (String) -> Unit
    ) {
        if (isRecordingFlag.get()) return
        val sampleRate = 44100
        val minBufBytes = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufBytes <= 0) {
            onError("Microphone shuru nahi ho paya.")
            return
        }

        val recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufBytes * 2
            )
        } catch (_: SecurityException) {
            onError("Mic permission nahi mili! Voice record karne ke liye Microphone permission allow karein.")
            return
        } catch (_: Exception) {
            onError("Microphone hardware busy hai ya available nahi hai.")
            return
        }

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            onError("Microphone initialize nahi ho paya.")
            return
        }

        synchronized(recordedSamples) {
            recordedSamples.clear()
        }
        isRecordingFlag.set(true)

        recordThread = Thread({
            val shortBuf = ShortArray(minBufBytes / 2)
            try {
                recorder.startRecording()
                while (isRecordingFlag.get()) {
                    val read = recorder.read(shortBuf, 0, shortBuf.size)
                    if (read > 0) {
                        var peak = 0f
                        synchronized(recordedSamples) {
                            for (i in 0 until read) {
                                val f = shortBuf[i] / 32768f
                                recordedSamples.add(f)
                                val a = abs(f)
                                if (a > peak) peak = a
                            }
                            if (recordedSamples.size > 44100 * 45) {
                                isRecordingFlag.set(false)
                            }
                        }
                        liveInputLevel = peak
                    }
                }
            } catch (_: Exception) {
                onError("Voice recording ruk gayi.")
            } finally {
                liveInputLevel = 0f
                runCatching { recorder.stop(); recorder.release() }
            }
        }, "BeatStudio-MicRecorder").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun stopAndSave(
        context: Context,
        applyPitchSnap: Boolean,
        rootNote: Int,
        scale: MusicalScale,
        snapStrength: Float
    ): SampleBuffer? {
        if (!isRecordingFlag.get() && recordedSamples.isEmpty()) return null
        isRecordingFlag.set(false)
        runCatching { recordThread?.join(600) }
        recordThread = null

        val raw: FloatArray
        synchronized(recordedSamples) {
            if (recordedSamples.size < 512) return null
            raw = recordedSamples.toFloatArray()
            recordedSamples.clear()
        }

        // Apply 5ms click-free fade in/out
        val fadeSamples = minOf(220, raw.size / 4)
        for (i in 0 until fadeSamples) {
            val g = i.toFloat() / fadeSamples
            raw[i] *= g
            raw[raw.lastIndex - i] *= g
        }

        val finalSamples = if (applyPitchSnap) {
            PitchSnap.processBufferToScale(raw, 44100, rootNote, scale, snapStrength)
        } else {
            raw
        }

        val outFile = File(FileUtils.getVocalsDir(context), "vocal_${System.currentTimeMillis()}.pcm")
        SampleStore.saveMonoFloatSample(outFile, finalSamples, 44100)
        return SampleBuffer(outFile.absolutePath, finalSamples, 44100)
    }

    fun cancel() {
        isRecordingFlag.set(false)
        runCatching { recordThread?.join(300) }
        recordThread = null
        synchronized(recordedSamples) { recordedSamples.clear() }
        liveInputLevel = 0f
    }
}
