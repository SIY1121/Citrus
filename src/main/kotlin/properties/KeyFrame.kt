package properties

import interpolation.Interpolator

/**
 * キーフレームを保持するデータクラス
 */
data class KeyFrame<T>(var frame: Int, var value: T, var interpolator: Interpolator)