package properties

import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.control.CheckBox

/**
 * On Off可能なプロパティ
 */
class CSwitchableProperty(def : Boolean = false) : CitrusProperty<Boolean> {
    private var property = SimpleBooleanProperty()
    override val valueProperty: Property<Boolean>
        get() = property
    private val checkBox = CheckBox()
    override val uiNode: CheckBox
        get() = checkBox

    init{
        checkBox.isSelected = def
        checkBox.setOnAction {
            value = checkBox.isSelected
        }
    }
}