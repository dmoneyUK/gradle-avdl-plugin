package com.dmoney.gradle.plugins.avdl

import com.commercehub.gradle.plugin.avro.GenerateAvroJavaTask
import com.commercehub.gradle.plugin.avro.GenerateAvroProtocolTask
import mu.KotlinLogging
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import java.io.File

class AvdlGeneratePlugin : Plugin<Project> {

    private val logger = KotlinLogging.logger {}

    override fun apply(project: Project) {
        logger.info("Configuring avro packing plugin for project ${project.name}")

        project.configurations.create("avdl")
        project.configurations.getByName("avdl").isTransitive = false

        (project.getProperties().get("sourceSets") as SourceSetContainer).getByName("main")
            .java.srcDirs.add(File("build/generated-avro-main-java"))

        configTask(project)
    }

    fun configTask(project: Project) {

        val AVRO_TASK_GROUP = "avro packing"
        val gatherAvdls = project.tasks.create("gatherAvdls", GatherAvdlsTask::class.java) {
            it.group = AVRO_TASK_GROUP
        }

        val generateAvroProtocol = project.tasks.create("generateAvroProtocol", GenerateAvroProtocolTask::class.java) {
            it.dependsOn(gatherAvdls)
            it.group = AVRO_TASK_GROUP
            it.source(project.file("${project.buildDir}/avdl"))
            it.includes.add("**/*.avdl")
            it.setOutputDir(project.file("${project.buildDir}/generated-avro-main-avpr"))
        }

        val generateAvroJava = project.tasks.create("generateAvroJava", GenerateAvroJavaTask::class.java) {
            it.dependsOn(generateAvroProtocol)
            it.group = AVRO_TASK_GROUP
            it.source(project.file("${project.buildDir}/generated-avro-main-avpr"))
            it.includes.add("**/*.avpr")
            it.setOutputDir(project.file("build/generated-avro-main-java"))
        }

        project.tasks.findByName("compileJava")?.dependsOn(generateAvroJava)
        project.tasks.findByName("compileKotlin")?.dependsOn(generateAvroJava)

    }
}

object GatherAvdlsTask : DefaultTask() {
    private val logger = KotlinLogging.logger {}

    private val outputLocation = "${project.buildDir}/avdl"

    @TaskAction
    fun exec() {
        project.configurations.getByName("avdl").resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
            project.copy {
                logger.info("gatherAvdls: copy zipTree ${artifact.getFile()} to ${project.buildDir}/avdl")
                it.from(project.zipTree(artifact.file))
                it.into(outputLocation)
            }
        }

        project.copy {
            logger.info("gatherAvdls: copy local avdl from src/main/avro to ${project.buildDir}/avdl")
            it.from("src/main/avro")
            it.into(outputLocation)
        }
    }

}