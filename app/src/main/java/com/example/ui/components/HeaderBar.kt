package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionStatus
import com.example.model.OperationMode
import com.example.model.TransportProtocol
import com.example.ui.theme.BleBlue
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.IndustrialAmber
import com.example.ui.theme.IndustrialRed
import com.example.ui.theme.IndustrialYellow
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusConnecting
import com.example.ui.theme.StatusDisconnected
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WifiOrange

import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import com.example.util.HapticManager

@Composable
fun HeaderBar(
    protocol: TransportProtocol,
    connectionStatus: ConnectionStatus,
    wifiUrl: String,
    bleDeviceName: String,
    currentMode: OperationMode,
    onProtocolSelected: (TransportProtocol) -> Unit,
    onModeSelected: (OperationMode) -> Unit,
    onConnectClicked: () -> Unit,
    onDisconnectClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onEmergencyStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticManager = remember { HapticManager.getInstance(context) }
    val statusColor by animateColorAsState(
        targetValue = when (connectionStatus) {
            is ConnectionStatus.Connected -> StatusConnected
            is ConnectionStatus.Connecting -> StatusConnecting
            is ConnectionStatus.Disconnected -> StatusDisconnected
            is ConnectionStatus.Error -> StatusDisconnected
        },
        label = "status_color"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("top_header_bar"),
        color = DarkBackground,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // LEFT SECTION: Connection Protocol Selector & Status Dot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Protocol Toggle Buttons (Wi-Fi / BLE)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
                        .padding(2.dp)
                ) {
                    ProtocolTab(
                        label = "Wi-Fi",
                        icon = Icons.Default.Wifi,
                        activeColor = WifiOrange,
                        selected = protocol == TransportProtocol.WIFI,
                        onClick = { onProtocolSelected(TransportProtocol.WIFI) },
                        tag = "tab_wifi"
                    )
                    ProtocolTab(
                        label = "BLE",
                        icon = Icons.Default.Bluetooth,
                        activeColor = BleBlue,
                        selected = protocol == TransportProtocol.BLE,
                        onClick = { onProtocolSelected(TransportProtocol.BLE) },
                        tag = "tab_ble"
                    )
                }

                // Status Dot & Indicator
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(6.dp))
                        .clickable { onSettingsClicked() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            .testTag("connection_status_dot")
                    )

                    Column {
                        Text(
                            text = when (connectionStatus) {
                                is ConnectionStatus.Connected -> "CONNECTED"
                                is ConnectionStatus.Connecting -> "CONNECTING"
                                is ConnectionStatus.Disconnected -> "OFFLINE"
                                is ConnectionStatus.Error -> "ERR"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = if (protocol == TransportProtocol.WIFI) {
                                wifiUrl.removePrefix("http://")
                            } else {
                                bleDeviceName
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            maxLines = 1
                        )
                    }

                    IconButton(
                        onClick = {
                            hapticManager.vibrateClick()
                            onSettingsClicked()
                        },
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Connection Settings",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Connect / Disconnect Toggle Button
                val isConnected = connectionStatus is ConnectionStatus.Connected
                IconButton(
                    onClick = {
                        hapticManager.vibrateClick()
                        if (isConnected) onDisconnectClicked() else onConnectClicked()
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isConnected) StatusConnected.copy(alpha = 0.2f) else DarkSurfaceVariant)
                        .border(
                            1.dp,
                            if (isConnected) StatusConnected.copy(alpha = 0.5f) else DarkSurfaceBorder,
                            RoundedCornerShape(6.dp)
                        )
                        .testTag("connect_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = if (isConnected) "Disconnect" else "Connect",
                        tint = if (isConnected) StatusConnected else IndustrialYellow,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // CENTER SECTION: Segmented Buttons [MANUAL] [VOICE] [AUTO]
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(10.dp))
                    .padding(3.dp)
                    .testTag("mode_segmented_bar"),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OperationMode.values().forEach { mode ->
                    val isSelected = currentMode == mode
                    val modeColor = when (mode) {
                        OperationMode.MANUAL -> IndustrialYellow
                        OperationMode.VOICE -> BleBlue
                        OperationMode.AUTO -> IndustrialAmber
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                if (isSelected) modeColor else Color.Transparent
                            )
                            .clickable {
                                hapticManager.vibrateClick()
                                onModeSelected(mode)
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("mode_tab_${mode.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.name,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) DarkBackground else TextSecondary,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }
            }

            // RIGHT SECTION: Emergency Stop Button
            Button(
                onClick = {
                    hapticManager.vibrateHeavy()
                    onEmergencyStop()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = IndustrialRed,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("emergency_stop_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = TextPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "E-STOP",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun ProtocolTab(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    activeColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    val context = LocalContext.current
    val hapticManager = remember { HapticManager.getInstance(context) }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) activeColor.copy(alpha = 0.25f) else Color.Transparent)
            .clickable {
                hapticManager.vibrateClick()
                onClick()
            }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) activeColor else TextMuted,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) activeColor else TextSecondary
                )
            )
        }
    }
}
