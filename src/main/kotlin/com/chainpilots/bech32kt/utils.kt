package com.chainpilots.bech32kt

import com.chainpilots.bech32kt.Constants.BECH32M_CONST
import com.chainpilots.bech32kt.Constants.BECH32_CONST
import com.chainpilots.bech32kt.Constants.CHARSET
import com.chainpilots.bech32kt.Constants.CHARSET_REV
import java.io.ByteArrayOutputStream

/** Find the polynomial with value coefficients mod the generator as 30-bit.  */
private fun polymod(values: ByteArray): Int {
    var c = 1
    for (vI in values) {
        val c0 = (c ushr 25) and 0xff
        c = ((c and 0x1ffffff) shl 5) xor (vI.toInt() and 0xff)
        if ((c0 and 1) != 0) c = c xor 0x3b6a57b2
        if ((c0 and 2) != 0) c = c xor 0x26508e6d
        if ((c0 and 4) != 0) c = c xor 0x1ea119fa
        if ((c0 and 8) != 0) c = c xor 0x3d4233dd
        if ((c0 and 16) != 0) c = c xor 0x2a1462b3
    }
    return c
}

/** Expand a HRP for use in checksum computation.  */
private fun expandHrp(hrp: String): ByteArray {
    val hrpLength = hrp.length
    val ret = ByteArray(hrpLength * 2 + 1)
    for (i in 0 until hrpLength) {
        val c = hrp[i].code and 0x7f // Limit to standard 7-bit ASCII
        ret[i] = ((c ushr 5) and 0x07).toByte()
        ret[i + hrpLength + 1] = (c and 0x1f).toByte()
    }
    ret[hrpLength] = 0
    return ret
}

/** Verify a checksum.  */
private fun verifyChecksum(hrp: String, values: ByteArray): Int? {
    val hrpExpanded = expandHrp(hrp)
    val combined = ByteArray(hrpExpanded.size + values.size)
    System.arraycopy(hrpExpanded, 0, combined, 0, hrpExpanded.size)
    System.arraycopy(values, 0, combined, hrpExpanded.size, values.size)
    val check = polymod(combined)
    return if (check != BECH32_CONST && check != BECH32M_CONST) null else check
}

/** Create a checksum.  */
private fun createChecksum(encoding: Int, hrp: String, values: ByteArray): ByteArray {
    val hrpExpanded = expandHrp(hrp)
    val enc = ByteArray(hrpExpanded.size + values.size + 6)
    System.arraycopy(hrpExpanded, 0, enc, 0, hrpExpanded.size)
    System.arraycopy(values, 0, enc, hrpExpanded.size, values.size)
    val mod = polymod(enc) xor (if (encoding == BECH32_CONST) BECH32_CONST else BECH32M_CONST)
    val ret = ByteArray(6)
    for (i in 0..5) {
        ret[i] = ((mod ushr (5 * (5 - i))) and 31).toByte()
    }
    return ret
}

/**
 * Encode a Bech32 string.
 * @param encoding The requested encoding
 * @param hrp The requested human-readable part
 * @param values Binary data (in 5-bit per byte format)
 * @return A string containing the Bech32-encoded data
 */
internal fun encode(encoding: Int, hrp: String, values: ByteArray, limit: Int?): String {
    val l = limit ?: 90
    if (hrp.isEmpty()) throw IllegalArgumentException("human-readable part is too short: ${hrp.length}")
    if (hrp.length > 83) throw IllegalArgumentException("human-readable part is too long: ${hrp.length}")
    if (hrp.length + 7 + values.size > l) throw IllegalArgumentException("Exceeds length limit: ${hrp.length + 7 + values.size}")
    val lcHrp = hrp.lowercase()
    val checksum = createChecksum(encoding, lcHrp, values)
    val combined = ByteArray(values.size + checksum.size)
    System.arraycopy(values, 0, combined, 0, values.size)
    System.arraycopy(checksum, 0, combined, values.size, checksum.size)
    val sb = StringBuilder(lcHrp.length + 1 + combined.size)
    sb.append(lcHrp)
    sb.append('1')
    for (b in combined) {
        sb.append(CHARSET[b.toInt()])
    }
    return sb.toString()
}

/**
 * Decode a Bech32 string.
 *
 *
 * To get the fully-decoded data, call [fromWords] on the returned `Bech32Data`.
 * @param str A string containing Bech32-encoded data
 * @return An object with the detected the hrp and the decoded data (in 5-bit per byte format)
 * @throws AddressFormatException if the string is invalid
 */
@Throws(AddressFormatException::class)
internal fun decode(str: String, limit: Int?): Bech32Data {
    val l = limit ?: 90
    var lower = false
    var upper = false
    if (str.length < 8) throw AddressFormatException.InvalidDataLength("Input too short: " + str.length)
    if (str.length > l) throw AddressFormatException.InvalidDataLength("Input too long: " + str.length)
    for (i in str.indices) {
        val c = str[i]
        if (c.code < 33 || c.code > 126) throw AddressFormatException.InvalidCharacter(c, i)
        if (c in 'a'..'z') {
            if (upper) throw AddressFormatException.InvalidCharacter(c, i)
            lower = true
        }
        if (c in 'A'..'Z') {
            if (lower) throw AddressFormatException.InvalidCharacter(c, i)
            upper = true
        }
    }
    val pos = str.lastIndexOf('1')
    if (pos < 1) throw AddressFormatException.InvalidPrefix("Missing human-readable part")
    val dataPartLength = str.length - 1 - pos
    if (dataPartLength < 6) throw AddressFormatException.InvalidDataLength("Data part too short: $dataPartLength")
    val values = ByteArray(dataPartLength)
    for (i in 0 until dataPartLength) {
        val c = str[i + pos + 1]
        if (CHARSET_REV[c.code].toInt() == -1) throw AddressFormatException.InvalidCharacter(c, i + pos + 1)
        values[i] = CHARSET_REV[c.code]
    }
    val hrp = str.substring(0, pos).lowercase()
    verifyChecksum(hrp, values) ?: throw AddressFormatException.InvalidChecksum()
    return Bech32Data(hrp, values.sliceArray(0..values.size - 7))
}

/**
 * Helper for re-arranging bits into groups.
 */
@Throws(AddressFormatException::class)
internal fun convertBits(
    input: ByteArray, inLen: Int, fromBits: Int,
    toBits: Int, pad: Boolean
): ByteArray {
    var acc = 0
    var bits = 0
    val out = ByteArrayOutputStream(64)
    val max = (1 shl toBits) - 1
    val maxAcc = (1 shl (fromBits + toBits - 1)) - 1
    for (i in 0 until inLen) {
        val value = input[i].toInt() and 0xff
        if ((value ushr fromBits) != 0) {
            throw AddressFormatException(
                String.format("Input value '%X' exceeds '%d' bit size", value, fromBits)
            )
        }
        acc = ((acc shl fromBits) or value) and maxAcc
        bits += fromBits
        while (bits >= toBits) {
            bits -= toBits
            out.write((acc ushr bits) and max)
        }
    }
    if (pad) {
        if (bits > 0) out.write((acc shl (toBits - bits)) and max)
    } else if (bits >= fromBits || ((acc shl (toBits - bits)) and max) != 0) {
        throw AddressFormatException("Could not convert bits, invalid padding")
    }
    return out.toByteArray()
}