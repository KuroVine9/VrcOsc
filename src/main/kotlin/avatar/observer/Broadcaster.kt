package avatar.observer

import com.illposed.osc.OSCMessage

abstract class OSCBroadcaster() {
    private val subs = HashMap<String, MutableSet<OSCSubscriber>>()
    fun attach(address: String, subscriber: OSCSubscriber) {
        if (subs.containsKey(address)) subs[address]!!.add(subscriber)
        else subs[address] = mutableSetOf(subscriber)
    }

    fun detach(address: String, subscriber: OSCSubscriber) {
        subs[address]?.remove(subscriber)
    }

    open fun broadcast(message: OSCMessage) {
        val address = message.address

        subs[address]?.let { subscribers ->
            subscribers.forEach { it.gotUpdate(message) }
        }
    }
}

abstract class AvatarBroadcaster() {
    private val subs = mutableSetOf<AvatarSubscriber>()
    fun attach(subscriber: AvatarSubscriber) {
        subs.add(subscriber)
    }

    fun detach(subscriber: AvatarSubscriber) {
        subs.remove(subscriber)
    }

    open fun broadcast(avtrId: String, params: List<String> = emptyList()) {
        subs.forEach { it.gotUpdate(avtrId, params) }
    }
}