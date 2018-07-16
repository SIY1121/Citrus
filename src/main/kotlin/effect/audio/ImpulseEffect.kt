package effect.audio

import annotation.CEffect
import annotation.CProperty
import objects.CitrusObject
import org.apache.commons.math3.complex.Complex
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType
import org.bytedeco.javacv.FFmpegFrameRecorder
import properties.CFileProperty
import ui.Main
import java.io.File
import java.nio.FloatBuffer

@CEffect("インパルス畳み込み")
class ImpulseEffect(parent: CitrusObject) : AudioEffect(parent) {

    @CProperty("インパルス応答", 0)
    val file = CFileProperty()

    init {
        file.valueProperty.addListener { _, _, n ->
            val provider = SimpleAudioSampleProvider(n)


            val samples = provider.getSamples((provider.grabber.lengthInTime / 1000_000.0 * provider.grabber.audioChannels * provider.grabber.sampleRate).toInt()/2)//なぜ割るに

            //応答の長さから配列を確保しておく
            //+1は丸め誤差が発生したときのための保険
            irSampleL = FloatArray((provider.grabber.lengthInTime / 1000_000.0 * provider.grabber.sampleRate).toInt() + 1)
            irSampleR = FloatArray((provider.grabber.lengthInTime / 1000_000.0 * provider.grabber.sampleRate).toInt() + 1)

            samples?.forEachIndexed { index, value ->
                if (index % 2 == 0)
                    irSampleL[index / 2] = value
                else
                    irSampleR[index / 2] = value
            }


            irSampleR = irSampleR.toPower2()
            irSampleL = irSampleL.toPower2()

            irSampleL = FloatArray(irSampleL.size) + irSampleL
            irSampleR = FloatArray(irSampleR.size) + irSampleR


            val fft = FastFourierTransformer(DftNormalization.STANDARD)
            impulseFFTL = fft.transform(irSampleL.map { it.toDouble() }.toDoubleArray(), TransformType.FORWARD)
            impulseFFTR = fft.transform(irSampleR.map { it.toDouble() }.toDoubleArray(), TransformType.FORWARD)

        }
    }

    var irSampleL = FloatArray(0)
    var irSampleR = FloatArray(0)

    var impulseFFTL = Array(0) { _ -> Complex(0.0) }
    var impulseFFTR = Array(0) { _ -> Complex(0.0) }

    override fun executeFilter(file: String, start: Int, end: Int): Boolean {
        val provider = SimpleAudioSampleProvider(file)
        val f = File(file)
        val recorder = FFmpegFrameRecorder(File("${f.parent}/.${f.name}.wav"), provider.grabber.audioChannels)
        recorder.sampleRate = Main.project.sampleRate
        recorder.start()

        var prevSamplesL = DoubleArray(irSampleL.size / 2)
        var prevSamplesR = DoubleArray(irSampleR.size / 2)
        val fft = FastFourierTransformer(DftNormalization.STANDARD)
        while (true) {
            val samples = provider.getSamples(irSampleL.size / 2 + irSampleR.size / 2) ?: break
            val srcL = prevSamplesL + samples.filterIndexed { index, value -> index % 2 == 0 }.map { it.toDouble() }.toDoubleArray()
            val srcR = prevSamplesR + samples.filterIndexed { index, value -> index % 2 == 1 }.map { it.toDouble() }.toDoubleArray()
            val srcFFTL = fft.transform(srcL, TransformType.FORWARD)
            val srcFFTR = fft.transform(srcR, TransformType.FORWARD)

            val dstFFTL = impulseFFTL.mapIndexed { index, complex -> complex.multiply(srcFFTL[index]) }.toTypedArray()
            val dstFFTR = impulseFFTR.mapIndexed { index, complex -> complex.multiply(srcFFTR[index]) }.toTypedArray()

            val dstL = fft.transform(dstFFTL, TransformType.INVERSE)
            val dstR = fft.transform(dstFFTR, TransformType.INVERSE)

            val dst = FloatArray(dstL.size / 2 + dstR.size / 2)

            for (i in 0 until dst.size) {
                dst[i] = if (i % 2 == 0) dstL[i / 2].real.toFloat() /50f else dstR[i / 2].real.toFloat()/50f
            }

            recorder.recordSamples(FloatBuffer.wrap(dst))

            prevSamplesL = samples.filterIndexed { index, value -> index % 2 == 0 }.map { it.toDouble() }.toDoubleArray()
            prevSamplesR = samples.filterIndexed { index, value -> index % 2 == 1 }.map { it.toDouble() }.toDoubleArray()

        }
        recorder.stop()

        return true
    }

    /**
     * 渡された配列を長さが２の累乗になるようにパディングして返す
     */
    fun FloatArray.toPower2(): FloatArray {
        var i = 1.0
        while (this.size > Math.pow(2.0, i)) {
            i++
        }

        return this + FloatArray(Math.pow(2.0, i).toInt() - this.size)
    }
}