package project

import com.jogamp.opengl.GL2
import objects.AudioSampleProvider
import objects.Drawable
import ui.Main

class Scene : ArrayList<Layer>(), Drawable, AudioSampleProvider {

    private var oldAudioFrame = 0

    override fun getSamples(frame: Int): ShortArray {
        val result = ShortArray((frame - oldAudioFrame) * Main.project.sampleRate * Main.project.audioChannel / Main.project.fps)
        //println("sample size: ${result.size}")
        this.forEach {
            it.getSamples(frame).forEachIndexed { index, value ->
                result[index] = (result[index] + value).toShort()
            }
        }
        oldAudioFrame = frame
        return result
    }

    override fun draw(gl: GL2, mode: Drawable.DrawMode, frame: Int) {
        forEach {
            it.draw(gl, mode, frame)
        }
    }
}