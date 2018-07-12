package effect

import annotation.CEffect
import annotation.CProperty
import javafx.geometry.HPos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TitledPane
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import objects.CitrusObject
import properties.CitrusProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

open class Effect(val parent : CitrusObject) {

    var editPane = VBox()

    lateinit var metadata: Metadata

    fun setup() {
        metadata = Effect.Metadata(this)
        setupEditPane()
    }

    fun setupEditPane() {
        val partsGrid = GridPane()
        val titledPane = TitledPane(metadata.name, partsGrid)

        partsGrid.columnConstraints.addAll(ColumnConstraints(), ColumnConstraints(150.0, 150.0, Double.POSITIVE_INFINITY, Priority.ALWAYS, HPos.LEFT, true))

        metadata.propertyData.forEachIndexed { index, propertyData ->
            val nameLabel = Label(propertyData.name)
            val uiNode = propertyData.property.uiNode

            partsGrid.add(nameLabel, 0, index)
            partsGrid.add(uiNode, 1, index)
        }
        val delButton = Button("x")
        delButton.setOnAction {
            parent.effects.remove(this)
        }
        partsGrid.add(delButton, 0, metadata.propertyData.size)

        editPane.children.add(titledPane)
    }

    class Metadata(val e: Effect) {
        val name: String
            get() = (e::class.annotations.first { it is CEffect } as CEffect).name

        val propertyData: MutableList<PropertyData> = ArrayList()

        init {
            e::class.memberProperties.filter { it.annotations.any { it is CProperty } }.forEach {
                propertyData.add(PropertyData(it as KProperty1<Effect, *>, e))
            }
            //ソート
            propertyData.sortBy { (it.kProperty.annotations.first { it is CProperty } as CProperty).index }
        }

        class PropertyData(val kProperty: KProperty1<Effect, *>, e: Effect) {
            val property = kProperty.get(e) as CitrusProperty<Any>
            val name: String
                get() = (kProperty.annotations.first { it is CProperty } as CProperty).displayName
        }
    }
}