package properties

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeInfo
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.control.CheckBox

/**
 * On Off可能なプロパティ
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
class CSwitchableProperty(def : Boolean = false) : CitrusProperty<Boolean> {
    private var property = SimpleBooleanProperty()
    override val valueProperty: Property<Boolean>
        get() = property
    private val checkBox = CheckBox()
    override val uiNode: CheckBox
        @JsonIgnore
        get() = checkBox

    init{
        checkBox.isSelected = def

        checkBox.selectedProperty().bindBidirectional(property)
    }
}