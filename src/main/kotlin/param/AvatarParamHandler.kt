package param

import config.Config
import di.CONTAINER
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import param.type.AvatarJson
import java.io.File

class AvatarParamHandler {
    private val config: Config by lazy {
        CONTAINER[Config::class.java] as Config
    }

    private var avatarSet = parseAvatarParam()

    fun refreshFile() {
        avatarSet = parseAvatarParam()
    }

    fun getAvatarParam(avtrId: String): AvatarJson? {
        var file = avatarSet[avtrId]
        if (file == null) {
            refreshFile()
            file = avatarSet[avtrId]
            if (file == null) return null
        }

        return Json.decodeFromString(deleteBOM(file.readText()))
    }

    fun parseAvatarParam(): HashMap<String, File> {
        val drive = config["window.drive"]
        val vrcOscAvatarParamPath = config["window.avatar_param_path"]
        val path = "$drive:${System.getenv("HOMEPATH")}$vrcOscAvatarParamPath"
        val fileMap = HashMap<String, File>()

        listFiles(File(path), fileMap)

        return fileMap
    }

    private fun listFiles(dir: File, resultMap: HashMap<String, File>) {
        val fileList = dir.listFiles() ?: return
        for (file in fileList) {
            if (file.isFile && file.extension == "json")
                resultMap[file.nameWithoutExtension] = file
            else if (file.isDirectory)
                listFiles(file, resultMap)
        }
    }

    // Avatar json이 UTF-8 with BOM인 관계로 일반 UTF-8로 컨버트
    private fun deleteBOM(str: String): String {
        if (!str.startsWith("\uFEFF")) return str

        return str.substring(1)
    }

}
