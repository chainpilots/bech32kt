package com.chainpilots.bech32kt

/**
 * Object to encode and decode Bech32M.
 */
object Bech32M {
    /**
     * Encode a Bech32M string.
     *
     * @param hrp The requested human-readable part
     * @param words Binary data (in 5-bit per byte format): call [toWords] first.
     * @return A string containing the Bech32M-encoded data
     */
    fun encode(hrp: String, words: ByteArray, limit: Int? = null): String {
        return encode(Constants.BECH32M_CONST, hrp, words, limit)
    }
    /**
     * Decode a Bech32M string.
     *
     * To get the fully-decoded data, call [fromWords] on the returned `[Bech32Data.data]`.
     * @param bech32 A string containing Bech32M-encoded data
     * @return An object with the hrp and the decoded data (in 5-bit per byte format)
     * @throws AddressFormatException if the string is invalid
     */
    fun decode(bech32: String, limit: Int? = null): Bech32Data {
        return com.chainpilots.bech32kt.decode(bech32, limit)
    }
}