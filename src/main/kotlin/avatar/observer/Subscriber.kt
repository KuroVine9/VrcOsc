package avatar.observer

import com.illposed.osc.OSCMessage

interface Subscriber {
    fun gotUpdate(message: OSCMessage)
}