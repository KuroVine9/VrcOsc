package config

import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

class Config {
    private val PATH = "src/main/resources/config.properties"
    private val properties = Properties().apply { load(FileInputStream(PATH)) }

    operator fun get(key: String): String = properties[key] as String
    operator fun set(key: String, value: String) {
        properties[key] = value
        properties.store(FileOutputStream(PATH), null)
    }
}
