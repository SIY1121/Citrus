package ui

import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2
import com.jogamp.opengl.GLAutoDrawable
import com.jogamp.opengl.GLEventListener
import com.jogamp.opengl.awt.GLJPanel
import com.jogamp.opengl.glu.GLU
import com.jogamp.opengl.util.FPSAnimator
import objects.CitrusObject
import util.Statics
import util.VideoRenderer
import java.nio.ByteBuffer
import java.nio.IntBuffer

class GlCanvas : GLJPanel(), GLEventListener {

    var currentFrame = 0
        set(value) {
            field = value
            for (i in 0 until Statics.project.Layer.size) {
                //i番目のレイヤーで、前回のオブジェクトの範囲を外れた場合か、そもそもなかった場合
                //新たなオブジェクトを検索
                if (currentObjects[i]?.isActive(field) != true) {
                    currentObjects.remove(i)
                    //println("remove$i")
                    for (o in Statics.project.Layer[i]) {
                        //新たなオブジェクトを見つけた場合
                        //セット
                        if (o.isActive(field)) {
                            currentObjects[i] = o
                            break
                        }
                    }
                }
            }
        }


    val animator = FPSAnimator(Statics.project.fps)

    companion object {
        lateinit var instance: GlCanvas
        lateinit var gl2: GL2
        val glu = GLU()
    }

    val currentObjects: HashMap<Int, CitrusObject> = HashMap()

    var frameBufID = 0
    var renderBufID = 0
    var rendering = false

    init {
        instance = this
        addGLEventListener(this)
    }

    override fun init(p0: GLAutoDrawable) {
        println("init")
        gl2 = p0.gl.gL2
        gl2.glDisable(GL2.GL_DEPTH_TEST)
        gl2.glEnable(GL2.GL_TEXTURE_2D)
        gl2.glEnable(GL2.GL_BLEND)
        gl2.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA)
        gl2.glClearColor(0f, 0f, 0f, 1f)


        val b = IntBuffer.allocate(1)
        gl2.glGenRenderbuffers(1, b)
        renderBufID = b.get()

        val bb = IntBuffer.allocate(1)
        gl2.glGenFramebuffers(1, bb)
        frameBufID = bb.get()

        gl2.glBindRenderbuffer(GL2.GL_RENDERBUFFER, renderBufID)
        gl2.glRenderbufferStorage(GL2.GL_RENDERBUFFER, GL.GL_RGB, Statics.project.width, Statics.project.height)

        gl2.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBufID)
        gl2.glFramebufferRenderbuffer(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0, GL2.GL_RENDERBUFFER, renderBufID)


        animator.add(p0)
        animator.start()
    }

    override fun reshape(p0: GLAutoDrawable?, p1: Int, p2: Int, p3: Int, p4: Int) {
        gl2.glMatrixMode(GL2.GL_PROJECTION)
        gl2.glLoadIdentity()
        //gl2.glOrtho(-Statics.project.width/2.0,Statics.project.width/2.0,-Statics.project.height/2.0,Statics.project.height/2.0,0.1,100.0)
        glu.gluPerspective(90.0, Statics.project.width.toDouble() / Statics.project.height, 1.0, Statics.project.width.toDouble())
        glu.gluLookAt(0.0, 0.0, Statics.project.height / 2.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0)
    }

    override fun display(p0: GLAutoDrawable?) {

        gl.glClear(GL2.GL_COLOR_BUFFER_BIT)
        gl2.glMatrixMode(GL2.GL_MODELVIEW)
        gl2.glLoadIdentity()


        if (rendering) {
            gl2.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBufID)
            gl2.glDrawBuffer(GL2.GL_COLOR_ATTACHMENT0)
            gl2.glViewport(0,0,Statics.project.width,Statics.project.height)
            gl2.glScaled(1.0,-1.0,1.0)
            gl.glClear(GL2.GL_COLOR_BUFFER_BIT)
        }

        for (o in currentObjects)
            o.value.onSuperFrame(currentFrame)

        if (rendering) {
            val buf = ByteBuffer.allocate(Statics.project.width * Statics.project.height * 3)
            gl2.glReadBuffer(GL2.GL_COLOR_ATTACHMENT0)
            gl2.glReadPixels(0, 0, Statics.project.width, Statics.project.height, GL.GL_BGR, GL.GL_UNSIGNED_BYTE, buf)
            VideoRenderer.recordFrame(buf)
        }

    }

    override fun dispose(p0: GLAutoDrawable?) {

    }

}