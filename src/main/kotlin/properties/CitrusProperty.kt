package properties

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
    val valueProperty : Property<T>

    /**
     * プロパティの生の値
     */
    var value : T
        set(value){valueProperty.value = value}
        get() = valueProperty.value

    /**
     * 値を制御するUI
     */
    val uiNode : Node
}