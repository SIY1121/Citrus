package objects

import annotation.CProperty
import effects.Effect
import properties.CitrusAnimatableProperty
import properties.CitrusProperty
import ui.Main
import ui.TimeLineObject
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * タイムラインに並ぶオブジェクトのスーパークラス
 * 格納先配列へのバインディング実装済み
 */
abstract class CitrusObject(defLayer: Int, defScene: Int) {

    open val id = "citrus"
    open val name = "CitrusObject"
    var displayName = "CitrusObject"
        set(value) {
            field = value
            displayNameChangeListener?.onDisplayNameChanged(value)
        }

    val effects: MutableList<Effect> = ArrayList()

    val linkedObjects: MutableList<CitrusObject> = ArrayList()

    var uiObject: TimeLineObject? = null

    interface DisplayNameChangeListener {
        fun onDisplayNameChanged(name: String)
    }

    interface PropertyChangedListener {
        fun onPropertyChanged()
    }

    var displayNameChangeListener: DisplayNameChangeListener? = null

    var propertyChangedListener: PropertyChangedListener? = null

    /**
     * タイムラインで動かされ終わった時に呼び出される
     */
    open fun onLayoutUpdate(mode: TimeLineObject.EditMode) {

    }

    /**
     * タイムラインのスケールが変更された時に呼び出される
     */
    open fun onScaleUpdate() {

    }

    open fun onFileDropped(file: String) {

    }

    fun isActive(frame: Int) = (frame in start..(end - 1))

    var start: Int = 0
        set(value) {
            field = value
            //Statics.project.Layer[layer].sortBy { it.start }
            //Todo ソートは保留
        }
    var end: Int = 1
    var layer: Int = defLayer
        set(value) {
            //変更された場合
            if (field != value) {

                Main.project.scene[scene][field].remove(this)

                Main.project.scene[scene][value].add(this)
                //Statics.project.Layer[value].sortBy { it.start }
                //TODO ソートは保留

                //field = value
            }

            field = value

        }

    var scene: Int = defScene
        private set(value) {
            //変更された場合
            if (field != value) {
                Main.project.scene[field][layer].remove(this)

                Main.project.scene[value][layer].add(this)
                //Statics.project.Layer[value].sortBy { it.start }
                //TODO ソートは保留

                //field = value
            }

            field = value
        }

    /**
     * アニメーション可能なプロパティを抜き出す
     */
    val animatableProperties: MutableList<CitrusAnimatableProperty<*>> = ArrayList()

    val allProperties: MutableList<CitrusProperty<*>> = ArrayList()


    fun updateAnimationProperty(frame: Int) {
        animatableProperties.forEach {
            it.frame = frame
        }
    }

    init {
        Main.project.scene[scene][layer].add(this)
    }

    fun setupProperties() {
        this.javaClass.kotlin.memberProperties.filter {
            println(it.name + " " + it.returnType)
            it.annotations.any { it is CProperty } && Class.forName(it.returnType.toString()).interfaces.any { it.name == "properties.CitrusProperty" || it.name == "properties.CitrusAnimatableProperty" }
        }.forEach {
            allProperties.add(it.get(this) as CitrusProperty<*>)
        }
        allProperties.forEach {
            if (it is CitrusAnimatableProperty<*>)
                animatableProperties.add(it)
            //TODO AnimatableDoublePropertyでユーザーによるイベントの発火かアニメーションによる発火かを見分ける機構が必要
            it.valueProperty.addListener { _, _, _ -> propertyChangedListener?.onPropertyChanged() }
        }
    }

}