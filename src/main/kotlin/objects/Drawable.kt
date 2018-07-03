package objects

import com.jogamp.opengl.GL2

/**
 * 描画可能オブジェクト
 */
interface Drawable {
    enum class DrawMode {
        Preview, Final
    }

    fun draw(gl: GL2, mode: DrawMode,frame : Int)
}