package objects

import annotation.CProperty
import effects.Effect
import properties.CitrusAnimatableProperty
import ui.TimeLineObject
import util.Statics
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * タイムラインに並ぶオブジェクトのスーパークラス
 * 格納先配列へのバインディング実装済み
 */
abstract class CitrusObject {

    open val id = "citrus"
    open val name = "CitrusObject"
    var displayName = "CitrusObject"
        set(value) {
            field = value
            displayNameChangeListener?.onDisplayNameChanged(value)
        }

    var frame: Int = 0
        private set

    val effects: MutableList<Effect> = ArrayList()

    val linkedObjects: MutableList<CitrusObject> = ArrayList()

    var uiObject: TimeLineObject? = null

    interface DisplayNameChangeListener {
        fun onDisplayNameChanged(name: String)
    }

    var displayNameChangeListener: DisplayNameChangeListener? = null

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

    fun onSuperFrame(frameInVideo: Int) {
        frame = frameInVideo - start
        pList.forEach {
            val v = it.get(this)
            if(v is CitrusAnimatableProperty<*>)v.frame = frame
        }
        onFrame()
    }

    protected abstract fun onFrame()

    fun isActive(frame: Int) = (frame in start..(end - 1))

    var start: Int = 0
        set(value) {
            field = value
            //Statics.project.Layer[layer].sortBy { it.start }
            //Todo ソートは保留
        }
    var end: Int = 1
    var layer: Int = -1
        set(value) {
            //変更された場合
            if (field != value) {

                //初回ではない場合は移動前のレイヤーに残っているはずなので消す
                if (field != -1)
                    Statics.project.Layer[field].remove(this)

                Statics.project.Layer[value].add(this)
                //Statics.project.Layer[value].sortBy { it.start }
                //TODO ソートは保留

                //field = value
            }

            field = value


        }

    val pList: List<KProperty1<CitrusObject, *>> = this.javaClass.kotlin.memberProperties.filter {
        println(it.name + " " + it.returnType)
        it.annotations.any { it is CProperty } && Class.forName(it.returnType.toString()).interfaces.any { it.name == "properties.CitrusAnimatableProperty" }
    }

    init {
        println(pList.size)
    }

}