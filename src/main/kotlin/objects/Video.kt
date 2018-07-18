package objects

import annotation.CDroppable
import annotation.CObject
import annotation.CProperty
import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2
import javafx.application.Platform
import javafx.scene.Cursor
import javafx.scene.canvas.Canvas
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.control.ProgressBar
import javafx.scene.image.ImageView
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import javafx.scene.layout.Pane
import javafx.scene.shape.Rectangle
import javafx.stage.FileChooser
import javafx.stage.Stage
import kotlinx.coroutines.experimental.launch
import mod.FFmpegFrameGrabberMod
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import project.ProjectRenderer
import properties.CAnimatableDoubleProperty
import properties.CDoubleProperty
import java.nio.ByteBuffer
import java.nio.IntBuffer
import properties.CFileProperty
import properties.CIntegerProperty
import ui.*
import java.io.*
import java.nio.ShortBuffer


@CObject("動画", "F57C00", "img/ic_movie.png")
@CDroppable(["asf", "wmv", "wma", "asf", "wmv", "wma", "avi", "flv", "h261", "h263", "m4v", "m4a", "ismv", "isma", "mkv", "mjpg", "mjpeg", "mp4", "mpg", "mpeg", "mpg", "mpeg", "m1v", "dvd", "vob", "vob", "ts", "m2t", "m2ts", "mts", "nut", "ogv", "webm", "chk"])
class Video(defLayer: Int, defScene: Int) : DrawableObject(defLayer, defScene) {

    override val id = "citrus/video"
    override val name = "動画"

    @CProperty("ファイル", 0)
    val file = CFileProperty(listOf(FileChooser.ExtensionFilter("動画ファイル", (this.javaClass.annotations.first { it is CDroppable } as CDroppable).filter.map { "*.$it" })))

    @CProperty("開始位置", 1)
    val startPos = CIntegerProperty(min = 0)

    var grabber: FFmpegFrameGrabberMod? = null
    var isGrabberStarted = false

    var oldFrame = -100
    var buf: Frame? = null

    var textureID: Int = 0

    var videoLength = 0

    var oldStart = 0

    var thumbPane = Pane()

    var thumsTimestamp: MutableList<Long> = ArrayList()

    var rect = Rectangle()

    init {
        file.valueProperty.addListener { _, _, n -> onFileLoad(n.toString()) }
        displayName = "[動画]"
    }

    override fun onFileDropped(f: String) {
        file.value = f
        TimelineController.instance.addObject(Audio::class.java, layer + 1, f)
    }

