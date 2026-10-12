package com.huimao.ghosttrack.ui.phone

import android.os.SystemClock
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.huimao.ghosttrack.R
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

/** 无国家码时可用的默认归属地（显示名走多语言资源）。 */
private val REGIONS = listOf(
    "CN", "HK", "TW", "MO",
    "US", "CA", "GB", "DE", "FR",
    "JP", "KR", "SG", "MY", "TH", "AU", "ID",
)

@Composable
private fun regionName(code: String): String = stringResource(
    when (code) {
        "CN" -> R.string.region_cn
        "HK" -> R.string.region_hk
        "TW" -> R.string.region_tw
        "MO" -> R.string.region_mo
        "US" -> R.string.region_us
        "CA" -> R.string.region_ca
        "GB" -> R.string.region_gb
        "DE" -> R.string.region_de
        "FR" -> R.string.region_fr
        "JP" -> R.string.region_jp
        "KR" -> R.string.region_kr
        "SG" -> R.string.region_sg
        "MY" -> R.string.region_my
        "TH" -> R.string.region_th
        "AU" -> R.string.region_au
        "ID" -> R.string.region_id
        else -> R.string.unknown
    },
)

@Composable
private fun phoneTypeLabel(typeKey: String): String = stringResource(
    when (typeKey) {
        "mobile" -> R.string.phone_type_mobile
        "fixed_line" -> R.string.phone_type_fixed_line
        "fixed_line_or_mobile" -> R.string.phone_type_fixed_line_or_mobile
        "toll_free" -> R.string.phone_type_toll_free
        "premium_rate" -> R.string.phone_type_premium_rate
        "shared_cost" -> R.string.phone_type_shared_cost
        "voip" -> R.string.phone_type_voip
        "personal_number" -> R.string.phone_type_personal_number
        "pager" -> R.string.phone_type_pager
        "uan" -> R.string.phone_type_uan
        "voicemail" -> R.string.phone_type_voicemail
        else -> R.string.phone_type_unknown
    },
)

sealed interface PhoneError {
    data object EmptyInput : PhoneError
    data class Failed(val raw: String) : PhoneError
}

@Composable
private fun PhoneError.text(): String = when (this) {
    PhoneError.EmptyInput -> stringResource(R.string.phone_enter)
    is PhoneError.Failed -> if (raw.startsWith("unparsable:")) {
        stringResource(R.string.phone_unparsable, raw.removePrefix("unparsable:"))
    } else {
        stringResource(R.string.phone_failed, raw)
    }
}

data class PhoneUiState(
    val loading: Boolean = false,
    val error: PhoneError? = null,
    val result: PhoneInfo? = null,
)

class PhoneViewModel : ViewModel() {
    var uiState by mutableStateOf(PhoneUiState())
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun analyze(input: String, region: String) {
        val raw = input.trim()
        if (raw.isEmpty()) {
            uiState = uiState.copy(error = PhoneError.EmptyInput)
            return
        }
        uiState = PhoneUiState(loading = true)
        scope.launch {
            val result = withContext(Dispatchers.IO) { PhoneLookup.analyze(raw, region) }
            uiState = PhoneUiState(
                result = result.getOrNull(),
                error = result.exceptionOrNull()?.let {
                    PhoneError.Failed(it.message ?: "?")
                },
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

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(stringResource(R.string.phone_region_title)) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(REGIONS) { code ->
                        ListItem(
                            headlineContent = { Text("${regionName(code)} ($code)") },
                            modifier = Modifier.clickable {
                                region = code
                                showPicker = false
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.phone_title)) }) },
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
                    label = { Text(stringResource(R.string.phone_hint)) },
                    placeholder = { Text(stringResource(R.string.phone_placeholder)) },
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
                        text = stringResource(
                            R.string.phone_default_region,
                            regionName(region),
                            region,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { showPicker = true }) {
                        Text(stringResource(R.string.phone_change))
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                    }
                }
            }
            item {
                Button(
                    onClick = { vm.analyze(input, region) },
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.phone_analyze)) }
            }
            if (state.loading) {
                item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            }
            state.error?.let { error ->
                item { ErrorCard(error.text()) }
            }
            state.result?.let { info ->
                item { PhoneResultCard(info) }
            }
            item {
                Text(
                    text = stringResource(R.string.phone_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PhoneResultCard(info: PhoneInfo) {
    SectionCard(title = stringResource(R.string.phone_result)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val valid = info.valid
            Icon(
                if (valid) Icons.Filled.CheckCircle else Icons.Filled.Error,
                contentDescription = null,
                tint = if (valid) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
            )
            Text(
                text = " " + stringResource(
                    if (valid) R.string.phone_valid else R.string.phone_invalid,
                ),
                style = MaterialTheme.typography.titleMedium,
                color = if (valid) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
            )
        }
        InfoRow(stringResource(R.string.phone_type), phoneTypeLabel(info.typeKey))
        InfoRow(stringResource(R.string.phone_carrier), info.carrier)
        InfoRow(stringResource(R.string.phone_location), info.location)
        InfoRow(stringResource(R.string.phone_region), info.regionCode)
        if (info.timezones.isNotEmpty()) {
            InfoRow(
                stringResource(R.string.phone_timezone),
                info.timezones.joinToString(", "),
            )
        }
        InfoRow("E.164", info.e164, copyable = true)
        InfoRow(stringResource(R.string.phone_intl), info.international, copyable = true)
        InfoRow(stringResource(R.string.phone_national), info.national, copyable = true)
        InfoRow(stringResource(R.string.phone_country_code), "+${info.countryCode}")
        InfoRow(
            stringResource(R.string.phone_national_number),
            info.nationalNumber.toString(),
        )
    }
}
