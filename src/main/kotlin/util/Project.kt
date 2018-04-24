package util

import objects.CitrusObject

class Project {
    var initialized = false
    val Layer : MutableList<MutableList<CitrusObject>> = ArrayList()
    var width = 1920
    var height = 1080
    var fps = 60
}