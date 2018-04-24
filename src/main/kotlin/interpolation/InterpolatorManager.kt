package interpolation

import annotation.CInterpolation
import objects.ObjectManager
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.ZipInputStream

class InterpolatorManager {
    companion object {

        val interpolator: HashMap<String, Class<*>> = HashMap()

        fun load() {
            if (javaClass.protectionDomain.codeSource.location.path.endsWith(".jar"))
                loadOnRelease()
            else
                loadOnDebug()
        }

        private fun loadOnDebug(){
            val list = ClassLoader.getSystemClassLoader().getResources("interpolation/")
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
                if(entry.name.startsWith("interpolation/") && !entry.isDirectory)
                    addClass(entry.name.replace("interpolation/", "").replace(".class", ""))
            }
        }

        private fun addClass(className : String){
            val clazz = Class.forName("interpolation.$className")
            if (!clazz.isInterface && clazz.interfaces.contains(Interpolator::class.java) && clazz.annotations[0] is CInterpolation)
            {
                interpolator[(clazz.annotations[0] as CInterpolation).name] = clazz
                println((clazz.annotations[0] as CInterpolation).name)
            }
        }
    }
}