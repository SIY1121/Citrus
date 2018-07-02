package objects

/**
 * オーディオサンプルを取得できるオブジェクト
 */
interface AudioSampleProvider {
    fun getSamples(frame : Int):FloatArray
}