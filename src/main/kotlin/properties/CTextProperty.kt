package properties

import javafx.beans.property.Property
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.TextArea

/**
 * テキストを入力可能なプロパティ
 */
class CTextProperty(def: String = "") : CitrusProperty<String> {
    private val property = SimpleStringProperty()
    override val valueProperty: Property<String>
        get() = property
    private val textArea = TextArea()
    override val uiNode: TextArea
        get() = textArea

    init {
        textArea.text = def
        textArea.prefWidth = 20.0
        textArea.textProperty().addListener { _, _, n -> value = n.toString() }
    }
}