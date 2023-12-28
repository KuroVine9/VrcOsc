package avatar

import OSCHandler
import avatar.observer.AvatarBroadcaster
import avatar.observer.OSCSubscriber
import avatar.type.AvatarSetting
import avatar.type.ProgramSetting
import com.illposed.osc.OSCMessage
import di.CONTAINER
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import param.AvatarParamHandler
import java.io.File
import java.util.*

class SettingHandler : OSCSubscriber, AvatarBroadcaster() {
    private val osc: OSCHandler by lazy {
        CONTAINER[OSCHandler::class.java] as OSCHandler
    }
    private val avatarParam: AvatarParamHandler by lazy {
        CONTAINER[AvatarParamHandler::class.java] as AvatarParamHandler
    }
    private val SETTING_PATH = "src/main/resources/setting.json"
    private val AVATAR_CHANGE = "/avatar/change"
    private val settingFile = File(SETTING_PATH)

    private var _nowAvtrId: String? = null
    private var _nowAvtrSetting: AvatarSetting? = null
    private var _myName: String? = null

    val nowAvtrId: String?
        get() = _nowAvtrId

    val nowAvtrSetting: AvatarSetting?
        get() = _nowAvtrSetting

    var myName: String
        get() {
            return _myName ?: Json.decodeFromString<ProgramSetting>(settingFile.readText()).name
        }
        set(value) {
            val programSetting = Json.decodeFromString<ProgramSetting>(settingFile.readText())
            programSetting.name = value
            settingFile.writeText(Json.encodeToString(programSetting))
            _myName = value
        }

    init {
        if (!settingFile.exists()) {
            settingFile.createNewFile()
            settingFile.writeText(Json.encodeToString(ProgramSetting(UUID.randomUUID().toString())))
        }

        osc.attach(AVATAR_CHANGE, this)

        val programSetting = Json.decodeFromString<ProgramSetting>(settingFile.readText())
        programSetting.lastUsedAvtr?.let { id ->
            _nowAvtrId = id
            _nowAvtrSetting = programSetting.avtrSetting.find { it.avtrId == id }
        }
        nowAvtrId?.let { broadcast(it, nowAvtrSetting?.param ?: emptyList()) }
    }

    fun modifyAvatarListeningParam(avtrId: String, params: List<String>) {
        val programSetting = Json.decodeFromString<ProgramSetting>(settingFile.readText())
        val avatarSetting = programSetting.avtrSetting.find { it.avtrId == avtrId }
        if (avatarSetting == null)
            programSetting.avtrSetting.add(AvatarSetting(avtrId, params))
        else avatarSetting.param = params

        settingFile.writeText(Json.encodeToString(programSetting))
    }

    private fun whenAvatarChanged(avtrId: String) {
        val avatar = avatarParam.getAvatarParam(avtrId)
        if (avatar == null) {
            println("Changed avatar but cannot parse it!")
            _nowAvtrSetting = null
            return
        }
        _nowAvtrId = avtrId
        println("Changed avatar to: ${avatar.name}")

        // 파일에 마지막 사용한 아바타 id 기록
        val setting = Json.decodeFromString<ProgramSetting>(settingFile.readText())
        setting.lastUsedAvtr = avtrId
        _nowAvtrSetting = setting.avtrSetting.find { it.avtrId == avtrId }
        broadcast(avtrId, _nowAvtrSetting?.param ?: emptyList())
        settingFile.writeText(Json.encodeToString(setting))
    }

    override fun gotUpdate(message: OSCMessage) {
        val avtrId = message.arguments.firstOrNull() as String?
        avtrId?.let { whenAvatarChanged(it) }
    }

}