package effect.audio

import annotation.CEffect

@CEffect("空のエフェクト")
class EmptyAudioEffect(file : String) : AudioEffect(file) {
    override fun executeFilter(start: Int, end: Int) {

    }
}