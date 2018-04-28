package objects

import com.jogamp.opengl.GL2

interface Drawable {
    enum class DrawMode {
        Preview, Final
    }

    fun draw(gl: GL2, mode: DrawMode)
}