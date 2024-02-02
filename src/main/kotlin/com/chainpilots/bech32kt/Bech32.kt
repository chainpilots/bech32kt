package com.chainpilots.bech32kt

import java.io.ByteArrayOutputStream

object Bech32 {
    /** The com.chainpilots.bech32kt.Bech32 character set for encoding.  */
    private const val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"

    /** The com.chainpilots.bech32kt.Bech32 character set for decoding.  */
    private val CHARSET_REV = byteArrayOf(
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        15, -1, 10, 17, 21, 20, 26, 30, 7, 5, -1, -1, -1, -1, -1, -1,
        -1, 29, -1, 24, 13, 25, 9, 8, 23, -1, 18, 22, 31, 27, 19, -1,
        1, 0, 3, 16, 11, 28, 12, 14, 6, 4, 2, -1, -1, -1, -1, -1,
        -1, 29, -1, 24, 13, 25, 9, 8, 23, -1, 18, 22, 31, 27, 19, -1,
        1, 0, 3, 16, 11, 28, 12, 14, 6, 4, 2, -1, -1, -1, -1, -1
    )

    private const val BECH32_CONST = 1
    private const val BECH32M_CONST = 0x2bc830a3

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
    private fun verifyChecksum(hrp: String, values: ByteArray): Encoding? {
        val hrpExpanded = expandHrp(hrp)
        val combined = ByteArray(hrpExpanded.size + values.size)
        System.arraycopy(hrpExpanded, 0, combined, 0, hrpExpanded.size)
        System.arraycopy(values, 0, combined, hrpExpanded.size, values.size)
        val check = polymod(combined)
        return when (check) {
            BECH32_CONST -> Encoding.BECH32
            BECH32M_CONST -> Encoding.BECH32M
            else -> null
        }
    }

    /** Create a checksum.  */
    private fun createChecksum(encoding: Encoding, hrp: String, values: ByteArray): ByteArray {
        val hrpExpanded = expandHrp(hrp)
        val enc = ByteArray(hrpExpanded.size + values.size + 6)
        System.arraycopy(hrpExpanded, 0, enc, 0, hrpExpanded.size)
        System.arraycopy(values, 0, enc, hrpExpanded.size, values.size)
        val mod = polymod(enc) xor (if (encoding == Encoding.BECH32) BECH32_CONST else BECH32M_CONST)
        val ret = ByteArray(6)
        for (i in 0..5) {
            ret[i] = ((mod ushr (5 * (5 - i))) and 31).toByte()
        }
        return ret
    }

    /**
     * Encode a byte array to a com.chainpilots.bech32kt.Bech32 string
     * @param encoding Desired encoding com.chainpilots.bech32kt.Bech32 or Bech32m
     * @param hrp human-readable part to use for encoding
     * @param bytes Arbitrary binary data (8-bits per byte)
     * @return A com.chainpilots.bech32kt.Bech32 string
     */
    fun encodeBytes(encoding: Encoding, hrp: String, bytes: ByteArray): String {
        return encode(encoding, hrp, Bech32Bytes.ofBytes(bytes))
    }

    /**
     * Decode a com.chainpilots.bech32kt.Bech32 string to a byte array.
     * @param bech32 A com.chainpilots.bech32kt.Bech32 format string
     * @param expectedHrp Expected value for the human-readable part
     * @param expectedEncoding Expected encoding
     * @return Decoded value as byte array (8-bits per byte)
     * @throws AddressFormatException if unexpected hrp or encoding
     */
    fun decodeBytes(bech32: String, expectedHrp: String, expectedEncoding: Encoding): ByteArray {
        val decoded = decode(bech32)
        if (decoded.hrp != expectedHrp || decoded.encoding != expectedEncoding) {
            throw AddressFormatException("unexpected hrp or encoding")
        }
        return decoded.decode5to8()
    }

    /**
     * Encode a com.chainpilots.bech32kt.Bech32 string.
     * @param bech32 Contains 5-bits/byte data, desired encoding and human-readable part
     * @return A string containing the com.chainpilots.bech32kt.Bech32-encoded data
     */
    fun encode(bech32: Bech32Data): String {
        return encode(bech32.encoding, bech32.hrp, bech32)
    }

    /**
     * Encode a com.chainpilots.bech32kt.Bech32 string.
     * @param encoding The requested encoding
     * @param hrp The requested human-readable part
     * @param values Binary data in 5-bit per byte format
     * @return A string containing the com.chainpilots.bech32kt.Bech32-encoded data
     */
    fun encode(encoding: Encoding, hrp: String, values: Bech32Bytes): String {
        if (hrp.isEmpty()) throw IllegalArgumentException("human-readable part is too short: " + hrp.length)
        if (hrp.length > 83) throw IllegalArgumentException("human-readable part is too long: " + hrp.length)
        val lcHrp = hrp.lowercase()
        val checksum = createChecksum(encoding, lcHrp, values.bytes)
        val combined = ByteArray(values.bytes.size + checksum.size)
        System.arraycopy(values.bytes, 0, combined, 0, values.bytes.size)
        System.arraycopy(checksum, 0, combined, values.bytes.size, checksum.size)
        val sb = StringBuilder(lcHrp.length + 1 + combined.size)
        sb.append(lcHrp)
        sb.append('1')
        for (b in combined) {
            sb.append(CHARSET[b.toInt()])
        }
        return sb.toString()
    }

