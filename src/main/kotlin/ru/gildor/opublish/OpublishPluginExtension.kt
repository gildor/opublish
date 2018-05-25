package ru.gildor.opublish

import org.gradle.api.Action
import org.gradle.api.artifacts.dsl.RepositoryHandler

open class OpublishPluginExtension(
    val repositories: RepositoryHandler
) {
    //TODO: add `@Input` annotation
    var publishTo: PublishTarget = PublishTarget.Maven
    var repo: Repo? = null
    var developer: Developer? = null
    var customLicense: License? = null
    var publishSources: Boolean = true
    var publishJavadoc: Boolean = true
    var releaseTag: String? = null
    var labels: List<String> = emptyList()

    val licenses: MutableList<License> = mutableListOf()

    fun licenses(configure: Action<in LicenseScope>) {
        configure.execute(LicenseScope(licenses))
    }
    fun licenses(scope: LicenseScope.() -> Unit) {
        scope(LicenseScope(licenses))
    }

    fun repositories(configure: Action<in RepositoryHandler>) {
        configure.execute(repositories)
    }

    fun repositories(configure: RepositoryHandler.() -> Unit) {
        repositories(Action { configure() })
    }
}

class LicenseScope(private val licenses: MutableList<License>) {
    fun apache() {
        licenses.add(ApacheLicense)
    }

    fun license(id: String, title: String = id, url: String) {
        licenses.add(
                License(id, title, url)
        )
    }

    companion object {

        object ApacheLicense : License(
                "Apache-2.0",
                "The Apache Software License, Version 2.0",
                "http://www.apache.org/licenses/LICENSE-2.0.txt"
        )
    }
}

enum class LicenseType(val id: String, val title: String = id, val url: String) {
    ;
    //TODO:
//    MIT,
//    GPLv3,
//    LGPL

}

fun LicenseType.toLicense() = License(id, title, url)

class Developer(
    val name: String,
    val email: String? = null,
    val url: String? = null
)

open class Repo(
    val vcs: String? = null,
    val website: String? = null,
    val issueTracker: String? = null
)

class GithubRepo(
    val user: String,
    val project: String,
    issueTracker: String = "https://github.com/$user/$project/issues"
) : Repo(
        website = "https://github.com/$user/$project",
        vcs = "https://github.com/$user/$project.git",
        issueTracker = issueTracker
)

open class License(val id: String, val title: String = id, val url: String)


enum class PublishTarget {
    Maven,
    Bintray,
    BintrayAndMavenCentral
    //TODO: Maybe support artifactory?
}