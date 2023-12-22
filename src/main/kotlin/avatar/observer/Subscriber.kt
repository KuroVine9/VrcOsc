package avatar.observer

import com.illposed.osc.OSCMessage

interface OSCSubscriber {
    fun gotUpdate(message: OSCMessage)
}

interface AvatarSubscriber {
    fun gotUpdate(avtrId: String, params: List<String>)
}