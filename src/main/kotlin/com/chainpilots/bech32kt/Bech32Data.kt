package com.chainpilots.bech32kt

/**
 * Bech32 data in 5-bit byte format with human-readable part (HRP) information.
 * Typically, the result of [decode].
 */
data class Bech32Data internal constructor(
    val hrp: String,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Bech32Data

        if (hrp != other.hrp) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hrp.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}