package objects

import annotation.CDroppable
import annotation.CObject
import annotation.CProperty
import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2
import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.stage.FileChooser
import kotlinx.coroutines.experimental.launch
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame
import ui.GlCanvas
import util.Statics
import java.nio.ByteBuffer
import java.nio.IntBuffer
import properties.CFileProperty
import properties.CIntegerProperty
import ui.WindowFactory
import ui.TimeLineObject
import ui.TimelineController
import java.io.*
import java.nio.ShortBuffer


@CObject("動画", "F57C00", "img/ic_movie.png")
@CDroppable(["asf", "wmv", "wma", "asf", "wmv", "wma", "avi", "flv", "h261", "h263", "m4v", "m4a", "ismv", "isma", "mkv", "mjpg", "mjpeg", "mp4", "mpg", "mpeg", "mpg", "mpeg", "m1v", "dvd", "vob", "vob", "ts", "m2t", "m2ts", "mts", "nut", "ogv", "webm", "chk"])
class Video : DrawableObject() {

    override val id = "citrus/video"
    override val name = "動画"

    @CProperty("ファイル", 0)
    val file = CFileProperty(listOf(FileChooser.ExtensionFilter("動画ファイル", (this.javaClass.annotations.first { it is CDroppable } as CDroppable).filter.map { "*.$it" })))

    @CProperty("開始位置", 1)
    val startPos = CIntegerProperty(min = 0)

    var grabber: FFmpegFrameGrabber? = null
    var isGrabberStarted = false

    var oldFrame = -100
    var buf: Frame? = null

    var textureID: Int = 0

    var videoLength = 0

    var oldStart = 0

    init {
        file.valueProperty.addListener { _, _, n -> onFileLoad(n.toString()) }
        displayName = "[動画]"
    }

    override fun onFileDropped(file: String) {
        onFileLoad(file)
        TimelineController.instance.addObject(Audio::class.java, layer + 1, file)
    }

    private fun onFileLoad(file: String) {
        val dialog = WindowFactory.buildOnProgressDialog("処理中", "動画を読み込み中...")
        dialog.show()
        launch {
            //デコーダ準備
            grabber = FFmpegFrameGrabber(file)
            grabber?.timestamp
            grabber?.start()
            if (grabber?.videoCodec == 0) {
                Platform.runLater {
                    val alert = Alert(Alert.AlertType.ERROR, "動画コーデックを識別できませんでした", ButtonType.CLOSE)
                    alert.headerText = null
                    dialog.close()
                    alert.showAndWait()
                }
                return@launch
            }

            videoLength = ((grabber?.lengthInFrames ?: 1) * (Statics.project.fps / (grabber?.frameRate
                    ?: 30.0))).toInt()
            startPos.max = videoLength
            end = start + videoLength
            //テクスチャ準備
            GlCanvas.instance.invoke(true, {
                if (textureID != 0) {
                    val b = IntBuffer.allocate(1)
                    b.put(textureID)
                    it.gl.glDeleteTextures(GL.GL_TEXTURE_2D, b)
                }
                val b = IntBuffer.allocate(1)
                it.gl.glGenTextures(1, b)
                textureID = b.get()
                println("textureID : $textureID")
                it.gl.glBindTexture(GL.GL_TEXTURE_2D, textureID)
                it.gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST)
                it.gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST)
                it.gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE)
                it.gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE)
                it.gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGB, grabber?.imageWidth ?: 0, grabber?.imageHeight
                        ?: 0, 0, GL.GL_BGR, GL.GL_UNSIGNED_BYTE, ByteBuffer.allocate((grabber?.imageWidth
                        ?: 0) * (grabber?.imageHeight ?: 0) * 3))


                println("allocate ${grabber?.imageWidth}x${grabber?.imageHeight}")
                false
            })
            Platform.runLater {
                dialog.close()
                uiObject?.onScaleChanged()
                displayName = "[動画] ${File(file).name}"
                isGrabberStarted = true
            }
        }
    }

    override fun onLayoutUpdate(mode: TimeLineObject.EditMode) {
        if (videoLength == 0) return
        if (end - start > videoLength - startPos.value.toInt())
            end = start + videoLength - startPos.value.toInt()

        if (mode == TimeLineObject.EditMode.DecrementLength) {
            val dif = start - oldStart
            startPos.value = startPos.value.toInt() + dif

        }
        oldStart = start
        uiObject?.onScaleChanged()
    }

    override fun onDraw(gl: GL2, mode: Drawable.DrawMode) {
        super.onDraw(gl, mode)

        if (isGrabberStarted) {
            gl.glBindTexture(GL.GL_TEXTURE_2D, textureID)

            //フレームが変わった場合にのみ処理
            if (oldFrame != frame) {
                val now = ((frame + startPos.value.toInt()) * (1.0 / Statics.project.fps) * 1000 * 1000).toLong()

                //移動距離が30フレーム以上でシーク処理を実行
                if (Math.abs(frame - oldFrame) > 30 || frame < oldFrame) {
                    TimelineController.wait = true
                    grabber?.timestamp = Math.max(now - 10000, 0)
                    TimelineController.wait = false
                    buf = grabber?.grabFrame()
                }
                //buf = null
                //画像フレームを取得できており、タイムスタンプが理想値より上回るまでループ
                while (grabber?.timestamp ?: 0 <= now && buf != null) {
                    buf = grabber?.grabImage()
                }
                gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, buf?.imageWidth ?: 0, buf?.imageHeight
                        ?: 0, GL.GL_BGR, GL2.GL_UNSIGNED_BYTE, buf?.image?.get(0))
            }

            gl.glBegin(GL2.GL_QUADS)
            gl.glTexCoord2d(0.0, 1.0)
            gl.glVertex3d(-(buf?.imageWidth ?: 0) / 2.0, -(buf?.imageHeight ?: 0) / 2.0, 0.0)
            gl.glTexCoord2d(0.0, 0.0)
            gl.glVertex3d(-(buf?.imageWidth ?: 0) / 2.0, (buf?.imageHeight ?: 0) / 2.0, 0.0)
            gl.glTexCoord2d(1.0, 0.0)
            gl.glVertex3d((buf?.imageWidth ?: 0) / 2.0, (buf?.imageHeight ?: 0) / 2.0, 0.0)
            gl.glTexCoord2d(1.0, 1.0)
            gl.glVertex3d((buf?.imageWidth ?: 0) / 2.0, -(buf?.imageHeight ?: 0) / 2.0, 0.0)
            gl.glEnd()
            gl.glBindTexture(GL.GL_TEXTURE_2D, 0)

            oldFrame = frame
        }
    }

    fun ShortBuffer.toByteArray(): ByteArray {
        val byteBuffer = ByteBuffer.allocate(this.limit() * 2)
        val shortArray = ShortArray(this.limit())
        this.get(shortArray)
        byteBuffer.asShortBuffer().put(shortArray)
        return byteBuffer.array()
    }

}