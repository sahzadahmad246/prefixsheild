package dev.shahzad.prefixshield

import android.app.role.RoleManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PhoneDisabled
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val app get() = application as PrefixShieldApp

    private var screeningEnabled by mutableStateOf(false)
    private var prefixInput by mutableStateOf("")
    private var rules by mutableStateOf(listOf<PrefixRule>())
    private var blockedCalls by mutableStateOf(listOf<BlockedCall>())

    private val roleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refresh()
        if (isCallScreeningHeld()) {
            Toast.makeText(this, "Screening on", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refresh()
        setContent {
            MaterialTheme(colorScheme = appColors) {
                App(
                    screeningEnabled = screeningEnabled,
                    prefixInput = prefixInput,
                    onPrefixInputChange = { prefixInput = it },
                    rules = rules,
                    blockedCalls = blockedCalls,
                    onEnableScreening = ::requestCallScreeningRole,
                    onAddPrefix = ::addPrefix,
                    onTogglePrefix = { prefix, enabled ->
                        rules = app.prefixStore.toggle(prefix, enabled)
                    },
                    onRemovePrefix = { prefix ->
                        rules = app.prefixStore.remove(prefix)
                    },
                    onClearLog = {
                        app.blockedLogStore.clear()
                        blockedCalls = emptyList()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        screeningEnabled = isCallScreeningHeld()
        rules = app.prefixStore.list()
        blockedCalls = app.blockedLogStore.list()
    }

    private fun addPrefix() {
        try {
            rules = app.prefixStore.add(prefixInput)
            prefixInput = ""
        } catch (_: Exception) {
            Toast.makeText(this, "Need 3+ digits", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isCallScreeningHeld(): Boolean {
        val roleManager = getSystemService(RoleManager::class.java)
        return roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    private fun requestCallScreeningRole() {
        val roleManager = getSystemService(RoleManager::class.java)
        if (!roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            Toast.makeText(this, "Not available on this phone", Toast.LENGTH_LONG).show()
            return
        }
        roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
    }
}

private val Bg = Color(0xFF0B0D10)
private val Surface = Color(0xFF15181E)
private val Line = Color(0xFF2A303A)
private val TextMain = Color(0xFFF2F4F7)
private val TextDim = Color(0xFF8B93A1)
private val Accent = Color(0xFF6B8CFF)
private val On = Color(0xFF3DDC84)
private val Off = Color(0xFFFF6B6B)

private val appColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    background = Bg,
    surface = Surface,
    onBackground = TextMain,
    onSurface = TextMain
)

@Composable
private fun App(
    screeningEnabled: Boolean,
    prefixInput: String,
    onPrefixInputChange: (String) -> Unit,
    rules: List<PrefixRule>,
    blockedCalls: List<BlockedCall>,
    onEnableScreening: () -> Unit,
    onAddPrefix: () -> Unit,
    onTogglePrefix: (String, Boolean) -> Unit,
    onRemovePrefix: (String) -> Unit,
    onClearLog: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    val navColors = NavigationBarItemDefaults.colors(
        selectedIconColor = Accent,
        selectedTextColor = Accent,
        unselectedIconColor = TextDim,
        unselectedTextColor = TextDim,
        indicatorColor = Color(0xFF1E2430)
    )

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            NavigationBar(containerColor = Surface, contentColor = TextMain) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
                    label = { Text("Home") },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Outlined.PhoneDisabled, contentDescription = null) },
                    label = { Text("Blocked") },
                    colors = navColors
                )
            }
        }
    ) { padding ->
        if (tab == 0) {
            HomeTab(
                padding = padding,
                screeningEnabled = screeningEnabled,
                prefixInput = prefixInput,
                onPrefixInputChange = onPrefixInputChange,
                rules = rules,
                onEnableScreening = onEnableScreening,
                onAddPrefix = onAddPrefix,
                onTogglePrefix = onTogglePrefix,
                onRemovePrefix = onRemovePrefix
            )
        } else {
            BlockedTab(
                padding = padding,
                blockedCalls = blockedCalls,
                onClearLog = onClearLog
            )
        }
    }
}

@Composable
private fun HomeTab(
    padding: PaddingValues,
    screeningEnabled: Boolean,
    prefixInput: String,
    onPrefixInputChange: (String) -> Unit,
    rules: List<PrefixRule>,
    onEnableScreening: () -> Unit,
    onAddPrefix: () -> Unit,
    onTogglePrefix: (String, Boolean) -> Unit,
    onRemovePrefix: (String) -> Unit
) {
    val focus = LocalFocusManager.current
    var pendingDelete by remember { mutableStateOf<PrefixRule?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "PrefixShield",
                color = TextMain,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp
            )
        }

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Surface)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (screeningEnabled) On else Off)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                if (screeningEnabled) "Call screening on" else "Call screening off",
                color = TextMain,
                modifier = Modifier.weight(1f),
                fontSize = 15.sp
            )
            if (!screeningEnabled) {
                Button(
                    onClick = onEnableScreening,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White)
                ) {
                    Text("Enable")
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Surface)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = prefixInput,
                onValueChange = onPrefixInputChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Series") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    onAddPrefix()
                    focus.clearFocus()
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Bg,
                    unfocusedContainerColor = Bg,
                    focusedTextColor = TextMain,
                    unfocusedTextColor = TextMain,
                    cursorColor = Accent,
                    focusedPlaceholderColor = TextDim,
                    unfocusedPlaceholderColor = TextDim
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    onAddPrefix()
                    focus.clearFocus()
                },
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White)
            ) {
                Text("Add")
            }
        }

        Spacer(Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(rules, key = { it.prefix }) { rule ->
                PrefixRow(
                    rule = rule,
                    onToggle = { onTogglePrefix(rule.prefix, it) },
                    onDelete = { pendingDelete = rule }
                )
            }
        }
    }

    pendingDelete?.let { rule ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = Surface,
            title = { Text("Delete ${rule.prefix}?", color = TextMain) },
            confirmButton = {
                TextButton(onClick = {
                    onRemovePrefix(rule.prefix)
                    pendingDelete = null
                }) {
                    Text("Delete", color = Off)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel", color = TextDim)
                }
            }
        )
    }
}

