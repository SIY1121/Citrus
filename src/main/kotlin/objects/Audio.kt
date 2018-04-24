package objects

import annotation.CDroppable
import annotation.CObject
import annotation.CProperty
import javafx.application.Platform
import javafx.scene.canvas.Canvas
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.stage.FileChooser
import kotlinx.coroutines.experimental.launch
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame
import properties.CAnimatableDoubleProperty
import properties.CFileProperty
import properties.CIntegerProperty
import ui.WindowFactory
import ui.TimeLineObject
import ui.TimelineController
import util.Statics
import java.io.File
import java.nio.ByteBuffer
import java.nio.ShortBuffer
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

@CObject("音声", "388E3CFF", "img/ic_music.png")
@CDroppable(["ac3", "aac", "adts", "aif", "aiff", "afc", "aifc", "amr", "au", "bit", "caf", "dts", "eac3", "flac", "g722", "tco", "rco", "gsm", "lbc", "latm", "loas", "mka", "mp2", "m2a", "mpa", "mp3", "oga", "oma", "opus", "spx", "tta", "voc", "wav", "wv"])
class Audio : CitrusObject() {

    @CProperty("ファイル", 0)
    val file = CFileProperty(listOf(FileChooser.ExtensionFilter("音声ファイル", (this.javaClass.annotations.first { it is CDroppable } as CDroppable).filter.map { "*.$it" })))

    @CProperty("音量", 1)
    val volume = CAnimatableDoubleProperty(0.0, 1.0, 1.0, 0.01)

    @CProperty("開始位置",2)
    val startPos = CIntegerProperty(min = 0)

    var grabber: FFmpegFrameGrabber? = null
    var isGrabberStarted = false

    var audioLine: SourceDataLine? = null

    var oldFrame = -100
    var buf: Frame? = null

    var audioLength = 0

    val hBox = HBox()

    //波形レンダリング用キャンバス
    var waveFormCanvases: Array<Canvas> = Array(1, { _ -> Canvas() })
    val resolution = 0.015
    val canvasSize = 4096

    val rect = Rectangle(100.0, 30.0)

    //val al: AL
    //val bufCount = 2
    //val buffers = IntArray(bufCount)
    //val sources = IntArray(1)

    //var first = true

    init {
        file.valueProperty.addListener { _,_,n->onFileLoad(n.toString()) }
        displayName = "[音声]"
//        ALut.alutInit()
//        al = ALFactory.getAL()
//        println("al error i : " + al.alGetError())
//        al.alGenBuffers(bufCount, buffers, 0)
//        al.alGenSources(1, sources, 0)
//        al.alSourcef(sources[0], AL.AL_PITCH, 1.0f)
//        al.alSourcef(sources[0], AL.AL_GAIN, 1.0f)
//        al.alSourcefv(sources[0], AL.AL_POSITION, floatArrayOf(0f, 0f, 0f), 0)
//        al.alSourcefv(sources[0], AL.AL_VELOCITY, floatArrayOf(0f, 0f, 0f), 0)
//        al.alSourcei(sources[0],AL.AL_LOOPING,AL.AL_FALSE)
//        al.alListenerfv(AL.AL_POSITION,	floatArrayOf(0f, 0f, 0f), 0)
//        al.alListenerfv(AL.AL_VELOCITY,    floatArrayOf(0f, 0f, 0f), 0)
//        al.alListenerfv(AL.AL_ORIENTATION, floatArrayOf( 0.0f, 0.0f, -1.0f,  0.0f, 1.0f, 0.0f), 0)
//        println("al error : " + al.alGetError())
    }

    override fun onFileDropped(file: String) {
        onFileLoad(file)
    }

    private fun onFileLoad(file: String) {
        val dialog = WindowFactory.buildOnProgressDialog("処理中", "音声を読み込み中...")
        dialog.show()
        launch {
            grabber = FFmpegFrameGrabber(file)
            grabber?.start()
            if (grabber?.videoCodec == 0) {
                Platform.runLater {
                    dialog.close()
                    val alert = Alert(Alert.AlertType.ERROR, "音声コーデックを識別できませんでした", ButtonType.CLOSE)
                    alert.headerText = null
                    alert.showAndWait()
                }
                return@launch
            }

            renderWaveForm()

            audioLength = ((grabber?.lengthInFrames ?: 1) * (Statics.project.fps / (grabber?.frameRate
                    ?: 30.0))).toInt()
            startPos.max = audioLength
            end = start + audioLength
            //オーディオ出力準備
            val audioFormat = AudioFormat((grabber?.sampleRate?.toFloat() ?: 0f), 16, 2, true, true)

            val info = DataLine.Info(SourceDataLine::class.java, audioFormat)
            audioLine = AudioSystem.getLine(info) as SourceDataLine
            audioLine?.open(audioFormat)
            audioLine?.start()
            isGrabberStarted = true

            Platform.runLater {
                uiObject?.onScaleChanged()
                dialog.close()
                displayName = "[音声] ${File(file).name}"
            }
        }

    }

