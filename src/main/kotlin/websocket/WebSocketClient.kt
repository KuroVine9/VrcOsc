package websocket

import OSCHandler
import avatar.SettingHandler
import di.CONTAINER
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.glassfish.tyrus.client.ClientManager
import websocket.type.DEFAULT_WS_URL
import websocket.type.ParamPayload
import websocket.type.ParamSetRequest
import websocket.type.PayloadType
import java.io.IOException
import java.net.ConnectException
import java.net.URI
import javax.websocket.*

@ClientEndpoint
class WebSocketClient(
    private val ip: String,
    private val path: String? = null,
    private val port: Int = 80,
    private val isWss: Boolean = false,
    val openCallback: (String, WebSocketClient) -> Unit,
    val closeCallback: (CloseReason, WebSocketClient) -> Unit,
    val connErrCallback: (ConnectException) -> Unit
) {
    private val osc: OSCHandler by lazy {
        CONTAINER[OSCHandler::class.java] as OSCHandler
    }
    private val setting: SettingHandler by lazy {
        CONTAINER[SettingHandler::class.java] as SettingHandler
    }
    private lateinit var serverName: String
    private var session: Session? = null

    init {
        try {
            val endpointUri =
                URI("${if (isWss) "wss" else "ws"}://$ip:$port/${path ?: (DEFAULT_WS_URL + '/' + setting.myName)}")
            val clientManager = ClientManager.createClient()
            session = clientManager.connectToServer(this, endpointUri)
        }
        catch (e: DeploymentException) {
            //TODO
            e.printStackTrace()
        }
        catch (e: IOException) {
            //TODO
            e.printStackTrace()
        }
        catch (e: IllegalStateException) {
            //TODO
            e.printStackTrace()
        }
        catch (e: ConnectException) {
            connErrCallback(e)
        }
    }

    fun <T> sendMessage(message: ParamPayload<T>) {
        session!!.asyncRemote.sendText(
            Json.encodeToString(message)
        )
    }

    @OnMessage
    fun onMessage(message: String) {
        val json = Json.decodeFromString<ParamPayload<Any>>(message)
        when (json.type) {
            PayloadType.SET.ordinal -> {
                val avtrSetting = setting.nowAvtrSetting ?: return
                val payload = json.payload as ParamSetRequest

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
                    ParamPayload(
                        setting.myName,
                        PayloadType.AVTR_INFO.ordinal,
                        avtrSetting
                    )
                )
            }

            PayloadType.NAME.ordinal -> {
                serverName = json.from
                openCallback(serverName, this)
            }

            else -> {
                println("Unhandled Type!")
            }
        }
    }

    @OnOpen
    fun onOpen(userSession: Session) {
        println("WS Client Open")
        session = userSession
    }

    @OnClose
    fun onClose(userSession: Session, reason: CloseReason) {
        closeCallback(reason, this)
        println("WS Connection Closed due to : ${reason.closeCode}, ${reason.reasonPhrase}")
        session = null
    }

    fun closeSession() {
        session?.close()
    }

    override fun toString(): String {
        return "[$serverName] ${if (isWss) "wss" else "ws"}://$ip:$port/${path ?: (DEFAULT_WS_URL + '/' + setting.myName)}"
    }
}