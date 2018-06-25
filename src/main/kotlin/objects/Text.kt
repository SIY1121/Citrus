package objects

import annotation.CObject
import annotation.CProperty
import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2
import javafx.application.Platform
import javafx.scene.SnapshotParameters
import javafx.scene.effect.DropShadow
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.TextAlignment
import org.opencv.core.Size
import project.ProjectRenderer
import properties.*
import java.awt.GraphicsEnvironment
import java.nio.ByteBuffer
import java.nio.IntBuffer

@CObject("テキスト", "1976D2FF", "img/ic_text.png")
class Text(defLayer: Int, defScene: Int) : DrawableObject(defLayer, defScene) {
    override val id = "citrus/text"
    override val name = "テキスト"

    @CProperty("フォント", 0)
    val font = CSelectableProperty(GraphicsEnvironment.getLocalGraphicsEnvironment().allFonts.map { it.fontName })

    @CProperty("文字色", 1)
    val color = CColorProperty()

    @CProperty("縁取り", 2)
    val isStroke = CSwitchableProperty(false)

    @CProperty("縁取り色", 3)
    val strokeColor = CColorProperty(Color.RED)

    @CProperty("影", 4)
    val isShadow = CSwitchableProperty(false)

    @CProperty("影の色", 5)
    val shadowColor = CColorProperty(Color.BLACK)

    @CProperty("サイズ", 6)
    val size = CAnimatableDoubleProperty(min = 0.0, def = 200.0)

    @CProperty("テキスト", 7)
    val text = CTextProperty()


    val t = javafx.scene.text.Text("text")
    var textureID: Int = 0

    init {

        font.valueProperty.addListener { _, _, _ -> updateTexture() }
        color.valueProperty.addListener { _, _, _ -> updateTexture() }
        isStroke.valueProperty.addListener { _, _, _ -> updateTexture() }
        strokeColor.valueProperty.addListener { _, _, _ -> updateTexture() }
        isShadow.valueProperty.addListener { _, _, _ -> updateTexture() }
        shadowColor.valueProperty.addListener { _, _, _ -> updateTexture() }
        size.valueProperty.addListener { _, _, _ -> updateTexture() }
        text.valueProperty.addListener { _, _, _ -> updateTexture() }

        displayName = "[テキスト]"
        ProjectRenderer.invoke(true) {
            val b = IntBuffer.allocate(1)
            it.gl.glGenTextures(1, b)
            textureID = b.get()
            it.gl.glBindTexture(GL.GL_TEXTURE_2D, textureID)
            it.gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR)
            it.gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR)
            it.gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE)
            it.gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE)
            Platform.runLater { updateTexture() }
            false
        }
    }

    private fun updateTexture() {
        t.text = text.value
        t.textAlignment = TextAlignment.CENTER
        t.style = "-fx-background-color:transparent"
        t.fill = color.value
        t.font = Font(font.list[font.value.toInt()], size.value.toDouble())
        if (isStroke.value) {
            t.strokeWidth = 5.0
            t.stroke = strokeColor.value
        } else {
            t.strokeWidth = 0.0
        }
        if (isShadow.value) {
            val ds = DropShadow()
            ds.color = shadowColor.value
            t.effect = ds
        } else
            t.effect = null

        if (t.boundsInLocal.width <= 0 || t.boundsInLocal.height <= 0) {
            println("w h 0 ")
            return
        }

        val w = WritableImage(t.boundsInLocal.width.toInt(), t.boundsInLocal.height.toInt())
        val params = SnapshotParameters()
        params.fill = Color.TRANSPARENT
        t.snapshot(params, w)
        println("w ${w.width} h ${w.height}")

        val buf = ByteBuffer.allocate(t.boundsInLocal.width.toInt() * t.boundsInLocal.height.toInt() * 4)

        w.pixelReader.getPixels(0, 0, w.width.toInt(), w.height.toInt(), PixelFormat.getByteBgraInstance(), buf, t.boundsInLocal.width.toInt() * 4)

        ProjectRenderer.invoke(true) {
            it.gl.glBindTexture(GL.GL_TEXTURE_2D, textureID)
            it.gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, t.boundsInLocal.width.toInt(), t.boundsInLocal.height.toInt()
                    , 0, GL.GL_BGRA, GL.GL_UNSIGNED_BYTE, buf)
            it.gl.glBindTexture(GL.GL_TEXTURE_2D, 0)

            bufferSize = Size(w.width, w.height)
            true
        }
        displayName = "[テキスト] ${text.value.replace("\n", " ")}"
    }

    override fun onDraw(gl: GL2, mode: Drawable.DrawMode, frame: Int) {
        super.onDraw(gl, mode, frame)



        gl.glBindTexture(GL.GL_TEXTURE_2D, textureID)
        gl.glBegin(GL2.GL_QUADS)
        gl.glTexCoord2d(0.0, 1.0)
        gl.glVertex3d(-t.layoutBounds.width / 2.0, -t.layoutBounds.height / 2.0, 0.0)
        gl.glTexCoord2d(0.0, 0.0)
        gl.glVertex3d(-t.layoutBounds.width / 2.0, t.layoutBounds.height / 2.0, 0.0)
        gl.glTexCoord2d(1.0, 0.0)
        gl.glVertex3d(t.layoutBounds.width / 2.0, t.layoutBounds.height / 2.0, 0.0)
        gl.glTexCoord2d(1.0, 1.0)
        gl.glVertex3d(t.layoutBounds.width / 2.0, -t.layoutBounds.height / 2.0, 0.0)
        gl.glEnd()
        gl.glBindTexture(GL.GL_TEXTURE_2D, 0)
    }
}