    override fun onLayoutUpdate(mode: TimeLineObject.EditMode) {
        if (audioLength == 0) return
        if (end - start > audioLength - startPos.value.toInt())
            end = start + audioLength - startPos.value.toInt()

        //uiObject?.label?.background = Background(BackgroundImage(waveFormImage, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition(Side.LEFT, 0.0, false, Side.BOTTOM, 0.0, false), BackgroundSize(audioLength.toDouble() / (end - start), 1.0, true, true, false, false)))


        uiObject?.onScaleChanged()
    }

    override fun onScaleUpdate() {
        //waveFormCanvas.scaleX = (end - start) * TimelineController.pixelPerFrame /waveFormCanvas.width
        //waveFormCanvas.translateX = -(1-waveFormCanvas.scaleX)*waveFormCanvas.width/2.0
        //println(waveFormCanvas.scaleX)
        hBox.scaleX = (audioLength) * TimelineController.pixelPerFrame / hBox.width
        hBox.translateX = -(1 - hBox.scaleX) * hBox.width / 2.0
    }

    override fun onFrame() {
        if (isGrabberStarted) {
            if (oldFrame != frame) {
                val now = ((frame + 5 + startPos.value.toInt()) * (1.0 / Statics.project.fps) * 1000 * 1000).toLong()

                if (Math.abs(frame - oldFrame) > 30) {
                    TimelineController.wait = true
                    grabber?.timestamp = now - 1000
                    TimelineController.wait = false
                    buf = grabber?.grabSamples()
                }

                while (grabber?.timestamp ?: 0 <= now && buf != null) {
                    // println("a:" + grabber?.timestamp + " ")
                    if (buf?.samples != null) {

                        val s = (buf?.samples?.get(0) as ShortBuffer)
                        val arr = s.toByteArray()
                        audioLine?.write(arr, 0, arr.size)
                        //println(audioLine?.framePosition)
                    }

//                    if (buf?.samples != null) {
//                        val s = (buf?.samples?.get(0) as ShortBuffer)
//                        val arr = s.toByteArray()
//                        if (first) {
//                            for(i in 0 until bufCount)
//                                 al.alBufferData(buffers[i], AL.AL_FORMAT_STEREO16, ByteBuffer.wrap(arr), arr.size, grabber?.sampleRate
//                                    ?: 44100)
//
//                            println("al error 1 : " + al.alGetError())
//                            al.alSourceQueueBuffers(sources[0], bufCount, buffers, 0)
//
//                            println("al error 11 : " + al.alGetError())
//                            al.alSourcePlay(sources[0])
//
//                            println("al error 12 : " + al.alGetError())
//                            first = false
//                        } else {
//                            val processed = IntArray(1)
//                            val tmpBuffer = IntArray(1)
//                            al.alGetSourcei(sources[0], AL.AL_BUFFERS_PROCESSED, processed, 0)
//                            println(processed[0])
//                            if (processed[0] > 0) {
//                                al.alSourceUnqueueBuffers(sources[0], 1, tmpBuffer, 0)
//                                println("tmp buf " + tmpBuffer[0])
//                                al.alBufferData(tmpBuffer[0], AL.AL_FORMAT_STEREO16, ByteBuffer.wrap(arr), arr.size, grabber?.sampleRate
//                                        ?: 44100)
//                                al.alSourceQueueBuffers(sources[0], 1, tmpBuffer, 0)
//                            }
//                            al.alSourcePlay(sources[0])
//                            println("al error 2 : " + al.alGetError())
//                        }
//                    }


                    buf = grabber?.grabSamples()
                }

            }
            oldFrame = frame
        }
    }

    fun ShortBuffer.toByteArray(): ByteArray {
        val byteBuffer = ByteBuffer.allocate(this.limit() * 2)
        val shortArray = ShortArray(this.limit())
        this.get(shortArray)

        byteBuffer.asShortBuffer().put(shortArray.map { (it * volume.value.toDouble()).toShort() }.toShortArray())
        return byteBuffer.array()
    }

