package com.zp4rker.repotracker

import org.kohsuke.github.GHRepository
import java.io.File

/**
 * @author zp4rker
 */
class Cache(private val file: File) {

    private val repos = mutableSetOf<String>()

    init {
        repos.addAll(file.readLines())
    }

    fun add(repo: GHRepository) = repos.add("${repo.id}")

    fun has(repo: GHRepository) = repos.contains("${repo.id}")

    fun save() = file.writeText(repos.joinToString("\n"))

}