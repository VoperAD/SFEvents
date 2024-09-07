package io.github.voperad.sfevents.configs

import io.github.voperad.sfevents.events.configs.BaseConfiguration
import io.github.voperad.sfevents.log
import io.github.voperad.sfevents.managers.EventType
import kotlinx.serialization.Transient
import org.bukkit.configuration.file.FileConfiguration
import java.io.File
import java.util.logging.Level
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

object ConfigUtils {

    fun instantiateConfiguration(fileConfiguration: FileConfiguration, file: File): BaseConfiguration? {
        val eventType = fileConfiguration.getString("event-type")?.let { EventType.valueOf(it) } ?: run {
            log(Level.WARNING, "Failed to load event type from ${file.absolutePath}")
            return null
        }

        val kClass = getClass(eventType) ?: return null
        val instance = kClass.constructors.firstOrNull { it.parameters.isEmpty() }?.call() ?: return null

        if (setProperties(instance, fileConfiguration, file.absolutePath)) {
            return instance
        }

        return null
    }

    @Suppress("UNCHECKED_CAST")
    private fun getClass(eventType: EventType): KClass<out BaseConfiguration>? {
        val className = eventType.className

        return try {
            Class.forName("io.github.voperad.sfevents.events.configs.$className").kotlin as? KClass<out BaseConfiguration>
        } catch (e: ClassNotFoundException) {
            try {
                Class.forName(className).kotlin as? KClass<out BaseConfiguration>
            } catch (e: ClassNotFoundException) {
                log(Level.WARNING, "Failed to load class $className")
                null
            }
        }
    }

    private fun setProperties(instance: BaseConfiguration, fileConfiguration: FileConfiguration, filePath: String): Boolean {
        getProperties(instance::class).forEach { prop ->
            prop.isAccessible = true
            getPropertyValue(prop, fileConfiguration, filePath)?.let {
                prop.set(instance, it)
            } ?: return false
        }

        return true
    }

    @Suppress("UNCHECKED_CAST")
    private fun getProperties(kClass: KClass<out BaseConfiguration>): List<KMutableProperty1<BaseConfiguration, Any?>> {
        return kClass.memberProperties
            .filter { it is KMutableProperty1<*, *> }
            .filterNot { it.hasAnnotation<Transient>() || it.isConst }
            .map { it as KMutableProperty1<BaseConfiguration, Any?> }
    }

    private fun getPropertyValue(property: KProperty<*>, fileConfiguration: FileConfiguration, filePath: String): Any? {
        val value: Any? = property.findAnnotation<ConfigPath>()?.let {
            val handler = it.handler.objectInstance ?: it.handler.createInstance()
            handler.process(it.path, fileConfiguration)
        } ?: fileConfiguration.get(getPath(property))

        return value ?: run {
            log(Level.SEVERE, "Failed to load property \"${property.name}\" from $filePath")
            null
        }
    }

    private fun getPath(property: KProperty<*>): String {
        return property.name.map { char ->
            if (char.isUpperCase()) "-${char.lowercase()}" else char.toString()
        }.joinToString("")
    }

}