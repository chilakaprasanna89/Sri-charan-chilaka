package com.example.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FieldMapEntity
import com.example.data.LatLngD
import com.example.data.PointD
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MapViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Observe StateFlow values from ViewModel
    val isTracking by viewModel.isTracking.collectAsStateWithLifecycle()
    val pulseCount by viewModel.pulseCount.collectAsStateWithLifecycle()
    val totalDistance by viewModel.totalDistance.collectAsStateWithLifecycle()
    val localPath by viewModel.localPath.collectAsStateWithLifecycle()
    val gpsPath by viewModel.gpsPath.collectAsStateWithLifecycle()
    val currentHeading by viewModel.currentHeading.collectAsStateWithLifecycle()
    val currentGpsLocation by viewModel.currentGpsLocation.collectAsStateWithLifecycle()
    val isGpsEnabled by viewModel.isGpsEnabled.collectAsStateWithLifecycle()
    val bluetoothStatus by viewModel.bluetoothStatus.collectAsStateWithLifecycle()
    val isBluetoothConnected by viewModel.isBluetoothConnected.collectAsStateWithLifecycle()
    val localArea by viewModel.localArea.collectAsStateWithLifecycle()
    val gpsArea by viewModel.gpsArea.collectAsStateWithLifecycle()
    val isSimulatorPulseRunning by viewModel.isSimulatorPulseRunning.collectAsStateWithLifecycle()
    val simulationDirection by viewModel.simulationDirection.collectAsStateWithLifecycle()
    val historyList by viewModel.history.collectAsStateWithLifecycle()

    var showSaveDialog by remember { mutableStateOf(false) }
    var fieldNameInput by remember { mutableStateOf("") }
    var showSimulatorControl by remember { mutableStateOf(true) }

    // Compass pulsing or radar sweeps
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Permission launcher for Location
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            viewModel.toggleGps(true)
        } else {
            viewModel.toggleGps(false)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(DeepNavy),
        containerColor = DeepNavy,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SPA FIELD MAPPER",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = NeonCyan,
                        modifier = Modifier.testTag("app_title")
                    )
                    Text(
                        text = "AGRITECH PRECISION ENGINE",
                        fontSize = 11.sp,
                        color = SlateGray,
                        letterSpacing = 1.5.sp
                    )
                }

                // Connection badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardNavy)
                        .border(1.dp, GridLineColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isBluetoothConnected) ElectricTeal else Color.Red)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = bluetoothStatus,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isBluetoothConnected) ElectricTeal else Color.Red
                    )
                }
            }

            // Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Linear Distance metric card
                MetricCard(
                    title = "LINEAR DISTANCE",
                    value = "%.2f".format(totalDistance),
                    unit = "meters",
                    color = NeonCyan,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("distance_metric_card")
                )

                // Enclosed Surface Area metric card
                MetricCard(
                    title = "SURFACE AREA",
                    value = if (isTracking) "---" else "%.2f".format(localArea),
                    unit = "sq. meters",
                    color = ElectricTeal,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("area_metric_card")
                )
            }

            // Live Canvas Tracing Section
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SpaceNavy)
                    .border(1.dp, GridLineColor, RoundedCornerShape(16.dp))
            ) {
                LiveTracingCanvas(
                    localPath = localPath,
                    gpsPath = gpsPath,
                    isTracking = isTracking,
                    modifier = Modifier.fillMaxSize()
                )

                // Calibration watermark and overlay telemetry
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "WHEEL CIRCUM: 1.000m",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = SlateGray
                    )
                    Text(
                        text = "HEADING: ${"%.1f".format(currentHeading + simulationDirection)}°",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = NeonCyan
                    )
                    if (isGpsEnabled) {
                        Text(
                            text = "GPS: ${if (currentGpsLocation != null) "LOCKED" else "ACQUIRING..."}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ElectricTeal
                        )
                    }
                }

                // Legend indicator
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xBB0F213E))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp, 2.dp).background(NeonCyan))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("HC-05 Wheel Path", fontSize = 9.sp, color = Color.White)
                    }
                    if (isGpsEnabled) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp, 2.dp).background(ElectricTeal))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("GPS Path", fontSize = 9.sp, color = Color.White)
                        }
                    }
                }
            }

            // GPS synchronized trigger & toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardNavy)
                    .border(1.dp, GridLineColor, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = "GPS Toggle icon",
                        tint = if (isGpsEnabled) ElectricTeal else SlateGray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "GPS Synchronized Mapping",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isGpsEnabled) "Parallel satellite path enabled" else "Wheel path tracking only",
                            fontSize = 10.sp,
                            color = SlateGray
                        )
                    }
                }
                Switch(
                    checked = isGpsEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        } else {
                            viewModel.toggleGps(false)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ElectricTeal,
                        checkedTrackColor = Color(0xFF003F33),
                        uncheckedThumbColor = SlateGray,
                        uncheckedTrackColor = Color(0xFF1E2E4E)
                    ),
                    modifier = Modifier.testTag("gps_tracking_toggle")
                )
            }

            // Interactive Simulator Control Panel for browser environment testing
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardNavy)
                    .border(1.dp, GridLineColor, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSimulatorControl = !showSimulatorControl },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SettingsInputAntenna,
                            contentDescription = "Simulator panel",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Developer Bluetooth Simulation Hub",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                    Icon(
                        imageVector = if (showSimulatorControl) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand/Collapse",
                        tint = SlateGray
                    )
                }

                if (showSimulatorControl) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Manual pulse injector
                        Button(
                            onClick = { viewModel.simulatePulse() },
                            enabled = isTracking,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF133F75),
                                disabledContainerColor = Color(0xFF0F213E)
                            ),
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("simulate_pulse_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Pulse count", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simulate Pulse", fontSize = 11.sp)
                        }

                        // Auto pulse toggler
                        Button(
                            onClick = { viewModel.togglePulseSimulator(!isSimulatorPulseRunning) },
                            enabled = isTracking,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSimulatorPulseRunning) Color.Red else ElectricTeal,
                                contentColor = DeepNavy,
                                disabledContainerColor = Color(0xFF0F213E)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("auto_pulse_button")
                        ) {
                            Text(if (isSimulatorPulseRunning) "STOP AUTO" else "AUTO PULSE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Simulated Steering (Heading Offset): ${"%.0f".format(simulationDirection)}°",
                        fontSize = 11.sp,
                        color = SlateGray,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.steerSimulation(-45f) },
                            enabled = isTracking,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2E4E)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Steer left", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Steer L 45°", fontSize = 10.sp)
                        }

                        Button(
                            onClick = { viewModel.resetSteering() },
                            enabled = isTracking,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2E4E)),
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Text("Reset", fontSize = 10.sp)
                        }

                        Button(
                            onClick = { viewModel.steerSimulation(45f) },
                            enabled = isTracking,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2E4E)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Steer R 45°", fontSize = 10.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Steer right", modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Primary Control Hub
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isTracking) {
                    // START mapping
                    Button(
                        onClick = { viewModel.startTracking() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricTeal,
                            contentColor = DeepNavy
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(54.dp)
                            .border(1.dp, ElectricTeal, RoundedCornerShape(12.dp))
                            .testTag("start_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Start survey")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "START SURVEY",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                } else {
                    // STOP & CALCULATE
                    Button(
                        onClick = {
                            viewModel.stopAndCalculate()
                            if (pulseCount > 0) {
                                showSaveDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(54.dp)
                            .border(
                                2.dp,
                                RedGlow.copy(alpha = pulseScale),
                                RoundedCornerShape(12.dp)
                            )
                            .testTag("stop_calculate_button")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop survey")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "STOP & COMPUTE",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // RESET button
                OutlinedButton(
                    onClick = {
                        viewModel.resetSession()
                        showSaveDialog = false
                    },
                    border = BorderStroke(1.dp, SlateGray),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateGray),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("reset_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset mapping data")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RESET", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // Save Field Dialog directly integrated inside main flow to avoid blocking overlays
            AnimatedVisibility(
                visible = showSaveDialog,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardNavy),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ElectricTeal, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "💾 SAVE COMPUTED SURVEY RUN",
                            fontWeight = FontWeight.Bold,
                            color = ElectricTeal,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = fieldNameInput,
                            onValueChange = { fieldNameInput = it },
                            placeholder = { Text("e.g. Paddy Crop Sector B") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = ElectricTeal,
                                unfocusedBorderColor = SlateGray,
                                focusedPlaceholderColor = SlateGray,
                                unfocusedPlaceholderColor = SlateGray
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("field_name_input_field")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showSaveDialog = false }) {
                                Text("CANCEL", color = SlateGray, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.saveSession(fieldNameInput)
                                    fieldNameInput = ""
                                    showSaveDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal, contentColor = DeepNavy)
                            ) {
                                Text("SAVE TO DATABASE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Historical Mapping Logs list
            Text(
                text = "📁 COMPLETED HISTORIC SURVEYS",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardNavy)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No saved surveys. Complete a path above and save it.",
                        color = SlateGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxWidth()
                        .testTag("history_logs_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(historyList) { item ->
                        HistoryItemCard(
                            item = item,
                            onDelete = { viewModel.deleteSession(item.id) }
                        )
                    }
                }
            }

            // Elegant "Deep Tech" Brand Footer
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Contact on Instagram: @_mr.charan_99_",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonCyan,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "© 2026 All Rights Reserved by Sri Charan Chilaka",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = SlateGray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CardNavy),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = SlateGray,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = value,
                    fontSize = 26.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    fontSize = 10.sp,
                    color = SlateGray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
fun LiveTracingCanvas(
    localPath: List<PointD>,
    gpsPath: List<LatLngD>,
    isTracking: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        // 1. Draw Tech Grid Lines in the Background
        val gridSpacing = 40.dp.toPx()
        val gridWidth = size.width
        val gridHeight = size.height

        var xPos = 0f
        while (xPos < gridWidth) {
            drawLine(
                color = GridLineColor,
                start = Offset(xPos, 0f),
                end = Offset(xPos, gridHeight),
                strokeWidth = 1f
            )
            xPos += gridSpacing
        }

        var yPos = 0f
        while (yPos < gridHeight) {
            drawLine(
                color = GridLineColor,
                start = Offset(0f, yPos),
                end = Offset(gridWidth, yPos),
                strokeWidth = 1f
            )
            yPos += gridSpacing
        }

        // Draw crosshairs at the exact center
        drawLine(
            color = GridLineColor.copy(alpha = 0.3f),
            start = Offset(gridWidth / 2f, 0f),
            end = Offset(gridWidth / 2f, gridHeight),
            strokeWidth = 2f
        )
        drawLine(
            color = GridLineColor.copy(alpha = 0.3f),
            start = Offset(0f, gridHeight / 2f),
            end = Offset(gridWidth, gridHeight / 2f),
            strokeWidth = 2f
        )

        // 2. Plotting Coordinates if we have path vertices
        if (localPath.isEmpty() && gpsPath.isEmpty()) {
            // Draw scanning radar sonar sweep if empty/ready
            drawCircle(
                color = NeonCyan.copy(alpha = 0.05f),
                center = Offset(gridWidth / 2f, gridHeight / 2f),
                radius = size.minDimension / 3f
            )
            drawCircle(
                color = NeonCyan.copy(alpha = 0.1f),
                center = Offset(gridWidth / 2f, gridHeight / 2f),
                radius = size.minDimension / 6f,
                style = Stroke(width = 2f)
            )
            drawCircle(
                color = NeonCyan.copy(alpha = 0.2f),
                center = Offset(gridWidth / 2f, gridHeight / 2f),
                radius = 10f
            )
            return@Canvas
        }

        // 3. Compute Cartesian meters for GPS points relative to start origin
        val gpsPointsMeters = if (gpsPath.isNotEmpty()) {
            val origin = gpsPath.first()
            gpsPath.map { point ->
                val y = (point.latitude - origin.latitude) * 111320.0
                val x = (point.longitude - origin.longitude) * 111320.0 * Math.cos(Math.toRadians(origin.latitude))
                PointD(x, y)
            }
        } else {
            emptyList()
        }

        // 4. Auto Scaling Map Boundary Computations
        val allPoints = localPath + gpsPointsMeters
        val allX = allPoints.map { it.x }
        val allY = allPoints.map { it.y }

        val minX = allX.minOrNull() ?: -10.0
        val maxX = allX.maxOrNull() ?: 10.0
        val minY = allY.minOrNull() ?: -10.0
        val maxY = allY.maxOrNull() ?: 10.0

        val rangeX = (maxX - minX).coerceAtLeast(1.0)
        val rangeY = (maxY - minY).coerceAtLeast(1.0)

        // Padding scale factors (15%)
        val padding = 0.15f
        val scaleX = (gridWidth * (1f - 2 * padding)) / rangeX
        val scaleY = (gridHeight * (1f - 2 * padding)) / rangeY
        val scale = Math.min(scaleX, scaleY).toFloat()

        val midX = (minX + maxX) / 2.0
        val midY = (minY + maxY) / 2.0

        // Helper to map PointD in meters to local Canvas Offset coordinates
        fun mapToCanvas(point: PointD): Offset {
            val cx = gridWidth / 2f + (point.x - midX).toFloat() * scale
            // Flipped Cartesian Y so up is positive
            val cy = gridHeight / 2f - (point.y - midY).toFloat() * scale
            return Offset(cx, cy)
        }

        // 5. Draw GPS Parallel Path
        if (gpsPointsMeters.size >= 2) {
            val gpsPathStroke = Path()
            val firstOffset = mapToCanvas(gpsPointsMeters.first())
            gpsPathStroke.moveTo(firstOffset.x, firstOffset.y)
            for (i in 1 until gpsPointsMeters.size) {
                val nextOffset = mapToCanvas(gpsPointsMeters[i])
                gpsPathStroke.lineTo(nextOffset.x, nextOffset.y)
            }
            drawPath(
                path = gpsPathStroke,
                color = ElectricTeal.copy(alpha = 0.6f),
                style = Stroke(width = 4.dp.toPx())
            )
            // Draw points
            gpsPointsMeters.forEach { pt ->
                drawCircle(
                    color = ElectricTeal,
                    radius = 3.dp.toPx(),
                    center = mapToCanvas(pt)
                )
            }
        }

        // 6. Draw local Wheel Path (Mandated Neon Cyan active path)
        if (localPath.size >= 2) {
            val wheelPathStroke = Path()
            val firstOffset = mapToCanvas(localPath.first())
            wheelPathStroke.moveTo(firstOffset.x, firstOffset.y)
            for (i in 1 until localPath.size) {
                val nextOffset = mapToCanvas(localPath[i])
                wheelPathStroke.lineTo(nextOffset.x, nextOffset.y)
            }

            // Semi-transparent Neon Cyan Polygon Fill if closed (on stop tracking)
            if (!isTracking) {
                wheelPathStroke.close()
                drawPath(
                    path = wheelPathStroke,
                    color = NeonCyan.copy(alpha = 0.15f)
                )
            }

            drawPath(
                path = wheelPathStroke,
                color = NeonCyan,
                style = Stroke(width = 6.dp.toPx())
            )

            // Plot distinct coordinate vertex dots
            localPath.forEachIndexed { index, pt ->
                val isLast = index == localPath.size - 1
                drawCircle(
                    color = if (isLast && isTracking) ElectricTeal else NeonCyan,
                    radius = if (isLast && isTracking) 6.dp.toPx() else 4.dp.toPx(),
                    center = mapToCanvas(pt)
                )
            }
        } else if (localPath.size == 1) {
            // Draw single origin point
            drawCircle(
                color = NeonCyan,
                radius = 6.dp.toPx(),
                center = mapToCanvas(localPath.first())
            )
        }
    }
}

@SuppressLint("SimpleDateFormat")
@Composable
fun HistoryItemCard(
    item: FieldMapEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm") }
    val dateString = formatter.format(Date(item.timestamp))

    Card(
        colors = CardDefaults.cardColors(containerColor = CardNavy),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, GridLineColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Agriculture,
                        contentDescription = "Farm Field icon",
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Distance: %.2fm  •  Area: %.2f sq.m".format(item.distance, item.areaLocal),
                    fontSize = 12.sp,
                    color = SlateGray,
                    fontFamily = FontFamily.Monospace
                )
                if (item.isGpsUsed) {
                    Text(
                        text = "GPS Area Ref: %.2f sq.m".format(item.areaGps),
                        fontSize = 10.sp,
                        color = ElectricTeal,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = dateString,
                    fontSize = 9.sp,
                    color = SlateGray
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_history_item_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete run logs",
                    tint = Color.Red.copy(alpha = 0.8f)
                )
            }
        }
    }
}
