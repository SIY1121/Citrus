package objects

import annotation.CObject
import annotation.CProperty
import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2
import javafx.stage.FileChooser
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.FrameGrabber
import org.jtransforms.fft.FloatFFT_1D
import project.ProjectRenderer
import properties.CAnimatableDoubleProperty
import properties.CColorProperty
import properties.CFileProperty
import properties.CIntegerProperty
import ui.Main
import ui.TimelineController
import java.nio.FloatBuffer
import java.nio.IntBuffer

@CObject("波形", "00796BFF", "img/ic_music.png")
class AudioWaveform(defLayer: Int, defScene: Int) : DrawableObject(defLayer, defScene) {
    @CProperty("ファイル", 0)
    val file = CFileProperty(listOf(FileChooser.ExtensionFilter("音声ファイル", listOf("*.ac3", "*.aac", ".adts", "*.aif", "*.aiff", "*.afc", "*.aifc", "*.amr", "*.au", "*.bit", "*.caf", "*.dts", "*.eac3", "*.flac", "*.g722", "*.tco", "*.rco", "*.gsm", "*.lbc", "*.latm", "*.loas", "*.mka", "*.mp2", "*.m2a", "*.mpa", "*.mp3", "*.oga", "*.oma", "*.opus", "*.spx", "*.tta", "*.voc", "*.wav", "*.wv"))))
    @CProperty("開始位置", 1)
    val startPos = CIntegerProperty(min = 0)
    @CProperty("色", 2)
    val color = CColorProperty()
    @CProperty("線幅", 3)
    val lineWidth = CAnimatableDoubleProperty(0.0, 100.0, 2.0, 1.0)
    @CProperty("最大表示周波数", 4)
    val maxHz = CAnimatableDoubleProperty(1.0, Double.POSITIVE_INFINITY, 1000.0, 1.0)


    var grabber: FFmpegFrameGrabber? = null
    var isGrabberStarted = false
    var oldFrame = 0
    private var buf: Frame? = null
    var vbo = 0
    var samplesPerFrame = 0


    init {
        file.valueProperty.addListener { _, _, n -> onFileLoad(n) }
        ProjectRenderer.invoke(true, {
            val gl2 = it.gl.gL2
            val b = IntBuffer.allocate(1)
            gl2.glGenBuffers(1, b)
            vbo = b.get()


            false
        })
    }

    override fun onFileDropped(file: String) {
        onFileLoad(file)
    }

    private fun onFileLoad(file: String) {
        grabber = FFmpegFrameGrabber(file)
        grabber?.sampleMode = FrameGrabber.SampleMode.FLOAT
        grabber?.audioChannels = 1
        grabber?.start()
        samplesPerFrame = (grabber?.sampleRate ?: Main.project.sampleRate) * (grabber?.audioChannels
                ?: 2) / Main.project.fps
        isGrabberStarted = true
    }

    override fun onDraw(gl: GL2, mode: Drawable.DrawMode, frame: Int) {
        super.onDraw(gl, mode, frame)
        if (isGrabberStarted) {
            gl.glEnableClientState(GL2.GL_VERTEX_ARRAY)
            gl.glLineWidth(lineWidth.value.toFloat())
            gl.glColor4d(color.value.red, color.value.green, color.value.blue, alpha.value.toDouble())
            val samples = getSamples(frame)
            if (samples.isEmpty()) return
            val fft = FloatFFT_1D(samples.size.toLong())
            fft.realForward(samples)
            val level = FloatArray(samples.size / 2)

            for (i in 0 until level.size) {
                level[i] = Math.sqrt(Math.pow(samples[i * 2].toDouble(), 2.0) + Math.pow(samples[i * 2 + 1].toDouble(), 2.0)).toFloat()
            }
            val max = level.max() ?: 1f
            for (i in 0 until level.size)
                level[i] /= max

            val samplePerHz = (grabber?.sampleRate ?: samples.size) / samples.size

            val buf = FloatBuffer.allocate((maxHz.value.toInt() / samplePerHz) * 6)
            for (i in 0 until buf.limit() / 6) {
                val x = i / (buf.limit()/6f) * Main.project.width - Main.project.width / 2f
                buf.put(x)
                buf.put(-Main.project.height / 2f)
                buf.put(0f)
                buf.put(x)
                buf.put(level[i] * Main.project.height - Main.project.height / 2f)
                buf.put(0f)
            }
            buf.position(0)

            gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vbo)
            gl.glBufferData(GL2.GL_ARRAY_BUFFER, buf.limit().toLong() * 4, buf, GL2.GL_DYNAMIC_DRAW)
            gl.glVertexPointer(3, GL.GL_FLOAT, 0, 0)
            gl.glDrawArrays(GL2.GL_LINES, 0, buf.limit() / 3)
            gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0)
        }


    }

    var audioBuf: FloatBuffer? = null
    private fun getSamples(frame: Int): FloatArray {
        if (isGrabberStarted) {
            if (frame == 0) {
                grabber?.timestamp = startPos.value.toLong()
                oldFrame = 0
                audioBuf = null
            } else
                if (oldFrame != frame) {
                    val now = ((frame + startPos.value.toInt()) * (1.0 / Main.project.fps) * 1000 * 1000).toLong()

                    val requiredSamples = if (frame - oldFrame in 1..99) (frame - oldFrame) * samplesPerFrame else samplesPerFrame
                    val result = FloatArray(requiredSamples)
                    var readed = 0

                    if (Math.abs(frame + startPos.value.toInt() - oldFrame) >= 100 || frame + startPos.value.toInt() < oldFrame) {
                        TimelineController.wait = true
                        grabber?.sampleMode = FrameGrabber.SampleMode.FLOAT
                        grabber?.timestamp = now - (1.0 / Main.project.fps * 1000 * 1000).toLong()
                        TimelineController.wait = false
                        buf = grabber?.grabSamples()
                    }

                    while (readed < requiredSamples) {
                        if (audioBuf?.remaining() == 0 || audioBuf == null)//バッファが空orNullだったら
                            buf = grabber?.grabSamples()//デコード

                        audioBuf = (buf?.samples?.get(0) as FloatBuffer)
                        val read = Math.min(requiredSamples - readed, audioBuf?.remaining()
                                ?: (requiredSamples - readed))
                        audioBuf?.get(result, readed, read)
                        //println("read $readed <- $read")
                        readed += read
                    }
                    oldFrame = frame

                    return result.map { it * 0.97f }.toFloatArray()
                }
        }

        return FloatArray(0)
    }
}