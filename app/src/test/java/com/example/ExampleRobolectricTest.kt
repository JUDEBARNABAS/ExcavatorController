package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.RobotState
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Excavator Controller", appName)
    }

    @Test
    fun `default robot state payload format matches specification`() {
        val state = RobotState()
        assertEquals("BS:0,BM:0,ST:90,BK:90,RV:0", state.toPayload())
    }

    @Test
    fun `active drive forward and arm elevation payload generation`() {
        val state = RobotState(
            baseDir = RobotState.BASE_LEFT,
            boomDir = RobotState.BOOM_UP,
            stickAngle = 120,
            bucketAngle = 45,
            driveDir = RobotState.DRIVE_FORWARD
        )
        assertEquals("BS:1,BM:1,ST:120,BK:45,RV:1", state.toPayload())
    }

    @Test
    fun `auto run and confirm position payloads match specification`() {
        assertEquals("AUTO:1", RobotState.PAYLOAD_AUTO_RUN)
        assertEquals("CONFIRM:1", RobotState.PAYLOAD_CONFIRM_POS)
        assertEquals("RV:0,BS:0,BM:0", RobotState.PAYLOAD_SAFETY_STOP)
    }

    @Test
    fun `app settings save and load wifi configuration and haptics preference`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = com.example.util.AppSettings.getInstance(context)

        // Verify default haptics is true
        org.junit.Assert.assertTrue(settings.hapticsEnabled.value)

        // Toggle haptics
        settings.setHapticsEnabled(false)
        org.junit.Assert.assertFalse(settings.hapticsEnabled.value)

        // Save Wi-Fi config
        settings.saveWifiConfig("EXCAVATOR_TEST_NET", "pass12345", "http://192.168.4.20")
        assertEquals("EXCAVATOR_TEST_NET", settings.wifiSsid.value)
        assertEquals("pass12345", settings.wifiPassword.value)
        assertEquals("http://192.168.4.20", settings.wifiBaseUrl.value)

        // Save BLE device
        settings.saveSelectedBleDevice("ESP32_EXCAVATOR_01", "AA:BB:CC:DD:EE:FF")
        assertEquals("ESP32_EXCAVATOR_01", settings.selectedBleName.value)
        assertEquals("AA:BB:CC:DD:EE:FF", settings.selectedBleAddress.value)
    }

    @Test
    fun `voice command manager parses structural phrases to correct payloads`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var lastPayload = ""
        var lastDesc = ""

        val voiceManager = com.example.voice.VoiceCommandManager(context) { payload, desc ->
            lastPayload = payload
            lastDesc = desc
        }

        voiceManager.processSpeechInput("move forward")
        assertEquals("RV:1", lastPayload)

        voiceManager.processSpeechInput("reverse")
        assertEquals("RV:2", lastPayload)

        voiceManager.processSpeechInput("turn left")
        assertEquals("RV:3", lastPayload)

        voiceManager.processSpeechInput("turn right")
        assertEquals("RV:4", lastPayload)

        voiceManager.processSpeechInput("stop")
        assertEquals("RV:0,BS:0,BM:0", lastPayload)

        voiceManager.processSpeechInput("dig")
        assertEquals("AUTO:1", lastPayload)

        voiceManager.processSpeechInput("confirm")
        assertEquals("CONFIRM:1", lastPayload)
    }
}
