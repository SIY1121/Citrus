package interpolation

import annotation.CInterpolation

@CInterpolation("バウンス")
class BounceInterpolator : Interpolator{
    override fun getInterpolation(input: Double): Double {
        return when {
            input < 0.3535 -> bounce(input)
            input < 0.7408 -> bounce(input - 0.54719f) + 0.7
            input < 0.9644 -> bounce(input - 0.8526f) + 0.9
            else -> bounce(input - 1.0435f) + 0.95
        }
    }

    private fun bounce(t:Double): Double {
        return t * t * 8.0
    }
}