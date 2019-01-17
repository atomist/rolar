package com.atomist.rolar.domain.model

data class LogKey(
        val path: List<String>,
        val host: String,
        val time: Long,
        val lastModified: Long,
        val isClosed: Boolean,
        val prepend: Boolean = false,
        val key: String? = null
) {
    companion object
}
