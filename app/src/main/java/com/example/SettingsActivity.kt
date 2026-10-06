package com.example

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ui.theme.BleBlue
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.IndustrialAmber
import com.example.ui.theme.IndustrialYellow
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WifiOrange
import com.example.util.AppSettings
import com.example.util.HapticManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ScannedBleDevice(
    val name: String,
    val address: String,
    val rssi: Int
)

class SettingsActivity : ComponentActivity() {

    private lateinit var appSettings: AppSettings
    private lateinit var hapticManager: HapticManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        appSettings = AppSettings.getInstance(this)
        hapticManager = HapticManager.getInstance(this)

        setContent {
            MyApplicationTheme {
                SettingsScreen(
                    appSettings = appSettings,
                    hapticManager = hapticManager,
                    onBackClicked = { finish() }
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    appSettings: AppSettings,
    hapticManager: HapticManager,
    onBackClicked: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val hapticsEnabled by appSettings.hapticsEnabled.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        containerColor = DarkBackground,
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                color = DarkBackground,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                hapticManager.vibrateClick()
                                onBackClicked()
                            },
                            modifier = Modifier.testTag("settings_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = IndustrialYellow
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = IndustrialYellow,
                            modifier = Modifier.size(20.dp)
                        )

                        Text(
                            text = "EXCAVATOR SETTINGS",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = TextPrimary
                            )
                        )
                    }

