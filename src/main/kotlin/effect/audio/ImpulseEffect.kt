package effect.audio

import objects.CitrusObject

class ImpulseEffect(parent : CitrusObject) : AudioEffect(parent) {

    override fun executeFilter(file: String, start: Int, end: Int): Boolean {

        return true
    }
}