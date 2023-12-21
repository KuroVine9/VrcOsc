package avatar

import OSCHandler
import avatar.observer.Broadcaster
import avatar.observer.Subscriber
import avatar.type.AvatarSetting
import avatar.type.ProgramSetting
import com.illposed.osc.OSCMessage
import di.CONTAINER
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import param.AvatarParamHandler
import java.io.File

class SettingHandler: Subscriber {
    private val osc: OSCHandler by lazy {
        CONTAINER[OSCHandler::class.java] as OSCHandler
    }
    private val avatarParam: AvatarParamHandler by lazy {
        CONTAINER[AvatarParamHandler::class.java] as AvatarParamHandler
    }
    private val SETTING_PATH = "src/main/resources/setting.json"
    private val AVATAR_CHANGE = "/avatar/change"
    private val settingFile = File(SETTING_PATH)
    init {

        if (!settingFile.exists()) {
            settingFile.createNewFile()
            settingFile.writeText(Json.encodeToString(ProgramSetting()))
        }

        osc.attach(AVATAR_CHANGE, this)
    }

    fun whenAvatarChanged(avtrId: String) {
        //TODO 바뀐 아바타의 param 구독정보 불러오기
        val avatar = avatarParam.getAvatarParam(avtrId)
        if (avatar == null) {
            println("Changed avatar but cannot parse it!")
            return
        }
        println("Changed avatar to: ${avatar.name}")

        // 파일에 마지막 사용한 아바타 id 기록
        val setting = Json.decodeFromString<ProgramSetting>(settingFile.readText())
        setting.lastUsedAvtr = avtrId
        settingFile.writeText(Json.encodeToString(setting))
    }

    override fun gotUpdate(message: OSCMessage) {
        val avtrId = message.arguments.firstOrNull() as String?
        avtrId?.let { whenAvatarChanged(it) }
    }

}