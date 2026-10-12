package com.huimao.ghosttrack.ui.phone

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.huimao.ghosttrack.data.PhoneInfo
import com.huimao.ghosttrack.data.PhoneLookup
import com.huimao.ghosttrack.ui.components.ErrorCard
import com.huimao.ghosttrack.ui.components.InfoRow
import com.huimao.ghosttrack.ui.components.SectionCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 无国家码时可用的默认归属地。 */
private val REGIONS = listOf(
    "CN" to "中国大陆",
    "HK" to "香港",
    "TW" to "台湾",
    "MO" to "澳门",
    "US" to "美国",
    "CA" to "加拿大",
    "GB" to "英国",
    "DE" to "德国",
    "FR" to "法国",
    "JP" to "日本",
    "KR" to "韩国",
    "SG" to "新加坡",
    "MY" to "马来西亚",
    "TH" to "泰国",
    "AU" to "澳大利亚",
    "ID" to "印尼",
)

data class PhoneUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val result: PhoneInfo? = null,
)

class PhoneViewModel : ViewModel() {
    var uiState by mutableStateOf(PhoneUiState())
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun analyze(input: String, region: String) {
        val raw = input.trim()
        if (raw.isEmpty()) {
            uiState = uiState.copy(error = "请输入电话号码")
            return
        }
        uiState = PhoneUiState(loading = true)
        scope.launch {
            val result = withContext(Dispatchers.IO) { PhoneLookup.analyze(raw, region) }
            uiState = PhoneUiState(
                result = result.getOrNull(),
                error = result.exceptionOrNull()?.let { "解析失败：${it.message}" },
            )
        }
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneScreen(
    modifier: Modifier = Modifier,
    vm: PhoneViewModel = viewModel(),
) {
    var input by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("CN") }
    var showPicker by remember { mutableStateOf(false) }
    val state = vm.uiState
    val regionName = REGIONS.firstOrNull { it.first == region }?.second ?: region

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("默认归属地") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(REGIONS) { (code, name) ->
                        ListItem(
                            headlineContent = { Text("$name ($code)") },
                            modifier = Modifier.clickable {
                                region = code
                                showPicker = false
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPicker = false }) { Text("取消") }
            },
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("号码解析") }) },
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
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("电话号码") },
                    placeholder = { Text("如 +8613800138000 或 13800138000") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "默认归属地：$regionName ($region)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { showPicker = true }) {
                        Text("更换")
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                    }
                }
            }
            item {
                Button(
                    onClick = { vm.analyze(input, region) },
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("解析") }
            }
            if (state.loading) {
                item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            }
            state.error?.let { error ->
                item { ErrorCard(error) }
            }
            state.result?.let { info ->
                item { PhoneResultCard(info) }
            }
            item {
                Text(
                    text = "基于 Google libphonenumber 离线解析，无需网络，结果仅供参考。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PhoneResultCard(info: PhoneInfo) {
    SectionCard(title = "解析结果") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val valid = info.valid
            Icon(
                if (valid) Icons.Filled.CheckCircle else Icons.Filled.Error,
                contentDescription = null,
                tint = if (valid) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
            )
            Text(
                text = if (valid) " 有效号码" else " 无效号码",
                style = MaterialTheme.typography.titleMedium,
                color = if (valid) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
            )
        }
        InfoRow("号码类型", info.typeLabel)
        InfoRow("运营商", info.carrier)
        InfoRow("归属地", info.location)
        InfoRow("国家/地区", info.regionCode)
        if (info.timezones.isNotEmpty()) {
            InfoRow("时区", info.timezones.joinToString(", "))
        }
        InfoRow("E.164", info.e164, copyable = true)
        InfoRow("国际格式", info.international, copyable = true)
        InfoRow("国内格式", info.national, copyable = true)
        InfoRow("国家码", "+${info.countryCode}")
        InfoRow("本地号码", info.nationalNumber.toString())
    }
}
