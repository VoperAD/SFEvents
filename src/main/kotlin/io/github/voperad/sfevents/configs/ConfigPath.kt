package io.github.voperad.sfevents.configs

import kotlin.reflect.KClass

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class ConfigPath(val path: String, val handler: KClass<out ConfigHandler> = DefaultConfigHandler::class)