@Composable
private fun PrefixRow(
    rule: PrefixRule,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            rule.prefix,
            color = if (rule.enabled) TextMain else TextDim,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "More", tint = TextDim)
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                containerColor = Color(0xFF1C212A)
            ) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (rule.enabled) "On" else "Off",
                                color = TextMain,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = rule.enabled,
                                onCheckedChange = {
                                    onToggle(it)
                                    menuOpen = false
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Accent
                                )
                            )
                        }
                    },
                    onClick = {
                        onToggle(!rule.enabled)
                        menuOpen = false
                    }
                )
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Delete, contentDescription = null, tint = Off)
                            Spacer(Modifier.width(10.dp))
                            Text("Delete", color = Off)
                        }
                    },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
private fun BlockedTab(
    padding: PaddingValues,
    blockedCalls: List<BlockedCall>,
    onClearLog: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Blocked",
                color = TextMain,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (blockedCalls.isNotEmpty()) {
                TextButton(onClick = onClearLog) {
                    Text("Clear", color = Accent)
                }
            }
        }

        if (blockedCalls.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhoneDisabled,
                    contentDescription = null,
                    tint = TextDim,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text("No blocked calls", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text("They’ll show up here", color = TextDim, fontSize = 13.sp)
            }
        } else {
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(blockedCalls, key = { "${it.atMillis}-${it.number}" }) { call ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            call.number,
                            color = TextMain,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(formatTime(call.atMillis), color = TextDim, fontSize = 13.sp)
                    }
                    HorizontalDivider(color = Line)
                }
            }
        }
    }
}

private fun formatTime(atMillis: Long): String {
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(atMillis))
}
