package effect

import annotation.CEffect
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.ZipInputStream

class EffectManager {
    companion object {

        val audioEffects: HashMap<String, Class<*>> = HashMap()
        val graphicsEffects: HashMap<String, Class<*>> = HashMap()

        fun load() {
            if (javaClass.protectionDomain.codeSource.location.path.endsWith(".jar"))
                loadOnRelease()
            else
                loadOnDebug()
        }

        private fun loadOnDebug() {
            val list = ClassLoader.getSystemClassLoader().getResources("effect/audio/")
            Files.walkFileTree(Paths.get(list.nextElement().toURI()), object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    val name = file.fileName.toString().replace(".class", "")
                    addAudioEffectClass(name)
                    return super.visitFile(file, attrs)
                }
            })

            val list2 = ClassLoader.getSystemClassLoader().getResources("effect/graphics/")
            Files.walkFileTree(Paths.get(list2.nextElement().toURI()), object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    val name = file.fileName.toString().replace(".class", "")
                    addGraphicsEffectClass(name)
                    return super.visitFile(file, attrs)
                }
            })
        }

        private fun loadOnRelease() {
            val zip = ZipInputStream(javaClass.protectionDomain.codeSource.location.openStream())
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name.startsWith("effect/audio/") && !entry.isDirectory)
                    addAudioEffectClass(entry.name.replace("effect/audio/", "").replace(".class", ""))
                if (entry.name.startsWith("effect/graphics/") && !entry.isDirectory)
                    addGraphicsEffectClass(entry.name.replace("effect/graphics/", "").replace(".class", ""))
            }
        }

        private fun addAudioEffectClass(className: String) {
            val clazz = Class.forName("effect.audio.$className")
            if (!clazz.isInterface && clazz.annotations.any { it is CEffect }) {
                println("add $className")
                audioEffects[(clazz.annotations.first { it is CEffect } as CEffect).name] = clazz
                println((clazz.annotations.first { it is CEffect } as CEffect).name)
            }
        }

        private fun addGraphicsEffectClass(className: String) {
            val clazz = Class.forName("effect.graphics.$className")
            if (!clazz.isInterface && clazz.annotations.any { it is CEffect }) {
                println("add $className")
                graphicsEffects[(clazz.annotations.first { it is CEffect } as CEffect).name] = clazz
                println((clazz.annotations.first { it is CEffect } as CEffect).name)
            }
        }
    }
}