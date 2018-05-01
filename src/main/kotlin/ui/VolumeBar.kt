package ui

import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.layout.Pane

class VolumeBar : Pane() {
    private val bar = Pane()

    val valueProperty = SimpleDoubleProperty()
    var value: Double
        get() = valueProperty.value
        set(value) {
            valueProperty.set(value)
        }

    init {
        bar.style = "-fx-background-color:yellow;"

        bar.minWidth = 28.0
        widthProperty().addListener({ _, _, n -> bar.prefWidth = n.toDouble() })
        valueProperty.addListener({ _, _, n ->
            bar.minHeight = n.toDouble() / 100.0 * height
            bar.layoutY = height - n.toDouble() / 100.0 * height
        })
        valueProperty.set(value)

        children.add(bar)

    }
}