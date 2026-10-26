package org.bookie.app.domain.model

data class Book(
    val id: String,
    val googleBookId: String,
    val title: String,
    val authors: List<String>,
    val description: String?,
    val genres: List<String>,
    val coverUrl: String?,
    val previewLink: String?,
    val infoLink: String?,
    val buyLink: String?,
)
