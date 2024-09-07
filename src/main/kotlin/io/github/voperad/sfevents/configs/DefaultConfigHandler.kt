package io.github.voperad.sfevents.configs

import org.bukkit.configuration.file.FileConfiguration

object DefaultConfigHandler : ConfigHandler {
    override fun process(path: String, config: FileConfiguration): Any? {
        return config.get(path)
    }
}