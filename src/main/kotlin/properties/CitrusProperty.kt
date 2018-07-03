package properties

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonIgnoreType
import com.fasterxml.jackson.annotation.JsonTypeInfo
import javafx.beans.property.Property
import javafx.scene.Node

/**
 *定数プロパティのインターフェース
 * 値と、それを制御するUIを提供する
 */
interface CitrusProperty<T> {
    /**
     * リスナー設定、バインド可能なプロパティ
     */
    val valueProperty: Property<T>

    /**
     * プロパティの生の値
     */
    var value: T
        set(value) {
            //両方向バインドの無限ループ防止
            if (valueProperty.value != value)
                valueProperty.value = value
        }
        get() = valueProperty.value

    /**
     * 値を制御するUI
     */
    val uiNode: Node
}