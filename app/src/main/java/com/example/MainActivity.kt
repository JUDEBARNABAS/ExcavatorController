package com.example

import android.Manifest
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.model.OperationMode
import com.example.ui.ExcavatorViewModel
import com.example.ui.components.ArmController
import com.example.ui.components.AutoSequenceOverlay
import com.example.ui.components.ChassisController
import com.example.ui.components.HeaderBar
import com.example.ui.components.TelemetryBar
import com.example.ui.components.VoiceModeOverlay
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import com.example.SettingsActivity
import com.example.util.AppSettings
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: ExcavatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Enforce landscape mode programmatically
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // 2. Hide Android action bar and status bar to maximize viewable screen area
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Keep screen awake during heavy-duty robot teleoperation
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            MyApplicationTheme {
                ExcavatorApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ExcavatorApp(viewModel: ExcavatorViewModel) {
    val context = LocalContext.current
    val appSettings = remember { AppSettings.getInstance(context) }
    val savedWifiUrl by appSettings.wifiBaseUrl.collectAsState()

    LaunchedEffect(savedWifiUrl) {
        viewModel.updateWifiBaseUrl(savedWifiUrl)
    }

    // Request required permissions for Android 12+ (BLE scan/connect, audio recording, location)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled
    }

    // System Speech Recognizer Intent Launcher for guaranteed speech input fallback
    val speechIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val topText = matches?.firstOrNull() ?: ""
            if (topText.isNotEmpty()) {
                viewModel.processVoiceCommand(topText)
            }
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    // Collect reactive state flows from ViewModel
    val robotState by viewModel.robotState.collectAsState()
    val operationMode by viewModel.operationMode.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val currentProtocol by viewModel.currentProtocol.collectAsState()
    val wifiBaseUrl by viewModel.wifiBaseUrl.collectAsState()
    val bleDeviceName by viewModel.bleDeviceName.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val audioRmsDb by viewModel.audioRmsDb.collectAsState()
    val lastVoiceFeedback by viewModel.lastVoiceFeedback.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("excavator_main_screen"),
        containerColor = DarkBackground,
        topBar = {
            HeaderBar(
                protocol = currentProtocol,
                connectionStatus = connectionStatus,
                wifiUrl = wifiBaseUrl,
                bleDeviceName = bleDeviceName,
                currentMode = operationMode,
                onProtocolSelected = { viewModel.setProtocol(it) },
                onModeSelected = { viewModel.setOperationMode(it) },
                onConnectClicked = { viewModel.connect() },
                onDisconnectClicked = { viewModel.disconnect() },
                onSettingsClicked = {
                    val intent = Intent(context, SettingsActivity::class.java)
                    context.startActivity(intent)
                },
                onEmergencyStop = { viewModel.emergencyStop() }
            )
        },
        bottomBar = {
            TelemetryBar(
                telemetry = telemetry,
                currentProtocol = currentProtocol
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            // Symmetrical, zero-scroll landscape UI split into distinct spatial zones:
            // Left Wing (Chassis Controller) + Right Wing (Arm & Bucket Controller)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // LEFT WING: Rover Mobility (4-Way Thumb D-Pad)
                ChassisController(
                    currentDriveDir = robotState.driveDir,
                    onDirectionChange = { viewModel.setDriveDirection(it) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                // RIGHT WING: Arm & Bucket Mechanics (Wipers & MG996R Servos)
                ArmController(
                    baseDir = robotState.baseDir,
                    boomDir = robotState.boomDir,
                    stickAngle = robotState.stickAngle,
                    bucketAngle = robotState.bucketAngle,
                    onBaseDirChange = { viewModel.setBaseDirection(it) },
                    onBoomDirChange = { viewModel.setBoomDirection(it) },
                    onStickAngleChange = { viewModel.setStickAngle(it) },
                    onBucketAngleChange = { viewModel.setBucketAngle(it) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            // AUTO SEQUENCE OVERLAY: Active when operationMode == AUTO
            AnimatedVisibility(
                visible = operationMode == OperationMode.AUTO,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                AutoSequenceOverlay(
                    onStartAutoRun = { viewModel.triggerAutoRun() },
                    onConfirmPosition = { viewModel.triggerConfirmPosition() },
                    onManualOverride = {
                        viewModel.checkAutoModeOverride()
                    }
                )
            }

            // VOICE MODE OVERLAY: Active when operationMode == VOICE
            AnimatedVisibility(
                visible = operationMode == OperationMode.VOICE,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                VoiceModeOverlay(
                    voiceState = voiceState,
                    audioRmsDb = audioRmsDb,
                    lastVoiceFeedback = lastVoiceFeedback,
                    onToggleListening = {
                        val hasAudioPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!hasAudioPerm) {
                            permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                        } else {
                            viewModel.toggleVoiceListening()
                        }
                    },
                    onPhraseSimulate = { commandText ->
                        viewModel.processVoiceCommand(commandText)
                    },
                    onLaunchSpeechIntent = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak excavator command (e.g. forward, stop, dig)...")
                        }
                        try {
                            speechIntentLauncher.launch(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "System speech dialog unavailable: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}
