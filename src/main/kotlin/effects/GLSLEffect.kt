package effects

import ui.GlCanvas
import java.nio.IntBuffer

abstract class GLSLEffect{
    val frameBufferID :Int
    init{
        val buf = IntBuffer.allocate(1)
        GlCanvas.gl2.glGenFramebuffers(1,buf)
        frameBufferID = buf.get()

        println("init effect")
    }

     abstract fun onDraw()
}