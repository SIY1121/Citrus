package objects

import annotation.CDroppable
import annotation.CObject
import annotation.CProperty
import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2
import com.jogamp.opengl.util.texture.Texture
import com.jogamp.opengl.util.texture.TextureIO
import javafx.stage.FileChooser
import properties.CFileProperty
import ui.GlCanvas
import java.io.File

@CObject("画像", "00796BFF", "img/ic_photo.png")
@CDroppable(["png", "jpg", "jpeg", "bmp", "gif", "tif"])
class Image(defLayer: Int, defScene: Int) : DrawableObject(defLayer,defScene) {


    override val id = "citrus/image"
    override val name = "画像"

    @CProperty("ファイル", 0)
    val file = CFileProperty(listOf(
            FileChooser.ExtensionFilter("画像ファイル", "*.png", "*.jpg", ".*jpeg", "*.bmp", "*.gif", "*.tif")
    ))

    var texture: Texture? = null

    init {
        file.valueProperty.addListener { _, _, n -> onFileLoad(n.toString()) }
        displayName = "[画像]"
    }

    override fun onFileDropped(file: String) {
        onFileLoad(file)
    }

    fun onFileLoad(file: String) {
        displayName = "[画像] ${File(file).name}"
        GlCanvas.instance.invoke(false, {
            texture = TextureIO.newTexture(File(file), false)
            texture?.enable(it.gl)
            false
        })
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