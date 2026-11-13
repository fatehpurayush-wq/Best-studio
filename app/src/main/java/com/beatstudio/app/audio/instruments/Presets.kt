package com.beatstudio.app.audio.instruments

import com.beatstudio.app.domain.OscWaveform
import com.beatstudio.app.domain.SynthParams
import com.beatstudio.app.domain.SynthPresetId
import com.beatstudio.app.music.GenreTemplates

object Presets {
    val allPresets: List<SynthPresetId> = SynthPresetId.entries

    fun descriptionHinglish(presetId: SynthPresetId): String = when (presetId) {
        SynthPresetId.SUB_BASS -> "Deep clean low-end sub bass"
        SynthPresetId.REESE_BASS -> "Thick detuned dark bass"
        SynthPresetId.BASS_808 -> "Long tail glide 808 bass"
        SynthPresetId.SAW_LEAD -> "Bright energetic synth lead"
        SynthPresetId.SQUARE_LEAD -> "Retro arcade square lead"
        SynthPresetId.PLUCK -> "Short snappy melody pluck"
        SynthPresetId.BELL -> "Shimmering FM crystal bell"
        SynthPresetId.EP_RHODES -> "Warm electric Rhodes keys"
        SynthPresetId.WARM_PAD -> "Soft dreamy atmosphere pad"
        SynthPresetId.LOFI_PIANO -> "Dusty warm lo-fi piano"
        SynthPresetId.STRING_PAD -> "Wide lush string ensemble"
        SynthPresetId.ORGAN -> "Classic soulful organ keys"
        SynthPresetId.FLUTE_SYNTH -> "Airy mellow flute lead"
        SynthPresetId.ARP_SYNTH -> "Punchy rhythmic synth arp"
        SynthPresetId.DARK_CHOIR_PAD -> "Haunting phonk choir pad"
    }

    fun applySimpleControls(
        current: SynthParams,
        tone: Float,
        attack: Float,
        release: Float,
        brightness: Float
    ): SynthParams {
        val mappedCutoff = (0.15f + brightness * 0.8f).coerceIn(0.08f, 0.98f)
        val mappedDetune = (tone * 0.6f).coerceIn(0f, 1f)
        return current.copy(
            tone = tone,
            attack = attack,
            release = release,
            brightness = brightness,
            cutoff = mappedCutoff,
            detune = mappedDetune
        )
    }

    fun forPreset(presetId: SynthPresetId): SynthParams = GenreTemplates.synthDefaultsForPreset(presetId)
}
