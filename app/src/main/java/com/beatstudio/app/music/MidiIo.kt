package com.beatstudio.app.music

import com.beatstudio.app.domain.Note
import com.beatstudio.app.domain.Project
import com.beatstudio.app.domain.TrackType
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

object MidiIo {

    private const val TICKS_PER_QUARTER = 480

    fun exportProjectToMidiBytes(project: Project): ByteArray {
        val trackChunks = mutableListOf<ByteArray>()

        // Track 0: Tempo & Time Signature meta track
        trackChunks.add(buildTempoTrack(project.bpm, project.timeSignatureTop))

        // Music tracks
        project.tracks.forEachIndexed { idx, track ->
            val pattern = track.patterns[track.activePatternId] ?: return@forEachIndexed
            val events = mutableListOf<MidiEvent>()
            val channel = if (track.type == TrackType.DRUMS) 9 else (idx % 15).let { if (it >= 9) it + 1 else it }
            val ticksPerStep = TICKS_PER_QUARTER / 4 // 16th note = 120 ticks

            if (track.type == TrackType.DRUMS) {
                track.drumRows.forEach { row ->
                    val steps = pattern.drumRows[row.rowIndex].orEmpty()
                    steps.forEachIndexed { stepIdx, step ->
                        if (step.active) {
                            val tick = stepIdx * ticksPerStep
                            val vel = (step.velocity * 127f).roundToInt().coerceIn(1, 127)
                            val midiPitch = row.soundType.defaultMidi
                            events.add(MidiEvent(tick, 0x90 or channel, midiPitch, vel))
                            events.add(MidiEvent(tick + ticksPerStep / 2, 0x80 or channel, midiPitch, 0))
                        }
                    }
                }
            } else {
                pattern.notes.forEach { note ->
                    val startTick = (note.startStep * ticksPerStep).roundToInt().coerceAtLeast(0)
                    val endTick = ((note.startStep + note.durationSteps) * ticksPerStep).roundToInt().coerceAtLeast(startTick + 30)
                    val vel = (note.velocity * 127f).roundToInt().coerceIn(1, 127)
                    val pitch = note.pitch.coerceIn(0, 127)
                    events.add(MidiEvent(startTick, 0x90 or channel, pitch, vel))
                    events.add(MidiEvent(endTick, 0x80 or channel, pitch, 0))
                }
            }
            if (events.isNotEmpty()) {
                trackChunks.add(encodeTrackEvents(track.name, events))
            }
        }

        val out = ByteArrayOutputStream()
        val dos = DataOutputStream(out)
        // MThd
        dos.writeBytes("MThd")
        dos.writeInt(6)
        dos.writeShort(1) // Format 1
        dos.writeShort(trackChunks.size)
        dos.writeShort(TICKS_PER_QUARTER)
        trackChunks.forEach { dos.write(it) }
        dos.flush()
        return out.toByteArray()
    }

    fun importMidiNotes(inputStream: InputStream, maxSteps: Int = 32): List<Note> {
        val data = inputStream.readBytes()
        if (data.size < 14) return emptyList()
        val dis = DataInputStream(data.inputStream())
        val header = ByteArray(4)
        dis.readFully(header)
        if (String(header) != "MThd") return emptyList()
        val headerLen = dis.readInt()
        dis.readShort() // format
        val numTracks = dis.readShort().toInt() and 0xFFFF
        val division = dis.readShort().toInt() and 0xFFFF
        if (headerLen > 6) dis.skipBytes(headerLen - 6)
        val ppqn = if (division > 0) division else 480
        val ticksPerStep = max(1f, ppqn / 4f)

        val imported = mutableListOf<Note>()
        for (t in 0 until numTracks) {
            if (dis.available() < 8) break
            val chunkId = ByteArray(4)
            dis.readFully(chunkId)
            val chunkLen = dis.readInt()
            val trackBytes = ByteArray(chunkLen.coerceAtLeast(0).coerceAtMost(dis.available()))
            dis.readFully(trackBytes)
            if (String(chunkId) != "MTrk") continue

            parseTrackBytes(trackBytes, ticksPerStep, maxSteps, imported)
        }
        return imported
    }

