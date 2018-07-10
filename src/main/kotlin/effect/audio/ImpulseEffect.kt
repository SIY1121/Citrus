package effect.audio

class ImpulseEffect : AudioEffect() {

    override fun executeFilter(file: String, start: Int, end: Int): Boolean {

        return true
    }
}