package effect.audio

import annotation.CEffect
import annotation.CProperty
import org.apache.commons.math3.complex.Complex
import org.bytedeco.javacv.FFmpegFrameRecorder
import properties.CDoubleProperty
import ui.Main
import java.nio.FloatBuffer

@CEffect("エコー")
class EchoEffect(file: String) : AudioEffect(file) {

    @CProperty("ディレイ", 0)
    val delay = CDoubleProperty(0.1)

    @CProperty("減衰ファクター", 1)
    val factor = CDoubleProperty(0.01, 1.0)

    var impulseResponse = Array<Complex>(0) { _ -> Complex(0.0) }


    init {
        println("hi")
    }

    override fun executeFilter(start: Int, end: Int) {

        val arr = provider.getSamples((end - start) / Main.project.fps * Main.project.sampleRate)
        val res = arr?.mapIndexed { index, value -> if (index % 2 == 0) 0f else value }?.toFloatArray()

        val recorder = FFmpegFrameRecorder("$file.tmp", provider.grabber.audioChannels)
        recorder.audioCodecName = "flac"
        recorder.start()

        recorder.recordSamples(FloatBuffer.wrap(res))

        recorder.stop()

    }
}