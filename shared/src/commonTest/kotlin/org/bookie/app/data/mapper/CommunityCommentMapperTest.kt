package org.bookie.app.data.mapper

import org.bookie.app.data.remote.dto.CommunityCommentDto
import org.bookie.app.domain.model.CommunityComment
import org.bookie.app.domain.model.CreateCommunityCommentInput
import kotlin.test.Test
import kotlin.test.assertEquals

class CommunityCommentMapperTest {
    @Test
    fun `maps server comment and optional parent to domain`() {
        val dto = CommunityCommentDto(
            id = "comment-2",
            content = "Respuesta",
            postId = "post-1",
            authorId = "user-1",
            parentId = "comment-1",
            createdAt = "2026-11-20T10:00:00Z",
        )

        assertEquals(
            CommunityComment(
                id = "comment-2",
                content = "Respuesta",
                postId = "post-1",
                authorId = "user-1",
                parentId = "comment-1",
                createdAt = "2026-11-20T10:00:00Z",
            ),
            dto.toDomain(),
        )
    }

    @Test
    fun `trims content and omits a blank parent id`() {
        assertEquals(
            org.bookie.app.data.remote.dto.CreateCommunityCommentDto("Hola", null),
            CreateCommunityCommentInput(" Hola ", " ").toDto(),
        )
    }
}
