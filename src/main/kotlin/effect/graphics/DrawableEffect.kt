package effect.graphics

interface DrawableEffect {
    abstract fun onDraw(textureId: Int): Int
}