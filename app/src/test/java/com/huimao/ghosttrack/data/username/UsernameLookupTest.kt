package com.huimao.ghosttrack.data.username

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsernameLookupTest {

    private val statusSite =
        UsernameSite("Status", "https://example.com/{}", CheckStrategy.StatusCode)
    private val redirectSite = UsernameSite(
        "Redirect", "https://example.com/{}",
        CheckStrategy.RedirectMarkers(listOf("accounts/login")),
    )
    private val markerSite = UsernameSite(
        "Marker", "https://example.com/{}",
        CheckStrategy.MissingMarkers(listOf("User not found")),
    )

    @Test
    fun `statusCode 200 means found`() {
        assertEquals(
            CheckStatus.FOUND,
            evaluate(statusSite, 200, "https://example.com/u", null),
        )
    }

    @Test
    fun `statusCode 404 means not found`() {
        assertEquals(
            CheckStatus.NOT_FOUND,
            evaluate(statusSite, 404, "https://example.com/u", null),
        )
    }

    @Test
    fun `statusCode other means unknown`() {
        assertEquals(
            CheckStatus.UNKNOWN,
            evaluate(statusSite, 403, "https://example.com/u", null),
        )
        assertEquals(
            CheckStatus.UNKNOWN,
            evaluate(statusSite, 999, "https://example.com/u", null),
        )
    }

    @Test
    fun `redirect to login means not found`() {
        assertEquals(
            CheckStatus.NOT_FOUND,
            evaluate(
                redirectSite, 200,
                "https://www.instagram.com/accounts/login/?next=%2Fu%2F",
                null,
            ),
        )
    }

    @Test
    fun `redirect 200 at profile means found`() {
        assertEquals(
            CheckStatus.FOUND,
            evaluate(redirectSite, 200, "https://www.instagram.com/someuser/", null),
        )
    }

    @Test
    fun `marker in body means not found`() {
        assertEquals(
            CheckStatus.NOT_FOUND,
            evaluate(markerSite, 200, "https://example.com/u", "<html>User not found</html>"),
        )
    }

    @Test
    fun `marker 404 means not found even without body`() {
        assertEquals(
            CheckStatus.NOT_FOUND,
            evaluate(markerSite, 404, "https://example.com/u", null),
        )
    }

    @Test
    fun `marker clean 200 means found`() {
        assertEquals(
            CheckStatus.FOUND,
            evaluate(markerSite, 200, "https://example.com/u", "<html>profile page</html>"),
        )
    }

    @Test
    fun `telegram contact-marker means not found`() {
        val tg = USERNAME_SITES.first { it.name == "Telegram" }
        val body = """<meta property="og:title" content="Telegram: Contact @nosuchuser" />"""
        assertEquals(
            CheckStatus.NOT_FOUND,
            evaluate(tg, 200, "https://t.me/nosuchuser", body),
        )
    }

    @Test
    fun `telegram real display name means found`() {
        val tg = USERNAME_SITES.first { it.name == "Telegram" }
        val body = """<meta property="og:title" content="Pavel Durov" />"""
        assertEquals(
            CheckStatus.FOUND,
            evaluate(tg, 200, "https://t.me/durov", body),
        )
    }

    @Test
    fun `tiktok statusCode 10221 means not found`() {
        val tt = USERNAME_SITES.first { it.name == "TikTok" }
        assertEquals(
            CheckStatus.NOT_FOUND,
            evaluate(tt, 200, "https://www.tiktok.com/@x", """{"statusCode":10221}"""),
        )
        assertEquals(
            CheckStatus.FOUND,
            evaluate(tt, 200, "https://www.tiktok.com/@x", """{"nickname":"x"}"""),
        )
    }

    @Test
    fun `all sites have unique names and valid templates`() {
        val names = USERNAME_SITES.map { it.name }
        assertEquals(names.size, names.toSet().size)
        USERNAME_SITES.forEach { site ->
            assertTrue("${site.name} 模板缺少 {}", site.urlTemplate.contains("{}"))
            assertTrue("${site.name} 模板不是 https", site.urlTemplate.startsWith("https://"))
        }
    }
}
