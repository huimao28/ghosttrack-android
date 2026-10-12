package com.huimao.ghosttrack.ui.username

import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.huimao.ghosttrack.R
import com.huimao.ghosttrack.data.username.CheckStatus
import com.huimao.ghosttrack.data.username.SiteResult
import com.huimao.ghosttrack.data.username.USERNAME_SITES
import com.huimao.ghosttrack.data.username.UsernameChecker
import com.huimao.ghosttrack.ui.components.openUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UsernameUiState(
    val searching: Boolean = false,
    val done: Int = 0,
    val total: Int = USERNAME_SITES.size,
    val results: List<SiteResult> = emptyList(),
    val searchedName: String = "",
)

class UsernameViewModel : ViewModel() {
    var uiState by mutableStateOf(UsernameUiState())
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val checker = UsernameChecker()
    private val siteOrder: Map<String, Int> =
        USERNAME_SITES.withIndex().associate { (i, s) -> s.name to i }

    fun search(username: String) {
        val name = username.trim()
        if (name.isEmpty() || uiState.searching) return
        uiState = UsernameUiState(searching = true, searchedName = name)
        scope.launch {
            // 结果批量刷新（最多每 400ms 一次），避免连续重组造成卡顿
            val collected = mutableListOf<SiteResult>()
            var lastEmit = 0L
            suspend fun emit() {
                val sorted = collected.sortedBy { siteOrder[it.site.name] ?: Int.MAX_VALUE }
                uiState = uiState.copy(done = collected.size, results = sorted)
            }
            withContext(Dispatchers.IO) {
                checker.checkAll(name) { result ->
                    collected.add(result)
                    val now = SystemClock.uptimeMillis()
                    if (now - lastEmit >= 400 || collected.size == USERNAME_SITES.size) {
                        lastEmit = now
                        scope.launch { emit() }
                    }
                }
            }
            val sorted = collected.sortedBy { siteOrder[it.site.name] ?: Int.MAX_VALUE }
            uiState = uiState.copy(searching = false, done = collected.size, results = sorted)
        }
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }
}

private data class StatusStyle(val icon: ImageVector, val color: Color, val labelRes: Int)

@Composable
private fun statusStyle(status: CheckStatus): StatusStyle = when (status) {
    CheckStatus.FOUND -> StatusStyle(
        Icons.Filled.CheckCircle,
        MaterialTheme.colorScheme.primary,
        R.string.status_found,
    )
    CheckStatus.NOT_FOUND -> StatusStyle(
        Icons.Filled.Cancel,
        MaterialTheme.colorScheme.onSurfaceVariant,
        R.string.status_not_found,
    )
    CheckStatus.UNKNOWN -> StatusStyle(
        Icons.Filled.Help,
        MaterialTheme.colorScheme.tertiary,
        R.string.status_unknown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsernameScreen(
    modifier: Modifier = Modifier,
    vm: UsernameViewModel = viewModel(),
) {
    var input by remember { mutableStateOf("") }
    val state = vm.uiState
    val context = LocalContext.current
    val foundCount = state.results.count { it.status == CheckStatus.FOUND }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.username_title)) }) },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text(stringResource(R.string.username_hint)) },
                    placeholder = { Text(stringResource(R.string.username_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { vm.search(input) },
                    enabled = !state.searching,
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null)
                    Text(stringResource(R.string.username_search))
                }
            }

            if (state.searching || state.results.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LinearProgressIndicator(
                        progress = state.done.toFloat() / state.total.coerceAtLeast(1),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = if (state.searching) {
                            "${state.done}/${state.total}"
                        } else {
                            stringResource(R.string.username_found, foundCount)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(state.results, key = { it.site.name }) { result ->
                    val style = statusStyle(result.status)
                    val label = stringResource(style.labelRes)
                    ListItem(
                        headlineContent = { Text(result.site.name) },
                        supportingContent = {
                            Text(
                                result.url,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        leadingContent = {
                            Icon(style.icon, contentDescription = label, tint = style.color)
                        },
                        trailingContent = {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelLarge,
                                color = style.color,
                            )
                        },
                        modifier = Modifier.clickable { context.openUrl(result.url) },
                    )
                }
            }

            if (!state.searching && state.results.isEmpty()) {
                Text(
                    text = stringResource(R.string.username_empty, USERNAME_SITES.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
