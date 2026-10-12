package com.huimao.ghosttrack.ui.ip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.huimao.ghosttrack.data.IpLookup
import com.huimao.ghosttrack.data.IpQuality
import com.huimao.ghosttrack.data.IpWhoIsResponse
import com.huimao.ghosttrack.ui.components.ErrorCard
import com.huimao.ghosttrack.ui.components.InfoRow
import com.huimao.ghosttrack.ui.components.SectionCard
import com.huimao.ghosttrack.ui.components.openUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class IpUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val result: IpWhoIsResponse? = null,
    val quality: IpQuality? = null,
    val qualityLoading: Boolean = false,
    val qualityError: String? = null,
)

class IpViewModel : ViewModel() {
    var uiState by mutableStateOf(IpUiState())
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val lookup = IpLookup()

    fun query(ipInput: String) {
        val ip = ipInput.trim()
        if (ip.isEmpty()) {
            uiState = uiState.copy(error = "请输入 IP 地址")
            return
        }
        uiState = IpUiState(loading = true)
        scope.launch {
            val result = withContext(Dispatchers.IO) { lookup.lookup(ip) }
            val info = result.getOrNull()
            if (info == null) {
                uiState = IpUiState(error = "查询失败：${result.exceptionOrNull()?.message}")
                return@launch
            }
            uiState = IpUiState(result = info, qualityLoading = true)
            loadQuality(info.ip.ifBlank { ip })
        }
    }

    fun queryMyIp() {
        uiState = IpUiState(loading = true)
        scope.launch {
            val myIp = withContext(Dispatchers.IO) { lookup.myIp() }
            val ip = myIp.getOrNull()
            if (ip == null) {
                uiState = IpUiState(error = "获取本机 IP 失败：${myIp.exceptionOrNull()?.message}")
                return@launch
            }
            val result = withContext(Dispatchers.IO) { lookup.lookup(ip) }
            val info = result.getOrNull()
            if (info == null) {
                uiState = IpUiState(error = "查询失败：${result.exceptionOrNull()?.message}")
                return@launch
            }
            uiState = IpUiState(result = info, qualityLoading = true)
            loadQuality(info.ip.ifBlank { ip })
        }
    }

    private fun loadQuality(ip: String) {
        scope.launch {
            val q = withContext(Dispatchers.IO) { lookup.quality(ip) }
            uiState = uiState.copy(
                qualityLoading = false,
                quality = q.getOrNull(),
                qualityError = q.exceptionOrNull()?.let { "质量检测失败：${it.message}" },
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
fun IpScreen(
    modifier: Modifier = Modifier,
    vm: IpViewModel = viewModel(),
) {
    var input by remember { mutableStateOf("") }
    val state = vm.uiState
    val context = LocalContext.current

    Scaffold(
        topBar = { TopAppBar(title = { Text("IP 追踪") }) },
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
                    label = { Text("IP 地址") },
                    placeholder = { Text("如 8.8.8.8，支持 IPv4 / IPv6") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { vm.query(input) },
                        enabled = !state.loading,
                        modifier = Modifier.weight(1f),
                    ) { Text("查询") }
                    OutlinedButton(
                        onClick = { vm.queryMyIp() },
                        enabled = !state.loading,
                        modifier = Modifier.weight(1f),
                    ) { Text("查本机 IP") }
                }
            }
            if (state.loading) {
                item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            }
            state.error?.let { error ->
                item { ErrorCard(error) }
            }
            state.result?.let { info ->
                item { IpResultCard(info) { url -> context.openUrl(url) } }
                item { IpQualityCard(state) }
            }
        }
    }
}

@Composable
private fun IpResultCard(info: IpWhoIsResponse, onOpenUrl: (String) -> Unit) {
    SectionCard(title = "查询结果") {
        InfoRow("IP 地址", info.ip, copyable = true)
        if (info.type.isNotBlank()) InfoRow("类型", info.type)

        val country = buildString {
            info.flag?.emoji?.ifBlank { null }?.let { append(it).append(' ') }
            append(info.country.ifBlank { "未知" })
            if (info.countryCode.isNotBlank()) append(" (${info.countryCode})")
        }
        InfoRow("国家", country)
        if (info.city.isNotBlank()) InfoRow("城市", info.city)
        if (info.region.isNotBlank()) {
            val region = if (info.regionCode.isNotBlank()) "${info.region} (${info.regionCode})" else info.region
            InfoRow("地区", region)
        }
        info.postal?.ifBlank { null }?.let { InfoRow("邮编", it) }
        info.isEu?.let { InfoRow("是否欧盟", if (it) "是" else "否") }
        info.callingCode?.ifBlank { null }?.let { InfoRow("电话区号", "+$it") }
        info.capital?.ifBlank { null }?.let { InfoRow("首都", it) }
        info.borders?.ifBlank { null }?.let { InfoRow("接壤", it) }

        info.connection?.let { conn ->
            conn.asn?.let { InfoRow("ASN", "AS$it") }
            conn.org?.ifBlank { null }?.let { InfoRow("组织", it) }
            conn.isp?.ifBlank { null }?.let { InfoRow("ISP", it) }
            conn.domain?.ifBlank { null }?.let { InfoRow("域名", it) }
        }

        info.timezone?.let { tz ->
            val label = buildString {
                append(tz.id.ifBlank { "未知" })
                if (tz.abbr.isNotBlank()) append(" (${tz.abbr})")
                if (tz.utc.isNotBlank()) append("  ${tz.utc}")
            }
            InfoRow("时区", label)
            tz.isDst?.let { InfoRow("夏令时", if (it) "是" else "否") }
        }

        val lat = info.latitude
        val lon = info.longitude
        if (lat != null && lon != null) {
            InfoRow("经纬度", "$lat, $lon")
            FilledTonalButton(
                onClick = { onOpenUrl("https://www.google.com/maps/@$lat,$lon,8z") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) { Text("在地图中打开") }
        }
    }
}

@Composable
private fun IpQualityCard(state: IpUiState) {
    SectionCard(title = "IP 质量检测") {
        when {
            state.qualityLoading -> {
                LinearProgressIndicator(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                )
            }
            state.qualityError != null -> {
                Text(
                    text = state.qualityError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            state.quality != null -> {
                val q = state.quality
                InfoRow("网络类型", q.networkTypeLabel())
                InfoRow("代理/VPN", if (q.proxy) "是（疑似中转）" else "否")
                InfoRow("原生 IP", if (q.isNative()) "是（估算）" else "否（估算）")
                InfoRow("纯净度", "${q.purityScore()} 分 · ${q.purityGrade()}（估算）")
                Text(
                    text = "网络类型 / 原生 / 纯净度均为基于公开标记的估算，仅供参考；数据源 ip-api.com。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
