package properties

import javafx.beans.property.SimpleDoubleProperty

/**
 * 小数を扱うプロパティ
 */
open class CDoubleProperty(min: Double = Double.NEGATIVE_INFINITY, max: Double = Double.POSITIVE_INFINITY, def: Double = 0.0, tick: Double = 0.1) : CNumberProperty(SimpleDoubleProperty(), min, max, def,tick) {
    init {
        uiNode.valueProperty.addListener({ _, _, n -> value = n.toDouble() })
        uiNode.value = def
    }
}