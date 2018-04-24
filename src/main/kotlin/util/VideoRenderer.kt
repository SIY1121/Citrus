package util

import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import org.bytedeco.javacv.FFmpegFrameRecorder
import ui.GlCanvas
import java.io.File
import java.nio.ByteBuffer

class VideoRenderer {
    companion object {
        var time = 0L
        lateinit var recorder: FFmpegFrameRecorder
        //lateinit var filter: FFmpegFrameFilterMod
        fun startEncode() {
            recorder = FFmpegFrameRecorder(File("out.mp4"), Statics.project.width, Statics.project.height)
            //recorder.videoCodec = 28
            recorder.videoCodecName = "nvenc_h264"
            recorder.videoBitrate = 10000000
            recorder.frameRate = Statics.project.fps.toDouble()
            time = System.currentTimeMillis()
            recorder.start()
            GlCanvas.instance.animator.stop()
            GlCanvas.instance.currentFrame = 0
            GlCanvas.instance.rendering = true
            //TODO テクスチャ反転問題をプレビュー時に反転して、出力時は普通に出すべきか
            //filter = FFmpegFrameFilterMod("vflip", Statics.project.width, Statics.project.height)
            //filter.start()
            val end = Statics.project.Layer.flatten().maxBy { it.end }?.end
            if (end == null) {
                Alert(Alert.AlertType.ERROR, "オブジェクトの最終位置を特定できませんでした", ButtonType.OK)
                return
            }
            while (GlCanvas.instance.currentFrame < end) {
                GlCanvas.instance.display()
                GlCanvas.instance.currentFrame++
            }

            endEncode()
            GlCanvas.instance.rendering = false
        }

        fun recordFrame(buf: ByteBuffer) {
            //filter.pushImage(Statics.project.width, Statics.project.height, 8, 3, Statics.project.width * 3, -1, buf)
            //recorder.record(filter.pullImage(), 3)
            recorder.recordImage(Statics.project.width, Statics.project.height, 8, 3, Statics.project.width * 3, -1, buf)
        }

        fun endEncode() {
            //filter.stop()
            recorder.stop()
            recorder.release()
            println(System.currentTimeMillis() - time)
        }
    }
}