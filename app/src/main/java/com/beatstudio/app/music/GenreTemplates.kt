package com.beatstudio.app.music

import com.beatstudio.app.domain.*

object GenreTemplates {

    fun defaultDrumRowsForGenre(genre: GenreId): List<DrumRowConfig> {
        return when (genre) {
            GenreId.PHONK, GenreId.DRIFT_PHONK -> listOf(
                DrumRowConfig(0, DrumSoundType.KICK_PUNCHY, "Phonk Kick", 0f, 1.0f),
                DrumRowConfig(1, DrumSoundType.BASS_808, "808 Glide", 0f, 1.0f),
                DrumRowConfig(2, DrumSoundType.SNARE, "Memphis Snare", 0f, 0.9f),
                DrumRowConfig(3, DrumSoundType.CLAP, "Crisp Clap", 0f, 0.8f),
                DrumRowConfig(4, DrumSoundType.HIHAT_CLOSED, "Closed Hat", 0f, 0.75f),
                DrumRowConfig(5, DrumSoundType.HIHAT_OPEN, "Open Hat", 0f, 0.7f),
                DrumRowConfig(6, DrumSoundType.PHONK_COWBELL, "Phonk Cowbell", 0f, 0.95f),
                DrumRowConfig(7, DrumSoundType.MEMPHIS_HIT, "Memphis Hit", 0f, 0.8f)
            )
            GenreId.LOFI, GenreId.HINDI_LOFI -> listOf(
                DrumRowConfig(0, DrumSoundType.KICK_SUB, "Soft Kick", -1f, 0.95f),
                DrumRowConfig(1, DrumSoundType.SNARE, "Dusty Snare", -2f, 0.82f),
                DrumRowConfig(2, DrumSoundType.RIM, "Wood Rim", 0f, 0.75f),
                DrumRowConfig(3, DrumSoundType.HIHAT_CLOSED, "Soft Hat", -2f, 0.62f),
                DrumRowConfig(4, DrumSoundType.SHAKER, "Shaker", 0f, 0.55f),
                DrumRowConfig(5, DrumSoundType.VINYL_CRACKLE, "Vinyl Crackle", 0f, 0.5f)
            )
            GenreId.TRAP -> listOf(
                DrumRowConfig(0, DrumSoundType.KICK_808, "Trap Kick", 0f, 1.0f),
                DrumRowConfig(1, DrumSoundType.BASS_808, "808 Sub Glide", 0f, 1.0f),
                DrumRowConfig(2, DrumSoundType.CLAP, "Trap Clap", 0f, 0.9f),
                DrumRowConfig(3, DrumSoundType.SNARE, "Snap Snare", 1f, 0.85f),
                DrumRowConfig(4, DrumSoundType.HIHAT_CLOSED, "Roll Hat", 0f, 0.8f),
                DrumRowConfig(5, DrumSoundType.HIHAT_OPEN, "Open Hat", 0f, 0.75f),
                DrumRowConfig(6, DrumSoundType.RIM, "Perc Rim", 2f, 0.7f)
            )
            GenreId.BOOM_BAP -> listOf(
                DrumRowConfig(0, DrumSoundType.KICK_PUNCHY, "Boom Kick", -1f, 1.0f),
                DrumRowConfig(1, DrumSoundType.SNARE, "Crack Snare", 0f, 0.92f),
                DrumRowConfig(2, DrumSoundType.HIHAT_CLOSED, "Swing Hat", -1f, 0.75f),
                DrumRowConfig(3, DrumSoundType.HIHAT_OPEN, "Open Hat", 0f, 0.7f),
                DrumRowConfig(4, DrumSoundType.CRASH, "Crash", 0f, 0.65f),
                DrumRowConfig(5, DrumSoundType.VINYL_CRACKLE, "Vinyl Dust", 0f, 0.45f)
            )
            GenreId.HOUSE, GenreId.EDM -> listOf(
                DrumRowConfig(0, DrumSoundType.KICK_PUNCHY, "4x4 Kick", 0f, 1.0f),
                DrumRowConfig(1, DrumSoundType.CLAP, "House Clap", 0f, 0.9f),
                DrumRowConfig(2, DrumSoundType.HIHAT_CLOSED, "Closed Hat", 0f, 0.78f),
                DrumRowConfig(3, DrumSoundType.HIHAT_OPEN, "Offbeat Hat", 0f, 0.82f),
                DrumRowConfig(4, DrumSoundType.SHAKER, "Groove Shaker", 0f, 0.7f),
                DrumRowConfig(5, DrumSoundType.FX_RISER, "Drop Riser", 0f, 0.75f),
                DrumRowConfig(6, DrumSoundType.FX_IMPACT, "Drop Impact", 0f, 0.85f)
            )
            GenreId.AFRO_BEATS, GenreId.REGGAETON -> listOf(
                DrumRowConfig(0, DrumSoundType.KICK_PUNCHY, "Dembow Kick", 0f, 1.0f),
                DrumRowConfig(1, DrumSoundType.SNARE, "Snappy Snare", 1f, 0.9f),
                DrumRowConfig(2, DrumSoundType.RIM, "Afro Rim", 0f, 0.88f),
                DrumRowConfig(3, DrumSoundType.SHAKER, "Shaker", 0f, 0.75f),
                DrumRowConfig(4, DrumSoundType.TAMBOURINE, "Tambourine", 0f, 0.7f),
                DrumRowConfig(5, DrumSoundType.TOM, "Afro Tom", 2f, 0.8f)
            )
            else -> listOf(
                DrumRowConfig(0, DrumSoundType.KICK_PUNCHY, "Kick", 0f, 0.95f),
                DrumRowConfig(1, DrumSoundType.SNARE, "Snare", 0f, 0.88f),
                DrumRowConfig(2, DrumSoundType.CLAP, "Clap", 0f, 0.82f),
                DrumRowConfig(3, DrumSoundType.HIHAT_CLOSED, "Closed Hat", 0f, 0.75f),
                DrumRowConfig(4, DrumSoundType.HIHAT_OPEN, "Open Hat", 0f, 0.7f),
                DrumRowConfig(5, DrumSoundType.TOM, "Tom", 0f, 0.75f),
                DrumRowConfig(6, DrumSoundType.FX_NOISE_SWEEP, "Noise Sweep", 0f, 0.65f)
            )
        }
    }

