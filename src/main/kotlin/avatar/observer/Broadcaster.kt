package avatar.observer

import com.illposed.osc.OSCMessage

abstract class Broadcaster() {
    private val subs = HashMap<String, MutableSet<Subscriber>>()
    fun attach(address: String, subscriber: Subscriber) {
        if (subs.containsKey(address)) subs[address]!!.add(subscriber)
        else subs[address] = mutableSetOf(subscriber)
    }

    fun detach(address: String, subscriber: Subscriber) {
        subs[address]?.remove(subscriber)
    }

    open fun broadcast(message: OSCMessage) {
        val address = message.address
        if (!subs.containsKey(address)) return
        for (sub in subs[address]!!) {
            sub.gotUpdate(message)
        }
    }
}