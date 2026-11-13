package com.beatstudio.app.music

import com.beatstudio.app.domain.MusicalScale
import kotlin.math.abs
import kotlin.math.pow

object Scales {
    val noteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    fun rootName(rootNote: Int): String = noteNames[((rootNote % 12) + 12) % 12]

    fun midiToName(midi: Int): String {
        val note = ((midi % 12) + 12) % 12
        val octave = (midi / 12) - 1
        return "${noteNames[note]}$octave"
    }

    fun midiToFreq(midi: Float): Float {
        return 440f * 2f.pow((midi - 69f) / 12f)
    }

    fun isNoteInScale(midi: Int, rootNote: Int, scale: MusicalScale): Boolean {
        if (scale == MusicalScale.CHROMATIC) return true
        val relative = ((midi - rootNote) % 12 + 12) % 12
        return relative in scale.intervals
    }

    fun snapToScale(midi: Int, rootNote: Int, scale: MusicalScale): Int {
        if (scale == MusicalScale.CHROMATIC || isNoteInScale(midi, rootNote, scale)) return midi
        for (offset in 1..6) {
            if (isNoteInScale(midi + offset, rootNote, scale)) return (midi + offset).coerceIn(24, 108)
            if (isNoteInScale(midi - offset, rootNote, scale)) return (midi - offset).coerceIn(24, 108)
        }
        return midi
    }

    fun snapFreqToScale(freqHz: Float, rootNote: Int, scale: MusicalScale): Float {
        if (freqHz <= 20f) return freqHz
        val midiFloat = 69.0 + 12.0 * kotlin.math.log2(freqHz / 440.0)
        val baseMidi = midiFloat.toInt()
        var bestMidi = baseMidi
        var bestDist = Double.MAX_VALUE
        for (candidate in (baseMidi - 6)..(baseMidi + 6)) {
            if (isNoteInScale(candidate, rootNote, scale)) {
                val d = abs(candidate - midiFloat)
                if (d < bestDist) {
                    bestDist = d
                    bestMidi = candidate
                }
            }
        }
        return midiToFreq(bestMidi.toFloat())
    }

    fun getScaleMidiNotes(rootNote: Int, scale: MusicalScale, lowMidi: Int = 36, highMidi: Int = 84): List<Int> {
        return (lowMidi..highMidi).filter { isNoteInScale(it, rootNote, scale) }
    }

    fun scaleDegreeMidi(rootNote: Int, scale: MusicalScale, degreeIndex: Int, baseOctaveMidi: Int = 48): Int {
        val intervals = scale.intervals
        val count = intervals.size
        val octaves = Math.floorDiv(degreeIndex, count)
        val idx = ((degreeIndex % count) + count) % count
        return (baseOctaveMidi + rootNote + intervals[idx] + octaves * 12).coerceIn(24, 108)
    }
}
