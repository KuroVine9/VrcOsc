import avatar.SettingHandler
import config.Config
import di.CONTAINER
import param.AvatarParamHandler
import ui.SelectListeningParam
import javax.swing.SwingUtilities

fun main() {
    CONTAINER[Config::class.java] = Config()
    CONTAINER[OSCHandler::class.java] = OSCHandler()
    CONTAINER[AvatarParamHandler::class.java] = AvatarParamHandler()
    CONTAINER[SettingHandler::class.java] = SettingHandler()

    SwingUtilities.invokeLater {
        SelectListeningParam()
    }
}