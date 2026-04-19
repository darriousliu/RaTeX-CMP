package io.ratex.buildlogic

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import javax.inject.Inject

abstract class RaTeXDesktopNativeExtension @Inject constructor(objects: ObjectFactory) {
    val nativeTarget: Property<String> = objects.property(String::class.java)
    val nativeFileName: Property<String> = objects.property(String::class.java)
    val artifactId: Property<String> = objects.property(String::class.java)
    val supportedHostOs: SetProperty<String> = objects.setProperty(String::class.java)
}
