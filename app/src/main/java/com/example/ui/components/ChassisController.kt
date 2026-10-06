package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RobotState
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.IndustrialAmber
import com.example.ui.theme.IndustrialRed
import com.example.ui.theme.IndustrialYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

import androidx.compose.ui.platform.LocalContext
import com.example.util.HapticManager

@Composable
fun ChassisController(
    currentDriveDir: Int,
    onDirectionChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticManager = remember { HapticManager.getInstance(context) }
    Card(
        modifier = modifier
            .fillMaxHeight()
            .testTag("chassis_controller_left_wing"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Zone Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = IndustrialYellow,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "ROVER CHASSIS",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = TextPrimary
                        )
                    )
                }

                // Active Drive Status Badge
                val driveStatusText = when (currentDriveDir) {
                    RobotState.DRIVE_FORWARD -> "FORWARD (RV:1)"
                    RobotState.DRIVE_REVERSE -> "REVERSE (RV:2)"
                    RobotState.DRIVE_LEFT -> "TURN LEFT (RV:3)"
                    RobotState.DRIVE_RIGHT -> "TURN RIGHT (RV:4)"
                    else -> "STOPPED (RV:0)"
                }
                Text(
                    text = driveStatusText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (currentDriveDir != 0) IndustrialYellow else TextMuted,
                        fontSize = 10.sp
                    )
                )
            }

            // 4-Way Thumb D-Pad Container (Zero Scroll, Ergonomic Reach)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Background D-Pad Cross Plate
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(DarkSurfaceVariant, DarkBackground)
                            )
                        )
                        .border(2.dp, DarkSurfaceBorder, CircleShape)
                )

                // Cross Layout for 4 Directions
                Column(
                    modifier = Modifier.size(210.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // FORWARD (Top)
                    DPadButton(
                        icon = Icons.Default.ArrowUpward,
                        label = "FWD",
                        direction = RobotState.DRIVE_FORWARD,
                        isActive = currentDriveDir == RobotState.DRIVE_FORWARD,
                        onPress = {
                            hapticManager.vibrateClick()
                            onDirectionChange(RobotState.DRIVE_FORWARD)
                        },
                        onRelease = { onDirectionChange(RobotState.DRIVE_STOP) },
                        testTag = "dpad_fwd_button"
                    )

                    // LEFT - CENTER STOP - RIGHT (Middle Row)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // LEFT
                        DPadButton(
                            icon = Icons.Default.ArrowBack,
                            label = "LEFT",
                            direction = RobotState.DRIVE_LEFT,
                            isActive = currentDriveDir == RobotState.DRIVE_LEFT,
                            onPress = {
                                hapticManager.vibrateClick()
                                onDirectionChange(RobotState.DRIVE_LEFT)
                            },
                            onRelease = { onDirectionChange(RobotState.DRIVE_STOP) },
                            testTag = "dpad_left_button"
                        )

                        // CENTER STOP INDICATOR / TOUCH
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    if (currentDriveDir == RobotState.DRIVE_STOP) {
                                        DarkSurfaceVariant
                                    } else {
                                        IndustrialAmber.copy(alpha = 0.2f)
                                    }
                                )
                                .border(
                                    2.dp,
                                    if (currentDriveDir == RobotState.DRIVE_STOP) DarkSurfaceBorder else IndustrialYellow,
                                    CircleShape
                                )
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        awaitFirstDown()
                                        hapticManager.vibrateClick()
                                        onDirectionChange(RobotState.DRIVE_STOP)
                                        waitForUpOrCancellation()
                                    }
                                }
                                .testTag("dpad_center_stop"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.StopCircle,
                                contentDescription = "Brake",
                                tint = if (currentDriveDir == RobotState.DRIVE_STOP) TextMuted else IndustrialRed,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // RIGHT
                        DPadButton(
                            icon = Icons.Default.ArrowForward,
                            label = "RIGHT",
                            direction = RobotState.DRIVE_RIGHT,
                            isActive = currentDriveDir == RobotState.DRIVE_RIGHT,
                            onPress = {
                                hapticManager.vibrateClick()
                                onDirectionChange(RobotState.DRIVE_RIGHT)
                            },
                            onRelease = { onDirectionChange(RobotState.DRIVE_STOP) },
                            testTag = "dpad_right_button"
                        )
                    }

                    // REVERSE (Bottom)
                    DPadButton(
                        icon = Icons.Default.ArrowDownward,
                        label = "REV",
                        direction = RobotState.DRIVE_REVERSE,
                        isActive = currentDriveDir == RobotState.DRIVE_REVERSE,
                        onPress = {
                            hapticManager.vibrateClick()
                            onDirectionChange(RobotState.DRIVE_REVERSE)
                        },
                        onRelease = { onDirectionChange(RobotState.DRIVE_STOP) },
                        testTag = "dpad_rev_button"
                    )
                }
            }

            // Quick instruction footer
            Text(
                text = "Hold direction for continuous drive • Release to brake",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextMuted,
                    fontSize = 9.sp
                )
            )
        }
    }
}

@Composable
private fun DPadButton(
    icon: ImageVector,
    label: String,
    direction: Int,
    isActive: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    testTag: String
) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) IndustrialYellow else DarkSurfaceVariant,
        label = "dpad_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isActive) DarkBackground else TextPrimary,
        label = "dpad_content"
    )

    Box(
        modifier = Modifier
            .size(width = 68.dp, height = 58.dp)
            .shadow(if (isActive) 8.dp else 2.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) IndustrialAmber else DarkSurfaceBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .pointerInput(direction) {
                awaitEachGesture {
                    awaitFirstDown()
                    onPress()
                    waitForUpOrCancellation()
                    onRelease()
                }
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = contentColor,
                    fontSize = 9.sp
                )
            )
        }
    }
}
