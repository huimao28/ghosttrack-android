package com.huimao.ghosttrack.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.huimao.ghosttrack.ui.home.HomeScreen
import com.huimao.ghosttrack.ui.ip.IpScreen
import com.huimao.ghosttrack.ui.phone.PhoneScreen
import com.huimao.ghosttrack.ui.username.UsernameScreen

enum class Dest(val title: String, val icon: ImageVector) {
    Home("主页", Icons.Filled.Home),
    Ip("IP 追踪", Icons.Filled.LocationOn),
    Phone("号码解析", Icons.Filled.Smartphone),
    Username("用户名", Icons.Filled.PersonSearch),
}

@Composable
fun GhostTrackApp() {
    var dest by remember { mutableStateOf(Dest.Home) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                Dest.entries.forEach { d ->
                    NavigationBarItem(
                        selected = dest == d,
                        onClick = { dest = d },
                        icon = { Icon(d.icon, contentDescription = d.title) },
                        label = { Text(d.title) },
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
        }
    }
}
