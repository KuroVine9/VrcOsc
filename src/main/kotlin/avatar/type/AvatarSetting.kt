package avatar.type

import kotlinx.serialization.Serializable

@Serializable
data class ProgramSetting (
    var lastUsedAvtr: String? = null,
    val avtrSetting: MutableList<AvatarSetting> = mutableListOf()
)

@Serializable
data class AvatarSetting (
    val avtrId: String,
    val param: MutableList<String> = mutableListOf()
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