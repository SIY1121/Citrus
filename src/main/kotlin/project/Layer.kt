package project

import com.jogamp.opengl.GL2
import objects.AudioSampleProvider
import objects.CitrusObject
import objects.Drawable

class Layer : ArrayList<CitrusObject>(),Drawable,AudioSampleProvider {

    private var oldFrame = 0

    override fun getSamples(frame: Int): ShortArray {

        return ShortArray(1)
    }

    override fun draw(gl: GL2, mode: Drawable.DrawMode, frame: Int) {

    }
}