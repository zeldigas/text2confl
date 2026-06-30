package com.github.zeldigas.text2confl.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import com.github.zeldigas.confclient.*
import com.github.zeldigas.confclient.model.Attachment
import com.github.zeldigas.confclient.model.PageProperty
import com.github.zeldigas.text2confl.core.ServiceProvider
import com.github.zeldigas.text2confl.core.config.readDirectoryConfig
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.readText
import kotlin.io.path.writeText

class Doctor : CliktCommand(name = "doctor") {
    override fun aliases() = mapOf("confl" to listOf("confluence"))
    override fun run() {}
    override fun help(context: Context) = "Checks connectivity and permissions for configured services"
}

class DoctorConfluence : CliktCommand(name = "confluence"), WithConfluenceServerOptions {

    override val confluenceUrl: Url? by confluenceUrl()
    override val confluenceUser: String? by confluenceUser()
    override val confluencePassword: String? by confluencePassword()
    override val accessToken: String? by accessToken()
    override val skipSsl: Boolean? by skipSsl()
    override val requestsPerSecond: Int? by requestsPerSecond()
    override val httpLogLevel: LogLevel by httpLoggingLevel()
    override val httpRequestTimeout: Long? by httpRequestTimeout()
    override val confluenceCloud: Boolean? by confluenceCloudFlag()

    private val spaceKey: String? by confluenceSpace()
    private val parentId: String? by option("--parent-id", help = "Id of parent page to create the test page under")
    private val parentTitle: String? by option(
        "--parent",
        help = "Title of parent page to create the test page under. Lower priority than --parent-id"
    )
    private val configPath: Path by option(
        "--config",
        help = "Path to directory containing .text2confl.yml config. Defaults to current directory"
    ).path(canBeFile = false, canBeDir = true).default(Path.of("."))

    private val serviceProvider: ServiceProvider by requireObject()

    override fun help(context: Context) = "Tests connectivity and permissions against Confluence"

    override fun run() {
        try {
            configureRequestLoggingIfEnabled()
            runBlocking { runChecks() }
        } catch (ex: Exception) {
            tryHandleException(ex)
        }
    }

    private suspend fun runChecks() {
        val directoryConfig = readDirectoryConfig(configPath)
        val server = confluenceUrl ?: directoryConfig.server?.let { Url(it) }
        ?: parameterMissing("Confluence url", "--confluence-url", "server")
        val space = spaceKey ?: directoryConfig.space
        ?: parameterMissing("Space", "--space", "space")

        val clientConfig = httpClientConfig(server, directoryConfig.client, directoryConfig.confluenceCloud)
        val client = serviceProvider.createConfluenceClient(clientConfig, false)

        val isScopedToken = server.toString().startsWith("https://api.atlassian.com/ex/confluence/", ignoreCase = true)
        val runner = DoctorRunner(
            client = client,
            space = space,
            parentId = parentId ?: directoryConfig.defaultParentId,
            parentTitle = parentTitle ?: directoryConfig.defaultParent,
            isCloud = clientConfig.cloudApi,
            isScopedToken = isScopedToken
        )

        terminal.println("Running Confluence access checks...")
        terminal.println()

        val reporter = PrintingDoctorReporter { terminal.println(it) }
        val results = runner.run(reporter)

        terminal.println()
        val passed = results.count { it.result is StepResult.Pass }
        val failed = results.count { it.result is StepResult.Fail }
        val skipped = results.count { it.result is StepResult.Skipped }
        if (failed == 0) {
            terminal.println("All checks passed ($passed/${results.size}).")
        } else {
            terminal.println("$failed check(s) failed, $passed passed, $skipped skipped.")
        }
    }

    override fun askForSecret(prompt: String, requireConfirmation: Boolean): String? =
        promptForSecret(prompt, requireConfirmation)
}

sealed class StepResult {
    object Pass : StepResult()
    data class Fail(val error: String, val hint: String) : StepResult()
    object Skipped : StepResult()
}

data class StepReport(val name: String, val result: StepResult)

interface DoctorReporter {
    fun onStep(report: StepReport)
}

