package effects.audio

import objects.AudioSampleProvider

open class AudioEffect : AudioSampleProvider {
    override fun getSamples(frame: Int): FloatArray {
        return FloatArray(0)
    }
}