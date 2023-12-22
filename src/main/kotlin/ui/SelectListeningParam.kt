package ui

import OSCHandler
import avatar.SettingHandler
import avatar.observer.AvatarSubscriber
import avatar.observer.OSCSubscriber
import com.illposed.osc.OSCMessage
import di.CONTAINER
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import param.AvatarParamHandler
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.*
import javax.swing.SpringLayout.*


class SelectListeningParam : JFrame("title"), OSCSubscriber, AvatarSubscriber {

    private val subscribedMap = HashMap<String, JLabel>()
    private var avatarName: String? = null
    private var avatarId: String? = null
    private var avatarParams: List<String>? = null
    private val osc: OSCHandler by lazy {
        CONTAINER[OSCHandler::class.java] as OSCHandler
    }
    private val parser: AvatarParamHandler by lazy {
        CONTAINER[AvatarParamHandler::class.java] as AvatarParamHandler
    }
    private val setting: SettingHandler by lazy {
        CONTAINER[SettingHandler::class.java] as SettingHandler
    }

    private var mode = false
    private lateinit var mainPanel: JPanel

    init {
        // contentPane = createMainUI()
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        setSize(800, 600)
        isVisible = true
        setting.attach(this)

        setting.nowAvtrId?.let { whenAvatarChanged(it, setting.nowAvtrSetting?.param ?: emptyList()) }
    }

    private fun createMainUI(): Container {
        mainPanel = JPanel()
        val window = JPanel()
        val button = JButton("setting")
        val nowAvatar = JLabel("Now Using Avatar : ${avatarName}")
        button.addActionListener {
            window.removeAll()
            window.add(if (mode) createMonitorPanel() else createSelectPanel())
            mode = !mode
            window.revalidate()
            window.repaint()

            if (mode) button.text = "monitor"
            else button.text = "setting"
        }
        window.add(createMonitorPanel())

        val scroll =
            JScrollPane(window, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)
        scroll.verticalScrollBar.unitIncrement = 16
        mainPanel.add(scroll)
        mainPanel.add(button)
        mainPanel.add(nowAvatar)

        val layout = SpringLayout()

        layout.putConstraint(NORTH, nowAvatar, 10, NORTH, mainPanel)
        layout.putConstraint(WEST, nowAvatar, 10, WEST, mainPanel)

        layout.putConstraint(SOUTH, button, -10, SOUTH, mainPanel)
        layout.putConstraint(EAST, button, -10, EAST, mainPanel)

        layout.putConstraint(NORTH, scroll, 10, SOUTH, nowAvatar)
        layout.putConstraint(EAST, scroll, -10, EAST, mainPanel)
        layout.putConstraint(WEST, scroll, 10, WEST, mainPanel)
        layout.putConstraint(SOUTH, scroll, -10, NORTH, button)

        mainPanel.layout = layout

        return mainPanel
    }

    private fun createMonitorPanel(): JPanel {
        val monitorPanel = JPanel()
        val layout = GridLayout(0, 3, 10, 10)
        monitorPanel.layout = layout

        for (param in subscribedMap.values) {
            monitorPanel.add(param)
        }

        var widthWhenThree = 0

        mainPanel.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {

                if (layout.columns == 3) widthWhenThree = monitorPanel.width
                if (widthWhenThree == 0) return

                layout.columns = if (mainPanel.width < widthWhenThree + 50) 1 else 3

                monitorPanel.revalidate()
                monitorPanel.repaint()
            }
        })

        return monitorPanel
    }

    private fun createSelectPanel(): JPanel {
        val selectParamPanel = JPanel()
        val layout = GridLayout(0, 3, 10, 10)
        selectParamPanel.layout = layout


        avatarParams?.forEach { item ->
            val paramName = item.split("/").last()
            val checkbox = JCheckBox(paramName).apply {
                isSelected = subscribedMap.containsKey(item)
                addActionListener {
                    val source = it.source as JCheckBox

                    if (source.isSelected) {
                        subscribedMap[item] = JLabel("$paramName : null")
                        osc.attach(item, this@SelectListeningParam)
                    }
                    else {
                        subscribedMap.remove(item)
                        osc.detach(item, this@SelectListeningParam)
                    }

                    avatarId?.let { setting.modifyAvatarListeningParam(it, subscribedMap.keys.toList()) }
                }
            }
            selectParamPanel.add(checkbox)
        }
        var widthWhenThree = 0

        mainPanel.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {

                if (layout.columns == 3) widthWhenThree = selectParamPanel.width
                if (widthWhenThree == 0) return

                layout.columns = if (mainPanel.width < widthWhenThree + 50) 1 else 3

                selectParamPanel.revalidate()
                selectParamPanel.repaint()
            }
        })

        return selectParamPanel
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun whenAvatarChanged(avtrId: String, params: List<String>) {
        GlobalScope.launch {
            avatarId = avtrId
            subscribedMap.keys.forEach {
                osc.detach(it, this@SelectListeningParam)
            }

            val avatarJson = parser.getAvatarParam(avtrId) ?: return@launch
            avatarName = avatarJson.name
            avatarParams = avatarJson.parameters.map { it.output.address }

            subscribedMap.clear()
            params.forEach {
                val paramName = it.split("/").last()
                subscribedMap[it] = JLabel("$paramName : null")
                osc.attach(it, this@SelectListeningParam)
            }

            contentPane.removeAll()
            contentPane = createMainUI()
            contentPane.revalidate()
            contentPane.repaint()
        }.start()
    }

    override fun gotUpdate(message: OSCMessage) {
        subscribedMap[message.address]?.let {
            val value = when (message.info.argumentTypeTags) {
                "T", "F" -> message.arguments.firstOrNull() as Boolean?
                "f" -> message.arguments.firstOrNull() as Float?
                "i" -> message.arguments.firstOrNull() as Int?
                "s" -> message.arguments.firstOrNull() as String?
                else -> throw RuntimeException("invalid info: ${message.info.argumentTypeTags}")
            }

            it.text = "${message.address.split("/").last()} : $value"
        }

    }

    override fun gotUpdate(avtrId: String, params: List<String>) {
        whenAvatarChanged(avtrId, params)
    }

}

