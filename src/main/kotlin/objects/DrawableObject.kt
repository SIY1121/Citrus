package objects

import annotation.CObject
import annotation.CProperty
import com.jogamp.opengl.GL2
import properties.CAnimatableDoubleProperty

/**
 * 描画が発生する小向ジェクトの親クラス
 * 座標、拡大率、透明度などをもつ
 */
@CObject("描画")
abstract class DrawableObject(defLayer: Int, defScene: Int) : CitrusObject(defLayer,defScene), Drawable {
    var selected: Boolean = false
    var enabledSelectedOutline: Boolean = true

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

    override fun draw(gl: GL2, mode: Drawable.DrawMode, frame: Int) {
        gl.glPushMatrix()
        onDraw(gl, mode, frame)
        gl.glPopMatrix()
    }

    open fun onDraw(gl: GL2, mode: Drawable.DrawMode, frame: Int) {
        gl.glTranslated(x.value.toDouble(), y.value.toDouble(), z.value.toDouble())
        gl.glRotated(rotate.value.toDouble(), 0.0, 0.0, 1.0)
        gl.glScaled(scale.value.toDouble(), scale.value.toDouble(), scale.value.toDouble())
        gl.glColor4d(1.0, 1.0, 1.0, alpha.value.toDouble())
        if (mode == Drawable.DrawMode.Preview && enabledSelectedOutline && selected) {

        }
        gl.glBindTexture(GL2.GL_TEXTURE_2D, 0)
    }
}