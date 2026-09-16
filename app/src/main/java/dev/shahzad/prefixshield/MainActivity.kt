package dev.shahzad.prefixshield

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ContactPhone
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhoneDisabled
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private val app get() = application as PrefixShieldApp

    private var screeningEnabled by mutableStateOf(false)
    private var blockingOn by mutableStateOf(true)
    private var blockSavedOn by mutableStateOf(true)
    private var phoneAccessEnabled by mutableStateOf(false)
    private var requestedPermsOnce = false
    private var prefixInput by mutableStateOf("")
    private var rules by mutableStateOf(listOf<PrefixRule>())
    private var blockedCalls by mutableStateOf(listOf<BlockedCall>())
    private var totalBlockedCount by mutableIntStateOf(0)

    private val roleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refresh()
        if (isCallScreeningHeld()) {
            Toast.makeText(this, "Screening on", Toast.LENGTH_SHORT).show()
        }
    }

    private val logListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        runOnUiThread { refresh() }
    }

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refresh()
        app.blockedLogStore.register(logListener)
        setContent {
            MaterialTheme(colorScheme = appColors) {
                App(
                    screeningEnabled = screeningEnabled,
                    blockingOn = blockingOn,
                    blockSavedOn = blockSavedOn,
                    phoneAccessEnabled = phoneAccessEnabled,
                    prefixInput = prefixInput,
                    onPrefixInputChange = { prefixInput = it },
                    rules = rules,
                    blockedCalls = blockedCalls,
                    totalBlockedCount = totalBlockedCount,
                    onEnableScreening = ::requestCallScreeningRole,
                    onToggleBlocking = ::toggleBlocking,
                    onToggleBlockSaved = ::toggleBlockSaved,
                    onGrantPhoneAccess = ::requestPhoneAccess,
                    onAddPrefix = ::addPrefix,
                    onTogglePrefix = { prefix, enabled ->
                        rules = app.prefixStore.toggle(prefix, enabled)
                    },
                    onRemovePrefix = { prefix ->
                        rules = app.prefixStore.remove(prefix)
                    },
                    onClearLog = {
                        app.blockedLogStore.clear()
                        PendingNotifications(this).clear()
                        BlockedNotifier.cancel(this)
                        blockedCalls = emptyList()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        clearUnseenNotes()
    }

    override fun onResume() {
        super.onResume()
        clearUnseenNotes()
        refresh()
    }

    override fun onDestroy() {
        app.blockedLogStore.unregister(logListener)
        super.onDestroy()
    }

    private fun refresh() {
        screeningEnabled = isCallScreeningHeld()
        blockingOn = screeningEnabled && BlockSettings(this).isBlockingEnabled()
        blockSavedOn = BlockSettings(this).blockSavedNumbers()
        phoneAccessEnabled = hasPhoneAccess()
        rules = app.prefixStore.list()
        blockedCalls = app.blockedLogStore.list()
        totalBlockedCount = app.blockedLogStore.totalCount()
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
        BlockSettings(this).setBlockingEnabled(true)
        requestPhoneAccess()
    }

    private fun toggleBlocking(enabled: Boolean) {
        if (enabled) {
            BlockSettings(this).setBlockingEnabled(true)
            if (!isCallScreeningHeld()) {
                requestCallScreeningRole()
            } else {
                refresh()
            }
        } else {
            BlockSettings(this).setBlockingEnabled(false)
            refresh()
        }
    }

    private fun toggleBlockSaved(enabled: Boolean) {
        BlockSettings(this).setBlockSavedNumbers(enabled)
        if (enabled) requestPhoneAccess()
        refresh()
    }

    private fun clearUnseenNotes() {
        PendingNotifications(this).clear()
        BlockedNotifier.cancel(this)
    }

    private fun hasPhoneAccess(): Boolean = requiredPhonePerms().all { permission ->
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPhoneAccess() {
        val missing = requiredPhonePerms().filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            refresh()
            return
        }
        val openSettings = requestedPermsOnce &&
            missing.any { !shouldShowRequestPermissionRationale(it) }
        requestedPermsOnce = true
        if (openSettings) {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
            )
        } else {
            permLauncher.launch(missing.toTypedArray())
        }
    }

    private fun requiredPhonePerms(): Array<String> {
        val perms = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.ANSWER_PHONE_CALLS
        )
        if (Build.VERSION.SDK_INT >= 33) {
            perms += Manifest.permission.POST_NOTIFICATIONS
        }
        return perms.toTypedArray()
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
    blockingOn: Boolean,
    blockSavedOn: Boolean,
    phoneAccessEnabled: Boolean,
    prefixInput: String,
    onPrefixInputChange: (String) -> Unit,
    rules: List<PrefixRule>,
    blockedCalls: List<BlockedCall>,
    totalBlockedCount: Int,
    onEnableScreening: () -> Unit,
    onToggleBlocking: (Boolean) -> Unit,
    onToggleBlockSaved: (Boolean) -> Unit,
    onGrantPhoneAccess: () -> Unit,
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
                blockingOn = blockingOn,
                blockSavedOn = blockSavedOn,
                phoneAccessEnabled = phoneAccessEnabled,
                prefixInput = prefixInput,
                onPrefixInputChange = onPrefixInputChange,
                rules = rules,
                totalBlockedCount = totalBlockedCount,
                onEnableScreening = onEnableScreening,
                onToggleBlocking = onToggleBlocking,
                onToggleBlockSaved = onToggleBlockSaved,
                onGrantPhoneAccess = onGrantPhoneAccess,
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
    blockingOn: Boolean,
    blockSavedOn: Boolean,
    phoneAccessEnabled: Boolean,
    prefixInput: String,
    onPrefixInputChange: (String) -> Unit,
    rules: List<PrefixRule>,
    totalBlockedCount: Int,
    onEnableScreening: () -> Unit,
    onToggleBlocking: (Boolean) -> Unit,
    onToggleBlockSaved: (Boolean) -> Unit,
    onGrantPhoneAccess: () -> Unit,
    onAddPrefix: () -> Unit,
    onTogglePrefix: (String, Boolean) -> Unit,
    onRemovePrefix: (String) -> Unit
) {
    val focus = LocalFocusManager.current
    var pendingDelete by remember { mutableStateOf<PrefixRule?>(null) }
    val activeRules = rules.count { it.enabled }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "PrefixShield",
                        color = TextMain,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        "Call screening & prefix blocking",
                        color = TextDim,
                        fontSize = 13.sp
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Surface)
                    .border(1.dp, if (blockingOn) On.copy(alpha = 0.35f) else Line, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (blockingOn) On.copy(alpha = 0.15f) else Off.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = if (blockingOn) On else Off,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (blockingOn) "Protection active" else "Protection paused",
                            color = TextMain,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            when {
                                !screeningEnabled -> "Enable call screening to start blocking"
                                blockingOn -> "$activeRules active series · $totalBlockedCount blocked total"
                                else -> "Screening on, blocking toggled off"
                            },
                            color = TextDim,
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = blockingOn,
                        onCheckedChange = onToggleBlocking,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Accent
                        )
                    )
                }
                if (!screeningEnabled) {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onEnableScreening,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White)
                    ) {
                        Icon(Icons.Outlined.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Enable call screening")
                    }
                }
            }
        }

        if (!phoneAccessEnabled) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1A2030))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null, tint = Accent)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Phone access needed", color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Contacts, call log & alerts", color = TextDim, fontSize = 12.sp)
                    }
                    TextButton(onClick = onGrantPhoneAccess) {
                        Text("Allow", color = Accent)
                    }
                }
            }
        }

        item {
            SettingToggleCard(
                title = "Block saved contacts",
                subtitle = "Reject numbers already in your address book",
                icon = Icons.Outlined.ContactPhone,
                checked = blockSavedOn,
                onCheckedChange = onToggleBlockSaved
            )
        }

        item {
            Text(
                "Number series",
                color = TextMain,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                "Calls starting with these digits are screened",
                color = TextDim,
                fontSize = 12.sp
            )
        }

        item {
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
                    leadingIcon = {
                        Icon(Icons.Outlined.Tag, contentDescription = null, tint = TextDim)
                    },
                    placeholder = { Text("e.g. 0300") },
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
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
        }

        if (rules.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Outlined.Tag, contentDescription = null, tint = TextDim, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("No series yet", color = TextMain, fontWeight = FontWeight.Medium)
                    Text("Add a prefix to block matching callers", color = TextDim, fontSize = 13.sp)
                }
            }
        } else {
            items(rules, key = { it.prefix }) { rule ->
                PrefixRow(
                    rule = rule,
                    onToggle = { onTogglePrefix(rule.prefix, it) },
                    onDelete = { pendingDelete = rule }
                )
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
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
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (rule.enabled) Accent.copy(alpha = 0.18f) else Color(0xFF1E2430)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Tag,
                contentDescription = null,
                tint = if (rule.enabled) Accent else TextDim,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                rule.prefix,
                color = if (rule.enabled) TextMain else TextDim,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                if (rule.enabled) "Active" else "Paused",
                color = TextDim,
                fontSize = 12.sp
            )
        }
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
private fun SettingToggleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
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
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextDim, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Accent
            )
        )
    }
}

