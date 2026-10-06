package com.example.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _hapticsEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_HAPTICS_ENABLED, true)
    )
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    private val _wifiSsid = MutableStateFlow(
        prefs.getString(KEY_WIFI_SSID, "ESP32_EXCAVATOR_AP") ?: "ESP32_EXCAVATOR_AP"
    )
    val wifiSsid: StateFlow<String> = _wifiSsid.asStateFlow()

    private val _wifiPassword = MutableStateFlow(
        prefs.getString(KEY_WIFI_PASSWORD, "12345678") ?: "12345678"
    )
    val wifiPassword: StateFlow<String> = _wifiPassword.asStateFlow()

    private val _wifiBaseUrl = MutableStateFlow(
        prefs.getString(KEY_WIFI_BASE_URL, "http://192.168.4.1") ?: "http://192.168.4.1"
    )
    val wifiBaseUrl: StateFlow<String> = _wifiBaseUrl.asStateFlow()

    private val _selectedBleAddress = MutableStateFlow(
        prefs.getString(KEY_BLE_ADDRESS, "") ?: ""
    )
    val selectedBleAddress: StateFlow<String> = _selectedBleAddress.asStateFlow()

    private val _selectedBleName = MutableStateFlow(
        prefs.getString(KEY_BLE_NAME, "ESP32-S3") ?: "ESP32-S3"
    )
    val selectedBleName: StateFlow<String> = _selectedBleName.asStateFlow()

    fun setHapticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTICS_ENABLED, enabled).apply()
        _hapticsEnabled.value = enabled
    }

    fun saveWifiConfig(ssid: String, password: String, baseUrl: String) {
        var cleanUrl = baseUrl.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "http://$cleanUrl"
        }
        if (cleanUrl.endsWith("/")) {
            cleanUrl = cleanUrl.substring(0, cleanUrl.length - 1)
        }

        prefs.edit()
            .putString(KEY_WIFI_SSID, ssid.trim())
            .putString(KEY_WIFI_PASSWORD, password)
            .putString(KEY_WIFI_BASE_URL, cleanUrl)
            .apply()

        _wifiSsid.value = ssid.trim()
        _wifiPassword.value = password
        _wifiBaseUrl.value = cleanUrl
    }

    fun saveSelectedBleDevice(name: String, address: String) {
        prefs.edit()
            .putString(KEY_BLE_NAME, name)
            .putString(KEY_BLE_ADDRESS, address)
            .apply()

        _selectedBleName.value = name
        _selectedBleAddress.value = address
    }

    companion object {
        private const val PREFS_NAME = "excavator_app_prefs"
        private const val KEY_HAPTICS_ENABLED = "pref_haptics_enabled"
        private const val KEY_WIFI_SSID = "pref_wifi_ssid"
        private const val KEY_WIFI_PASSWORD = "pref_wifi_password"
        private const val KEY_WIFI_BASE_URL = "pref_wifi_base_url"
        private const val KEY_BLE_NAME = "pref_ble_name"
        private const val KEY_BLE_ADDRESS = "pref_ble_address"

        @Volatile
        private var instance: AppSettings? = null

        fun getInstance(context: Context): AppSettings {
            return instance ?: synchronized(this) {
                instance ?: AppSettings(context.applicationContext).also { instance = it }
            }
        }
    }
}