class PrintingDoctorReporter(private val println: (String) -> Unit) : DoctorReporter {
    override fun onStep(report: StepReport) {
        val prefix = when (report.result) {
            is StepResult.Pass -> "[ OK  ]"
            is StepResult.Fail -> "[FAIL ]"
            is StepResult.Skipped -> "[SKIP ]"
        }
        println("$prefix ${report.name}")
        if (report.result is StepResult.Fail) {
            println("         Error: ${report.result.error}")
            println("         Hint:  ${report.result.hint}")
        }
    }
}

private const val ATTACHMENT_CONTENT = "text2confl doctor test attachment"

class DoctorRunner(
    private val client: ConfluenceClient,
    private val space: String,
    private val parentId: String?,
    private val parentTitle: String?,
    private val isCloud: Boolean,
    private val isScopedToken: Boolean
) {
    private var spaceHomepageId: String? = null
    private var resolvedParentId: String? = null
    private var createdPageId: String? = null
    private var createdPageTitle: String? = null
    private var createdPageVersion: Int = 1
    private var createdProperty: PageProperty? = null
    private var createdAttachment: Attachment? = null

    suspend fun run(reporter: DoctorReporter): List<StepReport> {
        val blocked = mutableSetOf<String>()
        val results = mutableListOf<StepReport>()

        suspend fun step(name: String, vararg deps: String, action: suspend () -> Unit) {
            val report = if (deps.any { it in blocked }) {
                blocked.add(name)
                StepReport(name, StepResult.Skipped)
            } else {
                try {
                    action()
                    StepReport(name, StepResult.Pass)
                } catch (e: Exception) {
                    blocked.add(name)
                    StepReport(name, StepResult.Fail(e.message ?: e.javaClass.simpleName, hintFor(name, e)))
                }
            }
            reporter.onStep(report)
            results.add(report)
        }

        val suffix = UUID.randomUUID().toString().take(8)
        val testPageTitle = "text2confl test $suffix"
        val updateOptions = PageUpdateOptions(notifyWatchers = false, message = null)

        step("describe-space") {
            val spaceInfo = client.describeSpace(space, includeHome = false)
            spaceHomepageId = spaceInfo.homepageId
        }

        step("get-page", "describe-space") {
            val metadata = setOf(
                SimplePageLoadOptions.Version,
                SimplePageLoadOptions.Content, SimplePageLoadOptions.Labels
            )
            val page = when {
                parentId != null -> {
                    client.getPageById(parentId, metadata)
                }

                parentTitle != null -> client.getPage(space, parentTitle, metadata)
                else -> client.getPageById(
                    spaceHomepageId ?: error("Space '$space' has no homepage. Specify --parent-id or --parent."),
                    metadata
                )
            }
            resolvedParentId = page.id
        }

        step("create-page", "get-page") {
            val page = client.createPage(
                PageContentInput(
                    parentPage = resolvedParentId,
                    title = testPageTitle,
                    content = "<p>text2confl doctor test page — safe to delete</p>",
                    space = space
                ),
                updateOptions
            )
            createdPageId = page.id
            createdPageTitle = page.title
            createdPageVersion = page.version?.number ?: 1
        }

        step("find-child-pages", "create-page") {
            client.findChildPages(createdPageId!!)
        }

        step("add-labels", "create-page") {
            client.addLabels(createdPageId!!, listOf("text2confl-doctor"))
        }

        step("read-labels", "add-labels") {
//            delay(5.seconds)
            val page = client.getPageById(createdPageId!!, setOf(SimplePageLoadOptions.Labels))
            check(page.labels?.any { it.name == "text2confl-doctor" } == true) {
                "Label 'text2confl-doctor' not found on page after adding it"
            }
        }

        step("create-property", "create-page") {
            client.createPageProperty(createdPageId!!, "text2confl-doctor", PagePropertyInput.newProperty("initial"))
            val page = client.getPageById(createdPageId!!, setOf(PagePropertyLoad("text2confl-doctor")))
            createdProperty = page.pageProperty("text2confl-doctor")
                ?: error("Property 'text2confl-doctor' not found after creating it")
        }

        step("add-attachment", "create-page") {
            val tempFile = Files.createTempFile("text2confl-doctor", ".txt")
            try {
                tempFile.writeText(ATTACHMENT_CONTENT)
                val input = PageAttachmentInput(
                    name = "text2confl-doctor.txt",
                    content = tempFile,
                    fileSize = Files.size(tempFile),
                    comment = "Doctor test attachment"
                )
                val attachments = client.addAttachments(createdPageId!!, listOf(input))
                createdAttachment = attachments.results.first()
            } finally {
                Files.deleteIfExists(tempFile)
            }
        }

        step("download-attachment", "add-attachment") {
            val tempFile = Files.createTempFile("text2confl-doctor-download", ".txt")
            try {
                client.downloadAttachment(createdAttachment!!, tempFile)
                val content = tempFile.readText()
                check(content.trimEnd() == ATTACHMENT_CONTENT) {
                    "Downloaded content does not match uploaded content"
                }
            } finally {
                Files.deleteIfExists(tempFile)
            }
        }

        step("update-page", "create-page") {
            val nextVersion = createdPageVersion + 1
            val page = client.updatePage(
                createdPageId!!,
                PageContentInput(
                    parentPage = resolvedParentId,
                    title = createdPageTitle!!,
                    content = "<p>text2confl doctor test page — updated</p>",
                    space = space,
                    version = nextVersion
                ),
                updateOptions
            )
            createdPageVersion = page.version?.number ?: nextVersion
        }

        step("rename-page", "create-page") {
            val currentPage = client.getPageById(createdPageId!!, setOf(SimplePageLoadOptions.Version))
            val renamed = client.renamePage(currentPage, "${createdPageTitle!!} (renamed)", updateOptions)
            createdPageTitle = renamed.title
        }

        step("update-property", "create-property") {
            client.updatePageProperty(
                createdPageId!!,
                createdProperty!!,
                PagePropertyInput.updateOf(createdProperty!!, "updated")
            )
        }

        step("update-attachment", "add-attachment") {
            val tempFile = Files.createTempFile("text2confl-doctor-upd", ".txt")
            try {
                tempFile.writeText("text2confl doctor test attachment — updated")
                val input = PageAttachmentInput(
                    name = "text2confl-doctor.txt",
                    content = tempFile,
                    fileSize = Files.size(tempFile),
                    comment = "Doctor test attachment updated"
                )
                client.updateAttachment(createdPageId!!, createdAttachment!!.id, input)
            } finally {
                Files.deleteIfExists(tempFile)
            }
        }

        step("delete-label", "add-labels") {
            client.deleteLabel(createdPageId!!, "text2confl-doctor")
        }

        // Cleanup steps: depend only on the resource-creating step, not intermediate ones
        step("delete-attachment", "add-attachment") {
            client.deleteAttachment(createdAttachment!!.id)
        }

        step("delete-page", "create-page") {
            client.deletePage(createdPageId!!)
        }

        return results
    }

    private fun hintFor(stepName: String, exception: Exception): String {
        val isAuthError = exception is ConfluenceAuthorizationException
        return when (stepName) {
            "describe-space" -> when {
                isAuthError && isCloud && isScopedToken ->
                    "Scoped token needs granular scope 'read:space:confluence'."

                isAuthError && isCloud ->
                    "Verify your API token is valid and the user has read access to space '$space'."

                isAuthError ->
                    "Verify credentials and that the user has permission to access space '$space'."

                else ->
                    "Check that space '$space' exists and that the server URL is correct."
            }

            "get-page" -> when {
                isAuthError && isCloud && isScopedToken ->
                    "Scoped token needs 'read:page:confluence' scope."

                isAuthError ->
                    "Ensure the user has read access to the parent page in space '$space'."

                else ->
                    "Check that the parent page exists and is accessible."
            }

            "create-page" -> when {
                isAuthError && isCloud && isScopedToken ->
                    "Scoped token needs 'write:page:confluence' scope."

                isAuthError && isCloud ->
                    "Ensure the user has 'Add' page permission in space '$space'."

                isAuthError ->
                    "Ensure the user has 'Create' page permission in space '$space'."

                else ->
                    "Check that the parent page exists and you have write access to it."
            }

            "find-child-pages" -> when {
                isAuthError && isCloud && isScopedToken ->
                    "Scoped token needs 'read:hierarchical-content:confluence' scope."

                isAuthError ->
                    "Ensure the user has read access to pages in space '$space'."

                else ->
                    "Check that child page listing is accessible."
            }

            "add-labels" -> when {
                isAuthError && isCloud && isScopedToken ->
                    "Scoped token needs 'write:label:confluence' scope."

                isAuthError ->
                    "Ensure the user has permission to add labels to pages in space '$space'."

                else ->
                    "Check that the page exists and labels can be added."
            }

            "read-labels" -> when {
                isAuthError && isCloud && isScopedToken ->
                    "Scoped token needs 'read:page:confluence' and 'read:label:confluence' scopes."

                isAuthError ->
                    "Ensure the user has read access to labels in space '$space'."

                else ->
                    "Labels were added but could not be verified on read-back. This may be a timing issue."
            }

            "create-property" -> when {
                isAuthError && isCloud && isScopedToken ->
                    "Scoped token needs 'write:page:confluence' scope (page properties are covered by the page write scope)."

                isAuthError ->
                    "Ensure the user has permission to set page properties in space '$space'."

                else ->
                    "Check that page properties are supported and accessible."
            }

            "add-attachment" -> when {
                isAuthError && isCloud && isScopedToken ->
                    "Scoped token needs 'write:confluence-file' scope."

                isAuthError ->
                    "Ensure the user has permission to add attachments to pages in space '$space'."

                else ->
                    "Check that attachments are enabled for this space."
            }

            "download-attachment" -> when {
                isAuthError && isCloud && isScopedToken ->
                    "Scoped token needs 'read:attachment:confluence' scope."

                isAuthError ->
                    "Ensure the user has read access to attachments in space '$space'."

                else ->
                    "Check that the attachment can be downloaded and its content is readable."
            }

            "update-page" -> when {
                isAuthError && isCloud && isScopedToken ->
                    "Scoped token needs 'write:page:confluence' scope."

                isAuthError ->
                    "Ensure the user has 'Edit' page permission in space '$space'."

                else ->
                    "Check that the page can be updated."
            }

            "rename-page" -> when {
                isAuthError && isCloud && isScopedToken ->
                    "Scoped token needs 'read:page:confluence' and 'write:page:confluence' scopes."

                isAuthError ->
                    "Ensure the user has 'Edit' page permission in space '$space'."

                else ->
                    "Check that the page can be renamed. You may need to clean up: page id=$createdPageId"
            }

            "update-property" -> when {
                isAuthError && isCloud && isScopedToken ->
                    "Scoped token needs 'write:page:confluence' scope (page properties are covered by the page write scope)."

                isAuthError ->
                    "Ensure the user has permission to update page properties."

                else ->
                    "Check that the property was created and can be updated."
            }

            "update-attachment" -> when {
                isAuthError && isCloud && isScopedToken ->
                    "Scoped token needs 'write:confluence-file' scope."

                isAuthError ->
                    "Ensure the user has permission to update attachments."

                else ->
                    "Check that the attachment was uploaded and can be updated."
            }

            "delete-label" -> when {
                isAuthError && isCloud && isScopedToken ->
                    "Scoped token needs 'write:label:confluence' scope."

                isAuthError ->
                    "Ensure the user has permission to delete labels from pages."

                else ->
                    "Check that the label exists and can be removed."
            }

            "delete-attachment" -> when {
                isAuthError && isCloud && isScopedToken ->
                    "Scoped token needs granular scope 'delete:attachment:confluence' (no classic equivalent)."

                isAuthError ->
                    "Ensure the user has permission to delete attachments."

                else ->
                    "Check that the attachment exists and can be deleted. You may need to delete it manually: attachment id=${createdAttachment?.id}"
            }

            "delete-page" -> when {
                isAuthError && isCloud && isScopedToken ->
                    "Scoped token needs granular scope 'delete:page:confluence' (no classic equivalent)."

                isAuthError && isCloud ->
                    "Ensure the user has 'Delete' page permission in space '$space'."

                isAuthError ->
                    "Ensure the user has page delete permission (may require space admin role on Server/DC)."

                else ->
                    "Check that the page can be deleted. You may need to delete it manually: page id=$createdPageId, title='$createdPageTitle'"
            }

            else -> "Check Confluence server logs for details."
        }
    }
}
