package properties

import com.fasterxml.jackson.annotation.JsonIgnore
import javafx.application.Platform
import javafx.beans.property.Property
import ui.CustomSlider

/**
 * 数値を持つプロパティの親クラス
 * スライダーUIをもつ
 */
abstract class CNumberProperty(@JsonIgnore override val valueProperty: Property<Number>, _min: Number, _max: Number, var def: Number, _tick: Number) : CitrusProperty<Number> {

    var min: Number
        get() = uiNode.min
        set(value) {
            uiNode.min = value.toDouble()
        }

    var max: Number
        get() = uiNode.max
        set(value) {
            uiNode.max = value.toDouble()
        }

    var tick: Number
        get() = uiNode.tick
        set(value) {
            uiNode.tick = value.toDouble()
        }

    private val slider = CustomSlider()

    override val uiNode: CustomSlider
        @JsonIgnore
        get() = slider

    //TODO プロパティ側の max min をUIに反映させる
    init {
        min = _min
        max = _max
        tick = _tick
        //uiNode.value = def.toDouble()
        uiNode.valueProperty.bindBidirectional(valueProperty)
        value = def
        //valueProperty.addListener { _, _, n -> Platform.runLater{uiNode.value = n.toDouble()} }
    }
}