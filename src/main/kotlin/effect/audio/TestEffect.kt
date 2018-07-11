package effect.audio

import annotation.CEffect
import annotation.CProperty
import objects.CitrusObject
import org.apache.commons.lang3.SystemUtils
import org.apache.commons.math3.complex.Complex
import org.bytedeco.javacpp.avcodec
import org.bytedeco.javacpp.avformat
import org.bytedeco.javacv.FFmpegFrameRecorder
import properties.CDoubleProperty
import sun.plugin2.util.SystemUtil
import ui.Main
import java.io.File
import java.nio.FloatBuffer
import java.nio.file.Files
import java.nio.file.LinkOption

@CEffect("テストエフェクト")
class TestEffect(parent : CitrusObject) : AudioEffect(parent) {

    @CProperty("ディレイ", 0)
    val delay = CDoubleProperty(0.1)

    @CProperty("減衰ファクター", 1)
    val factor = CDoubleProperty(0.01, 1.0)

    var impulseResponse = Array<Complex>(0) { _ -> Complex(0.0) }


    init {
        println("hi")
    }

    override fun executeFilter(file: String, start: Int, end: Int): Boolean {
        val provider = SimpleAudioSampleProvider(file)
        val arr = provider.getSamples((end - start) / Main.project.fps * Main.project.sampleRate * Main.project.audioChannel)
        val res = arr?.mapIndexed { index, value -> if (index % 2 == 0) 0f else value }?.toFloatArray()

        val f = File(file)

        val recorder = FFmpegFrameRecorder("${f.parent}/.${f.name}.wav", provider.grabber.audioChannels)
        recorder.sampleRate = Main.project.sampleRate
        //recorder.format = "tak"
        //recorder.audioCodec = avcodec.WAVE
        recorder.start()

        recorder.recordSamples(FloatBuffer.wrap(res))

        recorder.stop()

        if(SystemUtils.IS_OS_WINDOWS)
            Files.setAttribute(File("${f.parent}/.${f.name}.wav").toPath(), "dos:hidden", true, LinkOption.NOFOLLOW_LINKS)

        return true
    }
}