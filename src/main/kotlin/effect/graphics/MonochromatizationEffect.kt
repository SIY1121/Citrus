package effect.graphics

import annotation.CEffect
import com.jogamp.opengl.GL2
import objects.DrawableObject
import java.nio.FloatBuffer

@CEffect("単色化")
class MonochromatizationEffect(drawable: DrawableObject, gl: GL2) : GLSLEffect(drawable, gl) {


    init {
        initProgram(
                "attribute vec3 position;\n" +
                        " attribute vec2 uv;\n" +
                        " varying vec2 vuv;\n" +
                        " void main(void){\n" +
                        "     gl_Position = vec4(position, 1.0);\n" +
                        "      vuv = uv;\n" +
                        " }",

                "varying vec2 vuv;\n" +
                        "uniform sampler2D texture;\n" +
                        "void main(void){\n" +
                        "       gl_FragColor = texture2D(texture, vuv);\n" +
                        "}"
        )
        println("program ID : $program")
    }

    override fun onDraw(texId: Int): Int {
        gl.glUseProgram(program)

        val positionLocation = gl.glGetAttribLocation(program, "position")
        val uvLocation = gl.glGetAttribLocation(program, "uv")
        val textureLocation = gl.glGetUniformLocation(program, "texture")

        gl.glEnableVertexAttribArrayARB(positionLocation)
        gl.glEnableVertexAttribArrayARB(uvLocation)

        gl.glUniform1i(textureLocation, texId)

        gl.glVertexAttribPointerARB(positionLocation, 2, GL2.GL_FLOAT, false, 0, FloatBuffer.wrap(
                floatArrayOf(
                        drawable.bufferSize.width.toFloat(), drawable.bufferSize.height.toFloat(),
                        -drawable.bufferSize.width.toFloat(), drawable.bufferSize.height.toFloat(),
                        -drawable.bufferSize.width.toFloat(), -drawable.bufferSize.height.toFloat(),
                        drawable.bufferSize.width.toFloat(), -drawable.bufferSize.height.toFloat())
        )
        )
        gl.glVertexAttribPointerARB(uvLocation, 2, GL2.GL_FLOAT, false, 0, FloatBuffer.wrap(
                floatArrayOf(
                        1f, 0f,
                        0f, 0f,
                        0f, 1f,
                        1f, 1f
                ))
        )

        gl.glBindTexture(GL2.GL_TEXTURE_2D,texId)

        gl.glBegin(GL2.GL_QUADS)
        gl.glTexCoord2d(0.0, 0.0)
        gl.glVertex3d(-drawable.bufferSize.width / 2.0, -drawable.bufferSize.height / 2.0, 0.0)
        gl.glTexCoord2d(0.0, 1.0)
        gl.glVertex3d(-drawable.bufferSize.width / 2.0, drawable.bufferSize.height / 2.0, 0.0)
        gl.glTexCoord2d(1.0, 1.0)
        gl.glVertex3d(drawable.bufferSize.width / 2.0, drawable.bufferSize.height / 2.0, 0.0)
        gl.glTexCoord2d(1.0, 0.0)
        gl.glVertex3d(drawable.bufferSize.width / 2.0, -drawable.bufferSize.height / 2.0, 0.0)
        gl.glEnd()

        gl.glUseProgram(0)
        gl.glBindTexture(GL2.GL_TEXTURE_2D,0)
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0)
        return textureBufferID
    }
}