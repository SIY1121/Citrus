package effects

import annotation.CEffect
import annotation.CInterpolation
import interpolation.Interpolator
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.ZipInputStream

class EffectManager {companion object {

    val effects: HashMap<String, Class<*>> = HashMap()

    fun load() {
        if (javaClass.protectionDomain.codeSource.location.path.endsWith(".jar"))
            loadOnRelease()
        else
            loadOnDebug()
    }

    private fun loadOnDebug() {
        val list = ClassLoader.getSystemClassLoader().getResources("effects/audio/")
        Files.walkFileTree(Paths.get(list.nextElement().toURI()), object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                val name = file.fileName.toString().replace(".class", "")
                addClass(name)
                return super.visitFile(file, attrs)
            }
        })
    }

    private fun loadOnRelease(){
        val zip = ZipInputStream(javaClass.protectionDomain.codeSource.location.openStream())
        while (true) {
            val entry = zip.nextEntry ?: break
            if(entry.name.startsWith("effects/audio/") && !entry.isDirectory)
                addClass(entry.name.replace("effects/audio/", "").replace(".class", ""))
        }
    }

    private fun addClass(className : String){
        val clazz = Class.forName("effects.audio.$className")
        if (!clazz.isInterface && clazz.annotations.any { it is CEffect })
        {
            println("add $className")
            effects[(clazz.annotations.first { it is CEffect } as CEffect).name] = clazz
            println((clazz.annotations.first { it is CEffect } as CEffect).name)
        }
    }
}
}