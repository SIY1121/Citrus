package objects

import annotation.CDroppable
import annotation.CObject
import annotation.CProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2
import com.jogamp.opengl.util.texture.Texture
import com.jogamp.opengl.util.texture.TextureIO
import javafx.application.Platform
import javafx.stage.FileChooser
import org.opencv.core.Size
import project.ProjectRenderer
import properties.CFileProperty
import ui.WindowFactory
import java.io.File

@CObject("画像", "00796BFF", "img/ic_photo.png")
@CDroppable(["png", "jpg", "jpeg", "bmp", "gif", "tif"])
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
class Image(defLayer: Int, defScene: Int) : DrawableObject(defLayer, defScene) {

    //Jackson用
    constructor():this(-1,-1)

    override val id = "citrus/image"
    override val name = "画像"

    @CProperty("ファイル", 0)
    val file = CFileProperty(listOf(
            FileChooser.ExtensionFilter("画像ファイル", "*.png", "*.jpg", ".*jpeg", "*.bmp", "*.gif", "*.tif")
    ))

    private var texture: Texture? = null

    init {
        file.valueProperty.addListener { _, _, n -> onFileLoad(n.toString()) }
        displayName = "[画像]"
    }

    override fun onFileDropped(f: String) {
        file.value = f
    }

    fun onFileLoad(file: String) {
        displayName = "[画像] ${File(file).name}"
        ProjectRenderer.invoke(false) {
            texture = TextureIO.newTexture(File(file), false)
            texture?.enable(it.gl)
            bufferSize = Size(texture?.width?.toDouble() ?: 0.0, texture?.height?.toDouble() ?: 0.0)
            false
        }
    }

    override fun onDraw(gl: GL2, mode: Drawable.DrawMode, frame: Int) {
        super.onDraw(gl, mode, frame)

        val tex = texture ?: return

        tex.bind(gl)

        gl.glBegin(GL2.GL_QUADS)
        gl.glTexCoord2d(0.0, 0.0)
        gl.glVertex3d(-tex.width / 2.0, -tex.height / 2.0, 0.0)
        gl.glTexCoord2d(0.0, 1.0)
        gl.glVertex3d(-tex.width / 2.0, tex.height / 2.0, 0.0)
        gl.glTexCoord2d(1.0, 1.0)
        gl.glVertex3d(tex.width / 2.0, tex.height / 2.0, 0.0)
        gl.glTexCoord2d(1.0, 0.0)
        gl.glVertex3d(tex.width / 2.0, -tex.height / 2.0, 0.0)
        gl.glEnd()
        gl.glBindTexture(GL.GL_TEXTURE_2D, 0)
    }

}