    fun createNewProject(
        id: String,
        name: String,
        genre: GenreId,
        autoGenerate: Boolean = true
    ): Project {
        val (root, scale, swing, melodyPreset, bassPreset, padPreset) = when (genre) {
            GenreId.PHONK -> GenrePresetSpec(5, MusicalScale.MINOR, 0.08f, SynthPresetId.BELL, SynthPresetId.BASS_808, SynthPresetId.DARK_CHOIR_PAD)
            GenreId.DRIFT_PHONK -> GenrePresetSpec(4, MusicalScale.PHRYGIAN, 0.05f, SynthPresetId.SAW_LEAD, SynthPresetId.BASS_808, SynthPresetId.DARK_CHOIR_PAD)
            GenreId.LOFI -> GenrePresetSpec(0, MusicalScale.DORIAN, 0.22f, SynthPresetId.EP_RHODES, SynthPresetId.SUB_BASS, SynthPresetId.WARM_PAD)
            GenreId.HINDI_LOFI -> GenrePresetSpec(2, MusicalScale.BHAIRAV, 0.18f, SynthPresetId.LOFI_PIANO, SynthPresetId.SUB_BASS, SynthPresetId.FLUTE_SYNTH)
            GenreId.TRAP -> GenrePresetSpec(7, MusicalScale.HARMONIC_MINOR, 0.04f, SynthPresetId.PLUCK, SynthPresetId.BASS_808, SynthPresetId.BELL)
            GenreId.BOOM_BAP -> GenrePresetSpec(9, MusicalScale.MINOR, 0.28f, SynthPresetId.LOFI_PIANO, SynthPresetId.SUB_BASS, SynthPresetId.ORGAN)
            GenreId.HOUSE -> GenrePresetSpec(9, MusicalScale.MINOR, 0.06f, SynthPresetId.EP_RHODES, SynthPresetId.REESE_BASS, SynthPresetId.STRING_PAD)
            GenreId.SYNTHWAVE -> GenrePresetSpec(9, MusicalScale.MINOR, 0.0f, SynthPresetId.ARP_SYNTH, SynthPresetId.REESE_BASS, SynthPresetId.WARM_PAD)
            GenreId.REGGAETON -> GenrePresetSpec(11, MusicalScale.HARMONIC_MINOR, 0.05f, SynthPresetId.PLUCK, SynthPresetId.SUB_BASS, SynthPresetId.SQUARE_LEAD)
            GenreId.AMBIENT -> GenrePresetSpec(0, MusicalScale.LYDIAN, 0.0f, SynthPresetId.BELL, SynthPresetId.SUB_BASS, SynthPresetId.WARM_PAD)
            GenreId.EDM -> GenrePresetSpec(5, MusicalScale.MINOR, 0.0f, SynthPresetId.SAW_LEAD, SynthPresetId.REESE_BASS, SynthPresetId.STRING_PAD)
            GenreId.AFRO_BEATS -> GenrePresetSpec(7, MusicalScale.MAJOR, 0.16f, SynthPresetId.PLUCK, SynthPresetId.SUB_BASS, SynthPresetId.EP_RHODES)
        }

        val isLofi = genre == GenreId.LOFI || genre == GenreId.HINDI_LOFI || genre == GenreId.BOOM_BAP
        val isPhonk = genre == GenreId.PHONK || genre == GenreId.DRIFT_PHONK

        val drumTrack = Track(
            id = "track_drums",
            name = "Drums",
            type = TrackType.DRUMS,
            colorHex = 0xFFFF5252,
            volume = 0.95f,
            drumRows = defaultDrumRowsForGenre(genre),
            effects = EffectSettings(
                distortionEnabled = isPhonk,
                distortionDrive = if (isPhonk) 0.42f else 0.15f,
                distortionMix = if (isPhonk) 0.45f else 0.2f,
                compressorEnabled = true,
                compThreshold = 0.55f,
                compRatio = 0.45f
            )
        )

        val bassTrack = Track(
            id = "track_bass",
            name = "Bass / 808",
            type = TrackType.INSTRUMENT,
            colorHex = 0xFFFF9100,
            volume = 0.88f,
            synthParams = synthDefaultsForPreset(bassPreset),
            effects = EffectSettings(
                distortionEnabled = isPhonk || genre == GenreId.TRAP,
                distortionDrive = if (isPhonk) 0.55f else 0.22f,
                distortionMix = if (isPhonk) 0.45f else 0.25f,
                sidechainEnabled = genre == GenreId.HOUSE || genre == GenreId.EDM || genre == GenreId.SYNTHWAVE,
                sidechainAmount = 0.65f
            )
        )

        val melodyTrack = Track(
            id = "track_melody",
            name = "Melody",
            type = TrackType.INSTRUMENT,
            colorHex = 0xFF00E5FF,
            volume = 0.80f,
            synthParams = synthDefaultsForPreset(melodyPreset),
            effects = EffectSettings(
                reverbEnabled = true,
                reverbType = if (genre == GenreId.AMBIENT) ReverbType.HUGE else ReverbType.ROOM,
                reverbMix = if (isLofi || genre == GenreId.AMBIENT) 0.35f else 0.22f,
                tapeEnabled = isLofi,
                tapeWobble = if (isLofi) 0.38f else 0.15f,
                tapeWarmth = if (isLofi) 0.65f else 0.4f,
                vinylEnabled = isLofi,
                vinylLevel = if (isLofi) 0.24f else 0.1f,
                sidechainEnabled = genre == GenreId.HOUSE || genre == GenreId.EDM
            )
        )

        val padTrack = Track(
            id = "track_pad",
            name = if (isPhonk) "Dark Choir" else "Chords / Pad",
            type = TrackType.INSTRUMENT,
            colorHex = 0xFFE040FB,
            volume = 0.68f,
            synthParams = synthDefaultsForPreset(padPreset),
            effects = EffectSettings(
                reverbEnabled = true,
                reverbType = ReverbType.HALL,
                reverbMix = 0.38f,
                chorusEnabled = genre == GenreId.SYNTHWAVE || isLofi,
                chorusMix = 0.3f,
                sidechainEnabled = genre == GenreId.HOUSE || genre == GenreId.EDM
            )
        )

        val samplerTrack = Track(
            id = "track_sampler",
            name = "Sampler",
            type = TrackType.SAMPLER,
            colorHex = 0xFF00E676,
            volume = 0.85f
        )

        val vocalTrack = Track(
            id = "track_vocal",
            name = "Voice Mic",
            type = TrackType.VOCAL,
            colorHex = 0xFFFFEA00,
            volume = 0.90f,
            effects = EffectSettings(
                reverbEnabled = true,
                reverbType = ReverbType.PLATE,
                reverbMix = 0.22f,
                compressorEnabled = true,
                pitchSnapEnabled = false,
                pitchSnapStrength = 0.85f
            )
        )

        val baseProject = Project(
            id = id,
            name = name,
            genre = genre,
            bpm = genre.defaultBpm,
            swing = swing,
            rootNote = root,
            scale = scale,
            phonkDriveEnabled = isPhonk,
            masterEffects = EffectSettings(
                distortionEnabled = isPhonk,
                distortionDrive = if (isPhonk) 0.32f else 0.0f,
                distortionMix = if (isPhonk) 0.3f else 0.0f,
                tapeEnabled = isLofi,
                tapeWobble = if (isLofi) 0.2f else 0f,
                vinylEnabled = isLofi,
                vinylLevel = if (isLofi) 0.16f else 0f
            ),
            tracks = listOf(drumTrack, bassTrack, melodyTrack, padTrack, samplerTrack, vocalTrack)
        )

        return if (autoGenerate) {
            val generated = AutoBeatGenerator.generateFullBeat(baseProject, variation = 0.35f, seed = id.hashCode())
            ArrangeGenerator.generateSongArrangement(generated)
        } else {
            baseProject
        }
    }

