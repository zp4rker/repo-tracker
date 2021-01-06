package com.zp4rker.repotracker

import com.zp4rker.discore.extenstions.embed
import com.zp4rker.discore.extenstions.toJson
import org.kohsuke.github.GHEvent
import org.kohsuke.github.GHMyself
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHubBuilder
import java.io.File
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlin.concurrent.fixedRateTimer

/**
 * @author zp4rker
 */
var debug: Boolean = true

lateinit var webhook: String
lateinit var myself: GHMyself

val cache = Cache(File("cache.txt"))

fun main(args: Array<String>) {
    println("Running RepoTracker v${Cache::class.java.`package`.implementationVersion}")

    val auth = args[0]
    webhook = args[1]
    debug = args[2].toBoolean()

    val client = GitHubBuilder().withOAuthToken(auth).build()
    myself = client.myself

    val name = myself.login
    val avatar = myself.avatarUrl

    var lastCheck = ZonedDateTime.now(ZoneOffset.UTC)
    var cacheUpdated = false

    fixedRateTimer(period = TimeUnit.MINUTES.toMillis(1)) {
        debug("Starting loop")
        for (repo in myself.listRepositories(100, GHMyself.RepositoryListFilter.OWNER)) {
            val repoName = repo.name
            debug("Repo: $repoName")
            if (cache.has(repo)) continue
            debug("Not in cache")

            if (repo.createdAt.toInstant().epochSecond >= lastCheck.toInstant().epochSecond) {
                debug("Not new")
                val embed = embed {
                    color = "#202225"

                    author {
                        this.name = name
                        this.iconUrl = avatar
                    }

                    title {
                        this.text = "Created new repository: $name/$repoName"
                        this.url = repo.httpTransportUrl
                    }
                }

                request(
                    method = "POST",
                    baseUrl = webhook.dropLast("/github".length),
                    headers = mapOf("Content-Type" to "application/json"),
                    content = """{ "embeds": [${embed.toJson().toString(2)}] }"""
                )

                println("$name/$repoName was created")

                track(repo)
            } else if (repo.hooks.none { it.config["url"] == webhook }) {
                debug("Doesn't have webhook setup")
                track(repo)
            } else {
                debug("Has webhook setup")
            }

            if (cache.add(repo)) cacheUpdated = true
        }
        debug("Finished loop")

        lastCheck = ZonedDateTime.now(ZoneOffset.UTC)
        if (cacheUpdated) cache.save()
    }
}

private fun track(repo: GHRepository) {
    val repoName = repo.name
    debug("Tracking: $repoName")
    if (repo.isArchived) return
    debug("Is not archived")

    repo.createHook(
        "web",
        mapOf("content_type" to "json", "url" to webhook, "insecure_ssl" to "0"),
        listOf(GHEvent.PUSH),
        true
    )
    debug("Created webhook")

    val name = myself.login
    val avatar = myself.avatarUrl
    val embed = embed {
        color = "#202225"

        author {
            this.name = name
            this.iconUrl = avatar
        }

        title {
            this.text = "Started tracking: $name/$repoName"
            this.url = repo.httpTransportUrl
        }
    }

    request(
        method = "POST",
        baseUrl =  webhook.dropLast("/github".length),
        headers = mapOf("Content-Type" to "application/json"),
        content = """{ "embeds": [${embed.toJson().toString(2)}] }"""
    )

    println("Started tracking $name/$repoName")
    debug("Tracked")
}

private fun debug(message: Any) {
    if (debug) {
        println("DEBUG: $message")
    }
}