    /**
     * Decode a com.chainpilots.bech32kt.Bech32 string.
     *
     *
     * To get the fully-decoded data, call [Bech32Bytes.decode5to8] on the returned `Bech32Data`.
     * @param str A string containing com.chainpilots.bech32kt.Bech32-encoded data
     * @return An object with the detected encoding, hrp, and decoded data (in 5-bit per byte format)
     * @throws AddressFormatException if the string is invalid
     */
    @Throws(AddressFormatException::class)
    fun decode(str: String): Bech32Data {
        var lower = false
        var upper = false
        if (str.length < 8) throw AddressFormatException.InvalidDataLength("Input too short: " + str.length)
        if (str.length > 90) throw AddressFormatException.InvalidDataLength("Input too long: " + str.length)
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
        val encoding = verifyChecksum(hrp, values) ?: throw AddressFormatException.InvalidChecksum()
        return Bech32Data(encoding, hrp, values.sliceArray(0..values.size-7))
    }

    /**
     * Helper for re-arranging bits into groups.
     */
    @Throws(AddressFormatException::class)
    private fun convertBits(
        input: ByteArray, inStart: Int, inLen: Int, fromBits: Int,
        toBits: Int, pad: Boolean
    ): ByteArray {
        var acc = 0
        var bits = 0
        val out = ByteArrayOutputStream(64)
        val max = (1 shl toBits) - 1
        val maxAcc = (1 shl (fromBits + toBits - 1)) - 1
        for (i in 0 until inLen) {
            val value = input[i + inStart].toInt() and 0xff
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

    /**
     * Enumeration of known com.chainpilots.bech32kt.Bech32 encoding format types: com.chainpilots.bech32kt.Bech32 and Bech32m.
     */
    enum class Encoding {
        BECH32, BECH32M
    }

    /**
     * Binary data in 5-bits-per-byte format as used in com.chainpilots.bech32kt.Bech32 encoding/decoding.
     */
    open class Bech32Bytes {
        val bytes: ByteArray

        /**
         * Wrapper for a `byte[]` array.
         *
         * @param bytes bytes to be copied (5-bits per byte format)
         */
        protected constructor(bytes: ByteArray?) {
            this.bytes = bytes ?: byteArrayOf()
        }

        /**
         * Construct an instance, from two parts. Useful for the Segwit implementation,
         * see [.ofSegwit].
         * @param first first byte (5-bits per byte format)
         * @param rest remaining bytes (5-bits per byte format)
         */
        private constructor(first: Byte, rest: ByteArray) {
            bytes = concat(first, rest)
        }

        /**
         * Return the data, fully-decoded with 8-bits per byte.
         * @return The data, fully-decoded as a byte array.
         */
        fun decode5to8(): ByteArray {
            return convertBits(bytes, 0, bytes.size, 5, 8, false)
        }

        /**
         * @return the first byte (witness version if instance is a Segwit address)
         */
        fun witnessVersion(): Short {
            return bytes[0].toShort()
        }

        // Trim the version byte and return the witness program only
        private fun stripFirst(): Bech32Bytes {
            val program = ByteArray(bytes.size - 1)
            System.arraycopy(bytes, 1, program, 0, program.size)
            return Bech32Bytes(program)
        }

        /**
         * Assuming this instance contains a Segwit address, return the witness program portion of the data.
         * @return The witness program as a byte array
         */
        fun witnessProgram(): ByteArray {
            return stripFirst().decode5to8()
        }

        companion object {
            private fun concat(first: Byte, rest: ByteArray): ByteArray {
                val bytes = ByteArray(rest.size + 1)
                bytes[0] = first
                System.arraycopy(rest, 0, bytes, 1, rest.size)
                return bytes
            }

            /**
             * Create an instance from arbitrary data, converts from 8-bits per byte
             * format to 5-bits per byte format before construction.
             * @param data arbitrary byte array (8-bits of data per byte)
             * @return com.chainpilots.bech32kt.Bech32 instance containing 5-bit encoding
             */
            fun ofBytes(data: ByteArray): Bech32Bytes {
                return Bech32Bytes(encode8to5(data))
            }

            /**
             * Create an instance from Segwit address binary data.
             * @param witnessVersion A short containing (5-bit) witness version information
             * @param witnessProgram a witness program (8-bits-per byte)
             * @return com.chainpilots.bech32kt.Bech32 instance containing 5-bit encoding
             */
            fun ofSegwit(witnessVersion: Short, witnessProgram: ByteArray): Bech32Bytes {
                // convert witnessVersion, witnessProgram to 5-bit Bech32Bytes
                return Bech32Bytes((witnessVersion.toInt() and 0xff).toByte(), encode8to5(witnessProgram))
            }

            private fun encode8to5(data: ByteArray): ByteArray {
                return convertBits(data, 0, data.size, 8, 5, true)
            }
        }
    }

    /**
     * com.chainpilots.bech32kt.Bech32 data in 5-bit byte format with [Encoding] and human-readable part (HRP) information.
     * Typically, the result of [.decode].
     */
    class Bech32Data internal constructor(val encoding: Encoding, val hrp: String, data: ByteArray) : Bech32Bytes(data)
}