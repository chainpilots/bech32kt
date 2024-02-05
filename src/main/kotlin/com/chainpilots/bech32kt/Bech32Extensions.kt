package com.chainpilots.bech32kt

/**
 * Extension to encode a ByteArray to a Bech32 string with internal [toWords] call.
 *
 * @param hrp The requested human-readable part
 * @param limit Limitation of ByteArray length.
 * @return A string containing the Bech32-encoded data
 */
fun ByteArray.encodeToBech32(hrp: String, limit: Int? = null): String =
    Bech32.encode(hrp, this.toWords(), limit)

/**
 * Extension to decode a String to [Bech32Data].
 *
 * @param limit Limitation of ByteArray length.
 * @return [Bech32Data] in 5-bit byte format with human-readable part (HRP) information.
 */
fun String.decodeBech32ToBech32Data(limit: Int? = null): Bech32Data = Bech32.decode(this, limit)

/**
 * Extension to encode a ByteArray to a Bech32M string with internal [toWords] call.
 *
 * @param hrp The requested human-readable part
 * @param limit Limitation of ByteArray length.
 * @return A string containing the Bech32-encoded data
 */
fun ByteArray.encodeToBech32m(hrp: String, limit: Int? = null): String =
    Bech32M.encode(hrp, this.toWords(), limit)

/**
 * Extension to decode a String to a [Bech32Data].
 *
 * @param limit Limitation of ByteArray length.
 * @return [Bech32Data] in 5-bit byte format with human-readable part (HRP) information.
 */
fun String.decodeBech32mToWords(limit: Int? = null): Bech32Data = Bech32M.decode(this, limit)

/**
 * Extension to decode a Bech32-String to a ByteArray with internal [fromWords] call.
 *
 * @param limit Limitation of ByteArray length.
 * @return ByteArray without human-readable part (HRP) information.
 */
fun String.decodeBech32ToByteArray(limit: Int? = null): ByteArray = Bech32.decode(this, limit).data.fromWords()

/**
 * Extension to decode a Bech32m-String to a ByteArray with internal [fromWords] call.
 *
 * @param limit Limitation of ByteArray length.
 * @return ByteArray without human-readable part (HRP) information.
 */
fun String.decodeBech32mToByteArray(limit: Int? = null): ByteArray = Bech32M.decode(this, limit).data.fromWords()

/**
 * Create an instance from arbitrary data, converts from 8-bits per byte format
 * to 5-bits per byte format before construction.
 * @return Bech32 instance containing 5-bit encoding
 */
fun ByteArray.toWords(): ByteArray = convertBits(this, this.size, 8, 5, true)

/**
 * Return the data, fully-decoded with 8-bits per byte.
 * @return The data, fully-decoded as a byte array.
 */
fun ByteArray.fromWords(): ByteArray = convertBits(this, this.size, 5, 8, false)