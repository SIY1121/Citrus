package interpolation

import annotation.CInterpolation

@CInterpolation("ベジェ")
class BezierInterpolator : Interpolator {

    private val bez = Bezier(100)

    init{
        bez.setVertexes(0.0,0.0,0.5,-0.5,0.5,1.5,1.0,1.0)
    }
    
    override fun getInterpolation(input: Double): Double {
        return bez.getY(input)
    }

    internal inner class Bezier
    /**
     * コンストラクタ
     * sample_ は精度を入れる。100くらいで十分かな
     */
    (
            /**
             * sampleは精度
             * vertexesには4つの制御点が入る
             * ~Pointsには制度に従っていくつかの点の座標が入る
             */
            private val sample: Int) {
        private val vertexes: DoubleArray
        private val xPoints: DoubleArray
        private val yPoints: DoubleArray

        init {
            vertexes = DoubleArray(8)
            xPoints = DoubleArray(sample)
            yPoints = DoubleArray(sample)
        }

        /**
         * start~ には始点の座標を入れる
         * op1は第一制御点、始点についてるやつ
         * op2も同様
         * end~は終点の座標
         */
        fun setVertexes(
                startX: Double, startY: Double,
                op1X: Double, op1Y: Double,
                op2X: Double, op2Y: Double,
                endX: Double, endY: Double
        ) {
            vertexes[0] = startX
            vertexes[1] = startY
            vertexes[2] = op1X
            vertexes[3] = op1Y
            vertexes[4] = op2X
            vertexes[5] = op2Y
            vertexes[6] = endX
            vertexes[7] = endY

            setPoints()
        }

        private fun setPoints() {
            var t = 0.0
            for (i in 0 until sample) {
                xPoints[i] = calcBezierX(t)
                yPoints[i] = calcBezierY(t)
                t += (1.0 / sample)
            }
        }

        private fun calcBezierX(t: Double): Double {
            return ((1.0 - t) * (1.0 - t) * (1.0 - t) * vertexes[0] + 3.0 * t * (1.0 - t) * (1.0 - t) * vertexes[2]
                    + 3.0 * t * t * (1.0 - t) * vertexes[4] + t * t * t * vertexes[6])
        }

        private fun calcBezierY(t: Double): Double {
            return ((1.0 - t) * (1.0 - t) * (1.0 - t) * vertexes[1] + 3.0 * t * (1.0 - t) * (1.0 - t) * vertexes[3]
                    + 3.0 * t * t * (1.0 - t) * vertexes[5] + t * t * t * vertexes[7])
        }

        /**
         * x座標を入れればそれに近いxPointsの値からtを算出し、y座標を返す
         */
        fun getY(x: Double): Double {
            var t = sample - 1
            var d = Math.abs(x - xPoints[0])

            for (i in 1 until sample) {
                if (d > Math.abs(x - xPoints[i])) {
                    d = Math.abs(x - xPoints[i])
                } else {
                    t = i - 1
                    break
                }
            }

            return yPoints[t]
        }

    }
}