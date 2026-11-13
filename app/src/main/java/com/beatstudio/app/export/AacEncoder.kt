package com.beatstudio.app.export

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

class AacEncoder(
    outFile: File,
    private val sampleRate: Int = 44100,
    private val channels: Int = 2,
    bitRate: Int = 192_000
) {
    private val codec: MediaCodec
    private val muxer: MediaMuxer
    private var muxerTrackIndex = -1
    private var muxerStarted = false
    private var totalFramesFed = 0L
    private val bufferInfo = MediaCodec.BufferInfo()
    private val pcmByteScratch = ByteArray(256 * 2 * 2)

    init {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }
        codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
        muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }

    fun writeStereoFloatBlock(left: FloatArray, right: FloatArray, numFrames: Int) {
        val totalBytes = numFrames * channels * 2
        var pos = 0
        for (i in 0 until numFrames) {
            val l = (left[i].coerceIn(-1f, 1f) * 32767f).roundToInt()
            val r = (right[i].coerceIn(-1f, 1f) * 32767f).roundToInt()
            pcmByteScratch[pos++] = (l and 0xFF).toByte()
            pcmByteScratch[pos++] = ((l shr 8) and 0xFF).toByte()
            pcmByteScratch[pos++] = (r and 0xFF).toByte()
            pcmByteScratch[pos++] = ((r shr 8) and 0xFF).toByte()
        }

        var offset = 0
        while (offset < totalBytes) {
            val inIdx = codec.dequeueInputBuffer(10_000)
            if (inIdx >= 0) {
                val buf: ByteBuffer = codec.getInputBuffer(inIdx)!!
                buf.clear()
                val chunk = minOf(buf.remaining(), totalBytes - offset)
                buf.order(ByteOrder.LITTLE_ENDIAN)
                buf.put(pcmByteScratch, offset, chunk)
                val ptsUs = (totalFramesFed * 1_000_000L) / sampleRate
                codec.queueInputBuffer(inIdx, 0, chunk, ptsUs, 0)
                totalFramesFed += chunk / (channels * 2)
                offset += chunk
            }
            drainOutput(endOfStream = false)
        }
    }

    fun finish() {
        val inIdx = codec.dequeueInputBuffer(10_000)
        if (inIdx >= 0) {
            val ptsUs = (totalFramesFed * 1_000_000L) / sampleRate
            codec.queueInputBuffer(inIdx, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        }
        drainOutput(endOfStream = true)
        runCatching { codec.stop(); codec.release() }
        runCatching {
            if (muxerStarted) muxer.stop()
            muxer.release()
        }
    }

    private fun drainOutput(endOfStream: Boolean) {
        while (true) {
            val outIdx = codec.dequeueOutputBuffer(bufferInfo, if (endOfStream) 10_000 else 0)
            when {
                outIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) break
                }
                outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (!muxerStarted) {
                        muxerTrackIndex = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                }
                outIdx >= 0 -> {
                    val outBuf = codec.getOutputBuffer(outIdx)
                    if (outBuf != null && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && bufferInfo.size > 0 && muxerStarted) {
                        outBuf.position(bufferInfo.offset)
                        outBuf.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(muxerTrackIndex, outBuf, bufferInfo)
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
                }
            }
        }
    }
}
