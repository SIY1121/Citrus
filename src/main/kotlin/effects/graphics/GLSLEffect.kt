package effects.graphics

import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2
import objects.Drawable
import ui.Main
import java.nio.IntBuffer

abstract class GLSLEffect(val drawable: Drawable, val gl2: GL2) {
    val frameBufferID: Int
    val textureBufferID: Int

    init {
        val buf = IntBuffer.allocate(1)
        gl2.glGenFramebuffers(1, buf)
        frameBufferID = buf.get()

        val buf2 = IntBuffer.allocate(1)
        gl2.glGenTextures(GL.GL_TEXTURE_2D, buf2)
        textureBufferID = buf.get()
        gl2.glBindTexture(GL.GL_TEXTURE_2D, textureBufferID)
        gl2.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, Main.project.width, Main.project.height, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, 0)
        gl2.glTextureParameteriEXT(textureBufferID,GL.GL_TEXTURE_2D,GL.GL_TEXTURE_WRAP_S,GL.GL_CLAMP_TO_EDGE)
        gl2.glTextureParameteriEXT(textureBufferID,GL.GL_TEXTURE_2D,GL.GL_TEXTURE_WRAP_T,GL.GL_CLAMP_TO_EDGE)
        gl2.glTextureParameteriEXT(textureBufferID,GL.GL_TEXTURE_2D,GL.GL_TEXTURE_MIN_FILTER,GL.GL_LINEAR)
        gl2.glTextureParameteriEXT(textureBufferID,GL.GL_TEXTURE_2D,GL.GL_TEXTURE_MAG_FILTER,GL.GL_LINEAR)

        gl2.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBufferID)
        gl2.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0, GL.GL_TEXTURE_2D, textureBufferID, 0)

        println("init effect")
    }

    abstract fun onDraw()
}