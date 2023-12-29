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
import websocket.WebSocketClient
import websocket.WebSocketServer
import websocket.type.DEFAULT_WS_URL
import java.awt.*
import java.awt.event.*
import java.util.*
import javax.swing.*
import javax.swing.SpringLayout.*
import javax.swing.Timer
import javax.swing.border.EmptyBorder
import javax.swing.border.LineBorder


class SelectListeningParam : JFrame("title"), OSCSubscriber, AvatarSubscriber {

    private val subscribedMap = HashMap<String, JLabel>()
    private var avatarName: String? = null
    private var avatarId: String? = null
    private var avatarParams: List<String>? = null

    private var wsServer: WebSocketServer? = null
    private val wsClient: MutableList<WebSocketClient> = mutableListOf()

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
        window.border = LineBorder(Color.GREEN, 3)
        val button = JButton("setting")
        val wsSettingButton = JButton("외부연결 설정")
        val nowAvatar = JLabel("Now Using Avatar : $avatarName")
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

        wsSettingButton.addActionListener {
            window.removeAll()
            createManageConnectionPanel(window)
            window.revalidate()
            window.repaint()
        }

        val scroll =
            JScrollPane(window, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)
        scroll.verticalScrollBar.unitIncrement = 16

        mainPanel.apply {
            add(scroll)
            add(button)
            add(wsSettingButton)
            add(nowAvatar)
        }

        val layout = SpringLayout().apply {
            putConstraint(NORTH, nowAvatar, 10, NORTH, mainPanel)
            putConstraint(WEST, nowAvatar, 10, WEST, mainPanel)

            putConstraint(SOUTH, button, -10, SOUTH, mainPanel)
            putConstraint(EAST, button, -10, EAST, mainPanel)

            putConstraint(NORTH, scroll, 10, SOUTH, nowAvatar)
            putConstraint(EAST, scroll, -10, EAST, mainPanel)
            putConstraint(WEST, scroll, 10, WEST, mainPanel)
            putConstraint(SOUTH, scroll, -10, NORTH, button)

            putConstraint(EAST, wsSettingButton, -10, WEST, button)
            putConstraint(SOUTH, wsSettingButton, 0, SOUTH, button)
        }

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

    private fun createManageConnectionPanel(panel: JPanel) {
        val layout = BorderLayout()
        panel.layout = layout
        panel.border = LineBorder(Color.BLUE, 3)
        panel.size = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
        var listModel = DefaultListModel<String>().apply {
            addAll(wsClient.map { it.toString() })
        }
        val list = JList(listModel)
        fun refresh() {
            panel.removeAll()
            createManageConnectionPanel(panel)
            panel.revalidate()
            panel.repaint()
        }


        val clientAddButton = JButton("연결(클라이언트) 추가")
        val listSelected = object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount != 2 && !SwingUtilities.isRightMouseButton(e)) return
                val list = e.source as JList<String>
                val index = list.locationToIndex(e.point)

                val ans = JOptionPane.showConfirmDialog(
                    this@SelectListeningParam,
                    "해당 연결을 종료하시겠습니까?",
                    wsClient[index].toString(),
                    JOptionPane.YES_NO_OPTION
                )

