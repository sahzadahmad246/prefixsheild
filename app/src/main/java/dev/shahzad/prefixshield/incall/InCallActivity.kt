package dev.shahzad.prefixshield.incall

import android.os.Bundle
import android.telecom.Call
import android.telecom.CallAudioState
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

private val CallBg = Color.Black
private val CallGray = Color(0xFF3A3A3C)
private val CallGreen = Color(0xFF34C759)
private val CallRed = Color(0xFFFF3B30)
private val CallWhite = Color.White
private val CallDim = Color(0xFF8E8E93)

class InCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        setContent {
            val model by CallSession.ui.collectAsStateWithLifecycle()
            LaunchedEffect(model.hasCall) {
                if (!model.hasCall) finish()
            }
            InCallScreen(model)
        }
    }

    override fun onResume() {
        super.onResume()
        AppForeground.onResume()
    }

    override fun onPause() {
        AppForeground.onPause()
        super.onPause()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (CallSession.ui.value.incoming && isSilenceKey(keyCode)) {
            CallRingtone.silence()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun isSilenceKey(keyCode: Int): Boolean = keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
        keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
        keyCode == KeyEvent.KEYCODE_VOLUME_MUTE ||
        keyCode == KeyEvent.KEYCODE_MUTE ||
        keyCode == KeyEvent.KEYCODE_HEADSETHOOK
}

@Composable
private fun InCallScreen(model: CallUiModel) {
    var keypad by remember { mutableStateOf(false) }
    var audioMenu by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(model.state, model.connectedAt) {
        seconds = if (model.connectedAt > 0L) {
            ((System.currentTimeMillis() - model.connectedAt) / 1000L).toInt().coerceAtLeast(0)
        } else 0
        if (model.state == Call.STATE_ACTIVE) {
            while (true) {
                delay(1000)
                seconds++
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CallBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Text(model.title, color = CallWhite, fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                model.holding -> "On Hold"
                model.state == Call.STATE_ACTIVE -> formatElapsed(seconds)
                else -> model.status.ifBlank { "mobile" }
            },
            color = CallDim,
            fontSize = 17.sp
        )
        Spacer(Modifier.weight(1f))

        if (keypad && !model.incoming) {
            InCallKeypad(onDigit = { CallSession.dtmf(it) })
            Spacer(Modifier.height(16.dp))
        }

        if (!model.incoming) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IosAction(
                    if (model.muted) Icons.Filled.MicOff else Icons.Filled.Mic,
                    if (model.muted) "unmute" else "mute",
                    model.muted
                ) { CallSession.setMuted(!model.muted) }
                IosAction(Icons.Filled.Dialpad, if (keypad) "hide" else "keypad", keypad) {
                    keypad = !keypad
                }
                Box {
                    IosAction(audioIcon(model.audioRoute), model.audioLabel.lowercase(), model.audioRoute != CallAudioState.ROUTE_EARPIECE) {
                        audioMenu = true
                    }
                    DropdownMenu(
                        expanded = audioMenu,
                        onDismissRequest = { audioMenu = false },
                        containerColor = CallGray
                    ) {
                        model.audioOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label, color = CallWhite) },
                                onClick = {
                                    audioMenu = false
                                    CallSession.setAudioRoute(option.route)
                                },
                                leadingIcon = {
                                    Icon(audioIcon(option.route), contentDescription = null, tint = CallWhite)
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IosAction(
                    if (model.holding) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    if (model.holding) "resume" else "hold",
                    model.holding
                ) { CallSession.toggleHold() }
            }
            Spacer(Modifier.height(36.dp))
        }

        if (model.incoming && model.state == Call.STATE_RINGING) {
            IosCallButton(Icons.Filled.CallEnd, CallRed, "Decline") {
                CallRingtone.stop()
                CallSession.reject()
            }
            Spacer(Modifier.height(28.dp))
            SlideToAnswer {
                CallRingtone.stop()
                CallSession.answer()
            }
        } else {
            IosCallButton(Icons.Filled.CallEnd, CallRed, "End") { CallSession.hangup() }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun SlideToAnswer(onAnswer: () -> Unit) {
    var drag by remember { mutableFloatStateOf(0f) }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF1C1C1E))
    ) {
        val travel = with(LocalDensity.current) { (maxWidth - 64.dp).toPx().coerceAtLeast(1f) }
        Text(
            "slide to answer",
            color = CallDim,
            fontSize = 17.sp,
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer { alpha = (1f - drag / travel).coerceIn(0f, 1f) }
        )
        Box(
            modifier = Modifier
                .padding(4.dp)
                .offset { IntOffset(drag.roundToInt(), 0) }
                .size(56.dp)
                .clip(CircleShape)
                .background(CallGreen)
                .pointerInput(travel) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, delta ->
                            drag = (drag + delta).coerceIn(0f, travel)
                        },
                        onDragEnd = {
                            if (drag > travel * 0.72f) onAnswer() else drag = 0f
                        },
                        onDragCancel = { drag = 0f }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Call, contentDescription = "Answer", tint = CallWhite, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun IosAction(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(66.dp)
                .clip(CircleShape)
                .background(if (active) CallWhite else CallGray)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = if (active) CallBg else CallWhite, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = CallWhite, fontSize = 12.sp)
    }
}

@Composable
private fun IosCallButton(icon: ImageVector, color: Color, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(color)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = CallWhite, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = CallWhite, fontSize = 13.sp)
    }
}

@Composable
private fun InCallKeypad(onDigit: (Char) -> Unit) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
    keys.chunked(3).forEach { row ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            row.forEach { key ->
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(CallGray)
                        .clickable { onDigit(key.first()) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(key, color = CallWhite, fontSize = 24.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

private fun audioIcon(route: Int): ImageVector = when (route) {
    CallAudioState.ROUTE_SPEAKER -> Icons.Filled.VolumeUp
    CallAudioState.ROUTE_BLUETOOTH -> Icons.Filled.Bluetooth
    CallAudioState.ROUTE_WIRED_HEADSET -> Icons.Filled.Headset
    else -> Icons.Filled.PhoneInTalk
}

private fun formatElapsed(total: Int): String {
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}
