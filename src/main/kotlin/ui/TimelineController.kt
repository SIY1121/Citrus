package ui

import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.canvas.Canvas
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.input.TransferMode
import javafx.scene.layout.GridPane
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.paint.CycleMethod
import javafx.scene.paint.LinearGradient
import javafx.scene.paint.Stop
import javafx.scene.shape.Line
import javafx.scene.shape.Polygon
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font
import kotlinx.coroutines.experimental.launch
import objects.CitrusObject
import objects.ObjectManager
import project.Layer
import project.ProjectRenderer
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class TimelineController : Initializable {
    @FXML
    lateinit var labelVBox: VBox
    @FXML
    lateinit var layerScrollPane: ScrollPane
    @FXML
    lateinit var layerVBox: VBox
    @FXML
    lateinit var timelineRootPane: GridPane
    @FXML
    lateinit var labelScrollPane: ScrollPane
    @FXML
    lateinit var caret: Line
    @FXML
    lateinit var timelineAxis: Canvas
    @FXML
    lateinit var scaleSlider: Slider
    @FXML
    lateinit var hScrollBar: ScrollBar
    @FXML
    lateinit var sceneChoiceBox: ChoiceBox<String>
    @FXML
    lateinit var topCaret: Line
    @FXML
    lateinit var polygonCaret: Polygon
    @FXML
    lateinit var timelineAxisClipRectangle: Rectangle

    var projectRenderer: ProjectRenderer = ProjectRenderer(Main.project, null)

    var selectedScene = 0

    var currentFrame = 0
        set(value) {
            if (field != value) {
                field = if (value >= 0) value else 0
                projectRenderer.renderPreview(field)
                Platform.runLater {
                    drawVolumeBar()
                    caret.layoutX = field * pixelPerFrame
                    topCaret.layoutX = field * pixelPerFrame - offsetX
                    polygonCaret.layoutX = field * pixelPerFrame - offsetX

                    if (topCaret.layoutX >= timelineAxis.width)
                        if (playing) layerScrollPane.hvalue += layerScrollPane.width / (layerVBox.width - layerScrollPane.viewportBounds.width)
                        else layerScrollPane.hvalue += 0.05
                    else if (topCaret.layoutX < 0)
                        layerScrollPane.hvalue -= 0.05
                }

            }
        }

    var parentController: Controller = Controller()
        set(value) {
            field = value
            projectRenderer.glPanel = field.canvas
            drawVolumeBar()

//            parentController.rootPane.setOnKeyPressed {
//                when (it.code) {
//                    KeyCode.SPACE -> {
//                        if (!playing) play()
//                        else stop()
//                    }
//                    KeyCode.RIGHT -> {
//                        currentFrame++
//                        caret.layoutX = currentFrame * pixelPerFrame
//                    }
//                    KeyCode.LEFT -> {
//                        currentFrame--
//                        caret.layoutX = currentFrame * pixelPerFrame
//                    }
//                    KeyCode.DELETE -> {
//                        allTimelineObjects.filter { it.strictSelected }.forEach {
//                            it.onDelete()
//                            allTimelineObjects.remove(it)
//                        }
//                        glCanvas.currentObjects.clear()
//                        currentFrame = currentFrame
//                    }
//                    else -> {
//                        //Nothing to do
//                    }
//                }
//            }
        }

    var layerCount = 0
    val layerHeight = 30.0
    val defaultObjectLength = 5.0

    companion object {
        lateinit var instance: TimelineController
        var wait = false
            set(value) {
                field = value
                Platform.runLater {
                    instance.timelineRootPane.isDisable = field
                    if (!field)
                        instance.layerScrollPane.requestFocus()
                }
            }
        var pixelPerFrame = 2.0
    }

    var tick: Double = Main.project.fps.toDouble()

    val offsetX: Double
        get() = hScrollBar.value * (layerVBox.width - layerScrollPane.viewportBounds.width)

    var selectedObjects: MutableList<TimeLineObject> = ArrayList()
    var selectedObjectOldWidth: MutableList<Double> = ArrayList()
    val allTimelineObjects: MutableList<TimeLineObject> = ArrayList()
    var dragging = false
    var selectedOffsetX = 0.0
    var selectedOrigin = 0.0
    var editMode = TimeLineObject.EditMode.None

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        instance = this
        SplashController.notifyProgress(0.5, "UIを初期化中...")

        labelScrollPane.vvalueProperty().bindBidirectional(layerScrollPane.vvalueProperty())
        timelineRootPane.widthProperty().addListener({ _, _, n ->
            timelineAxis.width = n.toDouble() - 80
            drawAxis()
        })
        scaleSlider.valueProperty().addListener({ _, _, n ->
            pixelPerFrame = n.toDouble()
            tick = Main.project.fps * (1.0 / pixelPerFrame)
            //tick = 1.0 / pixelPerFrame
            for (pane in layerVBox.children)
                if (pane is Pane)
                    for (o in pane.children) {
                        (o as? TimeLineObject)?.onScaleChanged()
                    }
            caret.layoutX = currentFrame * pixelPerFrame
            topCaret.layoutX = currentFrame * pixelPerFrame - offsetX
            polygonCaret.layoutX = currentFrame * pixelPerFrame - offsetX
            drawAxis()
        })

        hScrollBar.minProperty().bind(layerScrollPane.hminProperty())
        hScrollBar.maxProperty().bind(layerScrollPane.hmaxProperty())
        layerScrollPane.hvalueProperty().bindBidirectional(hScrollBar.valueProperty())

        layerScrollPane.hvalueProperty().addListener({ _, _, n ->
            topCaret.layoutX = currentFrame * pixelPerFrame - offsetX
            polygonCaret.layoutX = currentFrame * pixelPerFrame - offsetX
            drawAxis()
        })
        timelineAxisClipRectangle.widthProperty().bind(timelineAxis.widthProperty())



        layerScrollPane.setOnKeyPressed {
            when (it.code) {
                KeyCode.SPACE -> {
                    if (!playing) play()
                    else stop()
                }
                KeyCode.RIGHT -> {
                    playing = true//再生中にしないとキーフレームが設定されているときに複数回描画されてしまう
                    currentFrame++
                    playing = false
                }
                KeyCode.LEFT -> {
                    playing = true//再生中にしないと(ry
                    currentFrame--
                    playing = false
                }
                KeyCode.DELETE -> {
                    allTimelineObjects.filter { it.strictSelected }.forEach {
                        it.onDelete()
                        allTimelineObjects.remove(it)
                    }
                    projectRenderer.updateObject()
                }
                else -> {
                    //Nothing to do
                }
            }
            it.consume()
        }

        sceneChoiceBox.items.addAll(arrayOf("Root", "Scene1", "Scene2", "Scene3"))

        for (i in 0..10)
            generateLayer()

        hScrollBar.requestLayout()


    }

    private fun generateLayer() {

        //レイヤーペイン生成
        val layerPane = Pane()
        layerPane.minHeight = layerHeight
        layerPane.maxHeight = layerHeight
        layerPane.minWidth = 2000.0
        layerPane.style = "-fx-background-color:" + if (layerCount % 2 == 0) "#343434;" else "#383838;"
        val thisLayer = layerCount

        layerPane.setOnDragOver {
            if (it.dragboard.hasFiles() && ObjectManager.detectObjectByExtension(it.dragboard.files[0].extension) != null)
                it.acceptTransferModes(TransferMode.COPY)
        }
        layerPane.setOnDragDropped {
            val board = it.dragboard
            if (board.hasFiles()) {
                val target = ObjectManager.detectObjectByExtension(board.files[0].extension)
                if (target != null)
                    addObject(target, thisLayer, board.files[0].absolutePath)


                it.isDropCompleted = true
            }
        }

        //サブメニュー
        val menu = ContextMenu()
        val menuObject = Menu("オブジェクトの追加")
        menu.items.add(menuObject)

        for (obj in ObjectManager.list) {
            val childMenu = MenuItem(obj.key)

            childMenu.setOnAction {
                addObject(obj.value, thisLayer)
            }
            menuObject.items.add(childMenu)
            layerPane.setOnMouseClicked {
                if (it.button == MouseButton.SECONDARY)
                    menu.show(layerPane, it.screenX, it.screenY)
            }
        }


        //label.minHeightProperty().bind(pane.heightProperty())
        //pane.heightProperty().addListener({_,_,n->println(n)})

        layerVBox.children.add(layerPane)
        //ラベル生成
        val labelPane = VBox()
        labelPane.minHeight = layerHeight
        labelPane.maxHeight = layerHeight
        labelPane.style = "-fx-background-color:" + if (layerCount % 2 == 0) "#343434" else "#383838"
        labelVBox.children.add(labelPane)

        val label = Label("Layer${layerCount + 1}")
        label.font = Font(15.0)
        label.prefWidth = 80.0
        label.minHeight = 20.0
        labelPane.children.add(label)

        val toggle = ToggleButton()
        toggle.maxHeight = 10.0
        toggle.minWidth = 30.0
        toggle.style = "-fx-font-size:2px"
        toggle.setOnAction {
            layerScrollPane.requestFocus()
            if (toggle.isSelected) {
                layerPane.maxHeight = Double.POSITIVE_INFINITY
                layerScrollPane.layout()//TODO LabelPaneのサイズ変更がおくれる原因の調査
            } else {
                layerPane.maxHeight = layerHeight
                layerScrollPane.layout()
            }
        }
        labelPane.children.add(toggle)

        labelPane.minHeightProperty().bind(layerPane.heightProperty())

        layerCount++

        Main.project.scene[selectedScene].add(Layer())
        caret.endY = layerCount * layerHeight
    }

    fun addObject(clazz: Class<*>, layerIndex: Int, file: String? = null, start: Int? = null, end: Int? = null, newObj: CitrusObject? = null) {
        val layerPane = layerVBox.children[layerIndex] as Pane

        val cObject = newObj ?: clazz.getDeclaredConstructor(Int::class.java, Int::class.java).newInstance(layerIndex, selectedScene) as CitrusObject

        cObject.setupProperties()
        val o = TimeLineObject(cObject, this)
        o.prefHeight = layerHeight * 2
        o.style = "-fx-background-color:#${o.color.darker().toString().substring(2)};"

        cObject.start = start ?: currentFrame
        cObject.end = end ?: (cObject.start + defaultObjectLength * Main.project.fps).toInt()
        //o.prefWidth = 200.0
        o.onScaleChanged()
        o.setOnMousePressed {
            allTimelineObjects.forEach {
                it.style = "-fx-background-color:#${it.color.darker().toString().substring(2)};"
                it.strictSelected = false
            }
            o.style = "-fx-background-color:#${o.color.toString().substring(2)};"
            println(o.style)
            o.strictSelected = true
            selectedObjects.add(o)
            selectedObjectOldWidth.add(o.width)
        }
        o.editModeChangeListener = object : TimeLineObject.EditModeChangeListener {
            override fun onEditModeChanged(mode: TimeLineObject.EditMode, offsetX: Double, offsetY: Double) {
                if (!dragging) {
                    editMode = mode
                    selectedOffsetX = offsetX
                }
            }
        }
        caret.layoutXProperty().addListener { _, _, _ ->
            if (cObject.isActive(currentFrame)) o.onCaretChanged(currentFrame)
        }
        allTimelineObjects.add(o)
        layerPane.children.add(o)
        o.onMoved(TimeLineObject.EditMode.Move)
        layerScrollPane.layout()
        if (file != null) cObject.onFileDropped(file)
        projectRenderer.updateObject()
    }

    fun seekTo(frame: Int) {
        caret.layoutX = frame * pixelPerFrame
        currentFrame = frame
    }

    private fun drawAxis() {
        val g = timelineAxis.graphicsContext2D
        g.clearRect(0.0, 0.0, g.canvas.width, g.canvas.height)
        g.fill = Color.WHITE
        g.stroke = Color.WHITE
        g.font = Font(13.0)

        for (i in (offsetX / (tick * pixelPerFrame)).toInt()..((timelineAxis.width / (tick * pixelPerFrame)).toInt() + (offsetX / (tick * pixelPerFrame)).toInt() + 1)) {
            val x = i * tick * pixelPerFrame - offsetX
            if (i % 6 == 0) {
                g.fillText("${(i * tick / Main.project.fps).toTimeString()}s", x, 20.0)
                g.strokeLine(x, 20.0, x, 35.0)
            } else {
                g.strokeLine(x, 25.0, x, 35.0)
            }
        }
    }

    var dvCount = 0
    var leftMaxVol = 0.0
    var rightMaxVol = 0.0

    private fun drawVolumeBar() {
        val canvas = parentController.volumeBar

        val levelL = 60 + 20 * Math.log10(projectRenderer.leftAudioLevel)
        val levelR = 60 + 20 * Math.log10(projectRenderer.rightAudioLevel)
        val g = canvas.graphicsContext2D
        g.clearRect(0.0, 0.0, canvas.width, canvas.height)

        //背景
        g.fill = Color.GRAY
        g.fillRect(0.0, 0.0, 25.0, canvas.height)
        g.fillRect(26.0, 0.0, 25.0, canvas.height)

        //メーター
        g.fill = LinearGradient(0.0, 0.0, 0.0, canvas.height, false, CycleMethod.NO_CYCLE, Stop(0.0, Color.RED), Stop(canvas.height, Color.YELLOW))
        g.fillRect(0.0, canvas.height - (levelL / 60.0) * canvas.height, 25.0, (levelL / 60.0) * canvas.height)
        g.fillRect(26.0, canvas.height - (levelR / 60.0) * canvas.height, 25.0, (levelR / 60.0) * canvas.height)
        //文字
        g.fill = Color.WHITE
        g.stroke = Color.WHITE
        g.font = Font.font(9.0)
        for (i in 0..60) {
            if (i % 6 == 0) {
                g.fillText("-${i}dB", 56.0, (i / 60.0) * canvas.height + g.font.size)
                g.strokeLine(51.0, (i / 60.0) * canvas.height, 55.0, (i / 60.0) * canvas.height)
            }

        }

        if (projectRenderer.leftAudioLevel > leftMaxVol) {
            dvCount = 0
            leftMaxVol = projectRenderer.leftAudioLevel
        }
        if (projectRenderer.rightAudioLevel > rightMaxVol) {
            dvCount = 0
            rightMaxVol = projectRenderer.rightAudioLevel
        }

        parentController.volumeLeftLight.fill =
                if (leftMaxVol > 1)
                    LinearGradient(0.0, 0.0, 0.0, 1.0, true, CycleMethod.NO_CYCLE, Stop(0.5, Color.YELLOW), Stop(1.0, Color.YELLOW.darker()))
                else LinearGradient(0.0, 0.0, 0.0, 1.0, true, CycleMethod.NO_CYCLE, Stop(0.5, Color.DARKGRAY), Stop(1.0, Color.BLACK))
        parentController.volumeRightLight.fill =
                if (leftMaxVol > 1)
                    LinearGradient(0.0, 0.0, 0.0, 1.0, true, CycleMethod.NO_CYCLE, Stop(0.5, Color.YELLOW), Stop(1.0, Color.YELLOW.darker()))
                else LinearGradient(0.0, 0.0, 0.0, 1.0, true, CycleMethod.NO_CYCLE, Stop(0.5, Color.DARKGRAY), Stop(1.0, Color.BLACK))

        //ピークホールド
        g.fill = Color.WHITE
        val maxL = 60 + 20 * Math.log10(leftMaxVol)
        val maxR = 60 + 20 * Math.log10(rightMaxVol)
        g.fillRect(0.0, canvas.height - (maxL / 60.0) * canvas.height, 25.0, 2.0)
        g.fillRect(26.0, canvas.height - (maxR / 60.0) * canvas.height, 25.0, 2.0)

        dvCount++
        if (dvCount.toDouble() / projectRenderer.project.fps > 1) {
            leftMaxVol = 0.0
            rightMaxVol = 0.0
            dvCount = 0
        }
    }


    fun layerScrollPaneOnMousePressed(mouseEvent: MouseEvent) {
        if (mouseEvent.button != MouseButton.PRIMARY) return
        selectedOrigin = mouseEvent.x
        dragging = true

        if (selectedObjects.isEmpty() && mouseEvent.button == MouseButton.PRIMARY) {
            //parentController.rightPane.children.clear()
            playing = true//再生中にしないと、キーフレームが打たれている際に複数回描画してしまう
            currentFrame = (mouseEvent.x / pixelPerFrame).toInt()
            playing = false
        }
    }

    fun layerScrollPaneOnMouseDragged(mouseEvent: MouseEvent) {
        if (mouseEvent.button != MouseButton.PRIMARY) return
        if (selectedObjects.isNotEmpty())
            for ((i, o) in selectedObjects.withIndex()) {
                when (editMode) {
                    TimeLineObject.EditMode.Move -> {
                        o.layoutX = mouseEvent.x - selectedOffsetX

                        if (o.layoutX < 0) o.layoutX = 0.0

                        o.onMoved(editMode)

                        snapObjectOnMove(o)//スナップ処理

                        if (layerVBox.children[(mouseEvent.y / layerHeight).toInt()] != o.parent) {
                            val src = (o.parent as Pane)
                            val dst = (layerVBox.children[(mouseEvent.y / layerHeight).toInt()] as Pane)
                            val srcIndex = o.cObject.layer
                            val dstIndex = (mouseEvent.y / layerHeight).toInt()

                            src.children.remove(o)
                            dst.children.add(o)

                            o.onLayerChanged(srcIndex, dstIndex)

                            layerScrollPane.layout()
                        }
                        o.onMoved(editMode)
                    }
                    TimeLineObject.EditMode.IncrementLength -> {
                        o.prefWidth = mouseEvent.x - o.layoutX
                        o.onMoved(editMode)
                        snapObjectOnIncrement(o)//スナップ処理
                        o.onMoved(editMode)
                    }
                    TimeLineObject.EditMode.DecrementLength -> {
                        o.layoutX = mouseEvent.x
                        if (o.layoutX < 0) o.layoutX = 0.0
                        o.prefWidth = (selectedOrigin - mouseEvent.x) + selectedObjectOldWidth[i] - selectedOffsetX
                        o.onMoved(editMode)
                        snapObjectOnDecrement(o)//スナップ処理
                        o.onMoved(editMode)
                    }
                    TimeLineObject.EditMode.None -> {
                        //Nothing to do
                    }
                }
            }
        else {
            playing = true
            currentFrame = (mouseEvent.x / pixelPerFrame).toInt()
            playing = false
        }

    }

    fun layerScrollPaneOnMouseReleased(mouseEvent: MouseEvent) {
        if (mouseEvent.button != MouseButton.PRIMARY) return

        dragging = false

        for (o in selectedObjects)
            o.onMoved(editMode)

        if (selectedObjects.isNotEmpty()) {
            projectRenderer.updateObject()
            projectRenderer.renderPreview(currentFrame)
            //println("${glCanvas.currentObjects.size}")
        }

        selectedObjects.clear()
        selectedObjectOldWidth.clear()
    }

    private fun snapObjectOnMove(o: TimeLineObject) {
        //スナップ実装

        val nearest = Main.project.scene[selectedScene].flatten().filter {
            it != o.cObject && it.start <= o.cObject.end + 5 && o.cObject.start <= it.end + 5//スナップの基準になりうる位置のオブジェクトを絞る
        }.minBy {
            intArrayOf(
                    Math.abs(it.start - o.cObject.end),
                    Math.abs(o.cObject.start - it.start),
                    Math.abs(it.end - o.cObject.end),
                    Math.abs(o.cObject.start - it.end)
            ).min() ?: 0//スナップしうる４つのパターンの内、最も移動距離が短いものを選び、さらに一番移動距離が短いものを選ぶ
        }
        if (nearest != null) {
            val map: HashMap<Int, Int> = HashMap()
            map[0] = Math.abs(nearest.start - o.cObject.end)
            map[1] = Math.abs(o.cObject.start - nearest.start)
            map[2] = Math.abs(nearest.end - o.cObject.end)
            map[3] = Math.abs(o.cObject.start - nearest.end)

            when (map.filter { it.value <= 4 }.minBy { it.value }?.key) {//４つのスナップ位置の中で最も近い位置へ移動
                0 -> o.layoutX = nearest.start * pixelPerFrame - o.width
                1 -> o.layoutX = nearest.start * pixelPerFrame
                2 -> o.layoutX = nearest.end * pixelPerFrame - o.width
                3 -> o.layoutX = nearest.end * pixelPerFrame
            }
        }
        //スナップ実装終わり

        //重複防止
        val block = Main.project.scene[selectedScene][o.cObject.layer].firstOrNull { it != o.cObject && it.start <= o.cObject.end && o.cObject.start <= it.end }//重複する当たり判定を行う
        if (block != null)
            o.layoutX = if (Math.abs(block.start - o.cObject.end) < Math.abs(o.cObject.start - block.end))
                block.start * pixelPerFrame - o.width
            else
                block.end * pixelPerFrame
        //重複防止終わり
    }

    private fun snapObjectOnIncrement(o: TimeLineObject) {
        val nearest = Main.project.scene[selectedScene].flatten().filter { it != o.cObject && it.start - 5 <= o.cObject.end && o.cObject.end <= it.end + 5 }//スナップの基準になりうる位置のオブジェクトを絞る
                .minBy { Math.min(Math.abs(it.start - o.cObject.end), Math.abs(it.end - o.cObject.end)) }//スナップしうる２つのパターンの内、最も移動距離が短いものを選び、さらに一番移動距離が短いものを選ぶ
        if (nearest != null && Math.min(Math.abs(nearest.start - o.cObject.end), Math.abs(nearest.end - o.cObject.end)) < 5) {
            o.prefWidth = if (Math.abs(nearest.start - o.cObject.end) < Math.abs(nearest.end - o.cObject.end))
                (nearest.start - o.cObject.start) * pixelPerFrame
            else
                (nearest.end - o.cObject.start) * pixelPerFrame

            println(o.prefWidth)
        }

        val block = Main.project.scene[selectedScene][o.cObject.layer].firstOrNull { it != o.cObject && it.start <= o.cObject.end }//重複する当たり判定を行う
        if (block != null)
            o.prefWidth = (block.start - o.cObject.start) * pixelPerFrame

    }

    private fun snapObjectOnDecrement(o: TimeLineObject) {
        val right = o.layoutX + o.prefWidth//スナップによる位置ずれを補正するために、あらかしめ右端の座標を記録しておく

        val nearest = Main.project.scene[selectedScene].flatten().filter { it != o.cObject && it.start - 5 <= o.cObject.start && o.cObject.start <= it.end + 5 }//スナップの基準になりうる位置のオブジェクトを絞る
                .minBy { Math.min(Math.abs(it.start - o.cObject.start), Math.abs(it.end - o.cObject.start)) }//スナップしうる２つのパターンの内、最も移動距離が短いものを選び、さらに一番移動距離が短いものを選ぶ

        if (nearest != null && Math.min(Math.abs(nearest.start - o.cObject.start), Math.abs(nearest.end - o.cObject.start)) < 5) {
            o.layoutX = if (Math.abs(nearest.start - o.cObject.start) < Math.abs(nearest.end - o.cObject.start))
                nearest.start * pixelPerFrame
            else
                nearest.end * pixelPerFrame
        }

        val block = Main.project.scene[selectedScene][o.cObject.layer].firstOrNull { it != o.cObject && o.cObject.start <= it.end }//重複する当たり判定を行う
        if (block != null)
            o.layoutX = block.end * pixelPerFrame

        o.prefWidth = (right - o.layoutX)//位置ずれを防止
    }

    var playing = false
    var fpsCount = 0
    var time = 0L
    fun play() {
        playing = true
        val timer = Timer(true)
        timer.schedule(object : TimerTask() {
            override fun run() {
                println("fps:$fpsCount")
                fpsCount = 0
            }
        }, 1000, 1000)

        val start = System.currentTimeMillis()
        val startFrame = currentFrame
        launch {
            var o = System.currentTimeMillis()
            var left = 0.0
            while (playing) {
                currentFrame = startFrame + ((System.currentTimeMillis() - start) / (1000.0 / Main.project.fps)).toInt()
                //println("time $left $currentFrame")

                left = 1.0 / Main.project.fps * 1000.0 - (System.currentTimeMillis() - o)


                Thread.sleep(Math.max(left.toLong() / 2, 0L))
                //else
                //    start-=left.toInt() フレームスキップを行わない場合
//                if (fpsCount == 60) {
//                    println(1000.0/(time/fpsCount))
//                    fpsCount = 0
//                    time = 0
//                }
                time += System.currentTimeMillis() - o
                fpsCount++
                o = System.currentTimeMillis()
            }
            timer.cancel()
            fpsCount = 0
        }
    }

    fun stop() {
        playing = false
    }

    fun topPaneOnMousePressed(mouseEvent: MouseEvent) {
        currentFrame = (mouseEvent.x / pixelPerFrame).toInt()
    }

    fun topPaneOnMouseDragged(mouseEvent: MouseEvent) {
        if (!wait) {
            currentFrame = (mouseEvent.x / pixelPerFrame).toInt()
        }
    }

    fun topPaneOnMouseReleased(mouseEvent: MouseEvent) {
        currentFrame = (mouseEvent.x / pixelPerFrame).toInt()
    }

    fun Double.toTimeString() = this.toInt().toTimeString()

    fun Int.toTimeString(): String {
        val HH = this / 3600
        val mm = this / 60
        val ss = this % 60
        return "${String.format("%02d", HH)}:${String.format("%02d", mm)}:${String.format("%02d", ss)}"
    }

}