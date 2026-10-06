package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RobotState
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.IndustrialAmber
import com.example.ui.theme.IndustrialYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import androidx.compose.ui.platform.LocalContext
import com.example.util.HapticManager
import kotlin.math.roundToInt

@Composable
fun ArmController(
    baseDir: Int,
    boomDir: Int,
    stickAngle: Int,
    bucketAngle: Int,
    onBaseDirChange: (Int) -> Unit,
    onBoomDirChange: (Int) -> Unit,
    onStickAngleChange: (Int) -> Unit,
    onBucketAngleChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticManager = remember { HapticManager.getInstance(context) }
    Card(
        modifier = modifier
            .fillMaxHeight()
            .testTag("arm_controller_right_wing"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
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
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = IndustrialAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "ARM & BUCKET MECHANICS",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = TextPrimary
                        )
                    )
                }

                // Wiper / Servo Status
                Text(
                    text = "BS:$baseDir • BM:$boomDir • ST:$stickAngle° • BK:$bucketAngle°",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = IndustrialAmber,
                        fontSize = 10.sp
                    )
                )
            }

            // UPPER QUADRANT: Two Directional Button Pairs (Base & Boom Wiper Motors)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Base Wiper Motor (Left / Right)
                MotorControlBox(
                    title = "BASE ROTATION (BS)",
                    direction = baseDir,
                    negLabel = "LEFT (1)",
                    negIcon = Icons.Default.KeyboardArrowLeft,
                    posLabel = "RIGHT (2)",
                    posIcon = Icons.Default.KeyboardArrowRight,
                    onNegPress = {
                        hapticManager.vibrateClick()
                        onBaseDirChange(RobotState.BASE_LEFT)
                    },
                    onNegRelease = { onBaseDirChange(RobotState.BASE_STOP) },
                    onPosPress = {
                        hapticManager.vibrateClick()
                        onBaseDirChange(RobotState.BASE_RIGHT)
                    },
                    onPosRelease = { onBaseDirChange(RobotState.BASE_STOP) },
                    modifier = Modifier.weight(1f),
                    negTag = "base_left_button",
                    posTag = "base_right_button"
                )

                // Boom Wiper Motor (Up / Down)
                MotorControlBox(
                    title = "BOOM ELEVATION (BM)",
                    direction = boomDir,
                    negLabel = "UP (1)",
                    negIcon = Icons.Default.KeyboardArrowUp,
                    posLabel = "DOWN (2)",
                    posIcon = Icons.Default.KeyboardArrowDown,
                    onNegPress = {
                        hapticManager.vibrateClick()
                        onBoomDirChange(RobotState.BOOM_UP)
                    },
                    onNegRelease = { onBoomDirChange(RobotState.BOOM_STOP) },
                    onPosPress = {
                        hapticManager.vibrateClick()
                        onBoomDirChange(RobotState.BOOM_DOWN)
                    },
                    onPosRelease = { onBoomDirChange(RobotState.BOOM_STOP) },
                    modifier = Modifier.weight(1f),
                    negTag = "boom_up_button",
                    posTag = "boom_down_button"
                )
            }

            // LOWER QUADRANT: Two Horizontal Seek-Bar Sliders for MG996R Servos
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant.copy(alpha = 0.6f))
                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // STICK SERVO (0° - 180°)
                ServoSliderRow(
                    label = "STICK SERVO (ST)",
                    angle = stickAngle,
                    onAngleChange = { newAngle ->
                        if (newAngle != stickAngle) {
                            hapticManager.vibrateTick()
                            onStickAngleChange(newAngle)
                        }
                    },
                    onCenterClick = {
                        hapticManager.vibrateClick()
                        onStickAngleChange(90)
                    },
                    sliderTag = "stick_angle_slider",
                    centerTag = "stick_center_button"
                )

                // BUCKET SERVO (0° - 180°)
                ServoSliderRow(
                    label = "BUCKET SERVO (BK)",
                    angle = bucketAngle,
                    onAngleChange = { newAngle ->
                        if (newAngle != bucketAngle) {
                            hapticManager.vibrateTick()
                            onBucketAngleChange(newAngle)
                        }
                    },
                    onCenterClick = {
                        hapticManager.vibrateClick()
                        onBucketAngleChange(90)
                    },
                    sliderTag = "bucket_angle_slider",
                    centerTag = "bucket_center_button"
                )
            }
        }
    }
}

@Composable
private fun MotorControlBox(
    title: String,
    direction: Int,
    negLabel: String,
    negIcon: ImageVector,
    posLabel: String,
    posIcon: ImageVector,
    onNegPress: () -> Unit,
    onNegRelease: () -> Unit,
    onPosPress: () -> Unit,
    onPosRelease: () -> Unit,
    modifier: Modifier = Modifier,
    negTag: String,
    posTag: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(10.dp))
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                fontSize = 10.sp
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Negative Action Button (Left / Up)
            MotorButton(
                label = negLabel,
                icon = negIcon,
                isActive = direction == 1,
                onPress = onNegPress,
                onRelease = onNegRelease,
                modifier = Modifier.weight(1f),
                tag = negTag
            )

            // Positive Action Button (Right / Down)
            MotorButton(
                label = posLabel,
                icon = posIcon,
                isActive = direction == 2,
                onPress = onPosPress,
                onRelease = onPosRelease,
                modifier = Modifier.weight(1f),
                tag = posTag
            )
        }
    }
}

@Composable
private fun MotorButton(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String
) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) IndustrialAmber else DarkBackground,
        label = "motor_btn_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isActive) DarkBackground else TextPrimary,
        label = "motor_btn_content"
    )

    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(
                1.dp,
                if (isActive) IndustrialYellow else DarkSurfaceBorder,
                RoundedCornerShape(8.dp)
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    onPress()
                    waitForUpOrCancellation()
                    onRelease()
                }
            }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
private fun ServoSliderRow(
    label: String,
    angle: Int,
    onAngleChange: (Int) -> Unit,
    onCenterClick: () -> Unit,
    sliderTag: String,
    centerTag: String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Quick 90° Center Reset
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(DarkBackground)
                        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(4.dp))
                        .clickable(onClick = onCenterClick)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag(centerTag),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = "Center 90",
                        tint = IndustrialYellow,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "90°",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = IndustrialYellow,
                            fontSize = 9.sp
                        )
                    )
                }

                // Current Angle Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(IndustrialAmber.copy(alpha = 0.2f))
                        .border(1.dp, IndustrialAmber.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$angle°",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = IndustrialAmber,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        Slider(
            value = angle.toFloat(),
            onValueChange = { onAngleChange(it.roundToInt()) },
            valueRange = 0f..180f,
            steps = 35, // 5 degree increments
            colors = SliderDefaults.colors(
                thumbColor = IndustrialYellow,
                activeTrackColor = IndustrialYellow,
                inactiveTrackColor = DarkSurfaceBorder,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            ),
            modifier = Modifier
                .height(28.dp)
                .fillMaxWidth()
                .testTag(sliderTag)
        )
    }
}
