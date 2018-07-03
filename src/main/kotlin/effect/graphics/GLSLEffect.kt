package effect.graphics

import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2
import effect.Effect
import objects.DrawableObject
import util.*

abstract class GLSLEffect(val drawable: DrawableObject, val gl: GL2) : Effect() , DrawableEffect{
    val frameBufferID: Int
    val textureBufferID: Int
    var program: Int = 0

    init {
        textureBufferID = gl.UGenTexture()
        gl.USetupTexture(textureBufferID, drawable.bufferSize.width.toInt(), drawable.bufferSize.width.toInt())
        frameBufferID = gl.UGenFrameBuffer()
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBufferID)
        gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0, GL.GL_TEXTURE_2D, textureBufferID, 0)
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0)
    }

    protected fun initProgram(vertexShader: String, fragmentShader: String) {
        program = gl.UCreateProgram(
                gl.UCreateVertexShader(
                        vertexShader
                ),
                gl.UCreateFragmentShader(
                        fragmentShader
                )
        )
    }

}