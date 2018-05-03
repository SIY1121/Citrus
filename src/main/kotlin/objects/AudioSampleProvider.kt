package objects

interface AudioSampleProvider {
    fun getSamples(frame : Int):FloatArray
}