package dev.shahzad.prefixshield.incall

import android.content.Intent
import android.os.Bundle
import android.telecom.Call
import android.view.KeyEvent
import android.view.Gravity
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class IncomingBannerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.TOP)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        )
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        setContent {
            val model by CallSession.ui.collectAsStateWithLifecycle()
            LaunchedEffect(model.hasCall, model.state) {
                if (!model.hasCall || model.state != Call.STATE_RINGING) finish()
            }
            IncomingBanner(
                title = model.title,
                onOpen = { openFullCall() },
                onDecline = {
                    CallRingtone.stop()
                    CallSession.reject()
                    finish()
                },
                onAnswer = {
                    CallRingtone.stop()
                    CallSession.answer()
                    openFullCall()
                }
            )
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (CallSession.ui.value.incoming && isSilenceKey(keyCode)) {
            CallRingtone.silence()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun openFullCall() {
        startActivity(
            Intent(this, InCallActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        )
        finish()
    }

    private fun isSilenceKey(keyCode: Int): Boolean = keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
        keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
        keyCode == KeyEvent.KEYCODE_VOLUME_MUTE ||
        keyCode == KeyEvent.KEYCODE_MUTE ||
        keyCode == KeyEvent.KEYCODE_HEADSETHOOK
}

@Composable
private fun IncomingBanner(
    title: String,
    onOpen: () -> Unit,
    onDecline: () -> Unit,
    onAnswer: () -> Unit
) {
    val pulse = rememberInfiniteTransition(label = "banner")
    val scale by pulse.animateFloat(
        1f, 1.18f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "s"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xF22C2C2E), Color(0xF11C1C1E))
                    )
                )
                .clickable(onClick = onOpen)
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF5AC8FA), Color(0xFF007AFF)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        title.firstOrNull()?.uppercaseChar()?.toString() ?: "#",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Incoming call", color = Color(0xFF8E8E93), fontSize = 13.sp)
                    Text(
                        title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("mobile · myPhone", color = Color(0xFF8E8E93), fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BannerAction(Color(0xFFFF3B30), Icons.Filled.CallEnd, "Decline", 1f, onDecline)
                BannerAction(Color(0xFF34C759), Icons.Filled.Call, "Accept", scale, onAnswer)
            }
        }
    }
}

@Composable
private fun BannerAction(
    color: Color,
    icon: ImageVector,
    label: String,
    scale: Float,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
            if (scale != 1f) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            alpha = (1.4f - scale).coerceIn(0.15f, 0.45f)
                        }
                        .background(color, CircleShape)
                )
            }
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(26.dp))
            }
        }
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}