    fun synthDefaultsForPreset(preset: SynthPresetId): SynthParams {
        return when (preset) {
            SynthPresetId.SUB_BASS -> SynthParams(
                presetId = preset, tone = 0.35f, attack = 0.03f, release = 0.25f, brightness = 0.25f,
                decay = 0.3f, sustain = 0.85f, waveform = OscWaveform.SINE, unisonVoices = 1, cutoff = 0.32f, resonance = 0.15f
            )
            SynthPresetId.REESE_BASS -> SynthParams(
                presetId = preset, tone = 0.65f, attack = 0.05f, release = 0.3f, brightness = 0.55f,
                decay = 0.4f, sustain = 0.8f, waveform = OscWaveform.SAW, unisonVoices = 3, detune = 0.45f, cutoff = 0.48f, resonance = 0.35f
            )
            SynthPresetId.BASS_808 -> SynthParams(
                presetId = preset, tone = 0.5f, attack = 0.01f, release = 0.55f, brightness = 0.4f,
                decay = 0.65f, sustain = 0.5f, waveform = OscWaveform.TRIANGLE, unisonVoices = 1, cutoff = 0.42f, resonance = 0.2f, glideTime = 0.35f
            )
            SynthPresetId.SAW_LEAD -> SynthParams(
                presetId = preset, tone = 0.7f, attack = 0.04f, release = 0.28f, brightness = 0.8f,
                decay = 0.35f, sustain = 0.7f, waveform = OscWaveform.SAW, unisonVoices = 3, detune = 0.32f, cutoff = 0.85f, resonance = 0.25f
            )
            SynthPresetId.SQUARE_LEAD -> SynthParams(
                presetId = preset, tone = 0.6f, attack = 0.02f, release = 0.22f, brightness = 0.72f,
                decay = 0.3f, sustain = 0.65f, waveform = OscWaveform.SQUARE, unisonVoices = 2, detune = 0.2f, cutoff = 0.75f, resonance = 0.3f
            )
            SynthPresetId.PLUCK -> SynthParams(
                presetId = preset, tone = 0.65f, attack = 0.01f, release = 0.28f, brightness = 0.7f,
                decay = 0.24f, sustain = 0.1f, waveform = OscWaveform.TRIANGLE, unisonVoices = 2, detune = 0.18f, cutoff = 0.72f, resonance = 0.25f
            )
            SynthPresetId.BELL -> SynthParams(
                presetId = preset, tone = 0.78f, attack = 0.01f, release = 0.68f, brightness = 0.82f,
                decay = 0.55f, sustain = 0.12f, waveform = OscWaveform.SINE, unisonVoices = 2, detune = 0.12f, cutoff = 0.9f, resonance = 0.15f
            )
            SynthPresetId.EP_RHODES -> SynthParams(
                presetId = preset, tone = 0.55f, attack = 0.02f, release = 0.45f, brightness = 0.52f,
                decay = 0.48f, sustain = 0.38f, waveform = OscWaveform.SINE, unisonVoices = 2, detune = 0.08f, cutoff = 0.6f, resonance = 0.12f,
                lfoRate = 0.3f, lfoDepth = 0.12f, lfoTarget = LfoTarget.AMP
            )
            SynthPresetId.WARM_PAD -> SynthParams(
                presetId = preset, tone = 0.48f, attack = 0.38f, release = 0.72f, brightness = 0.45f,
                decay = 0.6f, sustain = 0.85f, waveform = OscWaveform.SAW, unisonVoices = 4, detune = 0.38f, cutoff = 0.48f, resonance = 0.15f
            )
            SynthPresetId.LOFI_PIANO -> SynthParams(
                presetId = preset, tone = 0.45f, attack = 0.02f, release = 0.48f, brightness = 0.42f,
                decay = 0.5f, sustain = 0.28f, waveform = OscWaveform.TRIANGLE, unisonVoices = 2, detune = 0.14f, cutoff = 0.46f, resonance = 0.1f
            )
            SynthPresetId.STRING_PAD -> SynthParams(
                presetId = preset, tone = 0.6f, attack = 0.42f, release = 0.75f, brightness = 0.62f,
                decay = 0.55f, sustain = 0.88f, waveform = OscWaveform.SAW, unisonVoices = 5, detune = 0.42f, cutoff = 0.66f, resonance = 0.18f
            )
            SynthPresetId.ORGAN -> SynthParams(
                presetId = preset, tone = 0.62f, attack = 0.02f, release = 0.18f, brightness = 0.65f,
                decay = 0.2f, sustain = 0.9f, waveform = OscWaveform.SQUARE, unisonVoices = 2, detune = 0.06f, cutoff = 0.68f, resonance = 0.1f
            )
            SynthPresetId.FLUTE_SYNTH -> SynthParams(
                presetId = preset, tone = 0.52f, attack = 0.16f, release = 0.32f, brightness = 0.55f,
                decay = 0.3f, sustain = 0.78f, waveform = OscWaveform.TRIANGLE, unisonVoices = 1, detune = 0.05f, cutoff = 0.62f, resonance = 0.15f,
                lfoRate = 0.45f, lfoDepth = 0.18f, lfoTarget = LfoTarget.PITCH
            )
            SynthPresetId.ARP_SYNTH -> SynthParams(
                presetId = preset, tone = 0.72f, attack = 0.01f, release = 0.22f, brightness = 0.78f,
                decay = 0.25f, sustain = 0.3f, waveform = OscWaveform.SQUARE, unisonVoices = 2, detune = 0.22f, cutoff = 0.78f, resonance = 0.38f
            )
            SynthPresetId.DARK_CHOIR_PAD -> SynthParams(
                presetId = preset, tone = 0.42f, attack = 0.35f, release = 0.78f, brightness = 0.38f,
                decay = 0.6f, sustain = 0.85f, waveform = OscWaveform.SAW, unisonVoices = 5, detune = 0.5f, cutoff = 0.40f, resonance = 0.32f,
                lfoRate = 0.2f, lfoDepth = 0.15f, lfoTarget = LfoTarget.CUTOFF
            )
        }
    }

    fun builtInDemoProjects(): List<Project> {
        val phonkDemo = createNewProject(
            id = "demo_phonk_01",
            name = "Midnight Phonk (Demo)",
            genre = GenreId.PHONK,
            autoGenerate = true
        ).copy(isDemo = true)

        val lofiDemo = createNewProject(
            id = "demo_lofi_01",
            name = "Barish Lo-fi (Demo)",
            genre = GenreId.HINDI_LOFI,
            autoGenerate = true
        ).copy(isDemo = true)

        return listOf(phonkDemo, lofiDemo)
    }

    private data class GenrePresetSpec(
        val rootNote: Int,
        val scale: MusicalScale,
        val swing: Float,
        val melodyPreset: SynthPresetId,
        val bassPreset: SynthPresetId,
        val padPreset: SynthPresetId
    )
}
