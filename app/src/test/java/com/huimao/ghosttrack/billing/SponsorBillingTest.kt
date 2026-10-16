package com.huimao.ghosttrack.billing

import com.android.billingclient.api.Purchase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SponsorBillingTest {

    @Test
    fun `product ids are fixed and ordered`() {
        assertEquals("sponsor_coffee", SponsorTier.COFFEE.productId)
        assertEquals("sponsor_lunch", SponsorTier.LUNCH.productId)
        assertEquals("sponsor_feast", SponsorTier.FEAST.productId)
        assertEquals(
            listOf("sponsor_coffee", "sponsor_lunch", "sponsor_feast"),
            SponsorTier.allProductIds,
        )
    }

    @Test
    fun `fromProductId resolves tiers`() {
        assertEquals(SponsorTier.COFFEE, SponsorTier.fromProductId("sponsor_coffee"))
        assertEquals(SponsorTier.LUNCH, SponsorTier.fromProductId("sponsor_lunch"))
        assertEquals(SponsorTier.FEAST, SponsorTier.fromProductId("sponsor_feast"))
        assertNull(SponsorTier.fromProductId("unknown_sku"))
        assertNull(SponsorTier.fromProductId(""))
    }

    @Test
    fun `shouldConsume only for purchased and unacknowledged`() {
        assertTrue(
            SponsorPurchasePolicy.shouldConsume(
                Purchase.PurchaseState.PURCHASED,
                isAcknowledged = false,
            ),
        )
        assertFalse(
            SponsorPurchasePolicy.shouldConsume(
                Purchase.PurchaseState.PURCHASED,
                isAcknowledged = true,
            ),
        )
        assertFalse(
            SponsorPurchasePolicy.shouldConsume(
                Purchase.PurchaseState.PENDING,
                isAcknowledged = false,
            ),
        )
        assertFalse(
            SponsorPurchasePolicy.shouldConsume(
                Purchase.PurchaseState.UNSPECIFIED_STATE,
                isAcknowledged = false,
            ),
        )
    }

    @Test
    fun `tierOf finds sponsor tier in product id list`() {
        assertEquals(
            SponsorTier.LUNCH,
            SponsorPurchasePolicy.tierOf(listOf("sponsor_lunch")),
        )
        assertEquals(
            SponsorTier.FEAST,
            SponsorPurchasePolicy.tierOf(listOf("other_sku", "sponsor_feast")),
        )
        assertNull(SponsorPurchasePolicy.tierOf(listOf("other_sku")))
        assertNull(SponsorPurchasePolicy.tierOf(emptyList()))
    }
}
