package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.communication.ExcavatorCommunicator
import com.example.model.ConnectionStatus
import com.example.model.OperationMode
import com.example.model.RobotState
import com.example.model.RobotTelemetry
import com.example.model.TransportProtocol
import com.example.voice.VoiceCommandManager
import com.example.voice.VoiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ExcavatorViewModel(application: Application) : AndroidViewModel(application) {

    val communicator = ExcavatorCommunicator(application.applicationContext)

    private val _robotState = MutableStateFlow(RobotState())
    val robotState: StateFlow<RobotState> = _robotState.asStateFlow()

    private val _operationMode = MutableStateFlow(OperationMode.MANUAL)
    val operationMode: StateFlow<OperationMode> = _operationMode.asStateFlow()

    private val _lastVoiceFeedback = MutableStateFlow<String?>(null)
    val lastVoiceFeedback: StateFlow<String?> = _lastVoiceFeedback.asStateFlow()

    val connectionStatus: StateFlow<ConnectionStatus> = communicator.connectionStatus
    val telemetry: StateFlow<RobotTelemetry> = communicator.telemetry
    val currentProtocol: StateFlow<TransportProtocol> = communicator.currentProtocol
    val wifiBaseUrl: StateFlow<String> = communicator.wifiBaseUrl
    val bleDeviceName: StateFlow<String> = communicator.bleDeviceName

    val voiceManager = VoiceCommandManager(application.applicationContext) { payload, desc ->
        _lastVoiceFeedback.value = "$desc ($payload)"
        if (payload.startsWith("RV:0")) {
            _robotState.value = _robotState.value.copy(
                driveDir = RobotState.DRIVE_STOP,
                baseDir = RobotState.BASE_STOP,
                boomDir = RobotState.BOOM_STOP
            )
        } else if (payload.startsWith("RV:")) {
            val dir = payload.removePrefix("RV:").toIntOrNull() ?: 0
            _robotState.value = _robotState.value.copy(driveDir = dir)
        }
        communicator.sendPayload(payload)
    }

    val voiceState: StateFlow<VoiceState> = voiceManager.voiceState
    val audioRmsDb: StateFlow<Float> = voiceManager.audioRmsDb

    fun setOperationMode(mode: OperationMode) {
        if (_operationMode.value == OperationMode.VOICE && mode != OperationMode.VOICE) {
            voiceManager.stopListening()
        }
        _operationMode.value = mode
        if (mode == OperationMode.VOICE) {
            voiceManager.startListening()
        }
    }

    fun setProtocol(protocol: TransportProtocol) {
        communicator.setProtocol(protocol)
    }

    fun connect() {
        communicator.connect()
    }

    fun disconnect() {
        communicator.disconnect()
    }

    fun updateWifiBaseUrl(url: String) {
        communicator.updateWifiBaseUrl(url)
    }

    fun checkAutoModeOverride(): Boolean {
        if (_operationMode.value == OperationMode.AUTO) {
            _operationMode.value = OperationMode.MANUAL
            emergencyStop()
            return true
        }
        return false
    }

    fun setDriveDirection(dir: Int) {
        if (checkAutoModeOverride()) return
        val current = _robotState.value
        if (current.driveDir != dir) {
            val updated = current.copy(driveDir = dir)
            _robotState.value = updated
            communicator.sendPayload(updated.toPayload())
        }
    }

    fun setBaseDirection(dir: Int) {
        if (checkAutoModeOverride()) return
        val current = _robotState.value
        if (current.baseDir != dir) {
            val updated = current.copy(baseDir = dir)
            _robotState.value = updated
            communicator.sendPayload(updated.toPayload())
        }
    }

    fun setBoomDirection(dir: Int) {
        if (checkAutoModeOverride()) return
        val current = _robotState.value
        if (current.boomDir != dir) {
            val updated = current.copy(boomDir = dir)
            _robotState.value = updated
            communicator.sendPayload(updated.toPayload())
        }
    }

    fun setStickAngle(angle: Int) {
        if (checkAutoModeOverride()) return
        val clamped = angle.coerceIn(0, 180)
        val current = _robotState.value
        if (current.stickAngle != clamped) {
            val updated = current.copy(stickAngle = clamped)
            _robotState.value = updated
            communicator.sendPayload(updated.toPayload())
        }
    }

    fun setBucketAngle(angle: Int) {
        if (checkAutoModeOverride()) return
        val clamped = angle.coerceIn(0, 180)
        val current = _robotState.value
        if (current.bucketAngle != clamped) {
            val updated = current.copy(bucketAngle = clamped)
            _robotState.value = updated
            communicator.sendPayload(updated.toPayload())
        }
    }

    fun triggerAutoRun() {
        communicator.sendPayload(RobotState.PAYLOAD_AUTO_RUN)
    }

    fun triggerConfirmPosition() {
        communicator.sendPayload(RobotState.PAYLOAD_CONFIRM_POS)
    }

    fun emergencyStop() {
        val updated = _robotState.value.copy(
            driveDir = RobotState.DRIVE_STOP,
            baseDir = RobotState.BASE_STOP,
            boomDir = RobotState.BOOM_STOP
        )
        _robotState.value = updated
        communicator.sendPayload(RobotState.PAYLOAD_SAFETY_STOP)
        communicator.sendPayload(updated.toPayload())
    }

    fun toggleVoiceListening() {
        when (voiceState.value) {
            is VoiceState.Listening -> voiceManager.stopListening()
            else -> voiceManager.startListening()
        }
    }

    fun processVoiceCommand(commandText: String) {
        voiceManager.processSpeechInput(commandText)
    }

    override fun onCleared() {
        super.onCleared()
        emergencyStop()
        voiceManager.destroy()
        communicator.release()
    }
}
