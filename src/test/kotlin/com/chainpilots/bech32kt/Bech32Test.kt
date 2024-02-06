package com.chainpilots.bech32kt

import com.chainpilots.bech32kt.AddressFormatException.*
import com.chainpilots.bech32kt.Constants.BECH32M_CONST
import com.chainpilots.bech32kt.Constants.BECH32_CONST
import com.chainpilots.bech32kt.TestData.INVALID_BECH32
import com.chainpilots.bech32kt.TestData.INVALID_BECH32M
import com.chainpilots.bech32kt.TestData.VALID_BECH32
import com.chainpilots.bech32kt.TestData.VALID_BECH32M
import com.chainpilots.bech32kt.TestData.nip19Vectors
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith

class Bech32Test{

    @Test
    fun validBech32() {
        for (valid in VALID_BECH32) {
            valid(valid, BECH32_CONST)
            validWithExtension(valid, BECH32_CONST)
        }
    }

    @Test
    fun validBech32m() {
        for (valid in VALID_BECH32M) {
            valid(valid, BECH32M_CONST)
            validWithExtension(valid, BECH32M_CONST)
        }
    }

    private fun valid(valid: String, encoding: Int) {
        var bechData: Bech32Data = Bech32Data("", byteArrayOf())
        var recode: String = ""
        if (encoding == BECH32_CONST) {
            bechData = Bech32.decode(valid)
            recode = Bech32.encode(bechData.hrp, bechData.data)
        }
        if (encoding == BECH32M_CONST) {
            bechData = Bech32M.decode(valid)
            recode = Bech32M.encode(bechData.hrp, bechData.data)
        }
        assertEquals(valid.lowercase(Locale.ROOT), recode.lowercase(Locale.ROOT),"Failed to roundtrip '$valid' -> '$recode'" )
        // Test encoding with an uppercase HRP
        recode = encode(encoding, bechData.hrp.uppercase(Locale.ROOT), bechData.data, null)
        assertEquals(valid.lowercase(Locale.ROOT), recode.lowercase(Locale.ROOT), "Failed to roundtrip '$valid' -> '$recode'")
    }

    private fun validWithExtension(valid: String, encoding: Int) {
        val (hrp, data) = valid.decodeBech32ToBech32Data()
        var recode = ""
        if (encoding == BECH32_CONST) {
            recode = data.fromWords().encodeToBech32(hrp)
            assertEquals(valid.lowercase(Locale.ROOT), recode.lowercase(Locale.ROOT),"Failed to roundtrip '$valid' -> '$recode'" )
            recode = data.fromWords().encodeToBech32(hrp.uppercase(Locale.ROOT))
        }
        if (encoding == BECH32M_CONST) {
            recode = data.encodeToBech32m(hrp)
            assertEquals(valid.lowercase(Locale.ROOT), recode.lowercase(Locale.ROOT),"Failed to roundtrip '$valid' -> '$recode'" )
            recode = data.encodeToBech32m(hrp.uppercase(Locale.ROOT))
        }
        // Test encoding with an uppercase HRP
        assertEquals(valid.lowercase(Locale.ROOT), recode.lowercase(Locale.ROOT), "Failed to roundtrip '$valid' -> '$recode'")
    }

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
            invalid.decodeBech32ToBech32Data()
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
            val bech32 = Bech32.encode(vectors[i][1], vectors[i][0].hexToByteArray().toWords())
            assertEquals(vectors[i][2], bech32, "incorrect encoding")
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun decodeBytes() {
        val vectors = nip19Vectors()
        for (i in vectors.indices) {
            val decoded = Bech32.decode(vectors[i][2])
            val decodedData = decoded.data.fromWords().toHexString()
            assertEquals(vectors[i][1], decoded.hrp, "incorrect hrp")
            assertEquals(vectors[i][0], decodedData, "incorrect decoded data")
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun decodeBytes2() {
        val vectors = nip19Vectors()
        for (i in vectors.indices) {
            val decoded = Bech32.decode(vectors[i][2])
            assertEquals(vectors[i][0], decoded.data.fromWords().toHexString(), "incorrect decoded data")
        }
    }

    @Test
    fun decode_invalidCharacter_notInAlphabet() {
        assertFailsWith<InvalidCharacter> { Bech32.decode("A12OUEL5X") }
    }

    @Test
    fun decode_invalidCharacter_upperLowerMix() {
        assertFailsWith<InvalidCharacter> {  Bech32.decode("A12UeL5X") }
    }

    @Test
    fun decode_invalidNetwork() {
        assertFailsWith<InvalidChecksum> {  Bech32.decode("A12UEL5X") }
    }

    @Test
    fun decode_invalidHrp() {
        assertFailsWith<InvalidPrefix> {  Bech32.decode("1pzry9x0s0muk") }
    }
}