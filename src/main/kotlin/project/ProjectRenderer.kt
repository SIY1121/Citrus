package project

import com.jogamp.opengl.GL2
import com.jogamp.opengl.GLAutoDrawable
import com.jogamp.opengl.GLEventListener
import com.jogamp.opengl.awt.GLJPanel
import com.jogamp.opengl.glu.GLU
import objects.Drawable
import ui.Main

class ProjectRenderer(var project: Project, glp : GLJPanel?) : GLEventListener {
    var selectedScene = 0

    var glPanel: GLJPanel? = glp
        set(value){
            field?.removeGLEventListener(this)
            value?.addGLEventListener(this)
            field = value
        }

    private var frame = 0

    lateinit var gl2 : GL2
    val glu = GLU()

    fun renderPreview(frame: Int) {
        this.frame = frame
        glPanel?.display()
        project.scene[selectedScene].getSamples(frame)
    }

    fun renderFinal(frame: Int) {
        this.frame = frame
    }

    fun updateObject(){

    }

    override fun reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) {
        gl2.glMatrixMode(GL2.GL_PROJECTION)
        gl2.glLoadIdentity()
        //gl2.glOrtho(-Statics.project.width/2.0,Statics.project.width/2.0,-Statics.project.height/2.0,Statics.project.height/2.0,0.1,100.0)
        glu.gluPerspective(90.0, Main.project.width.toDouble() / Main.project.height, 1.0, Main.project.width.toDouble())
        glu.gluLookAt(0.0, 0.0, Main.project.height / 2.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0)
    }

    override fun display(drawable: GLAutoDrawable) {
        gl2.glMatrixMode(GL2.GL_MODELVIEW)
        gl2.glLoadIdentity()
        gl2.glClear(GL2.GL_COLOR_BUFFER_BIT)
        project.scene[selectedScene].draw(gl2, Drawable.DrawMode.Preview, frame)
    }

    override fun init(drawable: GLAutoDrawable) {
        gl2 = drawable.gl.gL2
    }

    override fun dispose(drawable: GLAutoDrawable) {

    }

}