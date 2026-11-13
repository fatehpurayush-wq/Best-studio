package com.beatstudio.app.domain

import kotlinx.serialization.Serializable

@Serializable
enum class TrackType(val labelHinglish: String) {
    DRUMS("Drums"),
    INSTRUMENT("Instrument"),
    SAMPLER("Sampler"),
    VOCAL("Voice / Mic")
}

@Serializable
enum class DrumSoundType(val labelHinglish: String, val defaultMidi: Int) {
    KICK_PUNCHY("Punchy Kick", 36),
    KICK_SUB("Sub Kick", 35),
    KICK_808("808 Kick", 36),
    BASS_808("808 Glide Bass", 36),
    SNARE("Snare", 38),
    CLAP("Clap", 39),
    RIM("Rimshot", 37),
    HIHAT_CLOSED("Closed Hat", 42),
    HIHAT_OPEN("Open Hat", 46),
    CRASH("Crash", 49),
    TOM("Tom", 45),
    SHAKER("Shaker", 70),
    TAMBOURINE("Tambourine", 54),
    PHONK_COWBELL("Phonk Cowbell", 56),
    MEMPHIS_HIT("Memphis Hit", 58),
    VINYL_CRACKLE("Vinyl Crackle", 60),
    FX_RISER("FX Riser", 62),
    FX_IMPACT("FX Impact", 64),
    FX_REVERSE_CYMBAL("Reverse Cymbal", 65),
    FX_NOISE_SWEEP("Noise Sweep", 67)
}

@Serializable
enum class SynthPresetId(val labelHinglish: String, val category: String) {
    SUB_BASS("Sub Bass", "Bass"),
    REESE_BASS("Reese Bass", "Bass"),
    BASS_808("808 Glide", "Bass"),
    SAW_LEAD("Saw Lead", "Lead"),
    SQUARE_LEAD("Square Lead", "Lead"),
    PLUCK("Pluck", "Pluck"),
    BELL("Crystal Bell", "Bell"),
    EP_RHODES("EP / Rhodes", "Keys"),
    WARM_PAD("Warm Pad", "Pad"),
    LOFI_PIANO("Lo-fi Piano", "Keys"),
    STRING_PAD("String Pad", "Pad"),
    ORGAN("Retro Organ", "Keys"),
    FLUTE_SYNTH("Flute Synth", "Lead"),
    ARP_SYNTH("Arp Synth", "Lead"),
    DARK_CHOIR_PAD("Dark Choir Pad", "Phonk")
}

@Serializable
enum class OscWaveform(val labelHinglish: String) {
    SINE("Sine"),
    SAW("Saw"),
    SQUARE("Square"),
    TRIANGLE("Triangle"),
    NOISE("Noise")
}

@Serializable
enum class LfoTarget(val labelHinglish: String) {
    PITCH("Pitch"),
    CUTOFF("Filter"),
    AMP("Volume")
}

@Serializable
data class SynthParams(
    val presetId: SynthPresetId = SynthPresetId.LOFI_PIANO,
    // Simple 4 sliders
    val tone: Float = 0.55f,
    val attack: Float = 0.08f, // 0..1
    val release: Float = 0.35f, // 0..1
    val brightness: Float = 0.6f, // 0..1
    // Pro mode controls
    val decay: Float = 0.4f,
    val sustain: Float = 0.65f,
    val waveform: OscWaveform = OscWaveform.SAW,
    val unisonVoices: Int = 1, // 1..5
    val detune: Float = 0.2f, // 0..1
    val filterType: Int = 0, // 0=LP, 1=HP, 2=BP
    val cutoff: Float = 0.72f, // 0..1
    val resonance: Float = 0.2f, // 0..1
    val lfoRate: Float = 0.25f, // 0..1
    val lfoDepth: Float = 0.0f, // 0..1
    val lfoTarget: LfoTarget = LfoTarget.CUTOFF,
    val glideTime: Float = 0.15f // 0..1 for 808/bass glide
)

@Serializable
data class SamplerParams(
    val sampleName: String = "Koi sample nahi",
    val sampleFilePath: String? = null,
    val trimStart: Float = 0f,
    val trimEnd: Float = 1f,
    val pitchSemitones: Float = 0f,
    val reverse: Boolean = false,
    val loop: Boolean = false,
    val rootMidiNote: Int = 60
)
