package com.beatstudio.app.util

import java.util.Locale

object TimeFormat {
    fun formatMillis(millis: Long): String {
        val totalSec = (millis / 1000L).coerceAtLeast(0L)
        val min = totalSec / 60L
        val sec = totalSec % 60L
        return String.format(Locale.US, "%02d:%02d", min, sec)
    }

    fun formatBarBeat(stepIndex: Int, stepsPerBeat: Int = 4, beatsPerBar: Int = 4): String {
        val stepsPerBar = stepsPerBeat * beatsPerBar
        val bar = (stepIndex / stepsPerBar) + 1
        val beat = ((stepIndex % stepsPerBar) / stepsPerBeat) + 1
        val sub = (stepIndex % stepsPerBeat) + 1
        return "$bar.$beat.$sub"
    }

    fun formatBpm(bpm: Int): String = "$bpm BPM"

    fun formatRelativeTimeHinglish(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val minutes = diff / 60_000L
        val hours = minutes / 60L
        val days = hours / 24L
        return when {
            minutes < 2L -> "Abhi"
            minutes < 60L -> "$minutes min pehle"
            hours < 24L -> "$hours hr pehle"
            days == 1L -> "Kal"
            else -> "$days din pehle"
        }
    }
}
