package ru.iuturakulov.mybudgetbackend.models.analytics

import io.ktor.http.ContentType

data class ExportFile(
    val contentType: ContentType,
    val fileName: String,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExportFile

        if (contentType != other.contentType) return false
        if (fileName != other.fileName) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = contentType.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}