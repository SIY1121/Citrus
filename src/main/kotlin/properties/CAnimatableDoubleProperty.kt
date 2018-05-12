package properties

import interpolation.AccelerateDecelerateInterpolator
import javafx.application.Platform
import javafx.scene.Cursor
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.Rectangle
import ui.CustomSlider
import ui.TimeLineObject
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

        editPane.minHeight = 20.0

        uiNode.keyPressedOnHoverListener = object : CustomSlider.KeyPressedOnHover {
            override fun onKeyPressed(it: KeyEvent) {
                if (isKeyFrame(frame)) {
                    keyFrames[getKeyFrameIndex(frame)].value = uiNode.value
                } else {
                    val k = KeyFrame<Number>(frame, uiNode.value, AccelerateDecelerateInterpolator())
                    keyFrames.add(k)
                    println("add $frame $value")
                    keyFrames.sortBy { it.frame }
                    val rect = Rectangle()
                    rect.cursor = Cursor.HAND
                    rect.fill = Color.YELLOW
                    rect.width = 10.0
                    rect.height = 10.0
                    rect.rotate = 45.0
                    rect.translateX = -5.0
                    rect.layoutY = 5.0
                    rect.layoutX = TimelineController.pixelPerFrame * k.frame
                    rect.setOnMouseDragged {
                        val x = it.sceneX - editPane.localToScene(editPane.layoutBounds).minX
                        rect.layoutX = (x / TimelineController.pixelPerFrame).toInt() * TimelineController.pixelPerFrame
                        it.consume()
                    }
                    rect.setOnMouseReleased {
                        k.frame = (rect.layoutX / TimelineController.pixelPerFrame).toInt()
                        keyFrames.sortBy { it.frame }
                    }
                    rect.setOnMouseClicked {
                        TimelineController.instance.currentFrame = k.frame + (editPane.parent as TimeLineObject).cObject.start
                        it.consume()
                    }

                    Platform.runLater {
                        uiNode.displayMode = CustomSlider.DisplayMode.KeyFrame
                        editPane.children.add(rect)
                    }
                }
            }
        }
    }

}