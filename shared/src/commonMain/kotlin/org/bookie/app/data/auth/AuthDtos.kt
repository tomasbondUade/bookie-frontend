package org.bookie.app.data.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseUserDto(
    val id: String,
    val email: String? = null,
    @SerialName("user_metadata") val metadata: UserMetadataDto = UserMetadataDto(),
)

@Serializable
data class UserMetadataDto(val username: String? = null)

@Serializable
data class SupabaseSessionDto(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    val user: SupabaseUserDto? = null,
)

@Serializable
internal data class PasswordGrantDto(val email: String, val password: String)

@Serializable
internal data class SignUpDto(
    val email: String,
    val password: String,
    val data: UserMetadataDto,
)

@Serializable
internal data class RefreshGrantDto(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
internal data class SyncUserDto(val username: String)
