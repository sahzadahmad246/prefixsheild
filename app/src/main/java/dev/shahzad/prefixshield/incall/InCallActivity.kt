package dev.shahzad.prefixshield.incall

import android.os.Bundle
import android.telecom.Call
import android.telecom.CallAudioState
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.shahzad.prefixshield.Accent
import dev.shahzad.prefixshield.Bg
import dev.shahzad.prefixshield.Off
import dev.shahzad.prefixshield.On
import dev.shahzad.prefixshield.Surface
import dev.shahzad.prefixshield.TextDim
import dev.shahzad.prefixshield.TextMain
import dev.shahzad.prefixshield.appColors
import kotlinx.coroutines.delay

class InCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        IncomingCallNotifier.cancel(this)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        setContent {
            MaterialTheme(colorScheme = appColors) {
                val model by CallSession.ui.collectAsStateWithLifecycle()
                LaunchedEffect(model.hasCall) {
                    if (!model.hasCall) finish()
                }
                InCallScreen(model)
            }
        }
    }
}

@Composable
private fun InCallScreen(model: CallUiModel) {
    var keypad by remember { mutableStateOf(false) }
    var audioMenu by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(model.state) {
        seconds = 0
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
            .background(Bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(36.dp))
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                model.title.firstOrNull()?.uppercaseChar()?.toString() ?: "#",
                color = Accent,
                fontSize = 36.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(model.title, color = TextMain, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        if (model.name.isNotBlank() && model.number.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(model.number, color = TextDim, fontSize = 16.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (model.state == Call.STATE_ACTIVE) formatElapsed(seconds) else model.status,
            color = TextDim,
            fontSize = 16.sp
        )
        Spacer(Modifier.weight(1f))

        if (keypad && !model.incoming) {
            InCallKeypad(onDigit = { CallSession.dtmf(it) })
            TextButton(onClick = { keypad = false }) {
                Text("Hide keypad", color = Accent)
            }
            Spacer(Modifier.height(8.dp))
        }

        if (!model.incoming && model.state != Call.STATE_DISCONNECTED) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RoundAction(
                    if (model.muted) Icons.Filled.MicOff else Icons.Filled.Mic,
                    if (model.muted) "Unmute" else "Mute",
                    model.muted
                ) { CallSession.setMuted(!model.muted) }
                RoundAction(Icons.Filled.Dialpad, if (keypad) "Hide" else "Keypad", keypad) {
                    keypad = !keypad
                }
                Box {
                    RoundAction(
                        audioIcon(model.audioRoute),
                        model.audioLabel,
                        model.audioRoute != CallAudioState.ROUTE_EARPIECE
                    ) { audioMenu = true }
                    DropdownMenu(
                        expanded = audioMenu,
                        onDismissRequest = { audioMenu = false },
                        containerColor = Color(0xFF1C212A)
                    ) {
                        model.audioOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        option.label,
                                        color = if (option.route == model.audioRoute) Accent else TextMain
                                    )
                                },
                                onClick = {
                                    audioMenu = false
                                    CallSession.setAudioRoute(option.route)
                                },
                                leadingIcon = {
                                    Icon(
                                        audioIcon(option.route),
                                        contentDescription = null,
                                        tint = if (option.route == model.audioRoute) Accent else TextMain
                                    )
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }

        if (model.incoming && model.state == Call.STATE_RINGING) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CircleButton(Icons.Filled.CallEnd, Off, "Decline") { CallSession.reject() }
                CircleButton(Icons.Filled.Call, On, "Accept") { CallSession.answer() }
            }
        } else {
            CircleButton(Icons.Filled.CallEnd, Off, "Hang up") { CallSession.hangup() }
        }
        Spacer(Modifier.height(20.dp))
    }
}

private fun audioIcon(route: Int): ImageVector = when (route) {
    CallAudioState.ROUTE_SPEAKER -> Icons.Filled.VolumeUp
    CallAudioState.ROUTE_BLUETOOTH -> Icons.Filled.Bluetooth
    CallAudioState.ROUTE_WIRED_HEADSET -> Icons.Filled.Headset
    else -> Icons.Filled.PhoneInTalk
}

@Composable
private fun RoundAction(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (active) Accent else Surface)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = if (active) Color.White else TextMain)
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = TextDim, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun CircleButton(icon: ImageVector, color: Color, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(74.dp)
                .clip(CircleShape)
                .background(color)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = TextDim, fontSize = 13.sp)
    }
}

@Composable
private fun InCallKeypad(onDigit: (Char) -> Unit) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
    keys.chunked(3).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            row.forEach { key ->
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Surface)
                        .clickable { onDigit(key.first()) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(key, color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

private fun formatElapsed(total: Int): String {
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}
