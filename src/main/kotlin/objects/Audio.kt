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
import ui.Main
import ui.WindowFactory
import ui.TimeLineObject
import ui.TimelineController
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

@CObject("音声", "388E3CFF", "img/ic_music.png")
@CDroppable(["ac3", "aac", "adts", "aif", "aiff", "afc", "aifc", "amr", "au", "bit", "caf", "dts", "eac3", "flac", "g722", "tco", "rco", "gsm", "lbc", "latm", "loas", "mka", "mp2", "m2a", "mpa", "mp3", "oga", "oma", "opus", "spx", "tta", "voc", "wav", "wv"])
class Audio(defLayer: Int, defScene: Int) : CitrusObject(defLayer, defScene), AudioSampleProvider {

    @CProperty("ファイル", 0)
    val file = CFileProperty(listOf(FileChooser.ExtensionFilter("音声ファイル", (this.javaClass.annotations.first { it is CDroppable } as CDroppable).filter.map { "*.$it" })))

    @CProperty("音量", 1)
    val volume = CAnimatableDoubleProperty(0.0, 1.0, 1.0, 0.01)

    @CProperty("開始位置", 2)
    val startPos = CIntegerProperty(min = 0)

    private var grabber: FFmpegFrameGrabber? = null
    private var isGrabberStarted = false

    private var audioLine: SourceDataLine? = null

    private var oldFrame = 0
    private var buf: Frame? = null

    private var audioLength = 0

    /**
     * 波形キャンバス格納用
     */
    private val hBox = HBox()

    /**
     * 波形レンダリング用キャンバス
     */
    private var waveFormCanvases: Array<Canvas> = Array(1, { _ -> Canvas() })

    /**
     * サンプリングの間隔(秒)
     */
    private val resolution = 0.015

    /**
     * キャンバスの最大幅
     */
    private val canvasSize = 4096

    /**
     * hboxクリップ用
     */
    private val rect = Rectangle(100.0, 30.0)

    private var samplesPerFrame = 0

    init {
        file.valueProperty.addListener { _, _, n -> onFileLoad(n.toString()) }
        displayName = "[音声]"
    }

    override fun onFileDropped(file: String) {
        onFileLoad(file)
    }