    private fun renderWaveForm() {

        //waveFormCanvas.height = 30.0
        //waveFormCanvas.width = (grabber?.lengthInTime ?: 0) / 1000.0 / 1000.0 / resolution
        //waveFormData = ShortArray(((grabber?.lengthInTime ?: 0) / 1000.0 / 1000.0 / resolution).toInt())


        waveFormCanvases = Array(((grabber?.lengthInTime
                ?: 0) / 1000.0 / 1000.0 / resolution / canvasSize.toDouble()).toInt() + 1, { _ -> Canvas(0.0, 30.0) })
        waveFormCanvases[0] = Canvas(canvasSize.toDouble(), 30.0)
        var g = waveFormCanvases[0].graphicsContext2D
        //g.fill = LinearGradient(0.0, 0.0, 0.0, 30.0, false, CycleMethod.NO_CYCLE, Stop(0.0, Color.WHITE), Stop(1.0, Color.GRAY))

        var buffer = grabber?.grabSamples()
        var blockCount = 0
        val shortArray = ShortArray(((grabber?.sampleRate ?: 44100) * (grabber?.audioChannels
                ?: 2) * resolution).toInt())
        var read = 0
        var canvasCount = 0
        while (buffer != null) {
            val s = (buffer.samples?.get(0) as ShortBuffer)
            while (s.remaining() > 0) {
                if (shortArray.size - read == 0) {
                    //val level = Math.max(Math.log10(shortArray.map { Math.abs(it.toInt()) }.average() / Short.MAX_VALUE.toDouble()) + 60,0.0)/60.0
                    val maxLevel = (shortArray.map { Math.abs(it.toInt()) }.max() ?: 0) / Short.MAX_VALUE.toDouble()
                    val averageLevel = shortArray.map { Math.abs(it.toInt()) }.average() / Short.MAX_VALUE.toDouble()
                    //g.fillRect(blockCount.toDouble(), (1 - level) * g.canvas.height, 1.0, level * g.canvas.height)
                    g.fill = Color.WHITE
                    g.fillRect(blockCount.toDouble(), (1 - maxLevel) * g.canvas.height, 1.0, maxLevel * g.canvas.height)
                    g.fill = Color.LIGHTGRAY
                    g.fillRect(blockCount.toDouble(), (1 - averageLevel) * g.canvas.height, 1.0, averageLevel * g.canvas.height)
                    read = 0
                    //println("block $blockCount")
                    blockCount++
                    if (blockCount == canvasSize) {
                        waveFormCanvases[canvasCount].width = canvasSize.toDouble()
                        canvasCount++
                        blockCount = 0
                        waveFormCanvases[canvasCount] = Canvas(canvasSize.toDouble(), 30.0)
                        g = waveFormCanvases[canvasCount].graphicsContext2D
                        //g.fill =  LinearGradient(0.0, 0.0, 0.0, 30.0, false, CycleMethod.NO_CYCLE, Stop(0.0, Color.WHITE), Stop(1.0, Color.GRAY))
                    }
                }
                val old = s.position()
                if (shortArray.size - read > s.remaining())
                    s.get(shortArray, read, s.remaining())
                else
                    s.get(shortArray, read, shortArray.size - read)

                read += (s.position() - old)
            }
            buffer = grabber?.grabSamples()
        }
        waveFormCanvases[canvasCount].width = blockCount.toDouble()


        Platform.runLater {
            //            waveFormImage = WritableImage(waveFormCanvas.width.toInt(), waveFormCanvas.height.toInt())
//            val params = SnapshotParameters()
//            params.fill = Color.TRANSPARENT
//            waveFormCanvas.snapshot(params, waveFormImage)
//            uiObject?.label?.background = Background(BackgroundImage(waveFormImage, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition(Side.LEFT, 0.0, true, Side.BOTTOM, 0.0, true), BackgroundSize(1.0, 1.0, true, true, false, false)))
            //waveFormCanvas.style = "-fx-background-color:blue"
            uiObject?.widthProperty()?.addListener({ _, _, n ->
                rect.width = n.toDouble() / hBox.scaleX
            })
            uiObject?.headerPane?.children?.add(0, hBox)
            hBox.clip = rect
            hBox.children.addAll(waveFormCanvases)

        }


    }
}