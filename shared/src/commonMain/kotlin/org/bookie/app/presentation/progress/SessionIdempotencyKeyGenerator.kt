package org.bookie.app.presentation.progress

import kotlin.random.Random

/** A new key identifies a new session attempt; a retry must keep its original key. */
fun interface SessionIdempotencyKeyGenerator {
    fun newKey(): String
}

/** Generates random UUID v4 identifiers; keys are not credentials. */
class RandomSessionIdempotencyKeyGenerator : SessionIdempotencyKeyGenerator {
    override fun newKey(): String {
        val bytes = Random.Default.nextBytes(16)
        bytes[6] = ((bytes[6].toInt() and 0x0f) or 0x40).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte()
        val digits = "0123456789abcdef"
        return buildString(36) {
            bytes.forEachIndexed { index, byte ->
                if (index == 4 || index == 6 || index == 8 || index == 10) append('-')
                val value = byte.toInt() and 0xff
                append(digits[value ushr 4])
                append(digits[value and 0x0f])
            }
        }
    }
}
