package properties

import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.ColorPicker
import javafx.scene.paint.Color

/**
 * 色を保持するプロパティ
 */
open class CColorProperty(def : Color = Color.WHITE) : CitrusProperty<Color> {
    private val property = SimpleObjectProperty<Color>()
    override val valueProperty: Property<Color>
        get() = property

    private val picker = ColorPicker()
    override val uiNode: ColorPicker
        get() = picker

    init{
        property.value = def
        picker.setOnAction {
            value = picker.value
        }
    }

}