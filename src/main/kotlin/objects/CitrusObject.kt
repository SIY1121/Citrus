package objects

import annotation.CProperty
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonTypeInfo
import effect.Effect
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
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
abstract class CitrusObject(defLayer: Int, defScene: Int) {

    //Jackson用
    constructor() : this(-1, -1)

    open val id = "citrus"
    open val name = "CitrusObject"

    var displayName = "CitrusObject"
        set(value) {
            field = value
            displayNameChangeListener?.onDisplayNameChanged(value)
        }

    val effects: MutableList<Effect> = ArrayList()

    val linkedObjects: MutableList<CitrusObject> = ArrayList()

    @JsonIgnore
    var uiObject: TimeLineObject? = null

    interface DisplayNameChangeListener {
        fun onDisplayNameChanged(name: String)
    }

    interface PropertyChangedListener {
        fun onPropertyChanged()
    }

    @JsonIgnore
    var displayNameChangeListener: DisplayNameChangeListener? = null
    @JsonIgnore
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
            if (field != value && value != -1) {

                if (field != -1 && scene != -1)
                    Main.project.scene[scene][field].remove(this)

                if (value != -1 && scene != -1)
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
            if (field != value && value != -1) {

                if (field != -1 && layer != -1)
                    Main.project.scene[field][layer].remove(this)

                if (value != -1 && layer != -1)
                    Main.project.scene[value][layer].add(this)
                //Statics.project.Layer[value].sortBy { it.start }
                //TODO ソートは保留

                //field = value
            }

            field = value
        }

    @JsonIgnore
    val memberProperties: MutableList<KProperty1<CitrusObject, *>> = ArrayList()

    /**
     * アニメーション可能なプロパティを抜き出す
     */
    @JsonIgnore
    val animatableProperties: MutableList<CitrusAnimatableProperty<*>> = ArrayList()
    @JsonIgnore
    val allProperties: MutableList<CitrusProperty<*>> = ArrayList()


    fun updateAnimationProperty(frame: Int) {
        animatableProperties.forEach {
            it.frame = frame
        }
    }

    init {
        if (scene != -1 && layer != -1)
            Main.project.scene[scene][layer].add(this)
    }

    fun setupProperties() {
        this.javaClass.kotlin.memberProperties.filter {
            //println(it.name + " " + it.returnType)
            it.annotations.any { it is CProperty } && Class.forName(it.returnType.toString()).interfaces.any { it.name == "properties.CitrusProperty" || it.name == "properties.CitrusAnimatableProperty" }
        }.forEach { memberProperties.add(it) }

        memberProperties.forEach {
            allProperties.add(it.get(this) as CitrusProperty<*>)
        }

        allProperties.forEach {
            if (it is CitrusAnimatableProperty<*>)
                animatableProperties.add(it)
            it.valueProperty.addListener { _, _, _ -> propertyChangedListener?.onPropertyChanged() }
        }
    }

    open fun clone(startPos: Int, endPos: Int): CitrusObject {
        val newObj = this::class.java.getDeclaredConstructor(Int::class.java, Int::class.java).newInstance(layer, scene) as CitrusObject
        newObj.layer = layer
        newObj.scene = scene
        newObj.start = startPos
        newObj.end = endPos

        memberProperties.forEach {
            println(it)
            val p = it.get(newObj) as CitrusProperty<Any>
            if (p is CitrusAnimatableProperty<*>) {
                val keyFrames = (it.get(this) as CitrusAnimatableProperty<Any>).keyFrames
                if (keyFrames.size > 0)
                    keyFrames.forEach {
                        (p as CitrusAnimatableProperty<Any>).keyFrames.add(it)
                    }
                else
                    (p as CitrusAnimatableProperty<Any>).valueProperty.value = (it.get(this) as CitrusAnimatableProperty<Any>).valueProperty.value

            } else {
                p.valueProperty.value = (it.get(this) as CitrusProperty<Any>).value
            }
        }
        return newObj
    }

}