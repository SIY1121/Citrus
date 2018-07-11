package objects

import annotation.CObject
import annotation.CProperty
import effect.Effect
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.scene.control.Label
import javafx.scene.control.TitledPane
import javafx.scene.layout.*
import properties.CitrusAnimatableProperty
import properties.CitrusProperty
import ui.Main
import ui.TimeLineObject
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.declaredMemberProperties

/**
 * タイムラインに並ぶオブジェクトのスーパークラス
 * 格納先配列へのバインディング実装済み
 */
abstract class CitrusObject(defLayer: Int, defScene: Int) {

    lateinit var metadata: Metadata

    open val id = "citrus"
    open val name = "CitrusObject"
    var displayName = "CitrusObject"
        set(value) {
            field = value
            displayNameChangeListener?.onDisplayNameChanged(value)
        }

    val effects: ObservableList<Effect> = FXCollections.observableArrayList()

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
     * 調整UI表示のルート
     */
    val editRootPane = VBox()
    /**
     * キーフレーム表示ルート
     */
    val keyframeRootPane = VBox()

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

                if (scene != -1 && field != -1)
                    Main.project.scene[scene][field].remove(this)

                if (scene != -1 && value != -1)
                    Main.project.scene[scene][value].add(this)
                //Statics.project.Layer[value].sortBy { it.start }
                //TODO ソートは保留