                if (ans != JOptionPane.YES_OPTION) return
                wsClient[index].closeSession()
            }
        }

        fun addClientPanel(): JPanel {
            val addClientPanel = JPanel(GridLayout(0, 1, 20, 20))
            addClientPanel.border = EmptyBorder(20, 100, 20, 100)

            fun createInputPanel(label: JLabel, field: JTextField): JPanel {
                val inputPanel = JPanel(GridBagLayout())

                val gbc = GridBagConstraints()
                gbc.gridx = 0
                gbc.gridy = 0
                gbc.anchor = GridBagConstraints.WEST

                inputPanel.add(label, gbc)

                gbc.gridx = 1
                gbc.weightx = 1.0
                gbc.fill = GridBagConstraints.HORIZONTAL  // 수평으로 늘어나도록 설정

                inputPanel.add(field, gbc)

                return inputPanel
            }

            // IP 입력 필드
            val ipLabel = JLabel("IP:")
            val ipField = JTextField()
            val ipPanel = createInputPanel(ipLabel, ipField)

            // Port 입력 필드
            val portLabel = JLabel("Port:")
            val portField = JTextField()
            portField.text = 80.toString()
            val portPanel = createInputPanel(portLabel, portField)

            // Path 입력 필드
            val pathLabel = JLabel("Path:")
            val pathField = JTextField()
            pathField.text = DEFAULT_WS_URL + '/' + setting.myName
            val pathPanel = createInputPanel(pathLabel, pathField)

            // 라디오 박스 (ws 또는 wss 선택)
            val radioPanel = JPanel()
            radioPanel.layout = GridLayout(1, 2)
            val wsRadio = JRadioButton("ws")
            val wssRadio = JRadioButton("wss")
            val radioGroup = ButtonGroup()
            radioGroup.add(wsRadio)
            radioGroup.add(wssRadio)
            radioPanel.add(wsRadio)
            radioPanel.add(wssRadio)
            wsRadio.isSelected = true

            val addButton = JButton("추가")
            val connectTextTimer = Timer(2000, ActionListener {
                addButton.text = addButton.text + "."
            })

            val timeoutTask: TimerTask = object : TimerTask() {
                override fun run() {
                    connectTextTimer.stop()
                    addButton.text = "확인"

                    val ans = JOptionPane.showConfirmDialog(
                        this@SelectListeningParam,
                        "연결 실패",
                        "WS Client",
                        JOptionPane.OK_CANCEL_OPTION
                    )

                    panel.removeAll()
                    panel.add(addClientPanel())
                    panel.revalidate()
                    panel.repaint()
                }
            }
            val timeoutTimer = java.util.Timer("timeout")


            addButton.addActionListener {
                connectTextTimer.start()
                timeoutTimer.schedule(timeoutTask, 10000)
                val connection = WebSocketClient(
                    ip = ipField.text,
                    port = portField.text.toInt(),
                    path = pathField.text,
                    isWss = wssRadio.isSelected,
                    openCallback = { serverName, client ->
                        timeoutTask.cancel()
                        timeoutTimer.cancel()
                        timeoutTimer.purge()
                        connectTextTimer.stop()
                        addButton.text = "확인"
                        wsClient.add(client)
                        val ans = JOptionPane.showConfirmDialog(
                            this,
                            "연결 성공 : $serverName",
                            "WS Client",
                            JOptionPane.OK_CANCEL_OPTION
                        )

                        refresh()
                    },
                    closeCallback = { reason, client ->
                        wsClient.remove(client)
                        println(wsClient)
                        refresh()
                    },
                    connErrCallback = {
                        it.printStackTrace()
                        timeoutTask.cancel()
                        timeoutTimer.cancel()
                        timeoutTimer.purge()
                        connectTextTimer.stop()
                        addButton.text = "확인"

                        val ans = JOptionPane.showConfirmDialog(
                            this,
                            "연결 실패",
                            "WS Client",
                            JOptionPane.OK_CANCEL_OPTION
                        )

                        panel.removeAll()
                        panel.add(addClientPanel())
                        panel.revalidate()
                        panel.repaint()
                    }
                )

                val bound = mainPanel.bounds

                addButton.isEnabled = false
                addButton.text = "연결 중"
            }

            addClientPanel.add(ipPanel)
            addClientPanel.add(portPanel)
            addClientPanel.add(pathPanel)
            addClientPanel.add(radioPanel)
            addClientPanel.add(addButton)

            return addClientPanel
        }

        list.addMouseListener(listSelected)
        clientAddButton.addActionListener {
            panel.removeAll()
            panel.add(addClientPanel())
            panel.revalidate()
            panel.repaint()
        }

        panel.apply {
            add(list, BorderLayout.CENTER)
            add(clientAddButton, BorderLayout.SOUTH)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun whenAvatarChanged(avtrId: String, params: List<String>) {
        GlobalScope.launch {
            println(avtrId)
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

