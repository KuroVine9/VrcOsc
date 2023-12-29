package avatar.type

import kotlinx.serialization.Serializable

@Serializable
data class ProgramSetting(
    var name: String,
    var lastUsedAvtr: String? = null,
    val avtrSetting: MutableList<AvatarSetting> = mutableListOf(),
    var wsSetting: WSSetting = WSSetting()
)

@Serializable
data class WSSetting(
    var isServerAutoOn: Boolean = false,
    var autoConnectServer: List<ConnectionInfo> = emptyList()
)

@Serializable
data class ConnectionInfo(
    var ip: String,
    var port: Int,
    var path: String = "",
    var isWss: Boolean = false
)


@Serializable
data class AvatarSetting(
    val avtrId: String,
    var param: List<String> = emptyList()
) {
    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is AvatarSetting) return false
        return avtrId == other.avtrId
    }

    override fun hashCode(): Int {
        var result = avtrId.hashCode()
        result = 31 * result + param.hashCode()
        return result
    }
}