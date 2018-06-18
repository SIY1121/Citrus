package effects.audio

import annotation.CEffect
import annotation.CProperty
import properties.CDoubleProperty

@CEffect("エコー")
class EchoEffect : AudioEffect() {

    @CProperty("ディレイ",0)
    val delay = CDoubleProperty(0.1)

    @CProperty("減衰ファクター",1)
    val factor = CDoubleProperty(0.01,1.0)


    init {
        println("hi")
    }

    override fun getSamples(frame: Int): FloatArray {
        return FloatArray(0)
    }
}