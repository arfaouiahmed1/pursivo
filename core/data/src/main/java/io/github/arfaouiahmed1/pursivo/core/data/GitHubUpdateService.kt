package io.github.arfaouiahmed1.pursivo.core.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.arfaouiahmed1.pursivo.core.model.UpdateChannel
import io.github.arfaouiahmed1.pursivo.core.model.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

interface UpdateService {
    suspend fun check(currentVersion: String, channel: UpdateChannel): UpdateInfo?
    suspend fun downloadAndVerify(update: UpdateInfo): Uri
}

@Singleton
class GitHubUpdateService @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val client: OkHttpClient,
) : UpdateService {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun check(currentVersion: String, channel: UpdateChannel): UpdateInfo? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.github.com/repos/arfaouiahmed1/pursivo/releases?per_page=20")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "Pursivo-Android")
            .build()
        val releases = client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "GitHub update check failed (${response.code})" }
            json.parseToJsonElement(response.body?.string().orEmpty()).jsonArray
        }
        val current = SemVersion.parse(currentVersion) ?: SemVersion(0, 0, 0)
        data class Candidate(val version: SemVersion, val info: UpdateInfo, val checksumUrl: String?)
        val selected = releases.mapNotNull { element ->
            val release = element.jsonObject
            if (release["draft"]?.jsonPrimitive?.booleanOrNull == true) return@mapNotNull null
            val prerelease = release["prerelease"]?.jsonPrimitive?.booleanOrNull == true
            if (channel == UpdateChannel.STABLE && prerelease) return@mapNotNull null
            val tag = release["tag_name"]?.jsonPrimitive?.contentOrNull.orEmpty().removePrefix("v")
            val version = SemVersion.parse(tag) ?: return@mapNotNull null
            val assets = release["assets"]?.jsonArray.orEmpty()
            val apk = assets.firstOrNull { it.jsonObject["name"]?.jsonPrimitive?.contentOrNull?.endsWith(".apk") == true }?.jsonObject
            val checksum = assets.firstOrNull { it.jsonObject["name"]?.jsonPrimitive?.contentOrNull?.endsWith(".apk.sha256") == true }?.jsonObject
            Candidate(version, UpdateInfo(
                versionName = tag,
                versionCode = apk?.get("id")?.jsonPrimitive?.longOrNull ?: 0,
                prerelease = prerelease,
                notes = release["body"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                releaseUrl = release["html_url"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                apkUrl = apk?.get("browser_download_url")?.jsonPrimitive?.contentOrNull,
                sizeBytes = apk?.get("size")?.jsonPrimitive?.longOrNull,
            ), checksum?.get("browser_download_url")?.jsonPrimitive?.contentOrNull)
        }.filter { it.version > current }.maxByOrNull { it.version }
        selected?.info?.copy(sha256 = selected.checksumUrl?.let(::fetchChecksum))
    }

    override suspend fun downloadAndVerify(update: UpdateInfo): Uri = withContext(Dispatchers.IO) {
        val url = requireNotNull(update.apkUrl) { "This release does not contain an APK." }
        val expected = requireNotNull(update.sha256).lowercase()
        require(Regex("^[a-f0-9]{64}$").matches(expected)) { "The release checksum is missing or invalid." }
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(directory, "pursivo-${update.versionName}.apk")
        val digest = MessageDigest.getInstance("SHA-256")
        val request = Request.Builder().url(url).header("User-Agent", "Pursivo-Android").build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "APK download failed (${response.code})" }
            val body = requireNotNull(response.body) { "The APK download was empty." }
            target.outputStream().buffered().use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                    }
                }
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (actual != expected) {
            target.delete()
            error("Checksum mismatch. The downloaded APK was discarded.")
        }
        FileProvider.getUriForFile(context, "io.github.arfaouiahmed1.pursivo.files", target)
    }

    private fun fetchChecksum(url: String): String? {
        val request = Request.Builder().url(url).header("User-Agent", "Pursivo-Android").build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use null
            response.body?.string()?.trim()?.substringBefore(' ')?.lowercase()?.takeIf { Regex("^[a-f0-9]{64}$").matches(it) }
        }
    }
}

data class SemVersion(val major: Int, val minor: Int, val patch: Int, val pre: List<String> = emptyList()) : Comparable<SemVersion> {
    override fun compareTo(other: SemVersion): Int {
        compareValuesBy(this, other, SemVersion::major, SemVersion::minor, SemVersion::patch).let { if (it != 0) return it }
        if (pre.isEmpty() && other.pre.isNotEmpty()) return 1
        if (pre.isNotEmpty() && other.pre.isEmpty()) return -1
        for (index in 0 until maxOf(pre.size, other.pre.size)) {
            val left = pre.getOrNull(index) ?: return -1
            val right = other.pre.getOrNull(index) ?: return 1
            val leftNumber = left.toIntOrNull()
            val rightNumber = right.toIntOrNull()
            val result = when {
                leftNumber != null && rightNumber != null -> leftNumber.compareTo(rightNumber)
                leftNumber != null -> -1
                rightNumber != null -> 1
                else -> left.compareTo(right)
            }
            if (result != 0) return result
        }
        return 0
    }

    companion object {
        private val pattern = Regex("^(\\d+)\\.(\\d+)\\.(\\d+)(?:-([0-9A-Za-z.-]+))?$")
        fun parse(value: String): SemVersion? = pattern.matchEntire(value)?.destructured?.let { (major, minor, patch, pre) ->
            SemVersion(major.toInt(), minor.toInt(), patch.toInt(), pre.takeIf { it.isNotBlank() }?.split('.') ?: emptyList())
        }
    }
}
