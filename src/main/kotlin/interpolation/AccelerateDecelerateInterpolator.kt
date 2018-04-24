package interpolation

import annotation.CInterpolation

@CInterpolation("加速減移動")
class AccelerateDecelerateInterpolator:Interpolator{
    override fun getInterpolation(input: Double): Double {
        return Math.cos((input + 1) * Math.PI) / 2.0 + 0.5
    }
}