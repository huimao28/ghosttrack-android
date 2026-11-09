package com.huimao.ghosttrack.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huimao.ghosttrack.R
import com.huimao.ghosttrack.data.username.USERNAME_SITES

private data class Feature(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accent: Color,
    val onClick: () -> Unit,
)

@Composable
fun HomeScreen(
    onOpenIp: () -> Unit,
    onOpenPhone: () -> Unit,
    onOpenUsername: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val features = listOf(
        Feature(stringResource(R.string.feat_ip_title), stringResource(R.string.feat_ip_desc), Icons.Filled.LocationOn, colors.primary, onOpenIp),
        Feature(stringResource(R.string.feat_myip_title), stringResource(R.string.feat_myip_desc), Icons.Filled.Public, colors.secondary, onOpenIp),
        Feature(stringResource(R.string.feat_phone_title), stringResource(R.string.feat_phone_desc), Icons.Filled.Smartphone, colors.tertiary, onOpenPhone),
        Feature(stringResource(R.string.feat_username_title), stringResource(R.string.feat_username_desc, USERNAME_SITES.size), Icons.Filled.PersonSearch, colors.primary, onOpenUsername),
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { HeroPanel() }
        item {
            Text(
                text = stringResource(R.string.home_tools),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
            )
        }
        itemsIndexed(features, key = { _, feature -> feature.title }) { index, feature ->
            FeatureCard(feature, index)
        }
        item {
            Text(
                text = stringResource(R.string.home_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp, start = 4.dp, end = 4.dp),
            )
        }
    }
}

@Composable
private fun HeroPanel() {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(listOf(colors.primaryContainer, colors.secondaryContainer.copy(alpha = 0.78f))),
                shape = RoundedCornerShape(
                    topStart = 40.dp,
                    topEnd = 40.dp,
                    bottomEnd = 40.dp,
                    bottomStart = 14.dp,
                ),
            )
            .padding(24.dp),
    ) {
        Column {
            Surface(color = colors.onPrimaryContainer.copy(alpha = 0.09f), shape = CircleShape) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Icon(Icons.Filled.Security, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(16.dp))
                    Text("OSINT TOOLKIT", style = MaterialTheme.typography.labelMedium, color = colors.onPrimaryContainer, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(22.dp))
            Text("GhostTrack", style = MaterialTheme.typography.headlineLarge, color = colors.onPrimaryContainer, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.home_subtitle), style = MaterialTheme.typography.bodyLarge, color = colors.onPrimaryContainer.copy(alpha = 0.78f))
        }
    }
}

@Composable
private fun FeatureCard(feature: Feature, index: Int) {
    val shape = if (index % 2 == 0) {
        RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomEnd = 10.dp, bottomStart = 28.dp)
    } else {
        RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomEnd = 28.dp, bottomStart = 10.dp)
    }
    Card(
        onClick = feature.onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(color = feature.accent.copy(alpha = 0.13f), shape = MaterialTheme.shapes.medium) {
                Icon(feature.icon, contentDescription = null, tint = feature.accent, modifier = Modifier.padding(12.dp).size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(feature.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(feature.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ArrowOutward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}
