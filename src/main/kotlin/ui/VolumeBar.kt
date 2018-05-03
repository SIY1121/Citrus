package ui

import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle

class VolumeBar : Pane() {
    private val bar = Rectangle()

    val valueProperty = SimpleDoubleProperty()
    var value: Double
        get() = valueProperty.value
        set(value) {
            valueProperty.set(value)
        }

    init {
        bar.fill = Color.YELLOW
        heightProperty().addListener({ _, _, n -> bar.height = n.toDouble() })
        widthProperty().addListener({ _, _, n -> bar.width = n.toDouble() })
        valueProperty.addListener({ _, _, n ->
            //bar.height = n.toDouble() / 100.0 * height
            bar.y = height - n.toDouble() / 100.0 * height
        })
        valueProperty.set(value)

        children.add(bar)

    }
}