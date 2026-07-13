package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.engine.BluetoothTrackingService
import com.example.engine.TrackingEngine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext
    val trackingEngine = TrackingEngine.getInstance(context)

    // DB and Repository
    private val database = AppDatabase.getDatabase(context)
    private val repository = FieldMapRepository(database.fieldMapDao())

    // Expose DB History
    val history: StateFlow<List<FieldMapEntity>> = repository.allFieldMaps
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI exposed states from TrackingEngine
    val isTracking = trackingEngine.isTracking
    val pulseCount = trackingEngine.pulseCount
    val totalDistance = trackingEngine.totalDistance
    val localPath = trackingEngine.localPath
    val gpsPath = trackingEngine.gpsPath
    val currentHeading = trackingEngine.currentHeading
    val currentGpsLocation = trackingEngine.currentGpsLocation
    val isGpsEnabled = trackingEngine.isGpsEnabled
    val bluetoothStatus = trackingEngine.bluetoothStatus
    val isBluetoothConnected = trackingEngine.isBluetoothConnected
    val localArea = trackingEngine.localArea
    val gpsArea = trackingEngine.gpsArea
    val isSimulatorPulseRunning = trackingEngine.isSimulatorPulseRunning
    val simulationDirection = trackingEngine.simulationDirection

    fun toggleGps(enabled: Boolean) {
        trackingEngine.toggleGps(enabled)
    }

    fun setBluetoothConnected(connected: Boolean) {
        trackingEngine.setBluetoothConnected(connected)
    }

    fun startTracking() {
        trackingEngine.startTracking()
        
        // Trigger background Foreground Service to prevent background sleep
        val intent = Intent(context, BluetoothTrackingService::class.java).apply {
            action = BluetoothTrackingService.ACTION_START
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopAndCalculate() {
        trackingEngine.stopAndCalculate()
        
        // Stop background service
        val intent = Intent(context, BluetoothTrackingService::class.java).apply {
            action = BluetoothTrackingService.ACTION_STOP
        }
        context.stopService(intent)
    }

    fun resetSession() {
        trackingEngine.resetSession()
    }

    fun simulatePulse() {
        trackingEngine.onPulseReceived()
    }

    fun togglePulseSimulator(active: Boolean) {
        trackingEngine.togglePulseSimulator(active)
    }

    fun steerSimulation(angleChange: Float) {
        trackingEngine.adjustSimulationSteering(angleChange)
    }

    fun resetSteering() {
        trackingEngine.resetSimulationSteering()
    }

    // Database Actions
    fun saveSession(customName: String) {
        viewModelScope.launch {
            val name = customName.ifEmpty { "Field Run #${System.currentTimeMillis() % 1000}" }
            val entity = FieldMapEntity(
                name = name,
                distance = totalDistance.value,
                areaLocal = localArea.value,
                areaGps = gpsArea.value,
                pulseCount = pulseCount.value,
                localPointsJson = CoordinateSerializer.serializeLocal(localPath.value),
                gpsPointsJson = CoordinateSerializer.serializeGps(gpsPath.value),
                isGpsUsed = isGpsEnabled.value
            )
            repository.insert(entity)
        }
    }

    fun deleteSession(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }
}