    private fun onFileLoad(file: String) {
        val dialog = WindowFactory.buildOnProgressDialog("処理中", "音声を読み込み中...")
        dialog.show()
        launch {
            grabber = FFmpegFrameGrabber(file)
            grabber?.sampleRate = Main.project.sampleRate
            grabber?.audioChannels = Main.project.audioChannel
            grabber?.start()
            if (grabber?.audioCodec == 0) {
                Platform.runLater {
                    dialog.close()
                    val alert = Alert(Alert.AlertType.ERROR, "音声コーデックを識別できませんでした", ButtonType.CLOSE)
                    alert.headerText = null
                    alert.showAndWait()
                }
                return@launch
            }
            samplesPerFrame = (grabber?.sampleRate ?: Main.project.sampleRate) * (grabber?.audioChannels
                    ?: 2) / Main.project.fps
            //波形描画
            //renderWaveForm()

            audioLength = ((grabber?.lengthInFrames ?: 1) * (Main.project.fps / (grabber?.frameRate
                    ?: 30.0))).toInt()
            startPos.max = audioLength
            end = start + audioLength
            //オーディオ出力準備
            val audioFormat = AudioFormat((grabber?.sampleRate?.toFloat() ?: 0f), 16, 2, true, false)

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

        uiObject?.onScaleChanged()
    }

    override fun onScaleUpdate() {
        hBox.scaleX = (audioLength) * TimelineController.pixelPerFrame / hBox.width
        hBox.translateX = -(1 - hBox.scaleX) * hBox.width / 2.0
    }

    var audioBuf: ShortBuffer? = null

    override fun getSamples(frame: Int): ShortArray {
        if (isGrabberStarted) {
            if (frame == 0)
                grabber?.timestamp = 0L
            else
                if (oldFrame != frame) {
                    val now = ((frame + startPos.value.toInt()) * (1.0 / Main.project.fps) * 1000 * 1000).toLong()
                    //
                    val requiredSamples = if (frame - oldFrame in 1..99) (frame - oldFrame) * samplesPerFrame else samplesPerFrame
                    val result = ShortArray(requiredSamples)
                    var readed = 0

                    if (Math.abs(frame - oldFrame) >= 100 || frame < oldFrame) {
                        TimelineController.wait = true
                        grabber?.timestamp = now - (1.0 / Main.project.fps * 1000 * 1000).toLong()
                        TimelineController.wait = false
                        buf = grabber?.grabSamples()
                    }

                    while (readed < requiredSamples) {

                        if (audioBuf?.remaining() == 0 || audioBuf == null)//バッファが空orNullだったら
                            buf = grabber?.grabSamples()//デコード

                        audioBuf = (buf?.samples?.get(0) as ShortBuffer)
                        val read = Math.min(requiredSamples - readed, audioBuf?.remaining()
                                ?: (requiredSamples - readed))
                        audioBuf?.get(result, readed, read)
                        //println("read $readed <- $read")
                        readed += read
                    }
                    oldFrame = frame
                    return result
                }
        }

        return ShortArray(0)
    }

    //ShortArrayをリトルエンディアンでbyte配列に変換
    private fun ShortBuffer.toByteArray(): ByteArray {
        val byteBuffer = ByteBuffer.allocate(this.limit() * 2).order(ByteOrder.LITTLE_ENDIAN)
        val shortArray = ShortArray(this.limit())
        this.get(shortArray)

        byteBuffer.asShortBuffer().put(shortArray.map { (it * volume.value.toDouble()).toShort() }.toShortArray())
        return byteBuffer.array()
    }

    private fun renderWaveForm() {
        //必要数のキャンバスを作成
        waveFormCanvases = Array(((grabber?.lengthInTime
                ?: 0) / 1000.0 / 1000.0 / resolution / canvasSize.toDouble()).toInt() + 1, { _ -> Canvas(0.0, 30.0) })
        waveFormCanvases[0] = Canvas(canvasSize.toDouble(), 30.0)
        var g = waveFormCanvases[0].graphicsContext2D

        var buffer = grabber?.grabSamples()

        //1キャンバス中で描画したブロックの数
        var blockCount = 0
        //1ブロック分のサンプルを保持しておく配列
        val shortArray = ShortArray(((grabber?.sampleRate ?: 44100) * (grabber?.audioChannels
                ?: 2) * resolution).toInt())
        //1ブロック分を読み取るまでのカウンタ
        var read = 0
        //描画を終えたキャンバスの数
        var canvasCount = 0
        while (buffer != null) {
            //デコード
            val s = (buffer.samples?.get(0) as ShortBuffer)

            //デコードしたサンプルを全て読み終わるまでループ
            while (s.remaining() > 0) {
                //1ブロック分を読み終わったら、描画
                if (shortArray.size - read == 0) {
                    val maxLevel = (shortArray.map { Math.abs(it.toInt()) }.max() ?: 0) / Short.MAX_VALUE.toDouble()
                    val averageLevel = shortArray.map { Math.abs(it.toInt()) }.average() / Short.MAX_VALUE.toDouble()
                    g.fill = Color.WHITE
                    g.fillRect(blockCount.toDouble(), (1 - maxLevel) * g.canvas.height, 1.0, maxLevel * g.canvas.height)
                    g.fill = Color.LIGHTGRAY
                    g.fillRect(blockCount.toDouble(), (1 - averageLevel) * g.canvas.height, 1.0, averageLevel * g.canvas.height)
                    read = 0
                    blockCount++

                    //キャンバスを全て埋め終わった場合
                    if (blockCount == canvasSize) {
                        waveFormCanvases[canvasCount].width = canvasSize.toDouble()
                        canvasCount++
                        blockCount = 0
                        waveFormCanvases[canvasCount] = Canvas(canvasSize.toDouble(), 30.0)
                        g = waveFormCanvases[canvasCount].graphicsContext2D
                    }
                }

                val old = s.position()

                //１ブロックに必要なサンプルを取り切れない場合、残ってるサンプルをとりあえず読んでおく
                if (shortArray.size - read > s.remaining())
                    s.get(shortArray, read, s.remaining())
                else//１ブロックに必要なデータをすべて読む
                    s.get(shortArray, read, shortArray.size - read)

                //読んだサンプル数だけ加算
                read += (s.position() - old)
            }
            buffer = grabber?.grabSamples()
        }

        //最後のキャンバスは、描画されていない部分が余るので、サイズ調整
        waveFormCanvases[canvasCount].width = blockCount.toDouble()


        Platform.runLater {
            //クリップ用のrectの幅をuiObjectにバインド
            uiObject?.widthProperty()?.addListener({ _, _, n ->
                rect.width = n.toDouble() / hBox.scaleX
            })
            uiObject?.headerPane?.children?.add(0, hBox)
            hBox.clip = rect
            hBox.children.addAll(waveFormCanvases)
        }
        grabber?.timestamp = 0L

    }
}