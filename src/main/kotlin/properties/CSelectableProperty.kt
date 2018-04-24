package properties

import javafx.beans.property.Property
import javafx.beans.property.SimpleIntegerProperty
import javafx.scene.control.ChoiceBox

/**
 * 選択型プロパティ
 */
class CSelectableProperty(val list: List<String>) : CitrusProperty<Number> {
    private val property = SimpleIntegerProperty()
    override val valueProperty: Property<Number>
        get() = property

    private val choice = ChoiceBox<String>()
    override val uiNode: ChoiceBox<String>
        get() = choice
    init{
        choice.items.addAll(list)
        choice.setOnAction {
            property.value = choice.selectionModel.selectedIndex
        }
    }
}
