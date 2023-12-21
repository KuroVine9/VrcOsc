package param.type

import kotlinx.serialization.Serializable

@Serializable
data class AvatarJson(
    val id: String,
    val name: String,
    val parameters: List<Parameter>
)

@Serializable
data class Parameter(
    val name: String,
    val input: OscIO? = null,
    val output: OscIO? = null
)

@Serializable
data class OscIO(
    val address: String,
    val type: String
)