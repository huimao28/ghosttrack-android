package com.huimao.ghosttrack.ui.about

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.billingclient.api.ProductDetails
import com.huimao.ghosttrack.R
import com.huimao.ghosttrack.billing.SponsorBillingManager
import com.huimao.ghosttrack.billing.SponsorTier
import com.huimao.ghosttrack.ui.components.SectionCard

/** 关于页「赞助」区：三个档位的一次性内购（可重复购买）。 */
@Composable
fun SponsorSection() {
    val context = LocalContext.current
    var products by remember { mutableStateOf<List<ProductDetails>?>(null) }
    var toastText by remember { mutableStateOf<String?>(null) }

    val manager = remember {
        SponsorBillingManager(
            context,
            onPurchaseSuccess = {
                toastText = context.getString(R.string.sponsor_thanks)
            },
            onError = {
                toastText = context.getString(R.string.sponsor_failed)
            },
        )
    }

    LaunchedEffect(manager) {
        manager.connect { products = it }
    }
    DisposableEffect(manager) {
        onDispose { manager.destroy() }
    }
    toastText?.let { text ->
        LaunchedEffect(text) {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
            toastText = null
        }
    }

    SectionCard(title = stringResource(R.string.about_sponsor)) {
        Text(
            text = stringResource(R.string.sponsor_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        when (val list = products) {
            null -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .size(28.dp),
                )
            }
            else -> {
                val items = list.mapNotNull { details ->
                    SponsorTier.fromProductId(details.productId)?.let { tier -> tier to details }
                }
                if (items.isEmpty()) {
                    Text(
                        text = stringResource(R.string.sponsor_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    items.forEach { (tier, details) ->
                        val price = details.oneTimePurchaseOfferDetails?.formattedPrice
                        OutlinedButton(
                            onClick = {
                                (context as? Activity)?.let { activity ->
                                    manager.launchPurchase(activity, details)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        ) {
                            val label = stringResource(tierNameRes(tier))
                            Text(if (price != null) "$label · $price" else label)
                        }
                    }
                }
            }
        }
    }
}

private fun tierNameRes(tier: SponsorTier): Int = when (tier) {
    SponsorTier.COFFEE -> R.string.sponsor_coffee
    SponsorTier.LUNCH -> R.string.sponsor_lunch
    SponsorTier.FEAST -> R.string.sponsor_feast
}