@Composable
private fun BlockedTab(
    padding: PaddingValues,
    blockedCalls: List<BlockedCall>,
    onClearLog: () -> Unit
) {
    val entries = remember(blockedCalls) { BlockedTimeFormat.buildListEntries(blockedCalls) }
    val nowMillis = remember { System.currentTimeMillis() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Blocked",
                    color = TextMain,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (blockedCalls.isNotEmpty()) {
                    Text(
                        "${blockedCalls.size} in log",
                        color = TextDim,
                        fontSize = 13.sp
                    )
                }
            }
            if (blockedCalls.isNotEmpty()) {
                TextButton(onClick = onClearLog) {
                    Text("Clear log", color = Accent)
                }
            }
        }

        if (blockedCalls.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhoneDisabled,
                        contentDescription = null,
                        tint = TextDim,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text("No blocked calls", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text("Blocked numbers appear here with time", color = TextDim, fontSize = 13.sp)
            }
        } else {
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    entries,
                    key = { entry ->
                        when (entry) {
                            is BlockedListEntry.Header -> "h-${entry.label}"
                            is BlockedListEntry.Item -> "${entry.call.atMillis}-${entry.call.number}"
                        }
                    }
                ) { entry ->
                    when (entry) {
                        is BlockedListEntry.Header -> BlockedDayHeader(entry.label)
                        is BlockedListEntry.Item -> BlockedCallRow(entry.call, nowMillis)
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockedDayHeader(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.CalendarToday,
            contentDescription = null,
            tint = Accent,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            color = TextMain,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun BlockedCallRow(call: BlockedCall, nowMillis: Long) {
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
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Off.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.PhoneDisabled,
                contentDescription = null,
                tint = Off,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                call.number,
                color = TextMain,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "Matched ${call.matchedPrefix}",
                color = TextDim,
                fontSize = 12.sp
            )
        }
        Text(
            BlockedTimeFormat.formatBlockedAt(call.atMillis, nowMillis),
            color = TextDim,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
