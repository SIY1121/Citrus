package project

import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2
import com.jogamp.opengl.GLAutoDrawable
import com.jogamp.opengl.GLEventListener
import com.jogamp.opengl.awt.GLJPanel
import com.jogamp.opengl.glu.GLU
import kotlinx.coroutines.experimental.launch
import objects.Drawable
import org.bytedeco.javacpp.avcodec
import org.bytedeco.javacv.FFmpegFrameRecorder
import ui.Main
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import java.nio.ShortBuffer
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

class ProjectRenderer(var project: Project, glp: GLJPanel?) : GLEventListener {
    var selectedScene = 0

    var glPanel: GLJPanel? = glp
        set(value) {
            field?.removeGLEventListener(this)
            value?.addGLEventListener(this)
            field = value
        }

    private var frame = 0

    lateinit var gl2: GL2
    val glu = GLU()

    val audioLine: SourceDataLine

    var leftAudioLevel = 0.0
    var rightAudioLevel = 0.0

    var encoding = false
    var recorder: FFmpegFrameRecorder? = null

    init {
        val audioFormat = AudioFormat(project.sampleRate.toFloat(), 16, project.audioChannel, true, false)
        val info = DataLine.Info(SourceDataLine::class.java, audioFormat)
        audioLine = AudioSystem.getLine(info) as SourceDataLine
        audioLine.open(audioFormat)
        audioLine.start()
    }

    fun renderPreview(frame: Int) {
        this.frame = frame
        glPanel?.display()
        val samples = project.scene[selectedScene].getSamples(frame)

        launch{
            val data = samples.toByteArray()
            audioLine.write(data, 0, data.size)
        }

        leftAudioLevel = Math.log(samples.filterIndexed { index, _ -> index % 2 == 0 }.map { Math.abs(it.toDouble()) / Short.MAX_VALUE }.average()
                ?: 0.01) * 20
        rightAudioLevel = Math.log(samples.filterIndexed { index, _ -> index % 2 == 1 }.map { Math.abs(it.toDouble()) / Short.MAX_VALUE }.average()
                ?: 0.01) * 20
    }

    fun startEncode(infoCallcack: EncodingInfoCallback) {
        launch {
            val endFrame = project.scene[selectedScene].flatten().maxBy { it.end }?.end
                    ?: throw Exception("オブジェクトの最終位置を見つけることができませんでした。")
            frame = 0
            encoding = true
            recorder = FFmpegFrameRecorder(File("out.mp4"), project.width, project.height)
            recorder?.frameRate = project.fps.toDouble()
            recorder?.sampleRate = project.sampleRate
            recorder?.videoBitrate = 10000000
            recorder?.videoCodecName = "h264_nvenc"
            recorder?.audioBitrate = 192_000
            recorder?.audioChannels = project.audioChannel
            recorder?.audioCodec = avcodec.AV_CODEC_ID_AAC
            recorder?.start()

            while (frame <= endFrame) {
                glPanel?.display()

                val samples = project.scene[selectedScene].getSamples(frame)
                val buf = ShortBuffer.allocate(samples.size).put(samples)
                buf.position(0)
                recorder?.recordSamples(project.sampleRate, project.audioChannel, buf)

                infoCallcack.onProgress(frame, endFrame)
                frame++
            }
            encoding = false
            recorder?.stop()
            recorder?.release()
            glPanel?.invoke(true, {
                it.gl.glViewport(0, 0, glPanel?.surfaceWidth ?: 1, glPanel?.surfaceHeight ?: 1)
                true
            })
            infoCallcack.onFinish()
        }
    }

    fun renderFinal(frame: Int) {
        this.frame = frame
    }

    fun updateObject() {

    }

    private var frameBufID = 0
    private var renderBufID = 0

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

        if (encoding) {
            gl2.glViewport(0, 0, Main.project.width, Main.project.height)
            gl2.glScaled(1.0, -1.0, 1.0)
            gl2.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBufID)
            gl2.glDrawBuffer(GL2.GL_COLOR_ATTACHMENT0)
        }

        gl2.glClear(GL2.GL_COLOR_BUFFER_BIT)
        project.scene[selectedScene].draw(gl2, Drawable.DrawMode.Preview, frame)

        if (encoding) {
            val buf = ByteBuffer.allocate(project.width * project.height * 3)
            gl2.glReadBuffer(GL2.GL_COLOR_ATTACHMENT0)
            gl2.glReadPixels(0, 0, project.width, project.height, GL.GL_BGR, GL.GL_UNSIGNED_BYTE, buf)
            recorder?.recordImage(project.width, project.height, 8, 3, project.width * 3, -1, buf)
        }
    }

    override fun init(drawable: GLAutoDrawable) {
        gl2 = drawable.gl.gL2
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
        gl2.glRenderbufferStorage(GL2.GL_RENDERBUFFER, GL.GL_RGB, Main.project.width, Main.project.height)

        gl2.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBufID)
        gl2.glFramebufferRenderbuffer(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0, GL2.GL_RENDERBUFFER, renderBufID)
    }

    override fun dispose(drawable: GLAutoDrawable) {

    }


    //ShortArrayをリトルエンディアンでbyte配列に変換
    private fun ShortArray.toByteArray(): ByteArray {
        val byteBuffer = ByteBuffer.allocate(this.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        byteBuffer.asShortBuffer().put(this)
        return byteBuffer.array()
    }

    interface EncodingInfoCallback {
        fun onInfo(msg: String)
        fun onProgress(progress: Int, max: Int)
        fun onFinish()
    }
}