package effect.graphics

import annotation.CEffect
import com.jogamp.opengl.GL2
import objects.CitrusObject
import objects.DrawableObject
import java.nio.FloatBuffer

@CEffect("単色化")
class MonochromatizationEffect(drawable: DrawableObject, gl: GL2, parent: CitrusObject) : GLSLEffect(drawable, gl, parent) {


    init {
        initProgram(
                """
attribute vec3 position;
attribute vec2 uv;
varying vec2 vuv;
void main(void){
    //gl_Position = vec4(position, 1.0);
    //gl_Position =  gl_ModelViewMatrix * gl_Vertex;
    gl_Position = vec4(position, 1.0);
    vuv = uv;
}
                """.trimIndent(),

                """
varying vec2 vuv;
uniform sampler2D texture;
void main(void){
vec4 color = texture2D(texture, vuv);
float a = (color.x + color.y + color.z)/3.0;
gl_FragColor = vec4(a,a,a,0.0);
}

                """.trimIndent()
        )
        println("program ID : $program")
    }

    override fun onDraw(texId: Int): Int {

        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBufferID)

        gl.glMatrixMode(GL2.GL_PROJECTION)
        gl.glLoadIdentity()
        gl.glMatrixMode(GL2.GL_MODELVIEW)
        gl.glLoadIdentity()


        gl.glUseProgram(program)
        gl.glBindTexture(GL2.GL_TEXTURE_2D, 0)
        val uv = floatArrayOf(
                1f, 1f,
                0f, 1f,
                0f, 0f,
                1f, 0f
        )

        val pos = floatArrayOf(
                0.5f, 0.5f,
                -0.5f, 0.5f,
                -0.5f, -0.5f,
                0.5f, -0.5f
        )

        val uvLocation = gl.glGetAttribLocation(program, "uv")
        val positionLocation = gl.glGetAttribLocation(program, "position")

        gl.glEnableVertexAttribArray(uvLocation)
        gl.glEnableVertexAttribArray(positionLocation)

        gl.glVertexAttribPointerARB(uvLocation, 2, GL2.GL_FLOAT, false, 0, FloatBuffer.wrap(uv))
        gl.glVertexAttribPointerARB(positionLocation, 2, GL2.GL_FLOAT, false, 0, FloatBuffer.wrap(pos))

        val textureLocation = gl.glGetUniformLocation(program, "texture")
        gl.glUniform1i(textureLocation, texId)

        gl.glClear(GL2.GL_COLOR_BUFFER_BIT)
        gl.glMatrixMode(GL2.GL_MODELVIEW)
        gl.glLoadIdentity()

        //gl.glRotated(   90.0,0.0, 0.0, 1.0)
        //gl.glScaled(1.5,1.5,1.5)
        gl.glBindTexture(GL2.GL_TEXTURE_2D, texId)

        gl.glDrawArrays(GL2.GL_TRIANGLE_FAN, 0, 4)

        gl.glUseProgram(0)
        gl.glBindTexture(GL2.GL_TEXTURE_2D, 0)
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0)
        return textureBufferID
    }
}