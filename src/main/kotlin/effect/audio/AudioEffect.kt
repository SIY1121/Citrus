package effect.audio

import effect.Effect
import objects.AudioSampleProvider
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FrameGrabber
import ui.Main
import java.nio.FloatBuffer

abstract class AudioEffect() : Effect() {
    /**
     * フィルターを実行し、テンポラリファイルに保存する
     * @oaram file フィルターを掛けるソース
     * @param start フィルターの開始位置
     * @param end フィルターの終了位置
     */
    abstract fun executeFilter(file: String, start: Int, end: Int): Boolean


    class SimpleAudioSampleProvider(file: String) {

        val grabber = FFmpegFrameGrabber(file)

        init {
            grabber.sampleRate = Main.project.sampleRate
            grabber.audioChannels = Main.project.audioChannel
            grabber.sampleMode = FrameGrabber.SampleMode.FLOAT
            grabber.start()
        }

        var tmpBuf: FloatBuffer? = null
        fun getSamples(requireSamples: Int): FloatArray? {
            var read = 0
            val res = FloatArray(requireSamples)
            while (read < requireSamples) {
                if (tmpBuf == null || tmpBuf?.remaining() == 0) {
                    tmpBuf = grabber.grabSamples()?.samples?.get(0) as? FloatBuffer ?: return null
                }

                val toRead = if (tmpBuf?.remaining() ?: 0 < (requireSamples - read))//必要分に足りていなければ
                    tmpBuf?.remaining() ?: 0
                else
                    requireSamples - read//足りていれば全部読む

                tmpBuf?.get(res, read, toRead)

                read += toRead

            }
            return res
        }

    }
}