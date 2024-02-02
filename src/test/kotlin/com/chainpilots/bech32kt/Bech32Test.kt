package com.chainpilots.bech32kt

import com.chainpilots.bech32kt.Bech32.decode
import com.chainpilots.bech32kt.Bech32.decodeBytes
import com.chainpilots.bech32kt.Bech32.encodeBytes
import com.chainpilots.bech32kt.AddressFormatException.*
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith


class Bech32Test{
    private val VALID_BECH32 = arrayOf(
        "A12UEL5L",
        "a12uel5l",
        "an83characterlonghumanreadablepartthatcontainsthenumber1andtheexcludedcharactersbio1tt5tgs",
        "abcdef1qpzry9x8gf2tvdw0s3jn54khce6mua7lmqqqxw",
        "11qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqc8247j",
        "split1checkupstagehandshakeupstreamerranterredcaperred2y9e3w",
        "?1ezyfcl",
    )
    private val VALID_BECH32M = arrayOf(
        "A1LQFN3A",
        "a1lqfn3a",
        "an83characterlonghumanreadablepartthatcontainsthetheexcludedcharactersbioandnumber11sg7hg6",
        "abcdef1l7aum6echk45nj3s0wdvt2fg8x9yrzpqzd3ryx",
        "11llllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllludsr8",
        "split1checkupstagehandshakeupstreamerranterredcaperredlc445v",
        "?1v759aa"
    )
    @Test
    fun validBech32() {
        for (valid in VALID_BECH32) {
            valid(valid)
        }
    }

    @Test
    fun validBech32m() {
        for (valid in VALID_BECH32M) {
            valid(valid)
        }
    }

    private fun valid(valid: String) {
        val bechData = Bech32.decode(valid)
        var recode = Bech32.encode(bechData)
        assertEquals(valid.lowercase(Locale.ROOT), recode.lowercase(Locale.ROOT),"Failed to roundtrip '$valid' -> '$recode'" )
        // Test encoding with an uppercase HRP
        recode = Bech32.encode(bechData.encoding, bechData.hrp.uppercase(Locale.ROOT), bechData)
        assertEquals(valid.lowercase(Locale.ROOT), recode.lowercase(Locale.ROOT), "Failed to roundtrip '$valid' -> '$recode'")
    }

    private val INVALID_BECH32 = arrayOf(
        " 1nwldj5",  // HRP character out of range
        String(charArrayOf(0x7f.toChar())) + "1axkwrx",  // HRP character out of range
        String(charArrayOf(0x80.toChar())) + "1eym55h",  // HRP character out of range
        "an84characterslonghumanreadablepartthatcontainsthenumber1andtheexcludedcharactersbio1569pvx",  // overall max length exceeded
        "pzry9x0s0muk",  // No separator character
        "1pzry9x0s0muk",  // Empty HRP
        "x1b4n0q5v",  // Invalid data character
        "li1dgmt3",  // Too short checksum
        "de1lg7wt" + String(charArrayOf(0xff.toChar())),  // Invalid character in checksum
        "A1G7SGD8",  // checksum calculated with uppercase form of HRP
        "10a06t8",  // empty HRP
        "1qzzfhee",  // empty HRP
    )

    private val INVALID_BECH32M = arrayOf(
        " 1xj0phk",  // HRP character out of range
        String(charArrayOf(0x7f.toChar())) + "1g6xzxy",  // HRP character out of range
        String(charArrayOf(0x80.toChar())) + "1vctc34",  // HRP character out of range
        "an84characterslonghumanreadablepartthatcontainsthetheexcludedcharactersbioandnumber11d6pts4",  // overall max length exceeded
        "qyrz8wqd2c9m",  // No separator character
        "1qyrz8wqd2c9m",  // Empty HRP
        "y1b0jsk6g",  // Invalid data character
        "lt1igcx5c0",  // Invalid data character
        "in1muywd",  // Too short checksum
        "mm1crxm3i",  // Invalid character in checksum
        "au1s5cgom",  // Invalid character in checksum
        "M1VUXWEZ",  // checksum calculated with uppercase form of HRP
        "16plkw9",  // empty HRP
        "1p2gdwpf",  // empty HRP
    )

    @Test
    fun invalid_bech32() {
        for (invalid in INVALID_BECH32) invalid(invalid)
    }

    @Test
    fun invalid_bech32m() {
        for (invalid in INVALID_BECH32M) invalid(invalid)
    }

    private fun invalid(invalid: String) {
        try {
            decode(invalid)
            assertFails { invalid(invalid) }
        } catch (x: AddressFormatException) {
            /* expected */
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun encodeBytes() {
        val vectors = nip19Vectors()
        for (i in vectors.indices) {
            val bech32 = encodeBytes(Bech32.Encoding.BECH32, vectors[i][1], vectors[i][0].hexToByteArray())
            assertEquals(vectors[i][2], bech32, "incorrect encoding")
        }

    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun decodeBytes() {
        val vectors = nip19Vectors()
        for (i in vectors.indices) {
            val decoded = decode(vectors[i][2])
            val decodedData = decoded.decode5to8().toHexString()
            assertEquals(Bech32.Encoding.BECH32, decoded.encoding, "incorrect encoding type")
            assertEquals(vectors[i][1], decoded.hrp, "incorrect hrp")
            assertEquals(vectors[i][0], decodedData, "incorrect decoded data")
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun decodeBytes2() {
        val vectors = nip19Vectors()
        for (i in vectors.indices) {
            val decoded = decodeBytes(vectors[i][2], vectors[i][1], Bech32.Encoding.BECH32)
            assertEquals(vectors[i][0], decoded.toHexString(), "incorrect decoded data")
        }
    }

    // These vectors are from NIP-19: https://github.com/nostr-protocol/nips/blob/master/19.md
    private fun nip19Vectors(): Array<Array<String>> {
        return arrayOf(
            arrayOf(
                "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d",
                "npub",
                "npub180cvv07tjdrrgpa0j7j7tmnyl2yr6yr7l8j4s3evf6u64th6gkwsyjh6w6"
            ),
            arrayOf(
                "7e7e9c42a91bfef19fa929e5fda1b72e0ebc1a4c1141673e2794234d86addf4e",
                "npub",
                "npub10elfcs4fr0l0r8af98jlmgdh9c8tcxjvz9qkw038js35mp4dma8qzvjptg"
            ),
            arrayOf(
                "67dea2ed018072d675f5415ecfaed7d2597555e202d85b3d65ea4e58d2d92ffa",
                "nsec",
                "nsec1vl029mgpspedva04g90vltkh6fvh240zqtv9k0t9af8935ke9laqsnlfe5"
            ),
        )
    }

    @Test
    fun decode_invalidCharacter_notInAlphabet() {
        assertFailsWith<InvalidCharacter> { decode("A12OUEL5X") }
    }

    @Test
    fun decode_invalidCharacter_upperLowerMix() {
        assertFailsWith<InvalidCharacter> { decode("A12UeL5X") }
    }

    @Test
    fun decode_invalidNetwork() {
        assertFailsWith<InvalidChecksum> { decode("A12UEL5X") }
    }

    @Test
    fun decode_invalidHrp() {
        assertFailsWith<InvalidPrefix> { decode("1pzry9x0s0muk") }
    }
}