package objects

import annotation.CDroppable
import annotation.CObject
import annotation.CProperty
import javafx.application.Platform
import javafx.scene.canvas.Canvas
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.control.ProgressBar
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.stage.FileChooser
import javafx.stage.Stage
import kotlinx.coroutines.experimental.launch
import mod.FFmpegFrameGrabberMod
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.FrameGrabber
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
import java.nio.FloatBuffer
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
    val volume = CAnimatableDoubleProperty(0.0, 2.0, 1.0, 0.01)

    @CProperty("開始位置", 2)
    val startPos = CIntegerProperty(min = 0)

    private var grabber: FFmpegFrameGrabber? = null
    private var isGrabberStarted = false

    private var audioLine: SourceDataLine? = null

    private var oldFrame = 0
    private var buf: Frame? = null

    /**
     * プロジェクトのビデオフレームでの音声の長さ
     */
    private var audioLength = 0

    /**
     * 波形レンダリング用キャンバス
     */
    private var waveFormCanvas = Canvas()

    private var waveLevelData = ByteArray(0)

    /**
     * サンプリングの間隔(秒)
     */
    private val resolution = 0.01

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
            grabber?.sampleMode = FrameGrabber.SampleMode.FLOAT
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

            audioLength = ((grabber?.lengthInFrames ?: 1) * (Main.project.fps / (grabber?.frameRate
                    ?: 30.0))).toInt()

            startPos.max = audioLength
            end = start + audioLength

            //波形データ生成
            cacheWaveData(dialog)
            setupWaveformCanvas()

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
        fitUIObjectSize()
    }

    override fun onScaleUpdate() {

    }

    var audioBuf: FloatBuffer? = null

    override fun getSamples(frame: Int): FloatArray {
        if (isGrabberStarted) {
            if (frame == 0) {
                grabber?.sampleMode = FrameGrabber.SampleMode.FLOAT
                grabber?.timestamp = startPos.value.toLong()
                oldFrame = 0
                audioBuf = null
            } else
                if (oldFrame != frame) {
                    val now = ((frame + startPos.value.toInt()) * (1.0 / Main.project.fps) * 1000 * 1000).toLong()

                    val requiredSamples = if (frame - oldFrame in 1..99) (frame - oldFrame) * samplesPerFrame else samplesPerFrame
                    val result = FloatArray(requiredSamples)
                    var readed = 0

                    if (Math.abs(frame + startPos.value.toInt() - oldFrame) >= 100 || frame + startPos.value.toInt() < oldFrame) {
                        TimelineController.wait = true
                        grabber?.sampleMode = FrameGrabber.SampleMode.FLOAT
                        grabber?.timestamp = now - (1.0 / Main.project.fps * 1000 * 1000).toLong()
                        TimelineController.wait = false
                        buf = grabber?.grabSamples()
                    }

                    while (readed < requiredSamples) {

                        if (audioBuf?.remaining() == 0 || audioBuf == null)//バッファが空orNullだったら
                            buf = grabber?.grabSamples()//デコード

                        audioBuf = (buf?.samples?.get(0) as FloatBuffer)
                        val read = Math.min(requiredSamples - readed, audioBuf?.remaining()
                                ?: (requiredSamples - readed))
                        audioBuf?.get(result, readed, read)
                        //println("read $readed <- $read")
                        readed += read
                    }
                    oldFrame = frame

                    return result.map { it * 0.97f * volume.value.toFloat() }.toFloatArray()
                }
        }

        return FloatArray(0)
    }

    private fun cacheWaveData(dialog: Stage) {
        val progressBar = dialog.scene.lookup("#progressBar") as ProgressBar
        waveFormCanvas.height = 30.0
        waveLevelData = ByteArray(((grabber?.lengthInTime ?: 0) / 1000.0 / 1000.0 / resolution).toInt())


        var buffer = grabber?.grabSamples()

        //1キャンバス中で描画したブロックの数
        var blockCount = 0
        //1ブロック分のサンプルを保持しておく配列
        val shortArray = FloatArray(((grabber?.sampleRate ?: 44100) * (grabber?.audioChannels
                ?: 2) * resolution).toInt())
        //1ブロック分を読み取るまでのカウンタ
        var read = 0
        while (buffer != null) {
            //デコード
            val s = (buffer.samples?.get(0) as FloatBuffer)

            //デコードしたサンプルを全て読み終わるまでループ
            while (s.remaining() > 0) {
                //1ブロック分を読み終わったら、データを格納
                if (shortArray.size - read == 0) {
                    val maxLevel = Math.min(((shortArray.map { Math.abs(it.toDouble()) }.max()
                            ?: 0.0) * Byte.MAX_VALUE), Byte.MAX_VALUE.toDouble()).toByte()
                    //val averageLevel = shortArray.map { Math.abs(it.toInt()) }.average() / Short.MAX_VALUE.toDouble()
                    waveLevelData[blockCount] = maxLevel
                    read = 0
                    blockCount++
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
            Platform.runLater {
                progressBar.progress = (grabber?.timestamp ?: 0L) / (grabber?.lengthInTime ?: 1L).toDouble()
            }
        }
        grabber?.timestamp = 0L

    }

    private fun setupWaveformCanvas() {
        Platform.runLater {
            uiObject?.timelineController?.hScrollBar?.valueProperty()?.addListener({ _, _, _ ->
                renderWaveform()
            })
            waveFormCanvas.width = uiObject?.timelineController?.hScrollBar?.width ?: 0.0
            uiObject?.timelineController?.hScrollBar?.widthProperty()?.addListener { _, _, n ->
                waveFormCanvas.width = Math.min(uiObject?.timelineController?.hScrollBar?.width ?: 0.0, (uiObject?.width
                        ?: 1.0) - waveFormCanvas.layoutX)
                renderWaveform()
            }
            uiObject?.widthProperty()?.addListener { _, _, n ->
                waveFormCanvas.width = Math.min(uiObject?.timelineController?.hScrollBar?.width ?: 0.0, (uiObject?.width
                        ?: 1.0) - waveFormCanvas.layoutX)
                renderWaveform()
            }
            startPos.valueProperty.addListener { _, _, _ ->
                fitUIObjectSize()
                renderWaveform()
            }

            uiObject?.headerPane?.children?.add(0, waveFormCanvas)
            renderWaveform()
        }
    }

    private fun renderWaveform() {
        //冗長なのを防ぐ
        val offsetX = uiObject?.timelineController?.offsetX ?: 0.0
        //offsetは通常正だが念の為
        if (offsetX >= 0) {
            waveFormCanvas.layoutX = Math.max(offsetX - (uiObject?.layoutX ?: 0.0), 0.0)
            waveFormCanvas.width = Math.min(uiObject?.timelineController?.hScrollBar?.width
                    ?: 0.0, (uiObject?.width ?: 1.0) - waveFormCanvas.layoutX)


            val startSec = Math.max(((offsetX - (uiObject?.layoutX
                    ?: 0.0)) / TimelineController.pixelPerFrame) / Main.project.fps + (startPos.value.toDouble() / Main.project.fps), 0.0)
            val pixelPerData = (Main.project.fps * TimelineController.pixelPerFrame) * resolution
            var x = 0.0
            var i = 0
            val g = waveFormCanvas.graphicsContext2D
            g.clearRect(0.0, 0.0, waveFormCanvas.width, waveFormCanvas.height)
            g.fill = Color.WHITE
            while (x < waveFormCanvas.width && (startSec / resolution).toInt() + i < waveLevelData.size) {
                val level = waveLevelData[(startSec / resolution).toInt() + i] / Byte.MAX_VALUE.toDouble() * waveFormCanvas.height
                g.fillRect(x, waveFormCanvas.height - level, pixelPerData, level)
                x += pixelPerData
                i++
            }
        }
    }

    private fun fitUIObjectSize() {
        if (end - start > audioLength - startPos.value.toInt())
            end = start + audioLength - startPos.value.toInt()

        uiObject?.onScaleChanged()
    }
}