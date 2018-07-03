package properties

import com.fasterxml.jackson.annotation.JsonTypeInfo
import javafx.beans.property.SimpleIntegerProperty

/**
 * 整数値を保持するプロパティ
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
class CIntegerProperty(min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE, def: Int = 0) : CNumberProperty(SimpleIntegerProperty(), min, max,def,1 ) {

    init {
        uiNode.valueProperty.addListener({ _, _, n -> value = n.toInt() })
        uiNode.value = def.toDouble()
    }
}