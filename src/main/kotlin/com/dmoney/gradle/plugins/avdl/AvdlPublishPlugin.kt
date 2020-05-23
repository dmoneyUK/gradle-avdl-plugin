package com.dmoney.gradle.plugins.avdl

import mu.KLogger
import mu.KotlinLogging
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.jvm.tasks.Jar
import javax.inject.Inject

class AvdlPublishPlugin @Inject constructor(val softwareComponentFactory: SoftwareComponentFactory) : Plugin<Project> {

    private val logger: KLogger = KotlinLogging.logger {}

    override fun apply(project: Project) {
        logger.info("Configuring avro publish plugin for project ${project.name}")

        val outgoing: Configuration = createOutgoingConfiguration(project)
        attachAvroJar(project)
        configurationPublication(project, outgoing)
    }


    private fun createOutgoingConfiguration(project: Project): Configuration =
        project.configurations.create("avroPublish") { conf ->
            conf.isCanBeResolved = false
            conf.isCanBeConsumed = true
            conf.attributes {
                it.attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category::class.java, Category.LIBRARY))
            }
        }

    private fun attachAvroJar(project: Project) {
        val avdlJar: Jar = project.tasks.create("avdlJar", Jar::class.java) {
            it.dependsOn("classes")
            it.from("src/main/avro")
            it.archiveClassifier.set("avdl")
        }

        project.artifacts.add("avroPublish", avdlJar)
    }

    private fun configurationPublication(project: Project, outgoing: Configuration) {

        (project.components.findByName("java") as AdhocComponentWithVariants)
            .addVariantsFromConfiguration(outgoing) {
                it.mapToMavenScope("runtime")
                it.mapToOptional()
            }
    }
}