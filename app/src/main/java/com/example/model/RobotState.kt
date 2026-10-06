package com.example.model

enum class OperationMode {
    MANUAL,
    VOICE,
    AUTO
}

enum class TransportProtocol {
    WIFI,
    BLE
}

sealed class ConnectionStatus {
    object Disconnected : ConnectionStatus()
    object Connecting : ConnectionStatus()
    object Connected : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}

data class RobotTelemetry(
    val lastPayload: String = "BS:0,BM:0,ST:90,BK:90,RV:0",
    val packetCount: Long = 0,
    val lastLatencyMs: Long = 0,
    val lastTimestamp: Long = 0
)

data class RobotState(
    val baseDir: Int = 0,      // 0 = stop, 1 = left, 2 = right
    val boomDir: Int = 0,      // 0 = stop, 1 = up, 2 = down
    val stickAngle: Int = 90,  // 0 - 180 deg
    val bucketAngle: Int = 90, // 0 - 180 deg
    val driveDir: Int = 0      // 0 = stop, 1 = forward, 2 = reverse, 3 = left, 4 = right
) {
    fun toPayload(): String {
        return "BS:$baseDir,BM:$boomDir,ST:$stickAngle,BK:$bucketAngle,RV:$driveDir"
    }

    companion object {
        const val BASE_STOP = 0
        const val BASE_LEFT = 1
        const val BASE_RIGHT = 2

        const val BOOM_STOP = 0
        const val BOOM_UP = 1
        const val BOOM_DOWN = 2

        const val DRIVE_STOP = 0
        const val DRIVE_FORWARD = 1
        const val DRIVE_REVERSE = 2
        const val DRIVE_LEFT = 3
        const val DRIVE_RIGHT = 4

        const val PAYLOAD_AUTO_RUN = "AUTO:1"
        const val PAYLOAD_CONFIRM_POS = "CONFIRM:1"
        const val PAYLOAD_SAFETY_STOP = "RV:0,BS:0,BM:0"
    }
}
