package com.huimao.ghosttrack.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IpLookupTest {

    private val lookup = IpLookup()

    @Test
    fun `parses full ipwhois sample response`() {
        val info = lookup.parseResponse(SAMPLE_JSON)
        assertTrue(info.success)
        assertEquals("8.8.8.8", info.ip)
        assertEquals("IPv4", info.type)
        assertEquals("North America", info.continent)
        assertEquals("United States", info.country)
        assertEquals("US", info.countryCode)
        assertEquals("California", info.region)
        assertEquals("San Jose", info.city)
        assertEquals(37.3393939, info.latitude!!, 1e-6)
        assertEquals(-121.8949553, info.longitude!!, 1e-6)
        assertEquals("95025", info.postal)
        assertEquals("1", info.callingCode)
        assertEquals("🇺🇸", info.flag?.emoji)
        assertEquals(15169L, info.connection?.asn)
        assertEquals("Google LLC", info.connection?.org)
        assertEquals("Google LLC", info.connection?.isp)
        assertEquals("America/Los_Angeles", info.timezone?.id)
        assertEquals("PDT", info.timezone?.abbr)
        assertEquals("-07:00", info.timezone?.utc)
    }

    @Test
    fun `parses failure response`() {
        val info = lookup.parseResponse("""{"success":false,"message":"Invalid IP address"}""")
        assertFalse(info.success)
        assertEquals("Invalid IP address", info.message)
    }

    @Test
    fun `tolerates missing optional fields`() {
        val info = lookup.parseResponse("""{"ip":"1.1.1.1","success":true}""")
        assertTrue(info.success)
        assertEquals("1.1.1.1", info.ip)
        assertEquals("", info.country)
        assertNull(info.latitude)
        assertNull(info.connection)
        assertNull(info.timezone)
    }

    companion object {
        // 真实抓取的 https://ipwho.is/8.8.8.8 返回（已精简空白）
        private const val SAMPLE_JSON = """{"ip":"8.8.8.8","success":true,"type":"IPv4","continent":"North America","continent_code":"NA","country":"United States","country_code":"US","region":"California","region_code":"CA","city":"San Jose","latitude":37.3393939,"longitude":-121.8949553,"is_eu":false,"postal":"95025","calling_code":"1","capital":"Washington D.C.","borders":"CA,MX","flag":{"img":"https://cdn.ipwhois.io/flags/us.svg","emoji":"🇺🇸","emoji_unicode":"U+1F1FA U+1F1F8"},"connection":{"asn":15169,"org":"Google LLC","isp":"Google LLC","domain":"google.com"},"timezone":{"id":"America/Los_Angeles","abbr":"PDT","is_dst":true,"offset":-25200,"utc":"-07:00"},"readme":"https://ipwhois.io/docs"}"""
    }
}
