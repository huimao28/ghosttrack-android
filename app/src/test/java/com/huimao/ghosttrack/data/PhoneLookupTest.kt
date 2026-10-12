package com.huimao.ghosttrack.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneLookupTest {

    @Test
    fun `CN mobile number parses as valid`() {
        val info = PhoneLookup.analyze("+8613800138000", "CN").getOrThrow()
        assertTrue(info.valid)
        assertTrue(info.possible)
        assertEquals("CN", info.regionCode)
        assertEquals("mobile", info.typeKey)
        assertEquals("+8613800138000", info.e164)
        assertEquals(86, info.countryCode)
        assertEquals(13800138000L, info.nationalNumber)
    }

    @Test
    fun `national format uses default region`() {
        val info = PhoneLookup.analyze("13800138000", "CN").getOrThrow()
        assertTrue(info.valid)
        assertEquals("+8613800138000", info.e164)
    }

    @Test
    fun `US number parses`() {
        val info = PhoneLookup.analyze("+16502530000", "US").getOrThrow()
        assertTrue(info.valid)
        assertEquals("US", info.regionCode)
        assertEquals(1, info.countryCode)
    }

    @Test
    fun `too-short number is not valid`() {
        val info = PhoneLookup.analyze("+86123", "CN").getOrThrow()
        assertFalse(info.valid)
    }

    @Test
    fun `garbage input fails`() {
        assertTrue(PhoneLookup.analyze("not-a-phone-number", "CN").isFailure)
    }

    @Test
    fun `empty input fails`() {
        assertTrue(PhoneLookup.analyze("   ", "CN").isFailure)
    }
}
