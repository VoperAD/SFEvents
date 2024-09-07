package io.github.voperad.sfevents.configs

import org.bukkit.configuration.file.FileConfiguration

interface ConfigHandler {
    fun process(path: String, config: FileConfiguration): Any?
}