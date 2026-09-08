package com.zhiwei.math.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedCornerStyle
import com.zhiwei.math.glass.AppMotion
import com.zhiwei.math.glass.LocalHapticsEnabled
import com.zhiwei.math.glass.pressableScale
import com.zhiwei.math.ui.icons.SfCheckmark
import com.zhiwei.math.ui.theme.IosShapes
import com.zhiwei.math.ui.theme.LocalIosPalette

/**
 * iOS 绿色开关（51×31 胶囊轨道 + 27dp 白色圆钮 bouncy spring + 按下放大 1.05）。
 */
@Composable
fun IosSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = LocalIosPalette.current
    val haptic = LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    val interaction = remember { MutableInteractionSource() }
    var pressed by remember { mutableStateOf(false) }

    val trackColor by animateColorAsState(
        targetValue = if (checked) palette.green else palette.fill,
        animationSpec = AppMotion.snappy(),
        label = "switchTrack",
    )
    val thumbScale by animateFloatAsState(
        targetValue = if (pressed) 1.05f else 1f,
        animationSpec = AppMotion.bouncy(),
        label = "switchThumbScale",
    )
    // 钮位：开=右（51-27-2*2=18dp 边距），关=左（2dp）
    val thumbX by animateFloatAsState(
        targetValue = if (checked) 20f else 2f,
        animationSpec = AppMotion.bouncy(),
        label = "switchThumbX",
    )

    Box(
        modifier = modifier
            .size(51.dp, 31.dp)
            .background(trackColor, IosShapes.Capsule)
            .then(
                if (enabled) Modifier
                    .clickable(interactionSource = interaction, indication = null) {
                        if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCheckedChange(!checked)
                    }
                    .pressableScale(interaction, pressedScale = 0.96f)
                else Modifier
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        // 按压态监听（放大用）
        androidx.compose.runtime.LaunchedEffect(interaction) {
            interaction.interactions.collect { i ->
                pressed = when (i) {
                    is androidx.compose.foundation.interaction.PressInteraction.Press -> true
                    is androidx.compose.foundation.interaction.PressInteraction.Release,
                    is androidx.compose.foundation.interaction.PressInteraction.Cancel -> false
                    else -> pressed
                }
            }
        }
        Box(
            modifier = Modifier
                .padding(start = thumbX.dp)
                .size(27.dp)
                .graphicsLayer {
                    scaleX = thumbScale
                    scaleY = thumbScale
                }
                .shadow(2.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.2f))
                .background(Color.White, CircleShape),
        )
    }
}
