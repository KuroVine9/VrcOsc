import avatar.observer.OSCBroadcaster
import com.illposed.osc.OSCBadDataEvent
import com.illposed.osc.OSCMessage
import com.illposed.osc.OSCPacketEvent
import com.illposed.osc.OSCPacketListener
import com.illposed.osc.transport.OSCPortIn
import com.illposed.osc.transport.OSCPortOut
import config.Config
import di.CONTAINER
import java.net.InetAddress
import java.net.InetSocketAddress

class OSCHandler : OSCBroadcaster() {
    private val config: Config by lazy {
        CONTAINER[Config::class.java] as Config
    }
    private val byteAddress = config["osc.address"].split(",").map {
        it.toByte()
    }.toByteArray()
    private val rxPort = config["osc.port.rx"].toInt()
    private val txPort = config["osc.port.tx"].toInt()

    private val oscTX = OSCPortOut(InetSocketAddress(InetAddress.getByAddress(byteAddress), txPort))
    private val oscRX = OSCPortIn(InetSocketAddress(InetAddress.getByAddress(byteAddress), rxPort))

    private var nowListener: OSCPacketListener = object : OSCPacketListener {
        override fun handlePacket(event: OSCPacketEvent?) {
            val message = event!!.packet as OSCMessage
            broadcast(message)
            println("${message.address}: ${message.arguments.firstOrNull()}")
        }

        override fun handleBadData(event: OSCBadDataEvent?) {
            println("bad data?")
        }

    }

    init {
        oscRX.addPacketListener(nowListener)
        oscRX.startListening()
        println("Listening to ${byteAddress.map { it.toInt() }}:$txPort:$rxPort")
    }

    fun <T> sendMsg(path: String, payload: T) {
        val msg = OSCMessage(path, listOf(payload))
        oscTX.send(msg)
    }

    private fun setNewListener(listener: OSCPacketListener) {
        oscRX.removePacketListener(this.nowListener)
        oscRX.stopListening()
        oscRX.addPacketListener(listener)
        oscRX.startListening()
        this.nowListener = listener
    }
}