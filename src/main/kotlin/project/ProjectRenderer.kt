package project

import com.jogamp.opengl.GL2
import com.jogamp.opengl.awt.GLJPanel
import objects.Drawable

class ProjectRenderer(val project: Project,val glPanel : GLJPanel) {
    var selectedScene = 0

    fun renderPreview(frame : Int){
        glPanel.invoke(true,{drawable->
            val gl2 = drawable.gl.gL2
            gl2.glMatrixMode(GL2.GL_MODELVIEW)
            gl2.glLoadIdentity()
            gl2.glClear(GL2.GL_COLOR_BUFFER_BIT)
            project.scene[selectedScene].draw(gl2,Drawable.DrawMode.Preview,frame)
            false
        })
        project.scene[selectedScene].getSamples(frame)

    }
    fun renderFinal(frame : Int){

    }
}