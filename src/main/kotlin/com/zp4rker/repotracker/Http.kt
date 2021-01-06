package com.zp4rker.repotracker

import java.net.HttpURLConnection
import java.net.URL

/**
 * @author zp4rker
 */

fun request(
    method: String,
    baseUrl: String,
    parameters: Map<String, Any> = mapOf(),
    headers: Map<String, String> = mapOf(),
    content: String? = null
): String {
    val url = URL(
        "$baseUrl${
            if (parameters.isNotEmpty()) parameters.map { "${it.key}=${it.value}" }.joinToString("&", "?") else ""
        }"
    )
    with(url.openConnection() as HttpURLConnection) {
        requestMethod = method.toUpperCase()
        headers.forEach { addRequestProperty(it.key, it.value) }
        content?.let {
            doOutput = true
            outputStream.use { os -> os.writer().use { wr -> wr.write(content) } }
        }
        val response = runCatching { inputStream.use { it.reader().use { r -> r.readText() } } }.getOrNull() ?: ""
        errorStream?.use {
            println(it.reader().use { r -> r.readText() })
        }
        return response
    }
}