package org.bookie.app.data.auth

import kotlinx.cinterop.*
import platform.CoreFoundation.CFTypeRefVar
import kotlinx.serialization.json.Json
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.posix.time
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/** Keychain generic-password item, local to this device and available only when unlocked. */
@OptIn(ExperimentalForeignApi::class)
class IosKeychainSessionStore(
    private val service: String = "org.bookie.app.supabase.auth",
) : SecureSessionStore {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun read(): StoredAuthSession? = withQuery { query ->
        CFDictionarySetValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionarySetValue(query, kSecMatchLimit, kSecMatchLimitOne)
        memScoped {
            val result = alloc<CFTypeRefVar>()
            result.value = null
            val status = SecItemCopyMatching(query, result.ptr)
            if (status == errSecItemNotFound) return@memScoped null
            check(status == errSecSuccess) { "No se pudo leer la sesión de Keychain ($status)." }
            val data = checkNotNull(result.value)
            try {
                val size = CFDataGetLength(data.reinterpret()).toInt()
                val pointer = checkNotNull(CFDataGetBytePtr(data.reinterpret()))
                val payload = ByteArray(size) { pointer[it].toByte() }.decodeToString()
                json.decodeFromString<StoredAuthSession>(payload)
            } finally {
                CFRelease(data)
            }
        }
    }

    override suspend fun write(session: StoredAuthSession) {
        val bytes = json.encodeToString(StoredAuthSession.serializer(), session).encodeToByteArray()
        val data = bytes.usePinned { pinned ->
            CFDataCreate(null, pinned.addressOf(0).reinterpret(), bytes.size.toLong())
        } ?: error("No se pudo preparar la sesión para Keychain.")
        try {
            withQuery { query ->
                val attributes = CFDictionaryCreateMutable(null, 0, null, null)
                    ?: error("No se pudo crear la consulta de Keychain.")
                try {
                    CFDictionarySetValue(attributes, kSecValueData, data)
                    val status = SecItemUpdate(query, attributes)
                    if (status == errSecItemNotFound) {
                        CFDictionarySetValue(query, kSecValueData, data)
                        CFDictionarySetValue(query, kSecAttrAccessible, kSecAttrAccessibleWhenUnlockedThisDeviceOnly)
                        val created = SecItemAdd(query, null)
                        check(created == errSecSuccess) { "No se pudo guardar la sesión en Keychain ($created)." }
                    } else {
                        check(status == errSecSuccess) { "No se pudo actualizar la sesión en Keychain ($status)." }
                    }
                } finally {
                    CFRelease(attributes)
                }
            }
        } finally {
            CFRelease(data)
        }
    }

    override suspend fun clear() {
        withQuery { query ->
            val status = SecItemDelete(query)
            check(status == errSecSuccess || status == errSecItemNotFound) {
                "No se pudo borrar la sesión de Keychain ($status)."
            }
        }
    }

    private inline fun <T> withQuery(block: (CFMutableDictionaryRef) -> T): T {
        val query = CFDictionaryCreateMutable(null, 0, null, null)
            ?: error("No se pudo crear la consulta de Keychain.")
        val serviceRef = CFStringCreateWithCString(null, service, kCFStringEncodingUTF8)
            ?: error("No se pudo crear el identificador de Keychain.")
        val accountRef = CFStringCreateWithCString(null, "supabase_session", kCFStringEncodingUTF8)
            ?: error("No se pudo crear el identificador de sesión.")
        try {
            // Null callbacks: CFString references must remain alive until the Keychain call returns.
            CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionarySetValue(query, kSecAttrService, serviceRef)
            CFDictionarySetValue(query, kSecAttrAccount, accountRef)
            return block(query)
        } finally {
            CFRelease(query)
            CFRelease(serviceRef)
            CFRelease(accountRef)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun currentEpochSeconds(): Long = time(null).toLong()
