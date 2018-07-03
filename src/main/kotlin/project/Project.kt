package project


import interpolation.AccelerateDecelerateInterpolator
import javafx.scene.paint.Color
import objects.CitrusObject
import org.json.JSONArray
import org.json.JSONObject
import properties.CitrusAnimatableProperty
import properties.CitrusProperty
import properties.KeyFrame
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
                    //基本情報の書き出し
                    objJson.put("@class", obj::class.qualifiedName)
                    objJson.put("start", obj.start)
                    objJson.put("end", obj.end)
                    objJson.put("layer", obj.layer)
                    objJson.put("scene", obj.scene)

                    //オブジェクトのすべてのプロパティに対して
                    obj.memberProperties.forEach {
                        val p = it.get(obj) as CitrusProperty<*>

                        //アニメーションプロパティの場合
                        if (p is CitrusAnimatableProperty<*>) {
                            val keyFrameJson = JSONArray()
                            //キーフレームが存在したら
                            if (p.keyFrames.size > 0) {
                                //すべてのキーフレームを書き出し
                                p.keyFrames.forEach {
                                    val k = JSONObject()
                                    k.put("frame", it.frame)
                                    k.put("value", it.value)
                                    keyFrameJson.put(k)
                                }
                                objJson.put(it.name, keyFrameJson)
                            } else {//キーフレームがなかったら普通に１つの値を書き出す
                                objJson.put(it.name, p.value)
                            }

                        } else//アニメーション可能でなければ普通に１つの値を書き出す
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

        scene.clear()

        val scenes = json.getJSONArray("scene")
        //必要分のシーンを追加
        for (i in 0 until scenes.length())
            scene.add(Scene())

        for (i in 0 until scenes.length()) {
            val layer = scenes.getJSONArray(i)
            //必要分のレイヤーを追加
            for (j in 0 until layer.length())
                scene[i].add(Layer())

            for (j in 0 until layer.length()) {
                //あるレイヤーの中にいるオブジェクト達
                val objs = layer.getJSONArray(j)

                //個々のオブジェクトに対して
                for (k in 0 until objs.length()) {
                    //マップオブジェクトのほうが扱いやすいので変換
                    val obj = objs.getJSONObject(k).toMap()
                    //@classの内容から生成するクラスを決定
                    val clazz = Class.forName(obj["@class"].toString())
                    //コンストラクタの取得
                    val cObject = clazz.getDeclaredConstructor(Int::class.java, Int::class.java).newInstance(-1, -1) as CitrusObject
                    cObject.setupProperties()

                    //基本設定
                    //この時点で自動的にscene配列に格納される
                    cObject.start = obj["start"] as Int
                    cObject.end = obj["end"] as Int
                    cObject.layer = obj["layer"] as Int
                    cObject.scene = obj["scene"] as Int

                    //メンバープロパティについて
                    cObject.memberProperties.forEach { m ->
                        //プロパティの取得
                        val p = m.get(cObject) as? CitrusProperty<Any> ?: return
                        val valueType = p.value::class.javaObjectType

                        //アニメーションプロパティでかつキーフレームを持っている場合
                        if (p is CitrusAnimatableProperty<Any> && obj[m.name] is List<*>) {
                            //特定のプロパティのキーフレームリストにアクセス
                            val array = obj[m.name] as ArrayList<HashMap<String, Number>>
                            //キーフレームを追加
                            array.forEach {
                                val frame: Int = it["frame"]?.toInt() ?: 0
                                val value: Double = it["value"]?.toDouble() ?: 0.0
                                val keyFrame = KeyFrame<Any>(frame, value, AccelerateDecelerateInterpolator())
                                p.keyFrames.add(keyFrame)
                            }
                        } else {
                            when {
                                //色は変換されないので仕方なく実装
                                valueType == Color::class.java -> p.value = Color.valueOf(p.value.toString())
                                //Int->Doubleの変換が勝手にされないので
                                valueType.name == "java.lang.Double" -> p.value = p.value as Double
                                else -> p.value = valueType.cast(obj.filterKeys { it == m.name }[m.name]!!)
                            }
                        }
                    }

                    //タイムラインにオブジェクトを追加
                    TimelineController.instance.addObject(cObject::class.java, cObject.layer, null, cObject.start, cObject.end, cObject)

                }
            }
        }
    }
}