                    // Navigation Tabs [Wi-Fi] [BLE Management] [Haptics & Control]
                    TabRow(
                        selectedTabIndex = selectedTab,
                        modifier = Modifier.width(460.dp),
                        containerColor = DarkBackground,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = when (selectedTab) {
                                    0 -> WifiOrange
                                    1 -> BleBlue
                                    else -> IndustrialYellow
                                }
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = {
                                hapticManager.vibrateClick()
                                selectedTab = 0
                            },
                            text = { Text("Wi-Fi Network", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = {
                                hapticManager.vibrateClick()
                                selectedTab = 1
                            },
                            text = { Text("BLE Devices", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = {
                                hapticManager.vibrateClick()
                                selectedTab = 2
                            },
                            text = { Text("Haptics & Audio", fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
                .padding(12.dp)
        ) {
            when (selectedTab) {
                0 -> WifiSettingsTab(appSettings = appSettings, hapticManager = hapticManager)
                1 -> BleManagementTab(appSettings = appSettings, hapticManager = hapticManager)
                2 -> HapticsSettingsTab(
                    hapticsEnabled = hapticsEnabled,
                    onToggleHaptics = {
                        appSettings.setHapticsEnabled(it)
                        if (it) hapticManager.vibrateHeavy()
                    },
                    hapticManager = hapticManager
                )
            }
        }
    }
}

// ----------------- TAB 1: WI-FI SETTINGS -----------------

@Composable
private fun WifiSettingsTab(
    appSettings: AppSettings,
    hapticManager: HapticManager
) {
    val initialSsid by appSettings.wifiSsid.collectAsState()
    val initialPass by appSettings.wifiPassword.collectAsState()
    val initialBaseUrl by appSettings.wifiBaseUrl.collectAsState()

    var ssid by remember(initialSsid) { mutableStateOf(initialSsid) }
    var password by remember(initialPass) { mutableStateOf(initialPass) }
    var baseUrl by remember(initialBaseUrl) { mutableStateOf(initialBaseUrl) }
    var passwordVisible by remember { mutableStateOf(false) }
    var savedConfirmation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left Column: Credentials Form
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = WifiOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "WI-FI AP & CREDENTIALS",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = WifiOrange,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    // SSID Input
                    OutlinedTextField(
                        value = ssid,
                        onValueChange = { ssid = it },
                        label = { Text("Network SSID") },
                        placeholder = { Text("e.g. ESP32_EXCAVATOR_AP") },
                        singleLine = true,
                        colors = outlinedFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wifi_ssid_input")
                    )

                    // Password Input
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Network Password") },
                        placeholder = { Text("e.g. 12345678") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = TextSecondary
                                )
                            }
                        },
                        colors = outlinedFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wifi_password_input")
                    )

                    // Target Base URL / IP
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("ESP32 Target Host / IP") },
                        placeholder = { Text("http://192.168.4.1") },
                        singleLine = true,
                        colors = outlinedFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wifi_base_url_input")
                    )
                }

                // Save Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AnimatedVisibility(visible = savedConfirmation) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = StatusConnected,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Credentials Saved!",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = StatusConnected,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    if (!savedConfirmation) Spacer(modifier = Modifier.width(1.dp))

                    Button(
                        onClick = {
                            hapticManager.vibrateClick()
                            appSettings.saveWifiConfig(ssid, password, baseUrl)
                            savedConfirmation = true
                            scope.launch {
                                delay(2500)
                                savedConfirmation = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WifiOrange,
                            contentColor = DarkBackground
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("save_wifi_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SAVE WI-FI SETTINGS", fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // Right Column: Information & Connection Topology
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ESP32-S3 WI-FI TOPOLOGY",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )

                    Text(
                        text = "1. The ESP32-S3 starts a standalone SoftAP with default IP 192.168.4.1.\n" +
                                "2. Connect this Android tablet/phone to the excavator Wi-Fi network.\n" +
                                "3. Teleoperation packets are pushed asynchronously via HTTP POST /control using lightweight plain-text payloads.\n" +
                                "4. Sub-50ms latency ensures rapid throttle & hydraulic arm response.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "CURRENT ACTIVE ENDPOINT:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            )
                            Text(
                                text = "$baseUrl/control",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = WifiOrange
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ----------------- TAB 2: BLE DEVICE MANAGEMENT -----------------

@Composable
private fun BleManagementTab(
    appSettings: AppSettings,
    hapticManager: HapticManager
) {
    val selectedBleAddress by appSettings.selectedBleAddress.collectAsState()
    val selectedBleName by appSettings.selectedBleName.collectAsState()

    val scannedDevices = remember { mutableStateListOf<ScannedBleDevice>() }
    var isScanning by remember { mutableStateOf(false) }
    var scanStatusMsg by remember { mutableStateOf("Ready to scan for ESP32 excavators") }

    val context = androidx.compose.ui.platform.LocalContext.current
    val bluetoothManager = remember {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    }
    val bluetoothAdapter = bluetoothManager?.adapter

    // BLE Scan callback
    val scanCallback = remember {
        object : ScanCallback() {
            @SuppressLint("MissingPermission")
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.let { res ->
                    val dev = res.device
                    val name = dev.name ?: "Unknown BLE Device"
                    val address = dev.address ?: ""
                    if (address.isNotEmpty() && scannedDevices.none { it.address == address }) {
                        scannedDevices.add(ScannedBleDevice(name, address, res.rssi))
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                isScanning = false
                scanStatusMsg = "Scan error code $errorCode"
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isScanning) {
                try {
                    bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
                } catch (_: Exception) {}
            }
        }
    }

    val scope = rememberCoroutineScope()

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            scanStatusMsg = "Bluetooth is turned off"
            return
        }
        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            scanStatusMsg = "BLE Scanner not available"
            return
        }

        hapticManager.vibrateClick()
        scannedDevices.clear()
        isScanning = true
        scanStatusMsg = "Scanning nearby BLE peripherals…"

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner.startScan(null, settings, scanCallback)
        } catch (e: Exception) {
            isScanning = false
            scanStatusMsg = "Failed to start scan: ${e.message}"
            return
        }

        scope.launch {
            delay(12000)
            if (isScanning) {
                try {
                    scanner.stopScan(scanCallback)
                } catch (_: Exception) {}
                isScanning = false
                scanStatusMsg = "Scan finished. Found ${scannedDevices.size} devices."
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        hapticManager.vibrateClick()
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (_: Exception) {}
        isScanning = false
        scanStatusMsg = "Scan stopped."
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left Column: Selected Device & Scan Controls
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = BleBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "BLE EXCAVATOR PAIRING",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = BleBlue,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    // Currently selected/paired device badge
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .border(
                                1.dp,
                                if (selectedBleAddress.isNotEmpty()) BleBlue else DarkSurfaceBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "CURRENT TARGET DEVICE:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            )
                            Text(
                                text = selectedBleName.ifEmpty { "ESP32-S3 Excavator" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedBleAddress.isNotEmpty()) BleBlue else TextPrimary
                                )
                            )
                            Text(
                                text = if (selectedBleAddress.isNotEmpty()) "MAC: $selectedBleAddress" else "UUID: 4FAFC201-1FB5-459E-8FCC-C5C9C331914B",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    Text(
                        text = scanStatusMsg,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary
                        )
                    )
                }

                // Scan / Stop Scanning Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { if (isScanning) stopScan() else startScan() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isScanning) IndustrialAmber else BleBlue,
                            contentColor = DarkBackground
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ble_scan_toggle_button")
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = DarkBackground,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("STOP SCANNING", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.BluetoothSearching,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SCAN BLE DEVICES", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Right Column: Discovered Devices List
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "DISCOVERED DEVICES (${scannedDevices.size})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (scannedDevices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isScanning) "Searching for ESP32 BLE beacons…" else "No devices discovered yet.\nTap 'SCAN BLE DEVICES' to begin.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(scannedDevices) { device ->
                            val isSelected = selectedBleAddress == device.address
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) BleBlue.copy(alpha = 0.2f) else DarkSurfaceVariant)
                                    .border(
                                        1.dp,
                                        if (isSelected) BleBlue else DarkSurfaceBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        hapticManager.vibrateClick()
                                        appSettings.saveSelectedBleDevice(device.name, device.address)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = device.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) BleBlue else TextPrimary
                                        )
                                    )
                                    Text(
                                        text = "${device.address} • RSSI: ${device.rssi} dBm",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = TextMuted,
                                            fontSize = 9.sp
                                        )
                                    )
                                }

                                if (isSelected) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = BleBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "PAIRED",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                color = BleBlue
                                            )
                                        )
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            hapticManager.vibrateClick()
                                            appSettings.saveSelectedBleDevice(device.name, device.address)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = DarkSurfaceContainer,
                                            contentColor = TextPrimary
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("PAIR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------- TAB 3: HAPTICS & AUDIO FEEDBACK -----------------

@Composable
private fun HapticsSettingsTab(
    hapticsEnabled: Boolean,
    onToggleHaptics: (Boolean) -> Unit,
    hapticManager: HapticManager
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left Column: Haptic Settings Switch
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = null,
                                tint = IndustrialYellow,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "HAPTIC TOUCH FEEDBACK",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = IndustrialYellow,
                                    letterSpacing = 1.sp
                                )
                            )
                        }

                        Switch(
                            checked = hapticsEnabled,
                            onCheckedChange = onToggleHaptics,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = IndustrialYellow,
                                checkedTrackColor = IndustrialYellow.copy(alpha = 0.5f),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkBackground
                            ),
                            modifier = Modifier.testTag("haptics_toggle_switch")
                        )
                    }

                    Text(
                        text = if (hapticsEnabled) {
                            "Status: ENABLED. Tactile feedback is active for all touch controls."
                        } else {
                            "Status: DISABLED. No vibration will be triggered on touch inputs."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (hapticsEnabled) StatusConnected else TextMuted
                        )
                    )

                    Text(
                        text = "Controls mapped to VibrationEffect system:\n" +
                                "• 4-Way Thumb D-Pad (Forward, Reverse, Left, Right, Brake)\n" +
                                "• Arm & Bucket Wiper Rockers (Base Rotation & Boom Elevation)\n" +
                                "• MG996R Stick & Bucket Servo Seek-bar increments\n" +
                                "• Voice Mode Microphone toggle button\n" +
                                "• Auto Sequence blocks (Start Auto Run & Confirm Position)\n" +
                                "• Header Emergency Stop (E-STOP) trigger",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    )
                }

                // Interactive Test Buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "TEST HAPTIC EFFECTS:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { hapticManager.vibrateClick() },
                            enabled = hapticsEnabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkSurfaceVariant,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_click_haptic_button")
                        ) {
                            Text("Test Click (25ms)")
                        }

                        Button(
                            onClick = { hapticManager.vibrateTick() },
                            enabled = hapticsEnabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkSurfaceVariant,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_tick_haptic_button")
                        ) {
                            Text("Test Slider Tick (10ms)")
                        }

                        Button(
                            onClick = { hapticManager.vibrateHeavy() },
                            enabled = hapticsEnabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IndustrialAmber,
                                contentColor = DarkBackground
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_heavy_haptic_button")
                        ) {
                            Text("Test Heavy Rumble", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Right Column: System Diagnostics
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "HARDWARE HAPTIC ENGINE",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )

                    Text(
                        text = "The application implements the modern Android 12+ (API 31+) VibratorManager system with automatic backward-compatibility to Android 8.0+ VibrationEffect.\n\n" +
                                "Standardized predefined effects:\n" +
                                "• VibrationEffect.EFFECT_CLICK: Light crisp feedback\n" +
                                "• VibrationEffect.EFFECT_TICK: Ultra-short servo notch feedback\n" +
                                "• VibrationEffect.EFFECT_HEAVY_CLICK: High-amplitude safety feedback",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Compliant with Android Least-Privilege & Play Store standards.",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = IndustrialYellow,
    unfocusedBorderColor = DarkSurfaceBorder,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = IndustrialYellow,
    focusedContainerColor = DarkBackground,
    unfocusedContainerColor = DarkBackground,
    focusedLabelColor = IndustrialYellow,
    unfocusedLabelColor = TextSecondary
)
