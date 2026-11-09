package com.huimao.ghosttrack.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huimao.ghosttrack.R
import com.huimao.ghosttrack.ui.about.AboutScreen
import com.huimao.ghosttrack.ui.home.HomeScreen
import com.huimao.ghosttrack.ui.ip.IpScreen
import com.huimao.ghosttrack.ui.phone.PhoneScreen
import com.huimao.ghosttrack.ui.username.UsernameScreen

enum class Dest(val titleRes: Int, val icon: ImageVector) {
    Home(R.string.nav_home, Icons.Filled.Home),
    Ip(R.string.nav_ip, Icons.Filled.LocationOn),
    Phone(R.string.nav_phone, Icons.Filled.Smartphone),
    Username(R.string.nav_username, Icons.Filled.PersonSearch),
    About(R.string.nav_about, Icons.Filled.Info),
}

@Composable
fun GhostTrackApp(
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
) {
    var dest by rememberSaveable { mutableStateOf(Dest.Home) }
    Scaffold(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
            ) {
                Dest.entries.forEach { d ->
                    val title = stringResource(d.titleRes)
                    val selected = dest == d
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.16f else 1f,
                        animationSpec = spring(dampingRatio = 0.62f, stiffness = 520f),
                        label = "navIconScale",
                    )
                    NavigationBarItem(
                        selected = selected,
                        onClick = { dest = d },
                        icon = {
                            Icon(
                                d.icon,
                                contentDescription = title,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                },
                            )
                        },
                        label = { Text(title, maxLines = 1) },
                    )
                }
            }
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (dest) {
            Dest.Home -> HomeScreen(
                onOpenIp = { dest = Dest.Ip },
                onOpenPhone = { dest = Dest.Phone },
                onOpenUsername = { dest = Dest.Username },
                modifier = modifier,
            )
            Dest.Ip -> IpScreen(modifier = modifier)
            Dest.Phone -> PhoneScreen(modifier = modifier)
            Dest.Username -> UsernameScreen(modifier = modifier)
            Dest.About -> AboutScreen(
                dynamicColor = dynamicColor,
                onDynamicColorChange = onDynamicColorChange,
                modifier = modifier,
            )
        }
    }
}
