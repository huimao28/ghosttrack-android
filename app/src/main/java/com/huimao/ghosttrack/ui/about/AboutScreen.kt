package com.huimao.ghosttrack.ui.about

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huimao.ghosttrack.R
import com.huimao.ghosttrack.ui.components.InfoRow
import com.huimao.ghosttrack.ui.components.SectionCard
import com.huimao.ghosttrack.ui.components.openUrl

private const val URL_RELEASES = "https://github.com/huimao28/ghosttrack-android/releases"
private const val URL_REPO = "https://github.com/huimao28/ghosttrack-android"
private const val URL_HOMEPAGE = "https://github.com/huimao28"
private const val URL_ORIGINAL = "https://github.com/HunxByts/GhostTrack"

private val CREDITS = listOf(
    "Jetpack Compose · Material 3",
    "OkHttp",
    "kotlinx.serialization",
    "kotlinx.coroutines",
    "libphonenumber · carrier · geocoder",
    "AndroidX (Core / Activity / Lifecycle / AppCompat)",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var lang by remember { mutableStateOf(LanguageSetting.current(context)) }

    @Suppress("DEPRECATION")
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0)?.versionName
        } catch (_: Exception) {
            null
        } ?: "?"
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.about_title)) }) },
        modifier = modifier,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = "GhostTrack",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.version, versionName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.about_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SectionCard(title = stringResource(R.string.about_language)) {
                    val options = listOf(
                        LanguageSetting.SYSTEM to stringResource(R.string.lang_system),
                        LanguageSetting.ZH to stringResource(R.string.lang_zh),
                        LanguageSetting.EN to stringResource(R.string.lang_en),
                    )
                    options.forEach { (value, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    lang = value
                                    LanguageSetting.apply(context, value)
                                    (context as? Activity)?.recreate()
                                }
                                .padding(vertical = 4.dp),
                        ) {
                            RadioButton(
                                selected = lang == value,
                                onClick = {
                                    lang = value
                                    LanguageSetting.apply(context, value)
                                    (context as? Activity)?.recreate()
                                },
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }

            item {
                SectionCard(title = stringResource(R.string.about_links)) {
                    LinkButton(
                        label = stringResource(R.string.about_check_update),
                        onClick = { context.openUrl(URL_RELEASES) },
                    )
                    LinkButton(
                        label = stringResource(R.string.about_repo),
                        onClick = { context.openUrl(URL_REPO) },
                    )
                    LinkButton(
                        label = stringResource(R.string.about_homepage),
                        onClick = { context.openUrl(URL_HOMEPAGE) },
                    )
                    LinkButton(
                        label = stringResource(R.string.about_original),
                        onClick = { context.openUrl(URL_ORIGINAL) },
                    )
                }
            }

            item {
                SectionCard(title = stringResource(R.string.about_license)) {
                    Text(
                        text = stringResource(R.string.about_license_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                val apache = stringResource(R.string.about_apache)
                SectionCard(title = stringResource(R.string.about_credits)) {
                    CREDITS.forEach { name ->
                        InfoRow(name, apache)
                    }
                }
            }

            item {
                SectionCard(title = stringResource(R.string.about_sources)) {
                    Text(
                        text = stringResource(R.string.about_sources_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun LinkButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.OpenInNew, contentDescription = null)
    }
}
