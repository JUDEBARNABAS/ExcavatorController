package com.example.communication

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import com.example.model.ConnectionStatus
import com.example.model.RobotTelemetry
import com.example.model.TransportProtocol
import com.example.util.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class ExcavatorCommunicator(private val context: Context) {

    companion object {
        private const val TAG = "ExcavatorComm"
        val SERVICE_UUID: UUID = UUID.fromString("4FAFC201-1FB5-459E-8FCC-C5C9C331914B")
        val CHARACTERISTIC_UUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
        const val DEFAULT_WIFI_BASE_URL = "http://192.168.4.1"
    }

    private val appSettings = AppSettings.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _telemetry = MutableStateFlow(RobotTelemetry())
    val telemetry: StateFlow<RobotTelemetry> = _telemetry.asStateFlow()

    private val _currentProtocol = MutableStateFlow(TransportProtocol.WIFI)
    val currentProtocol: StateFlow<TransportProtocol> = _currentProtocol.asStateFlow()

    private val _wifiBaseUrl = MutableStateFlow(appSettings.wifiBaseUrl.value)
    val wifiBaseUrl: StateFlow<String> = _wifiBaseUrl.asStateFlow()

    private val _bleDeviceName = MutableStateFlow(appSettings.selectedBleName.value)
    val bleDeviceName: StateFlow<String> = _bleDeviceName.asStateFlow()

    // OkHttpClient optimized for low-latency UDP-style control bursts
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(1500, TimeUnit.MILLISECONDS)
        .writeTimeout(1000, TimeUnit.MILLISECONDS)
        .readTimeout(1000, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(false)
        .build()

    // BLE instances
    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var isScanning = false

    private val packetCounter = AtomicLong(0)
    private val isTransmitting = AtomicBoolean(false)
    private var pendingPayload: String? = null

    fun setProtocol(protocol: TransportProtocol) {
        if (_currentProtocol.value != protocol) {
            disconnect()
            _currentProtocol.value = protocol
        }
    }

    fun updateWifiBaseUrl(url: String) {
        var sanitized = url.trim()
        if (!sanitized.startsWith("http://") && !sanitized.startsWith("https://")) {
            sanitized = "http://$sanitized"
        }
        if (sanitized.endsWith("/")) {
            sanitized = sanitized.substring(0, sanitized.length - 1)
        }
        _wifiBaseUrl.value = sanitized
    }

    fun connect() {
        when (_currentProtocol.value) {
            TransportProtocol.WIFI -> testWifiConnection()
            TransportProtocol.BLE -> startBleScanAndConnect()
        }
    }

    fun disconnect() {
        when (_currentProtocol.value) {
            TransportProtocol.WIFI -> {
                _connectionStatus.value = ConnectionStatus.Disconnected
            }
            TransportProtocol.BLE -> {
                disconnectBle()
            }
        }
    }

    fun sendPayload(payload: String) {
        pendingPayload = payload
        if (isTransmitting.compareAndSet(false, true)) {
            scope.launch {
                drainPayloadQueue()
            }
        }
    }

    private suspend fun drainPayloadQueue() {
        try {
            while (pendingPayload != null) {
                val next = pendingPayload
                pendingPayload = null
                if (next != null) {
                    dispatchPayload(next)
                }
                // Yield briefly to avoid bus starvation
                delay(15)
            }
        } finally {
            isTransmitting.set(false)
            // If another payload arrived while resetting, kick off loop again
            if (pendingPayload != null && isTransmitting.compareAndSet(false, true)) {
                drainPayloadQueue()
            }
        }
    }

    private fun dispatchPayload(payload: String) {
        val startTime = System.currentTimeMillis()
        when (_currentProtocol.value) {
            TransportProtocol.WIFI -> {
                sendWifiPayload(payload, startTime)
            }
            TransportProtocol.BLE -> {
                sendBlePayload(payload, startTime)
            }
        }
    }

    // --- Wi-Fi HTTP Transmission ---

    private fun testWifiConnection() {
        _connectionStatus.value = ConnectionStatus.Connecting
        val url = "${_wifiBaseUrl.value}/control"
        val requestBody = "PING".toRequestBody("text/plain; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.w(TAG, "Wi-Fi connection check failed: ${e.message}")
                _connectionStatus.value = ConnectionStatus.Error("Cannot reach ${_wifiBaseUrl.value}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.close()
                _connectionStatus.value = ConnectionStatus.Connected
            }
        })
    }

    private fun sendWifiPayload(payload: String, startTime: Long) {
        val url = "${_wifiBaseUrl.value}/control"
        val requestBody = payload.toRequestBody("text/plain; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to send Wi-Fi payload: ${e.message}")
                val latency = System.currentTimeMillis() - startTime
                _telemetry.value = _telemetry.value.copy(
                    lastPayload = payload,
                    lastLatencyMs = latency,
                    lastTimestamp = System.currentTimeMillis()
                )
            }

            override fun onResponse(call: Call, response: Response) {
                val latency = System.currentTimeMillis() - startTime
                response.close()
                val count = packetCounter.incrementAndGet()
                _connectionStatus.value = ConnectionStatus.Connected
                _telemetry.value = RobotTelemetry(
                    lastPayload = payload,
                    packetCount = count,
                    lastLatencyMs = latency,
                    lastTimestamp = System.currentTimeMillis()
                )
            }
        })
    }

    // --- BLE Transmission ---

    @SuppressLint("MissingPermission")
    private fun startBleScanAndConnect() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _connectionStatus.value = ConnectionStatus.Error("Bluetooth is disabled")
            return
        }

        _connectionStatus.value = ConnectionStatus.Connecting

        // Check if user has explicitly paired/selected a BLE device address in Settings
        val savedAddress = appSettings.selectedBleAddress.value
        if (savedAddress.isNotEmpty() && BluetoothAdapter.checkBluetoothAddress(savedAddress)) {
            try {
                val targetDevice = bluetoothAdapter.getRemoteDevice(savedAddress)
                connectToBleDevice(targetDevice)
                return
            } catch (e: Exception) {
                Log.w(TAG, "Direct connect to $savedAddress failed, scanning... ${e.message}")
            }
        }

        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            _connectionStatus.value = ConnectionStatus.Error("BLE scanner unavailable")
            return
        }

        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        isScanning = true
        scanner.startScan(listOf(scanFilter), settings, bleScanCallback)

        // Safety timeout for scanning: 10 seconds
        scope.launch {
            delay(10000)
            if (isScanning && _connectionStatus.value is ConnectionStatus.Connecting) {
                stopBleScan()
                _connectionStatus.value = ConnectionStatus.Error("Scan timed out (ESP32-S3 not found)")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopBleScan() {
        if (isScanning) {
            isScanning = false
            try {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(bleScanCallback)
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping scan: ${e.message}")
            }
        }
    }

    private val bleScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                stopBleScan()
                connectToBleDevice(device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
            _connectionStatus.value = ConnectionStatus.Error("BLE Scan failed (code: $errorCode)")
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToBleDevice(device: BluetoothDevice) {
        _bleDeviceName.value = device.name ?: device.address ?: "ESP32-S3"
        bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    @SuppressLint("MissingPermission")
    private fun disconnectBle() {
        stopBleScan()
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing gatt: ${e.message}")
        }
        bluetoothGatt = null
        writeCharacteristic = null
        _connectionStatus.value = ConnectionStatus.Disconnected
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "BLE Connected, discovering services...")
                    _connectionStatus.value = ConnectionStatus.Connecting
                    gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "BLE Disconnected")
                    writeCharacteristic = null
                    _connectionStatus.value = ConnectionStatus.Disconnected
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                val service = gatt.getService(SERVICE_UUID)
                if (service != null) {
                    val characteristic = service.getCharacteristic(CHARACTERISTIC_UUID)
                    if (characteristic != null) {
                        writeCharacteristic = characteristic
                        _connectionStatus.value = ConnectionStatus.Connected
                        Log.i(TAG, "Found target characteristic: $CHARACTERISTIC_UUID")
                    } else {
                        _connectionStatus.value = ConnectionStatus.Error("Characteristic not found")
                    }
                } else {
                    _connectionStatus.value = ConnectionStatus.Error("Service not found")
                }
            } else {
                _connectionStatus.value = ConnectionStatus.Error("Service discovery failed: $status")
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                packetCounter.incrementAndGet()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendBlePayload(payload: String, startTime: Long) {
        val gatt = bluetoothGatt
        val char = writeCharacteristic
        if (gatt == null || char == null) {
            val latency = System.currentTimeMillis() - startTime
            _telemetry.value = _telemetry.value.copy(
                lastPayload = payload,
                lastLatencyMs = latency,
                lastTimestamp = System.currentTimeMillis()
            )
            return
        }

        val bytes = payload.toByteArray(Charsets.UTF_8)
        val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val res = gatt.writeCharacteristic(char, bytes, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
            res == BluetoothGatt.GATT_SUCCESS
        } else {
            @Suppress("DEPRECATION")
            char.value = bytes
            @Suppress("DEPRECATION")
            char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(char)
        }

        val latency = System.currentTimeMillis() - startTime
        val count = packetCounter.incrementAndGet()
        _telemetry.value = RobotTelemetry(
            lastPayload = payload,
            packetCount = count,
            lastLatencyMs = latency,
            lastTimestamp = System.currentTimeMillis()
        )
    }

    fun release() {
        disconnect()
    }
}
