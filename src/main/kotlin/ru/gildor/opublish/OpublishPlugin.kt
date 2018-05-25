package ru.gildor.opublish

import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.BintrayPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.internal.artifacts.ArtifactPublicationServices
import org.gradle.api.internal.artifacts.BaseRepositoryFactory
import org.gradle.api.internal.artifacts.dsl.DefaultRepositoryHandler
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.internal.reflect.Instantiator
import org.gradle.internal.service.ServiceRegistry
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.delegateClosureOf
import org.gradle.kotlin.dsl.get
import javax.inject.Inject

@Suppress("unused")
open class OpublishPlugin @Inject constructor(
    private val services: ArtifactPublicationServices,
    private val instantiator: Instantiator
) : Plugin<Project> {

    init {

    }

    override fun apply(project: Project) {
        val repositoryHandler = services.createRepositoryHandler()

        project.extensions.create(
                "opublish",
                OpublishPluginExtension::class.java,
                repositoryHandler
        )

        project.afterEvaluate {
            initTasks(project)
        }
    }

    private val publicationId = "OPublication"

    private fun initTasks(project: Project) {
        val config = project.extension<OpublishPluginExtension>()
        configureMaven(project, config)
        configureBintray(project, config)
    }

    private fun configureMaven(project: Project, config: OpublishPluginExtension) {
        project.plugins.withType(JavaPlugin::class.java) {
            project.plugins.withType(MavenPublishPlugin::class.java) {
                val publishing = project.extension<PublishingExtension>()
                publishing.apply {
                    publications {
                        create(publicationId, MavenPublication::class.java) {
                            from(project.components["java"])
                            if (config.publishSources) {
                                artifact(project.createSourceTask())
                            }
                            if (config.publishJavadoc) {
                                artifact(project.createJavadocTask())
                            }
                            repositories {
                                maven {  }
                            }
                            pom.withXml {
                                configurePom(project, config)
                            }
                        }
                    }
                }
            }
        }
    }

    //TODO: configure dokka
    /*val dokka by tasks.getting(DokkaTask::class) {
    outputFormat = "javadoc"
    outputDirectory = "$buildDir/javadoc"

    externalDocumentationLink(delegateClosureOf<DokkaConfiguration.ExternalDocumentationLink.Builder> {
        url = URL("https://square.github.io/okhttp/3.x/okhttp/")
    })
}*/

    private fun Project.createJavadocTask(): Jar {
        return tasks.create("opublishJavadocJar", Jar::class.java) {
            //    dependsOn(dokka)
            classifier = "javadoc"
            from("$buildDir/javadoc")
        }
    }

    private fun Project.createSourceTask(): Jar {
        return tasks.create("opublishSourcesJar", Jar::class.java) {
            dependsOn("classes")
            classifier = "sources"
            val java = convention<JavaPluginConvention>()
            from(java.sourceSets["main"].allSource)
        }
    }

    private fun XmlProvider.configurePom(project: Project, config: OpublishPluginExtension) {
        NodeScope(asNode()) {
            "name" to project.name
            "description" to project.description.toString()
            "url" to "${config.repo?.website}"
            val developer = config.developer
            if (developer != null) {
                "developers" {
                    "developer" {
                        "name" to developer.name
                        "email" to developer.email
                        "organizationUrl" to developer.url
                    }
                }
            }
            val repo = config.repo
            if (repo != null) {
                if (repo.issueTracker != null) {
                    "issueManagement" {
                        "url" to "${repo.issueTracker}"
                    }
                }
                "scm" {
                    "url" to repo.issueTracker
                    "connection" to "scm:git:${repo.issueTracker}"
                    //TODO: Check this property
                    // "developerConnection" to "scm:git:$repoVcs"
                    "tag" to config.releaseTag
                }
            }
            if (config.licenses.isNotEmpty()) {
                "licenses" {
                    config.licenses.forEach { license ->
                        "license" {
                            "name" to license.title
                            "url" to license.url
                        }
                    }

                }
            }
        }
    }

    private fun configureBintray(project: Project, config: OpublishPluginExtension) {
        project.plugins.withType(BintrayPlugin::class.java) {
            val bintray = requireNotNull(project.extensions.findByType(BintrayExtension::class.java)) {
                "Cannot configure bintray, probably incompatible version of Bintray plugin"
            }
            bintray.apply {
                user = project.properties["bintray.user"]?.toString()
                key = project.properties["bintray.key"]?.toString()
                setPublications(publicationId)
                publish = true
                pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
                    repo = project.properties["bintray.repo"]?.toString() ?: "maven"
                    name = project.name
                    desc = project.description
                    val repo = config.repo
                    if (repo != null) {
                        websiteUrl = repo.website
                        issueTrackerUrl = repo.issueTracker
                        vcsUrl = repo.vcs
                    }
                    val github = config.repo as? GithubRepo
                    if (github != null) {
                        githubRepo = "${github.user}/${github.project}"
                        githubReleaseNotesFile = findChangelog()
                    }
                    if (config.licenses.isNotEmpty()) {
                        setLicenses(*config.licenses.map { it.id }.toTypedArray())
                    }
                    if (config.labels.isNotEmpty()) {
                        setLabels(*config.labels.toTypedArray())
                    }
                    version(delegateClosureOf<BintrayExtension.VersionConfig> {
                        name = project.version.toString()
                        vcsTag = config.releaseTag
                        //TODO: check how we can disable maven publishing
                        mavenCentralSync(delegateClosureOf<BintrayExtension.MavenCentralSyncConfig> {
                            sync = project.properties["sonatype.user"] != null ?: sync
                            user = project.properties["sonatype.user"]?.toString() ?: user
                            password = project.properties["sonatype.password"]?.toString() ?: password
                            close = "true"
                        })
                    })
                })
            }
        }
    }

    //TODO: Implement changelog search
    private fun findChangelog(): String? = "CHANGELOG.md"

}

private inline fun <reified T : Any> Project.extension(): T {
    return extensions.getByType(T::class.java)
}

private inline fun <reified T : Any> Project.convention(): T {
    return convention.getPlugin(T::class.java)
}
