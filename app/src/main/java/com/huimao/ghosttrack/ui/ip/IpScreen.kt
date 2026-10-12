package com.huimao.ghosttrack.ui.ip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.huimao.ghosttrack.R
import com.huimao.ghosttrack.data.IpLookup
import com.huimao.ghosttrack.data.IpQuality
import com.huimao.ghosttrack.data.IpWhoIsResponse
import com.huimao.ghosttrack.data.NetworkType
import com.huimao.ghosttrack.ui.components.ErrorCard
import com.huimao.ghosttrack.ui.components.InfoRow
import com.huimao.ghosttrack.ui.components.PurityBar
import com.huimao.ghosttrack.ui.components.SectionCard
import com.huimao.ghosttrack.ui.components.openUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface IpError {
    data object EmptyInput : IpError
    data class Lookup(val raw: String) : IpError
    data class MyIp(val raw: String) : IpError
}

@Composable
private fun IpError.text(): String = when (this) {
    IpError.EmptyInput -> stringResource(R.string.ip_enter_ip)
    is IpError.Lookup -> stringResource(R.string.ip_lookup_failed, raw)
    is IpError.MyIp -> stringResource(R.string.ip_myip_failed, raw)
}

data class IpUiState(
    val loading: Boolean = false,
    val error: IpError? = null,
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
            uiState = uiState.copy(error = IpError.EmptyInput)
            return
        }
        uiState = IpUiState(loading = true)
        scope.launch {
            val result = withContext(Dispatchers.IO) { lookup.lookup(ip) }
            val info = result.getOrNull()
            if (info == null) {
                uiState = IpUiState(
                    error = IpError.Lookup(result.exceptionOrNull()?.message ?: "?"),
                )
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
                uiState = IpUiState(
                    error = IpError.MyIp(myIp.exceptionOrNull()?.message ?: "?"),
                )
                return@launch
            }
            val result = withContext(Dispatchers.IO) { lookup.lookup(ip) }
            val info = result.getOrNull()
            if (info == null) {
                uiState = IpUiState(
                    error = IpError.Lookup(result.exceptionOrNull()?.message ?: "?"),
                )
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
                qualityError = q.exceptionOrNull()?.message,
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
        topBar = { TopAppBar(title = { Text(stringResource(R.string.ip_title)) }) },
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
                    label = { Text(stringResource(R.string.ip_hint)) },
                    placeholder = { Text(stringResource(R.string.ip_placeholder)) },
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
                    ) { Text(stringResource(R.string.ip_lookup)) }
                    OutlinedButton(
                        onClick = { vm.queryMyIp() },
                        enabled = !state.loading,
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.ip_my_ip)) }
                }
            }
            if (state.loading) {
                item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            }
            state.error?.let { error ->
                item { ErrorCard(error.text()) }
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
    val unknown = stringResource(R.string.unknown)
    val yes = stringResource(R.string.yes)
    val no = stringResource(R.string.no)
    SectionCard(title = stringResource(R.string.ip_result)) {
        InfoRow(stringResource(R.string.ip_hint), info.ip, copyable = true)
        if (info.type.isNotBlank()) InfoRow(stringResource(R.string.ip_type), info.type)

        val country = buildString {
            info.flag?.emoji?.ifBlank { null }?.let { append(it).append(' ') }
            append(info.country.ifBlank { unknown })
            if (info.countryCode.isNotBlank()) append(" (${info.countryCode})")
        }
        InfoRow(stringResource(R.string.ip_country), country)
        if (info.city.isNotBlank()) InfoRow(stringResource(R.string.ip_city), info.city)
        if (info.region.isNotBlank()) {
            val region =
                if (info.regionCode.isNotBlank()) "${info.region} (${info.regionCode})" else info.region
            InfoRow(stringResource(R.string.ip_region), region)
        }
        info.postal?.ifBlank { null }?.let { InfoRow(stringResource(R.string.ip_postal), it) }
        info.isEu?.let { InfoRow(stringResource(R.string.ip_is_eu), if (it) yes else no) }
        info.callingCode?.ifBlank { null }?.let { InfoRow(stringResource(R.string.ip_calling_code), "+$it") }
        info.capital?.ifBlank { null }?.let { InfoRow(stringResource(R.string.ip_capital), it) }
        info.borders?.ifBlank { null }?.let { InfoRow(stringResource(R.string.ip_borders), it) }

        info.connection?.let { conn ->
            conn.asn?.let { InfoRow("ASN", "AS$it") }
            conn.org?.ifBlank { null }?.let { InfoRow(stringResource(R.string.ip_org), it) }
            conn.isp?.ifBlank { null }?.let { InfoRow(stringResource(R.string.ip_isp), it) }
            conn.domain?.ifBlank { null }?.let { InfoRow(stringResource(R.string.ip_domain), it) }
        }

        info.timezone?.let { tz ->
            val label = buildString {
                append(tz.id.ifBlank { unknown })
                if (tz.abbr.isNotBlank()) append(" (${tz.abbr})")
                if (tz.utc.isNotBlank()) append("  ${tz.utc}")
            }
            InfoRow(stringResource(R.string.ip_timezone), label)
            tz.isDst?.let { InfoRow(stringResource(R.string.ip_dst), if (it) yes else no) }
        }

        val lat = info.latitude
        val lon = info.longitude
        if (lat != null && lon != null) {
            InfoRow(stringResource(R.string.ip_latlon), "$lat, $lon")
            FilledTonalButton(
                onClick = { onOpenUrl("https://www.google.com/maps/@$lat,$lon,8z") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) { Text(stringResource(R.string.ip_open_map)) }
        }
    }
}

@Composable
private fun IpQualityCard(state: IpUiState) {
    SectionCard(title = stringResource(R.string.ip_quality_title)) {
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
                    text = stringResource(R.string.ip_quality_failed, state.qualityError),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            state.quality != null -> {
                val q = state.quality
                val netRes = when (q.networkType()) {
                    NetworkType.DATACENTER -> R.string.ip_net_datacenter
                    NetworkType.MOBILE -> R.string.ip_net_mobile
                    NetworkType.RESIDENTIAL -> R.string.ip_net_residential
                }
                InfoRow(stringResource(R.string.ip_network_type), stringResource(netRes))
                InfoRow(
                    stringResource(R.string.ip_proxy),
                    if (q.proxy) stringResource(R.string.ip_proxy_yes)
                    else stringResource(R.string.no),
                )
                InfoRow(
                    stringResource(R.string.ip_native),
                    if (q.isNative()) stringResource(R.string.ip_native_yes)
                    else stringResource(R.string.ip_native_no),
                )
                val score = q.purityScore()
                val gradeRes = when (score) {
                    in 90..100 -> R.string.ip_purity_excellent
                    in 70..89 -> R.string.ip_purity_good
                    in 40..69 -> R.string.ip_purity_fair
                    else -> R.string.ip_purity_poor
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                ) {
                    Text(
                        text = stringResource(R.string.ip_purity),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(92.dp),
                    )
                    Text(
                        text = stringResource(R.string.ip_purity_value, score, stringResource(gradeRes)),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
                PurityBar(
                    score = score,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                )
                Text(
                    text = stringResource(R.string.ip_quality_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
