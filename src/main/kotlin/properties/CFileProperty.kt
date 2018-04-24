package properties

import javafx.beans.property.Property
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.Button
import javafx.stage.FileChooser

/**
 * ファイルを選択、パスを管理するプロパティ
 */
class CFileProperty(val filters: List<FileChooser.ExtensionFilter>) : CitrusProperty<String> {
    private val property = SimpleStringProperty()
    override val valueProperty: Property<String>
        get() = property

    private val button = Button("ファイルを選択")
    override val uiNode: Button
        get() = button

    init{
        button.setOnAction {
            val chooser = FileChooser()
            chooser.title = "ファイルを選択"
            chooser.extensionFilters.addAll(filters)
            value = chooser.showOpenDialog(button.scene.window).path
        }
    }
}