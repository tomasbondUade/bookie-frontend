package org.bookie.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class BookDto(
    val id: String,
    val googleBookId: String,
    val title: String,
    val authors: List<String> = emptyList(),
    val description: String? = null,
    val genres: List<String> = emptyList(),
    val coverUrl: String? = null,
    val previewLink: String? = null,
    val infoLink: String? = null,
    val buyLink: String? = null,
)
