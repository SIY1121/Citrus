package ui

import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.value.ChangeListener
import javafx.event.EventHandler
import javafx.fxml.Initializable
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.VPos
import javafx.scene.Cursor
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import java.awt.Robot
import java.net.URL
import java.util.*


class CustomSlider : Pane() {

    val valueProperty = SimpleDoubleProperty()
    var min = Double.NEGATIVE_INFINITY
    var max = Double.POSITIVE_INFINITY
    var tick = 1.0
    var name = ""
        set(value){
            field = value
            nameLabel.text = field
        }
    var value : Double
        get() = valueProperty.value
        set(value){
            valueProperty.set(value)
        }

    interface KeyPressedOnHover{
        fun onKeyPressed(it : KeyEvent)
    }
    var keyPressedOnHoverListener  : KeyPressedOnHover? = null

    private val textField = TextField()
    private val grid = GridPane()
    private val nameLabel = Label("x")
    private val valueLabel = Label("0.0")
    private val percentIndicator = Pane()
    private val robot = Robot()
    private var dragged = false

    private var px = 0.0
    private var py = 0.0
    private var oldValue = 0.0
    private var oldX = 0.0

    private val keyPressed = EventHandler<KeyEvent> {
        if (it.code == KeyCode.ENTER) {
            grid.isVisible = true
            percentIndicator.isVisible = true
            textField.text = ""
            textField.isDisable = true
            valueLabel.text = valueProperty.value.toString()
        }
    }
    private val focusChanged = ChangeListener<Boolean> { _, _, n ->
        if (!n) {
            grid.isVisible = true
            percentIndicator.isVisible = true
            textField.text = ""
            textField.isDisable = true
            valueLabel.text = valueProperty.value.toString()
        }
    }
    private val textChanged = ChangeListener<String> { _, _, n ->
        val v = n.toDoubleOrNull()
        if (v != null)
            valueProperty.set(v)
    }
    private val mouseClicked = EventHandler<MouseEvent> {
        if (dragged) {
            dragged = false
            return@EventHandler
        }
        grid.isVisible = false
        percentIndicator.isVisible = false
        textField.isDisable = false
        textField.text =  String.format("%.2f",value)
        textField.requestFocus()
    }
    private val mousePressed = EventHandler<MouseEvent> {
        px = it.screenX
        py = it.screenY
        oldValue = valueProperty.value
        oldX = it.screenX
    }

    private val mouseDragged = EventHandler<MouseEvent> {
        scene.cursor = Cursor.NONE
        requestFocus()
        when {
            valueProperty.value + (it.screenX - oldX)*tick > max -> valueProperty.set(max)
            valueProperty.value + (it.screenX - oldX)*tick < min -> valueProperty.set(min)
            else -> valueProperty.set(valueProperty.value + (it.screenX - oldX)*tick)
        }

        textField.style = "-fx-background-color:#cecece"
        valueLabel.textFill = Color.WHITE
        dragged = true
        oldX = it.screenX
        if (it.x < 0 || grid.width < it.x) {
            robot.mouseMove(px.toInt(), py.toInt())
            oldX = px
        }
        //
    }

    private val mouseReleased = EventHandler<MouseEvent> {
        if (dragged) {
            robot.mouseMove(px.toInt(), py.toInt())
            scene.cursor = Cursor.DEFAULT
            textField.style = ""
            valueLabel.textFill = Color.LIGHTGRAY
        }
    }
    private val mouseEntered = EventHandler<MouseEvent> {
        requestFocus()
        scene.setOnKeyPressed {
            keyPressedOnHoverListener?.onKeyPressed(it)
        }
    }

    private val mouseExited = EventHandler<MouseEvent> {
        scene.onKeyPressed = null
    }

    private val keyPressedPane = EventHandler<KeyEvent> {
        println(it.code)
    }

    init {
        uiSetup()
        listenerSetup()
        isFocusTraversable = true
    }

    private fun uiSetup() {
        textField.prefWidthProperty().bind(widthProperty())
        textField.prefHeightProperty().bind(heightProperty())
        textField.isDisable = true
        children.add(textField)

        percentIndicator.style = "-fx-background-color:#444444;-fx-background-radius:3;"

        children.add(percentIndicator)

        grid.prefWidthProperty().bind(widthProperty())
        grid.prefHeightProperty().bind(heightProperty())
        grid.columnConstraints.add(ColumnConstraints())
        grid.columnConstraints.add(ColumnConstraints())
        grid.columnConstraints[0].prefWidth = 25.0
        grid.columnConstraints[1].hgrow = Priority.ALWAYS
        valueLabel.textFill = Color.LIGHTGRAY
        nameLabel.text = name
        GridPane.setHalignment(valueLabel, HPos.RIGHT)
        GridPane.setValignment(valueLabel, VPos.CENTER)
        GridPane.setValignment(nameLabel, VPos.CENTER)
        grid.padding = Insets(5.0)
        grid.addColumn(0, nameLabel)
        grid.addColumn(1, valueLabel)
        children.add(grid)
    }

    private fun listenerSetup() {
        textField.textProperty().addListener(textChanged)
        textField.onKeyPressed = keyPressed
        textField.focusedProperty().addListener(focusChanged)
        grid.onMouseClicked = mouseClicked
        grid.onMouseDragged = mouseDragged
        grid.onMousePressed = mousePressed
        grid.onMouseReleased = mouseReleased
        grid.onMouseEntered = mouseEntered
        grid.onMouseExited = mouseExited
        grid.onKeyPressed = keyPressedPane

        valueProperty.addListener({ _, _, n ->
            valueLabel.text = String.format("%.2f",n.toDouble())
        })
        valueProperty.addListener({_,_,n->
            if(max!=Double.POSITIVE_INFINITY && min!=Double.NEGATIVE_INFINITY)
                percentIndicator.prefWidth = n.toDouble()/(max-min)*width -2.0
        })
        widthProperty().addListener { _,_,n->
            if(max!=Double.POSITIVE_INFINITY && min!=Double.NEGATIVE_INFINITY)
            percentIndicator.prefWidth = value/(max-min)*n.toDouble() - 2.0
        }
        percentIndicator.prefHeightProperty().bind(heightProperty().subtract(2.0))
        percentIndicator.layoutY = 1.0
        percentIndicator.layoutX = 1.0
    }
}