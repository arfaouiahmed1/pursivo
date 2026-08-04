package io.github.arfaouiahmed1.pursivo.core.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.arfaouiahmed1.pursivo.core.model.VaultDocument
import java.io.File
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentVault @Inject constructor(@param:ApplicationContext private val context: Context) {
    init { PDFBoxResourceLoader.init(context) }

    fun import(uri: Uri): VaultDocument {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: "document-${System.currentTimeMillis()}"
        val directory = File(context.filesDir, "vault").apply { mkdirs() }
        val target = uniqueFile(directory, sanitize(name))
        resolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Unable to open selected document" }
            target.outputStream().use(input::copyTo)
        }
        val text = when {
            mime == "application/pdf" || target.extension.equals("pdf", true) -> extractPdf(target)
            mime.contains("wordprocessingml") || target.extension.equals("docx", true) -> extractDocx(target)
            mime.startsWith("text/") || target.extension.equals("txt", true) -> target.readText()
            else -> ""
        }.replace(Regex("[\\t ]+"), " ").replace(Regex("\\n{3,}"), "\n\n").trim()
        return VaultDocument(name = name, mimeType = mime, localPath = target.absolutePath, normalizedText = text)
    }

    fun delete(document: VaultDocument) {
        val vault = File(context.filesDir, "vault").canonicalFile
        val target = File(document.localPath).canonicalFile
        require(target.parentFile == vault) { "Refusing to delete a file outside the Pursivo vault" }
        if (target.exists()) check(target.delete()) { "Unable to delete ${document.name}" }
    }

    private fun extractPdf(file: File): String = runCatching {
        PDDocument.load(file).use { PDFTextStripper().getText(it) }
    }.getOrDefault("")

    private fun extractDocx(file: File): String = runCatching {
        ZipInputStream(file.inputStream().buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    return@use zip.readBytes().toString(Charsets.UTF_8)
                        .replace(Regex("<w:tab[^>]*/>"), "\t")
                        .replace(Regex("</w:p>"), "\n")
                        .replace(Regex("<[^>]+>"), "")
                        .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                }
                entry = zip.nextEntry
            }
            ""
        }
    }.getOrDefault("")

    private fun uniqueFile(directory: File, name: String): File {
        var candidate = File(directory, name)
        var index = 2
        while (candidate.exists()) {
            candidate = File(directory, "${candidate.nameWithoutExtension}-$index.${candidate.extension}")
            index++
        }
        return candidate
    }

    private fun sanitize(name: String) = name.replace(Regex("[^A-Za-z0-9._ -]"), "_").take(120)
}