    private fun onFileLoad(file: String) {
        val dialog = WindowFactory.buildOnProgressDialog("処理中", "動画を読み込み中...")
        dialog.show()
        launch {
            //デコーダ準備
            grabber = FFmpegFrameGrabberMod(file)
            grabber?.setVideoOption("threads", "0")
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

            videoLength = ((grabber?.lengthInFrames ?: 1) * (Main.project.fps / (grabber?.frameRate
                    ?: 30.0))).toInt()
            startPos.max = videoLength
            end = start + videoLength
            //テクスチャ準備
            ProjectRenderer.invoke(true) {
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

                bufferSize = Size(grabber?.imageWidth?.toDouble() ?: 0.0, grabber?.imageHeight?.toDouble() ?: 0.0)

                println("allocate ${grabber?.imageWidth}x${grabber?.imageHeight}")
                false
            }
            renderThumbs(dialog)
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

    override fun onScaleUpdate() {
        super.onScaleUpdate()
        thumbPane.children.forEachIndexed { index, node ->
            node.layoutX = (thumsTimestamp[index] / 1000.0 / 1000.0 * Main.project.fps - startPos.value.toInt()) * TimelineController.pixelPerFrame
        }
    }

    override fun onDraw(gl: GL2, mode: Drawable.DrawMode, frame: Int) {
        super.onDraw(gl, mode, frame)

        if (isGrabberStarted) {
            gl.glBindTexture(GL.GL_TEXTURE_2D, textureID)

            //フレームが変わった場合にのみ処理
            if (oldFrame != frame) {
                val now = ((frame + startPos.value.toInt()) * (1.0 / Main.project.fps) * 1000 * 1000).toLong()

                //移動距離が30フレーム以上でシーク処理を実行
                if (Math.abs(frame - oldFrame) > 100 || frame < oldFrame) {
                    TimelineController.wait = true
                    grabber?.timestamp = Math.max(now - 10000, 0)
                    //buf = grabber?.fastSeek(now)
                    TimelineController.wait = false
                    buf = grabber?.grabImage()
                    println("video $file seek $oldFrame to $frame")
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

    fun FFmpegFrameGrabber.fastSeek(timestamp: Long): Frame? {

        var beforeTimestamp = 0L
        while (this.timestamp < timestamp - 1000 * 1000 * 10) {
            beforeTimestamp = this.timestamp
            grabKeyFrame()
            println("key")
        }

        if (this.timestamp > timestamp)
            this.timestamp = beforeTimestamp


        var frame: Frame? = null
        while (this.timestamp < timestamp) {
            frame = grabImage()
            println("f")
        }

        return frame
    }

    fun renderThumbs(dialog: Stage) {
        val progressBar = dialog.scene.lookup("#progressBar") as ProgressBar
        while (true) {
            val frame = grabber?.grabKeyFrame() ?: break
            println(grabber?.timestamp)
            Platform.runLater {
                progressBar.progress = (grabber?.timestamp ?: 0L) / (grabber?.lengthInTime ?: 1L).toDouble()
            }
            val mat = Mat(frame.imageHeight, frame.imageWidth, CvType.CV_8UC3, frame.image[0] as ByteBuffer)
            val small = Mat(30, ((30.0 / frame.imageHeight) * frame.imageWidth).toInt(), CvType.CV_8UC3)


            Imgproc.resize(mat, small, Size(small.width().toDouble(), small.height().toDouble()))
            Imgproc.cvtColor(small, small, Imgproc.COLOR_BGR2BGRA)

            for (x in 0 until small.width()) {
                val a = (Math.min(1.0, (3.0 - (x.toDouble() / small.width()) * 3)) * 255).toByte()
                for (y in 0 until small.height()) {
                    val p = ByteArray(4)
                    small.get(y, x, p)
                    small.put(y, x, byteArrayOf(p[0], p[1], p[2], a))
                }
            }


            val image = WritableImage(small.width(), small.height())
            val buf = ByteArray((image.width * image.height * 4).toInt())
            small.get(0, 0, buf)
            image.pixelWriter.setPixels(0, 0, image.width.toInt(), image.height.toInt(), PixelFormat.getByteBgraInstance(), buf, 0, (image.width * 4).toInt())
            val view = ImageView(image)
            val thumbFrame = (grabber?.timestamp ?: 0L) / 1000.0 / 1000.0 * Main.project.fps
            thumsTimestamp.add(grabber?.timestamp ?: 0)
            view.layoutX = (thumbFrame - startPos.value.toInt()) * TimelineController.pixelPerFrame
            view.cursor = Cursor.HAND
            view.style = "linear-gradient(to left right, #FFFFFFFF, #FFFFFF00)"
            view.setOnMouseClicked {
                uiObject?.timelineController?.seekTo(thumbFrame.toInt() - startPos.value.toInt())
            }
            thumbPane.children.add(view)
        }
        grabber?.timestamp = 0L
        buf = grabber?.grabImage()
        Platform.runLater {
            rect.height = 30.0
            uiObject?.widthProperty()?.addListener { _, _, n ->
                rect.width = n.toDouble()
            }
            startPos.valueProperty.addListener { _, _, _ -> onScaleUpdate() }
            uiObject?.headerPane?.children?.add(0, thumbPane)
            thumbPane.clip = rect
        }
    }
}