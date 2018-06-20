package objects

import annotation.CObject
import annotation.CProperty
import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2
import org.opencv.core.Size
import project.ProjectRenderer
import project.ProjectRenderer.Companion.glu
import properties.CAnimatableDoubleProperty
import ui.Main
import util.UGenFrameBuffer
import util.UGenTexture
import util.USetupTexture

/**
 * 描画が発生する小向ジェクトの親クラス
 * 座標、拡大率、透明度などをもつ
 */
@CObject("描画")
abstract class DrawableObject(defLayer: Int, defScene: Int) : CitrusObject(defLayer, defScene), Drawable {
    var selected: Boolean = false
    var enabledSelectedOutline: Boolean = true

    var bufferSize: Size = Size(Main.project.width.toDouble(), Main.project.height.toDouble())
        set(value) {
            field = value
            initFrameBuffer()
        }

    var frameBufferID = 0
    var textureBufferID = 0

    var setupFinished = false

    @CProperty("X", 0)
    val x = CAnimatableDoubleProperty()
    @CProperty("Y", 1)
    val y = CAnimatableDoubleProperty()
    @CProperty("Z", 2)
    val z = CAnimatableDoubleProperty()

    @CProperty("拡大率", 3)
    val scale = CAnimatableDoubleProperty(0.0, 10.0, tick = 0.05, def = 1.0)
    @CProperty("透明度", 4)
    val alpha = CAnimatableDoubleProperty(0.0, 1.0, 1.0, 0.01)
    @CProperty("回転", 5)
    val rotate = CAnimatableDoubleProperty()

    protected fun initFrameBuffer() {
        ProjectRenderer.invoke(true) {
            val gl = it.gl.gL2
            textureBufferID = gl.UGenTexture()
            gl.USetupTexture(textureBufferID, bufferSize.width.toInt(), bufferSize.height.toInt())
            frameBufferID = gl.UGenFrameBuffer()
            gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBufferID)
            gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0, GL.GL_TEXTURE_2D, textureBufferID, 0)
            gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0)
            setupFinished = true
            true
        }

    }

    override fun draw(gl: GL2, mode: Drawable.DrawMode, frame: Int) {
        if (!setupFinished) return

        gl.glMatrixMode(GL2.GL_MODELVIEW)
        gl.glPushMatrix()
        gl.glLoadIdentity()
        gl.glMatrixMode(GL2.GL_PROJECTION)
        gl.glPushMatrix()
        gl.glLoadIdentity()

        glu.gluPerspective(90.0, bufferSize.width / bufferSize.height, 1.0, bufferSize.width)
        glu.gluLookAt(0.0, 0.0, bufferSize.height / 2.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0)
        gl.glViewport(0, 0, bufferSize.width.toInt(), bufferSize.height.toInt())
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBufferID)

        gl.glClearColor(0f, 0f, 0f, 0f)
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT)

        onDraw(gl, mode, frame)
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0)

        gl.glMatrixMode(GL2.GL_PROJECTION)
        gl.glPopMatrix()
        gl.glMatrixMode(GL2.GL_MODELVIEW)
        gl.glPopMatrix()

        if (mode == Drawable.DrawMode.Preview)
            gl.glViewport(0, 0, ProjectRenderer.instance.glPanel?.width ?: 0, ProjectRenderer.instance.glPanel?.height
                    ?: 0)
        else
            gl.glViewport(0, 0, Main.project.width, Main.project.height)
//
        gl.glTranslated(x.value.toDouble(), y.value.toDouble(), z.value.toDouble())
        gl.glRotated(rotate.value.toDouble(), 0.0, 0.0, 1.0)
        gl.glScaled(scale.value.toDouble(), scale.value.toDouble(), scale.value.toDouble())
        gl.glBindTexture(GL.GL_TEXTURE_2D, textureBufferID)

        gl.glBegin(GL2.GL_QUADS)
        gl.glTexCoord2d(0.0, 0.0)
        gl.glVertex3d(-bufferSize.width / 2.0, -bufferSize.height / 2.0, 0.0)
        gl.glTexCoord2d(0.0, 1.0)
        gl.glVertex3d(-bufferSize.width / 2.0, bufferSize.height / 2.0, 0.0)
        gl.glTexCoord2d(1.0, 1.0)
        gl.glVertex3d(bufferSize.width / 2.0, bufferSize.height / 2.0, 0.0)
        gl.glTexCoord2d(1.0, 0.0)
        gl.glVertex3d(bufferSize.width / 2.0, -bufferSize.height / 2.0, 0.0)
        gl.glEnd()

        gl.glMatrixMode(GL2.GL_MODELVIEW)
        gl.glPopMatrix()
    }

    open fun onDraw(gl: GL2, mode: Drawable.DrawMode, frame: Int) {

        gl.glColor4d(1.0, 1.0, 1.0, alpha.value.toDouble())
        if (mode == Drawable.DrawMode.Preview && enabledSelectedOutline && selected) {

        }
        gl.glBindTexture(GL2.GL_TEXTURE_2D, 0)
    }
}