                //field = value
            }

            field = value

        }

    var scene: Int = defScene
        set(value) {
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

    /**
     * アニメーション可能なプロパティを抜き出す
     */
    val animatableProperties: MutableList<CitrusAnimatableProperty<Any>> = ArrayList()


    fun updateAnimationProperty(frame: Int) {
        animatableProperties.forEach {
            it.frame = frame
        }
    }

    init {
        if (scene != -1 && layer != -1)
            Main.project.scene[scene][layer].add(this)
    }

    /**
     * インスタンス化後に実行すべし
     */
    fun setup() {
        setupProperties()
        setupEditPane()
    }

    /**
     * 自らのプロパティをリスト化する
     */
    private fun setupProperties() {
        metadata = Metadata(this)
        //アニメーション可能プロパティは別枠で取っておく
        metadata.allProperties.filter { it.property is CitrusAnimatableProperty<*> }.forEach { animatableProperties.add(it.property as CitrusAnimatableProperty<Any>) }
        //値が変化したときに画面を再描画するリスナを設定
        metadata.allProperties.forEach { it.property.valueProperty.addListener { _, _, _ -> propertyChangedListener?.onPropertyChanged() } }
    }

    /**
     * 編集画面を生成する
     */
    private fun setupEditPane() {

        //セクションごとについて
        metadata.propertiesSections.forEach {

            val partsGrid = GridPane().apply {
                //調整UIが張り付くペイン

                columnConstraints.addAll(ColumnConstraints(),
                        ColumnConstraints(150.0, 150.0, Double.POSITIVE_INFINITY, Priority.ALWAYS, HPos.LEFT, true))
                hgap = 5.0
                vgap = 5.0

            }
            val titledPane = TitledPane(it.sectionName, partsGrid)//タイトル付きペイン


            val keyframeVBox = VBox()//キーフレームのUIを縦に並べるペイン
            keyframeVBox.padding = Insets(0.0)
            val keyframeTitledPane = TitledPane(it.sectionName, keyframeVBox)

            it.properties.forEachIndexed { index, propertyData ->
                val nameLabel = Label(propertyData.name)
                val uiNode = propertyData.property.uiNode
                partsGrid.add(nameLabel, 0, index)
                partsGrid.add(uiNode, 1, index)
                //アニメーション可能プロパティの場合
                if (propertyData.property is CitrusAnimatableProperty<*>) {
                    val keyframeWrapperPane = Pane()//ラベルと調整UIをラップ
                    val keyframeNameLabel = Label(propertyData.name)//名前表示用
                    val editPane = propertyData.property.editPane

                    keyframeWrapperPane.children.add(keyframeNameLabel)
                    keyframeWrapperPane.children.add(editPane)
                    keyframeWrapperPane.minHeight = 20.0

                    keyframeVBox.children.add(keyframeWrapperPane)
                }
            }

            //ルートペインにそれぞれ追加
            editRootPane.children.add(titledPane)
            keyframeRootPane.children.add(keyframeTitledPane)


        }

    }

    open fun clone(startPos: Int, endPos: Int): CitrusObject {
        val newObj = this::class.java.getDeclaredConstructor(Int::class.java, Int::class.java).newInstance(layer, scene) as CitrusObject
        newObj.layer = layer
        newObj.scene = scene
        newObj.start = startPos
        newObj.end = endPos

        metadata.allProperties.forEach {
            println(it)
            val p = it.kProperty.get(newObj) as CitrusProperty<Any>
            if (p is CitrusAnimatableProperty<*>) {
                val keyFrames = (it.property as CitrusAnimatableProperty<Any>).keyFrames
                if (keyFrames.size > 0)
                    keyFrames.forEach {
                        (p as CitrusAnimatableProperty<Any>).keyFrames.add(it)
                    }
                else
                    (p as CitrusAnimatableProperty<Any>).valueProperty.value = it.property.valueProperty.value

            } else {
                p.valueProperty.value = it.property.value
            }
        }
        return newObj
    }

    /**
     * CitrusObjectのメタデータを格納するクラス
     */
    class Metadata(cObject: CitrusObject) {
        /**
         * オブジェクトの名前
         */
        val name = (cObject::class.annotations.first { it is CObject } as CObject).name

        /**
         * クラスとそれに属するプロパティのセットのリスト
         * @see PropertySection
         */
        val propertiesSections: MutableList<PropertySection> = ArrayList()

        /**
         * 継承しているクラスのリスト
         */
        val superClasses: List<KClass<*>>
            get() = propertiesSections.map { it.kClass }

        /**
         * すべてのプロパティ
         */
        val allProperties: ArrayList<PropertyData>
            get() {
                val eachProperties = ArrayList<PropertyData>()
                propertiesSections.forEach { it.properties.forEach { eachProperties.add(it) } }
                return eachProperties
            }

        init {
            cObject::class.allSuperclasses.reversed().forEach {
                //クラスを放り込むと自動でやってくれる
                if (it.annotations.any { it is CObject })//アノテーションを持っていたら
                    propertiesSections.add(PropertySection(it, cObject))
            }
            //自身も追加
            propertiesSections.add(PropertySection(cObject::class, cObject))
        }

        /**
         * クラスとそれに属するプロパティのセット
         */
        class PropertySection(val kClass: KClass<*>, cObject: CitrusObject) {
            val sectionName: String = (kClass.annotations.first { it is CObject } as CObject).name
            val properties: MutableList<PropertyData> = ArrayList()

            init {
                kClass.declaredMemberProperties
                        .filter {
                            it.annotations.any { it is CProperty } &&
                                    Class.forName(it.returnType.toString()).interfaces.any { it.name == "properties.CitrusProperty" || it.name == "properties.CitrusAnimatableProperty" }
                        }
                        .forEach {
                            properties.add(PropertyData(it as KProperty1<CitrusObject, *>, cObject))
                        }
                //ソート
                properties.sortBy { (it.kProperty.annotations.first { it is CProperty } as CProperty).index }
            }
        }

        /**
         * KPropertyとプロパティの実態のセット
         */
        class PropertyData(val kProperty: KProperty1<CitrusObject, *>, cObject: CitrusObject) {
            val property: CitrusProperty<Any> = kProperty.get(cObject) as CitrusProperty<Any>
            val name: String
                get() = (kProperty.annotations.first { it is CProperty } as CProperty).displayName
        }
    }
}