    private fun parseTrackBytes(
        bytes: ByteArray,
        ticksPerStep: Float,
        maxSteps: Int,
        outNotes: MutableList<Note>
    ) {
        var pos = 0
        var absTick = 0L
        var runningStatus = 0
        val activeNotes = mutableMapOf<Int, Pair<Long, Int>>() // pitch -> (startTick, velocity)

        fun readVarLen(): Int {
            var value = 0
            while (pos < bytes.size) {
                val b = bytes[pos++].toInt() and 0xFF
                value = (value shl 7) or (b and 0x7F)
                if ((b and 0x80) == 0) break
            }
            return value
        }

        while (pos < bytes.size) {
            val delta = readVarLen()
            absTick += delta
            if (pos >= bytes.size) break
            var status = bytes[pos].toInt() and 0xFF
            if (status < 0x80) {
                status = runningStatus
            } else {
                pos++
                if (status < 0xF0) runningStatus = status
            }

            when {
                status == 0xFF -> {
                    if (pos >= bytes.size) break
                    pos++ // meta type
                    val len = readVarLen()
                    pos += len
                }
                status == 0xF0 || status == 0xF7 -> {
                    val len = readVarLen()
                    pos += len
                }
                (status and 0xF0) == 0x90 -> {
                    if (pos + 1 >= bytes.size) break
                    val pitch = bytes[pos++].toInt() and 0x7F
                    val vel = bytes[pos++].toInt() and 0x7F
                    if (vel > 0) {
                        activeNotes[pitch] = absTick to vel
                    } else {
                        activeNotes.remove(pitch)?.let { (st, v) ->
                            addNoteFromTicks(st, absTick, pitch, v, ticksPerStep, maxSteps, outNotes)
                        }
                    }
                }
                (status and 0xF0) == 0x80 -> {
                    if (pos + 1 >= bytes.size) break
                    val pitch = bytes[pos++].toInt() and 0x7F
                    pos++ // release vel
                    activeNotes.remove(pitch)?.let { (st, v) ->
                        addNoteFromTicks(st, absTick, pitch, v, ticksPerStep, maxSteps, outNotes)
                    }
                }
                (status and 0xF0) in listOf(0xA0, 0xB0, 0xE0) -> {
                    pos += 2
                }
                (status and 0xF0) in listOf(0xC0, 0xD0) -> {
                    pos += 1
                }
                else -> break
            }
        }
    }

    private fun addNoteFromTicks(
        startTick: Long,
        endTick: Long,
        pitch: Int,
        vel: Int,
        ticksPerStep: Float,
        maxSteps: Int,
        outNotes: MutableList<Note>
    ) {
        val startStep = (startTick / ticksPerStep)
        if (startStep >= maxSteps) return
        val durSteps = ((endTick - startTick) / ticksPerStep).coerceAtLeast(0.5f)
        outNotes.add(
            Note(
                id = UUID.randomUUID().toString(),
                pitch = pitch.coerceIn(24, 96),
                startStep = (startStep * 4f).roundToInt() / 4f,
                durationSteps = (durSteps * 4f).roundToInt().coerceAtLeast(1) / 4f,
                velocity = (vel / 127f).coerceIn(0.15f, 1f)
            )
        )
    }

    private fun buildTempoTrack(bpm: Int, timeSigTop: Int): ByteArray {
        val body = ByteArrayOutputStream()
        // Tempo meta event: FF 51 03 <us per quarter>
        val usPerQuarter = (60_000_000 / bpm.coerceIn(40, 220))
        writeVarLen(body, 0)
        body.write(0xFF); body.write(0x51); body.write(0x03)
        body.write((usPerQuarter shr 16) and 0xFF)
        body.write((usPerQuarter shr 8) and 0xFF)
        body.write(usPerQuarter and 0xFF)

        // Time Signature: FF 58 04 nn dd cc bb
        writeVarLen(body, 0)
        body.write(0xFF); body.write(0x58); body.write(0x04)
        body.write(timeSigTop.coerceIn(2, 7))
        body.write(2) // denominator 2^2 = 4
        body.write(24)
        body.write(8)

        // End of track
        writeVarLen(body, 0)
        body.write(0xFF); body.write(0x2F); body.write(0x00)

        return wrapMTrk(body.toByteArray())
    }

    private fun encodeTrackEvents(name: String, events: List<MidiEvent>): ByteArray {
        val sorted = events.sortedBy { it.tick }
        val body = ByteArrayOutputStream()

        // Track name meta
        val nameBytes = name.toByteArray(Charsets.UTF_8)
        writeVarLen(body, 0)
        body.write(0xFF); body.write(0x03)
        writeVarLen(body, nameBytes.size)
        body.write(nameBytes)

        var lastTick = 0
        sorted.forEach { ev ->
            val delta = (ev.tick - lastTick).coerceAtLeast(0)
            lastTick = ev.tick
            writeVarLen(body, delta)
            body.write(ev.status)
            body.write(ev.data1)
            body.write(ev.data2)
        }

        writeVarLen(body, 0)
        body.write(0xFF); body.write(0x2F); body.write(0x00)

        return wrapMTrk(body.toByteArray())
    }

    private fun wrapMTrk(payload: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        val dos = DataOutputStream(out)
        dos.writeBytes("MTrk")
        dos.writeInt(payload.size)
        dos.write(payload)
        dos.flush()
        return out.toByteArray()
    }

    private fun writeVarLen(out: ByteArrayOutputStream, value: Int) {
        var v = value
        var buffer = v and 0x7F
        while ((v shr 7) > 0) {
            v = v shr 7
            buffer = buffer shl 8
            buffer = buffer or ((v and 0x7F) or 0x80)
        }
        while (true) {
            out.write(buffer and 0xFF)
            if ((buffer and 0x80) != 0) buffer = buffer shr 8 else break
        }
    }

    private data class MidiEvent(val tick: Int, val status: Int, val data1: Int, val data2: Int)
}
