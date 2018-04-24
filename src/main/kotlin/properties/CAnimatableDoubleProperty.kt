package properties

import interpolation.BounceInterpolator
import javafx.scene.input.KeyEvent
import ui.CustomSlider

/**
 * 数値をアニメーションできるプロパティ
 */
class CAnimatableDoubleProperty(min: Double = Double.NEGATIVE_INFINITY, max: Double = Double.POSITIVE_INFINITY, def: Double = 0.0,tick:Double = 0.1) : CDoubleProperty(min,max,def,tick), CitrusAnimatableProperty<Number> {

    private val _keyFrames: MutableList<KeyFrame<Number>> = ArrayList()
    override val keyFrames: MutableList<KeyFrame<Number>>
        get() = _keyFrames

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
                    else -> keyFrames[index].value.toDouble() + ((keyFrames[index + 1].value.toDouble() - keyFrames[index].value.toDouble()) * keyFrames[index].interpolator.getInterpolation((frame.toDouble() - keyFrames[index].frame) / (keyFrames[index + 1].frame - keyFrames[index].frame)))
                }
            }
        }

    init {
        uiNode.keyPressedOnHoverListener = object : CustomSlider.KeyPressedOnHover {
            override fun onKeyPressed(it: KeyEvent) {
                if (isKeyFrame(frame)) {
                    keyFrames[getKeyFrameIndex(frame)].value = uiNode.value
                } else {
                    keyFrames.add(KeyFrame(frame, uiNode.value, BounceInterpolator()))
                    println("add $frame $value")
                    keyFrames.sortBy { it.frame }
                }
            }
        }
    }

}