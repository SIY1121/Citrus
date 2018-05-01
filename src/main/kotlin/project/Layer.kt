package project

import com.jogamp.opengl.GL2
import objects.AudioSampleProvider
import objects.CitrusObject
import objects.Drawable
import objects.DrawableObject

class Layer : ArrayList<CitrusObject>(), Drawable, AudioSampleProvider {

    private var oldFrame = 0
    private var currentObject: CitrusObject? = null


    override fun getSamples(frame: Int): ShortArray {
        if (currentObject?.isActive(frame) == true) {
            val a = currentObject
            if (a is AudioSampleProvider)
                return a.getSamples(frame)
        } else if (currentObject?.isActive(frame) == false) {
            currentObject = null
            currentObject = firstOrNull { it.isActive(frame) }
        } else {
            currentObject = firstOrNull { it.isActive(frame) }
        }
        return ShortArray(0)
    }

    override fun draw(gl: GL2, mode: Drawable.DrawMode, frame: Int) {
        if (currentObject?.isActive(frame) == true) {
            val d = currentObject
            if (d is Drawable) d.draw(gl, mode, frame)
        } else if (currentObject?.isActive(frame) == false) {
            currentObject = null
            currentObject = firstOrNull { it.isActive(frame) }
        } else {
            currentObject = firstOrNull { it.isActive(frame) }
        }
    }
}