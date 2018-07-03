package project


import javafx.scene.paint.Color
import objects.CitrusObject
import org.json.JSONArray
import org.json.JSONObject
import properties.CitrusAnimatableProperty
import properties.CitrusProperty
import ui.TimelineController
import java.io.*
import kotlin.reflect.full.starProjectedType

class Project {
    var initialized = false
    val scene: MutableList<Scene> = ArrayList()
    var width = 1920
    var height = 1080
    var fps = 60
    var sampleRate = 48000
    var audioChannel = 2

    init {
        scene.add(Scene())
    }

    fun save() {
        val json = JSONObject()
        json.put("width", width)
        json.put("height", height)
        json.put("fps", fps)
        json.put("sampleRate", sampleRate)
        json.put("audioChannel", audioChannel)

        //シーン
        val sceneJsonArray = JSONArray()
        scene.forEach {
            //レイヤー
            val layerJsonArray = JSONArray()
            it.forEach {
                //オブジェクト単体
                val objJsonArray = JSONArray()
                it.forEach { obj ->
                    val objJson = JSONObject()
                    objJson.put("@class", obj::class.qualifiedName)
                    objJson.put("start", obj.start)
                    objJson.put("end", obj.end)
                    objJson.put("layer", obj.layer)
                    objJson.put("scene", obj.scene)

                    obj.memberProperties.forEach {
                        val p = it.get(obj) as CitrusProperty<*>
                        objJson.put(it.name, p.value)
                    }

                    objJsonArray.put(objJson)
                }
                layerJsonArray.put(objJsonArray)
            }
            sceneJsonArray.put(layerJsonArray)
        }

        json.put("scene", sceneJsonArray)

        val fos = FileOutputStream("project.json").bufferedWriter()
        fos.write(json.toString(4))
        fos.close()

        println(json.toString(4))
    }

    fun load(file: String) {
        val text = BufferedReader(FileReader(file)).readText()
        val json = JSONObject(text)
        width = json.getInt("width")
        height = json.getInt("height")
        fps = json.getInt("fps")
        sampleRate = json.getInt("sampleRate")
        audioChannel = json.getInt("audioChannel")

        val scenes = json.getJSONArray("scene")

        for (i in 0 until scenes.length())
            scene.add(Scene())

        for (i in 0 until scenes.length()) {
            val layer = scenes.getJSONArray(i)

            for (j in 0 until layer.length())
                scene[i].add(Layer())

            for (j in 0 until layer.length()) {
                val objs = layer.getJSONArray(j)
                for (k in 0 until objs.length()) {
                    val obj = objs.getJSONObject(k).toMap()
                    val clazz = Class.forName(obj["@class"].toString())
                    val cObject = clazz.getDeclaredConstructor(Int::class.java, Int::class.java).newInstance(-1, -1) as CitrusObject
                    cObject.setupProperties()

                    cObject.start = obj["start"] as Int
                    cObject.end = obj["end"] as Int
                    cObject.layer = obj["layer"] as Int
                    cObject.scene = obj["scene"] as Int



                    cObject.memberProperties.forEach { m ->
                        val p = m.get(cObject) as? CitrusProperty<Any> ?: return
                        val valueType = p.value::class.javaObjectType
                        when {
                            valueType == Color::class.java -> p.value = Color.valueOf(p.value.toString())
                            valueType.name == "java.lang.Double" -> p.value = p.value as Double
                            else -> p.value = valueType.cast(obj.filterKeys { it == m.name }[m.name]!!)
                        }
                    }


                    TimelineController.instance.addObject(cObject::class.java, cObject.layer, null, cObject.start, cObject.end, cObject)

                }
            }
        }
    }
}