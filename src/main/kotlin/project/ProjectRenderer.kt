package project

import com.jogamp.opengl.*
import com.jogamp.opengl.awt.GLJPanel
import com.jogamp.opengl.glu.GLU
import kotlinx.coroutines.experimental.launch
import objects.Drawable
import org.bytedeco.javacpp.avcodec
import org.bytedeco.javacv.FFmpegFrameRecorder
import ui.Main
import java.io.File
import java.nio.*
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine
import com.jogamp.opengl.GLCapabilities
import java.awt.Dimension
import java.util.*
import kotlin.concurrent.schedule


class ProjectRenderer(var project: Project, glp: GLJPanel?) : GLEventListener {
    companion object {
        val glu = GLU()
        lateinit var instance: ProjectRenderer
        fun invoke(wait: Boolean, runnable: (drawable: GLAutoDrawable) -> Boolean): Boolean {
            return instance.glPanel?.invoke(wait, runnable) ?: false
        }
    }

    var selectedScene = 0

    var glPanel: GLJPanel? = glp
        set(value) {
            field?.removeGLEventListener(this)
            value?.addGLEventListener(this)
            value?.gl?.swapInterval = 0
            field = value
        }

    var frame = 0

    lateinit var gl2: GL2

    val audioLine: SourceDataLine

    var leftAudioLevel = 0.0
    var rightAudioLevel = 0.0

    var encoding = false
    var recorder: FFmpegFrameRecorder? = null

    val bufferFrameCount = 1

    var frameBufID = IntArray(bufferFrameCount)

    init {
        instance = this
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

        launch {
            val data = samples.map { Math.min(it * Short.MAX_VALUE, Short.MAX_VALUE.toFloat()).toShort() }.toShortArray().toByteArray()
            audioLine.write(data, 0, data.size)
        }

        leftAudioLevel = (samples.filterIndexed { index, _ -> index % 2 == 0 }.map { Math.abs(it.toDouble()) }.max()
                ?: 0.0)
        rightAudioLevel = (samples.filterIndexed { index, _ -> index % 2 == 1 }.map { Math.abs(it.toDouble()) }.max()
                ?: 0.0)
    }

