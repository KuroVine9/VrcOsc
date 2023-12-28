package websocket.type

import avatar.type.AvatarSetting
import kotlinx.serialization.Serializable
import websocket.type.PayloadType.*

const val DEFAULT_WS_URL = "kuro9/osc"

/**
 * [SET]: [ParamSetRequest] 파라미터 설정 요청
 * [AVTR_CHANGE]: [String] 아바타 변경 알림
 * [AVTR_INFO]: [AvatarSetting] 아바타 정보
 * [REQ_AVTR_INFO]: [String] 아바타 정보 요청
 * [NAME]: [String] 클라이언트 이름 송신
 * */
enum class PayloadType { SET, AVTR_CHANGE, AVTR_INFO, REQ_AVTR_INFO, NAME }

@Serializable
data class ParamPayload<T>(
    val from: String,
    val type: Int,
    val payload: T
)

/**
 * [paramType]는 OSC Tag Type 정보를 담습니다.
 * { i: int32, f: float32, s: OSC-string, T: True, F: False }
 *
 * [참조](https://forum.derivative.ca/t/osc-tag-types/11269)
 *
 * [setTo]는 설정할 값을 stringify한 값입니다.
 * */
@Serializable
data class ParamSetRequest(
    val param: String,
    val paramType: Char,
    val setTo: String
)

