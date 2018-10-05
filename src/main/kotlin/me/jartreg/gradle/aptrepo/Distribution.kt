package me.jartreg.gradle.aptrepo

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil
import java.time.Instant

class Distribution(val name: String, private val project: Project) {
    val components = mutableMapOf<String, Component>()

    var origin: String? = null
    var label: String? = null
    var description: String? = null
    var version: String? = null

    var suite = name
    var codename: String? = null

    var date = Instant.now()

    var additionalFields = mutableMapOf<String, String>()

    fun component(name: String) = components.computeIfAbsent(name) { Component(name, project) }

    fun component(name: String, function: Component.() -> Unit): Component {
        return component(name).also { it.function() }
    }

    fun component(name: String, closure: Closure<Unit>): Component = ConfigureUtil.configure(closure, component(name))
}
