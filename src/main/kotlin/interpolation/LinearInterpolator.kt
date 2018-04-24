package interpolation

import annotation.CInterpolation

/**
 * 直線補間を行う
 */
@CInterpolation("直線補間")
class LinearInterpolator : Interpolator{
    override fun getInterpolation(input: Double):Double  {
        return input
    }
}