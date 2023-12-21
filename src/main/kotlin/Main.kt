import avatar.SettingHandler
import avatar.observer.Broadcaster
import config.Config
import di.CONTAINER
import param.AvatarParamHandler

fun main() {
    CONTAINER[Config::class.java] = Config()
    CONTAINER[OSCHandler::class.java] = OSCHandler()
    CONTAINER[AvatarParamHandler::class.java] = AvatarParamHandler()
    CONTAINER[SettingHandler::class.java] = SettingHandler()

    while (true)
        Thread.sleep(10000)
}