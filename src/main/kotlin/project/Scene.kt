package project

import com.jogamp.opengl.GL2
import objects.AudioSampleProvider
import objects.Drawable

class Scene : ArrayList<Layer>(), Drawable,AudioSampleProvider {

    override fun getSamples(frame: Int): ShortArray {
        this.forEach {
            it.getSamples(frame)
        }
        return ShortArray(1)
    }

    override fun draw(gl: GL2, mode: Drawable.DrawMode, frame: Int) {

    }
}