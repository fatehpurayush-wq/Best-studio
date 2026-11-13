package com.beatstudio.app.export

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.math.roundToInt

class WavWriter(
    private val file: File,
    private val sampleRate: Int = 44100,
    private val channels: Int = 2,
    private val bitsPerSample: Int = 16 // 16 or 24
) {
    private var dataBytesWritten: Long = 0L
    private val out = BufferedOutputStream(FileOutputStream(file), 65536)
    private val byteBuf = ByteArray(256 * 2 * 3)

    init {
        writeHeaderPlaceholder()
    }

    private fun writeHeaderPlaceholder() {
        val header = ByteArray(44)
        // RIFF
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        // WAVE
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        // fmt
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        writeIntLE(header, 16, 16) // PCM chunk size
        writeShortLE(header, 20, 1) // PCM format
        writeShortLE(header, 22, channels.toShort())
        writeIntLE(header, 24, sampleRate)
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
        writeIntLE(header, 28, byteRate)
        val blockAlign = (channels * (bitsPerSample / 8)).toShort()
        writeShortLE(header, 32, blockAlign)
        writeShortLE(header, 34, bitsPerSample.toShort())
        // data
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        out.write(header)
    }

    fun writeStereoFloatBlock(left: FloatArray, right: FloatArray, numFrames: Int) {
        var pos = 0
        if (bitsPerSample == 24) {
            for (i in 0 until numFrames) {
                val l = (left[i].coerceIn(-1f, 1f) * 8388607f).roundToInt()
                val r = (right[i].coerceIn(-1f, 1f) * 8388607f).roundToInt()
                byteBuf[pos++] = (l and 0xFF).toByte()
                byteBuf[pos++] = ((l shr 8) and 0xFF).toByte()
                byteBuf[pos++] = ((l shr 16) and 0xFF).toByte()
                byteBuf[pos++] = (r and 0xFF).toByte()
                byteBuf[pos++] = ((r shr 8) and 0xFF).toByte()
                byteBuf[pos++] = ((r shr 16) and 0xFF).toByte()
            }
        } else {
            for (i in 0 until numFrames) {
                val l = (left[i].coerceIn(-1f, 1f) * 32767f).roundToInt()
                val r = (right[i].coerceIn(-1f, 1f) * 32767f).roundToInt()
                byteBuf[pos++] = (l and 0xFF).toByte()
                byteBuf[pos++] = ((l shr 8) and 0xFF).toByte()
                byteBuf[pos++] = (r and 0xFF).toByte()
                byteBuf[pos++] = ((r shr 8) and 0xFF).toByte()
            }
        }
        out.write(byteBuf, 0, pos)
        dataBytesWritten += pos
    }

    fun close() {
        out.flush()
        out.close()
        RandomAccessFile(file, "rw").use { raf ->
            val riffSize = (36L + dataBytesWritten).toInt()
            raf.seek(4)
            raf.write(intToLE(riffSize))
            raf.seek(40)
            raf.write(intToLE(dataBytesWritten.toInt()))
        }
    }

    private fun intToLE(v: Int): ByteArray = byteArrayOf(
        (v and 0xFF).toByte(),
        ((v shr 8) and 0xFF).toByte(),
        ((v shr 16) and 0xFF).toByte(),
        ((v shr 24) and 0xFF).toByte()
    )

    private fun writeIntLE(dst: ByteArray, offset: Int, v: Int) {
        dst[offset] = (v and 0xFF).toByte()
        dst[offset + 1] = ((v shr 8) and 0xFF).toByte()
        dst[offset + 2] = ((v shr 16) and 0xFF).toByte()
        dst[offset + 3] = ((v shr 24) and 0xFF).toByte()
    }

    private fun writeShortLE(dst: ByteArray, offset: Int, v: Short) {
        val iv = v.toInt()
        dst[offset] = (iv and 0xFF).toByte()
        dst[offset + 1] = ((iv shr 8) and 0xFF).toByte()
    }
}
