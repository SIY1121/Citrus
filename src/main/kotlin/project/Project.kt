package project

class Project{
    var initialized = false
    val scene: MutableList<Scene> = ArrayList()
    var width = 1920
    var height = 1080
    var fps = 60
    var sampleRate = 48000
    var audioChannel = 2

    init{
        scene.add(Scene())
    }

    companion object {
        fun load(file : String){

        }
    }
}