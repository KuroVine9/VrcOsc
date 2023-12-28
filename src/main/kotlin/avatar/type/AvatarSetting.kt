package avatar.type

import kotlinx.serialization.Serializable

@Serializable
data class ProgramSetting(
    var name: String,
    var lastUsedAvtr: String? = null,
    val avtrSetting: MutableList<AvatarSetting> = mutableListOf()
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