package properties

import javafx.scene.paint.Color

/**
 * RGB補間でアニメーション可能なプロパティ
 */
class CAnimatableColorProperty : CColorProperty(), CitrusAnimatableProperty<Color> {
    private var _frame = 0
    override var frame: Int
        get() = _frame
        set(value) {
            _frame = value
            if (keyFrames.size > 0) {
                val index = getKeyFrameIndex(frame)
                this.value = when (index) {
                    -1 -> keyFrames[0].value
                    keyFrames.size - 1 -> keyFrames.last().value
                    else -> {
                        val input = (frame.toDouble() - keyFrames[index].frame) / (keyFrames[index + 1].frame - keyFrames[index].frame)
                        val p = keyFrames[index].interpolator.getInterpolation(input)
                        val r = keyFrames[index].value.red + ((keyFrames[index + 1].value.red - keyFrames[index].value.red) * p)
                        val g = keyFrames[index].value.green + ((keyFrames[index + 1].value.green - keyFrames[index].value.green) * p)
                        val b = keyFrames[index].value.blue + ((keyFrames[index + 1].value.blue - keyFrames[index].value.blue) * p)

                        Color.color(r, g, b)
                    }
                }
            }
        }
    private val _keyFrames: MutableList<KeyFrame<Color>> = ArrayList()
    override val keyFrames: MutableList<KeyFrame<Color>>
        get() = _keyFrames

    //TODO UI実装
}