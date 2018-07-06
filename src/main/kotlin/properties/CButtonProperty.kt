package properties

import javafx.beans.property.Property
import javafx.beans.property.SimpleIntegerProperty
import javafx.scene.Node
import javafx.scene.control.Button

/**
 * プロパティ画面にアクションを設定できるボタンを表示する
 */
class CButtonProperty(text : String) : CitrusProperty<Number> {
    val button = Button(text)

    val _unused = SimpleIntegerProperty()
    override val valueProperty: Property<Number>
        get() = _unused
    override val uiNode: Node
        get() = button

    var onAction : (()->Unit)? = null
        set(value){
            field = value
            button.setOnAction { value?.invoke() }
        }

}