package interpolation

/**
 * キーフレームの補完をする抽象クラス
 */
interface Interpolator {
    fun getInterpolation(input : Double):Double
}