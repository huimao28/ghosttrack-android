package com.huimao.ghosttrack.ui.username

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
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
            withContext(Dispatchers.IO) {
                checker.checkAll(name) { result ->
                    // 切回主线程更新 Compose 状态
                    scope.launch {
                        val updated = (uiState.results + result)
                            .sortedBy { siteOrder[it.site.name] ?: Int.MAX_VALUE }
                        uiState = uiState.copy(
                            done = uiState.done + 1,
                            results = updated,
                        )
                    }
                }
            }
            uiState = uiState.copy(searching = false)
        }
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }
}

private data class StatusStyle(val icon: ImageVector, val color: Color, val label: String)

@Composable
private fun statusStyle(status: CheckStatus): StatusStyle = when (status) {
    CheckStatus.FOUND -> StatusStyle(
        Icons.Filled.CheckCircle,
        MaterialTheme.colorScheme.primary,
        "存在",
    )
    CheckStatus.NOT_FOUND -> StatusStyle(
        Icons.Filled.Cancel,
        MaterialTheme.colorScheme.onSurfaceVariant,
        "不存在",
    )
    CheckStatus.UNKNOWN -> StatusStyle(
        Icons.Filled.Help,
        MaterialTheme.colorScheme.tertiary,
        "未知",
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
        topBar = { TopAppBar(title = { Text("用户名搜索") }) },
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
                    label = { Text("用户名") },
                    placeholder = { Text("如 huimao28") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { vm.search(input) },
                    enabled = !state.searching,
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null)
                    Text("搜索")
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
                            "找到 $foundCount 个"
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
                            Icon(style.icon, contentDescription = style.label, tint = style.color)
                        },
                        trailingContent = {
                            Text(
                                style.label,
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
                    text = "在 ${USERNAME_SITES.size} 个站点并发检查用户名是否存在。\n" +
                        "「未知」表示该站点拦截了自动请求（如反爬），可点进链接手动确认。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