    fun startEncode(infoCallback: EncodingInfoCallback) {
        //glPanelのサイズを大きくすることで強制的に描画範囲を広げる
        glPanel?.size = Dimension(project.width, project.height)
        launch {
            val endFrame = project.scene[selectedScene].flatten().maxBy { it.end }?.end
                    ?: throw Exception("オブジェクトの最終位置を見つけることができませんでした。")
            frame = 0
            encoding = true
            recorder = FFmpegFrameRecorder(File("out.mp4"), project.width, project.height)
            recorder?.frameRate = project.fps.toDouble()
            recorder?.sampleRate = project.sampleRate
            recorder?.videoQuality = 21.0
            recorder?.setVideoOption("preset", "medium")
            recorder?.videoCodecName = "h264"
            recorder?.setVideoOption("threads", "0")
            recorder?.audioBitrate = 192_000
            recorder?.audioChannels = project.audioChannel
            recorder?.audioCodec = avcodec.AV_CODEC_ID_AAC
            recorder?.start()


            val b = IntBuffer.allocate(bufferFrameCount)
            gl2.glGenRenderbuffers(bufferFrameCount, b)
            val renderBufID = b.array()

            val bb = IntBuffer.allocate(bufferFrameCount)
            gl2.glGenFramebuffers(bufferFrameCount, bb)
            frameBufID = bb.array()

            for (i in 0 until bufferFrameCount) {
                gl2.glBindRenderbuffer(GL2.GL_RENDERBUFFER, renderBufID[i])
                gl2.glRenderbufferStorage(GL2.GL_RENDERBUFFER, GL.GL_RGB, project.width, project.height)

                gl2.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBufID[i])
                gl2.glFramebufferRenderbuffer(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0, GL2.GL_RENDERBUFFER, renderBufID[i])
            }


            val task = Timer().schedule(0L, 1000L) {
                infoCallback.onProgress(frame, endFrame)
            }


            glPanel?.invoke(true) {
                var oldTime = 0L
                while (frame <= endFrame) {
                    oldTime = System.currentTimeMillis()

                    for (i in 0 until bufferFrameCount) {
                        gl2.glMatrixMode(GL2.GL_MODELVIEW)
                        gl2.glLoadIdentity()
                        //reshape(drawable, 0, 0, project.width, project.height)
                        gl2.glViewport(0, 0, project.width, project.height)
                        gl2.glScissor(0, 0, project.width, project.height)
                        //レンダリング用バッファをバインド
                        gl2.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBufID[i])
                        gl2.glMatrixMode(GL2.GL_MODELVIEW)
                        gl2.glLoadIdentity()//モデルビュー行列を初期化
                        gl2.glScaled(1.0, -1.0, 1.0)//出力後に反転するので予め反転させておく


                        gl2.glClearColor(0f, 0f, 0f, 1f)
                        gl2.glClear(GL2.GL_COLOR_BUFFER_BIT)
                        project.scene[selectedScene].draw(gl2, if (encoding) Drawable.DrawMode.Final else Drawable.DrawMode.Preview, frame)

                        gl2.glViewport(0, 0, Main.project.width, Main.project.height)//ビューポートを動画のサイズにセット

                        frame++
                    }




                    println("render ${System.currentTimeMillis() - oldTime}")
                    oldTime = System.currentTimeMillis()

                    for (i in 0 until bufferFrameCount) {
                        gl2.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBufID[i])

                        //レコーダーに流し込む
                        val buf = ByteBuffer.allocate(project.width * project.height * 3)
                        gl2.glReadBuffer(GL2.GL_COLOR_ATTACHMENT0)
                        gl2.glReadPixels(0, 0, project.width, project.height, GL.GL_BGR, GL.GL_UNSIGNED_BYTE, buf)
                        recorder?.recordImage(project.width, project.height, 8, 3, project.width * 3, -1, buf)

                    }

                    frame -= bufferFrameCount
                    println("record ${System.currentTimeMillis() - oldTime}")
                    oldTime = System.currentTimeMillis()

                    for (i in 0 until bufferFrameCount) {


                        //音声サンプルを取得
                        val samples = project.scene[selectedScene].getSamples(frame)
                        val audioBuf = FloatBuffer.allocate(samples.size).put(samples)
                        audioBuf.position(0)
                        recorder?.recordSamples(project.sampleRate, project.audioChannel, audioBuf)
                        gl2.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0)



                        frame++
                    }
                    println("audio ${System.currentTimeMillis() - oldTime}")
                    oldTime = System.currentTimeMillis()
                }

                false
            }
            task.cancel()
            encoding = false
            recorder?.stop()
            recorder?.release()
            glPanel?.invoke(true) {
                it.gl.glViewport(0, 0, glPanel?.surfaceWidth ?: 1, glPanel?.surfaceHeight ?: 1)
                true
            }
            infoCallback.onFinish()
        }
    }

    private fun renderFinal(frame: Int, recorder: FFmpegFrameRecorder) {
        this.frame = frame

    }

    fun updateObject() {
        project.scene.forEach { scene ->
            scene.forEach {
                it.currentObject = null
            }
        }
    }

    override fun reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) {
        gl2.glMatrixMode(GL2.GL_PROJECTION)
        gl2.glLoadIdentity()
        //gl2.glOrtho(-Statics.project.width/2.0,Statics.project.width/2.0,-Statics.project.height/2.0,Statics.project.height/2.0,0.1,100.0)
        glu.gluPerspective(90.0, Main.project.width.toDouble() / Main.project.height, 1.0, Main.project.width.toDouble())
        glu.gluLookAt(0.0, 0.0, Main.project.height / 2.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0)
    }

    override fun display(drawable: GLAutoDrawable) {
        println("display")
        gl2.glMatrixMode(GL2.GL_MODELVIEW)
        gl2.glLoadIdentity()

        //reshape(drawable, 0, 0, glPanel?.width ?: 0, glPanel?.height ?: 0)
        gl2.glViewport(0, 0, glPanel?.width ?: 0, glPanel?.height ?: 0)


        gl2.glClearColor(0f, 0f, 0f, 1f)
        gl2.glClear(GL2.GL_COLOR_BUFFER_BIT)
        project.scene[selectedScene].draw(gl2, if (encoding) Drawable.DrawMode.Final else Drawable.DrawMode.Preview, frame)

    }

    override fun init(drawable: GLAutoDrawable) {
        gl2 = drawable.gl.gL2
        gl2.swapInterval = 0
        gl2.glDisable(GL2.GL_DEPTH_TEST)
        gl2.glEnable(GL2.GL_TEXTURE_2D)
        gl2.glEnable(GL2.GL_BLEND)
        gl2.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA)
        gl2.glClearColor(0f, 0f, 0f, 1f)
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