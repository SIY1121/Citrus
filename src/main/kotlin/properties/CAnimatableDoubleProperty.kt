package properties

import interpolation.AccelerateDecelerateInterpolator
import interpolation.BounceInterpolator
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import ui.CustomSlider
import ui.TimelineController

/**
 * 数値をアニメーションできるプロパティ
 */
class CAnimatableDoubleProperty(min: Double = Double.NEGATIVE_INFINITY, max: Double = Double.POSITIVE_INFINITY, def: Double = 0.0, tick: Double = 0.1) : CDoubleProperty(min, max, def, tick), CitrusAnimatableProperty<Number> {

    private val _editPane = Pane()
    override val editPane: Pane
        get() = _editPane

    override fun onTimelineScaleChanged() {
        editPane.children.forEachIndexed { index, node ->
            node.layoutX = keyFrames[index].frame * TimelineController.pixelPerFrame
        }
    }

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

                when {
                    isKeyFrame(frame) -> Platform.runLater { uiNode.displayMode = CustomSlider.DisplayMode.KeyFrame }
                    keyFrames.size > 0 -> Platform.runLater { uiNode.displayMode = CustomSlider.DisplayMode.NotKeyFrame }
                    else -> Platform.runLater { uiNode.displayMode = CustomSlider.DisplayMode.None }
                }

            }
        }

    init {
        editPane.background = Background(BackgroundFill(Color.BLUE, CornerRadii(0.0), Insets(0.0)))
        editPane.minHeight = 15.0

        uiNode.keyPressedOnHoverListener = object : CustomSlider.KeyPressedOnHover {
            override fun onKeyPressed(it: KeyEvent) {
                if (isKeyFrame(frame)) {
                    keyFrames[getKeyFrameIndex(frame)].value = uiNode.value
                } else {
                    val k = KeyFrame<Number>(frame, uiNode.value, AccelerateDecelerateInterpolator())
                    keyFrames.add(k)
                    println("add $frame $value")
                    keyFrames.sortBy { it.frame }
                    val circle = Circle()
                    circle.fill = Color.YELLOW
                    circle.radius = 5.0
                    circle.layoutX = TimelineController.pixelPerFrame * k.frame
                    circle.layoutXProperty().addListener { _, _, n ->
                        k.frame = (n.toDouble() / TimelineController.pixelPerFrame).toInt()
                        keyFrames.sortBy { it.frame }
                    }
                    circle.setOnMouseDragged {
                        circle.layoutX = it.sceneX
                    }
                    Platform.runLater {
                        uiNode.displayMode = CustomSlider.DisplayMode.KeyFrame
                        editPane.children.add(circle)
                    }
                }
            }
        }
    }

}