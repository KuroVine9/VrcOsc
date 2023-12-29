package websocket

import OSCHandler
import avatar.SettingHandler
import di.CONTAINER
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import websocket.type.ParamInfo
import websocket.type.ParamPayload
import websocket.type.PayloadInfo
import websocket.type.PayloadType
import javax.websocket.*
import javax.websocket.server.PathParam
import javax.websocket.server.ServerEndpoint

@ServerEndpoint(value = "/kuro9/osc/{user}")
class WebSocketServer {
    private val osc: OSCHandler by lazy {
        CONTAINER[OSCHandler::class.java] as OSCHandler
    }
    private val setting: SettingHandler by lazy {
        CONTAINER[SettingHandler::class.java] as SettingHandler
    }

    //id, session
    private val sessionMap: HashMap<String, Session> = HashMap()

    //name, id
    private val nameMap: HashMap<String, String> = HashMap()

    @OnMessage
    fun onMessage(session: Session, message: String) {
        val jsonParser = Json { ignoreUnknownKeys = true }
        val info = jsonParser.decodeFromString<PayloadInfo>(message)
        when (info.type) {
            PayloadType.SET.ordinal -> {
                val json = jsonParser.decodeFromString<ParamPayload<ParamInfo>>(message)
                val avtrSetting = setting.nowAvtrSetting ?: return
                val payload = json.payload

                if (payload.param in avtrSetting.param) {
                    // 파라미터 set
                    osc.sendMsg(
                        payload.param, when (payload.paramType) {
                            'T', 'F' -> payload.setTo.toBoolean()
                            'f' -> payload.setTo.toFloat()
                            'i' -> payload.setTo.toInt()
                            's' -> payload.setTo
                            else -> throw RuntimeException("Unhandled Type Tag: ${payload.paramType}")
                        }
                    )
                }
                else {
                    //아바타 정보 전송
                    sendMessage(
                        session,
                        ParamPayload(
                            setting.myName,
                            PayloadType.AVTR_INFO.ordinal,
                            avtrSetting
                        )
                    )
                }
            }

            PayloadType.AVTR_CHANGE.ordinal -> {}
            PayloadType.AVTR_INFO.ordinal -> {}
            PayloadType.REQ_AVTR_INFO.ordinal -> {
                val avtrSetting = setting.nowAvtrSetting
                sendMessage(
                    session,
                    ParamPayload(
                        setting.myName,
                        PayloadType.AVTR_INFO.ordinal,
                        avtrSetting
                    )
                )
            }

            else -> {
                println("Unhandled Type!")
            }
        }
    }

    fun <T> sendMessage(session: Session, message: ParamPayload<T>) {
        session.asyncRemote.sendText(
            Json.encodeToString(message)
        )
    }

    fun <T> sendMessage(name: String, message: ParamPayload<T>) {
        val session = sessionMap[nameMap[name] ?: return] ?: return
        sendMessage(session, message)
    }

    fun <T> broadcastMessage(message: ParamPayload<T>) {
        sessionMap.values.forEach { sendMessage(it, message) }
    }

    @OnOpen
    fun onOpen(session: Session, @PathParam("user") userName: String) {
        sessionMap[session.id] = session
        nameMap[userName] = session.id
        val payload = ParamPayload(
            setting.myName,
            PayloadType.NAME.ordinal,
            setting.myName
        )
        session.asyncRemote.sendText(Json.encodeToString(payload))
        println("WS Connection Created: $userName")
    }

    @OnClose
    fun onClose(session: Session) {
        val iter = nameMap.entries.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            if (entry.value != session.id) continue
            iter.remove()
            break
        }
        sessionMap.remove(session.id)
    }

    @OnError
    fun onError(session: Session, e: Throwable) {
        e.printStackTrace()
    }
}