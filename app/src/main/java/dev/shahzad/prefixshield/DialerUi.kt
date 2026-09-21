@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package dev.shahzad.prefixshield

import android.provider.CallLog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.CallMade
import androidx.compose.material.icons.automirrored.outlined.CallMissed
import androidx.compose.material.icons.automirrored.outlined.CallReceived
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContactPhone
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneDisabled
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val Bg = Color(0xFFF2F2F7)
internal val Surface = Color(0xFFFFFFFF)
internal val Line = Color(0xFFC6C6C8)
internal val TextMain = Color(0xFF000000)
internal val TextDim = Color(0xFF8E8E93)
internal val Fill = Color(0xFFE5E5EA)
internal val Accent = Color(0xFF007AFF)
internal val On = Color(0xFF34C759)
internal val Off = Color(0xFFFF3B30)

internal val appColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    background = Bg,
    surface = Surface,
    onBackground = TextMain,
    onSurface = TextMain
)

@Composable
fun DialerApp(
    screeningEnabled: Boolean,
    dialerEnabled: Boolean,
    blockingOn: Boolean,
    blockSavedOn: Boolean,
    phoneAccessEnabled: Boolean,
    prefixInput: String,
    onPrefixInputChange: (String) -> Unit,
    rules: List<PrefixRule>,
    blockedCalls: List<BlockedCall>,
    phoneLogs: List<PhoneLogEntry>,
    contacts: List<DeviceContact>,
    contactAccounts: List<ContactAccount>,
    canReadCallLog: Boolean,
    canReadContacts: Boolean,
    promptDefaultDialer: Boolean,
    totalBlockedCount: Int,
    spokenQuery: String,
    pendingDialDigits: String,
    onPendingDialConsumed: () -> Unit,
    onSpokenQueryConsumed: () -> Unit,
    onEnableScreening: () -> Unit,
    onEnableDialer: () -> Unit,
    onSnoozeDialerPrompt: () -> Unit,
    onToggleBlocking: (Boolean) -> Unit,
    onToggleBlockSaved: (Boolean) -> Unit,
    onGrantPhoneAccess: () -> Unit,
    onVoiceSearch: () -> Unit,
    onPlaceCall: (String) -> Unit,
    onCopyNumber: (String) -> Unit,
    onBlockNumber: (String) -> Unit,
    onUnblockNumber: (String) -> Unit,
    onDeleteLog: (List<DialLogEntry>) -> Unit,
    onClearHistory: () -> Unit,
    onSaveContact: (ContactDraft) -> Unit,
    onUpdateContact: (DeviceContact, ContactDraft) -> Unit,
    onDeleteContact: (DeviceContact) -> Unit,
    onToggleContactStar: (DeviceContact) -> Unit,
    onAddPrefix: () -> Unit,
    onTogglePrefix: (String, Boolean) -> Unit,
    onRemovePrefix: (String) -> Unit,
    appVersion: String,
    appVersionCode: Long,
    updateBusy: Boolean,
    updateRelease: AppRelease?,
    showUpdateDialog: Boolean,
    onCheckUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onDismissUpdate: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var settingsBack by remember { mutableStateOf("Recents") }
    var showDefaultPrompt by remember { mutableStateOf(false) }
    var showDialer by remember { mutableStateOf(false) }
    var selectedLog by remember { mutableStateOf<DialLogEntry?>(null) }
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(LogFilter.ALL) }

    LaunchedEffect(spokenQuery) {
        if (spokenQuery.isNotBlank()) {
            search = spokenQuery
            onSpokenQueryConsumed()
        }
    }
    LaunchedEffect(pendingDialDigits) {
        if (pendingDialDigits.isNotBlank()) {
            tab = 0
            showDialer = true
        }
    }
    BackHandler(enabled = selectedLog != null && !showSettings) { selectedLog = null }
    BackHandler(enabled = showDialer && selectedLog == null && !showSettings) { showDialer = false }
    BackHandler(enabled = showSettings) { showSettings = false }
    LaunchedEffect(dialerEnabled, promptDefaultDialer) {
        if (!dialerEnabled && promptDefaultDialer) {
            kotlinx.coroutines.delay(700)
            showDefaultPrompt = true
        } else {
            showDefaultPrompt = false
        }
    }

    val logs = remember(phoneLogs, blockedCalls) {
        DialLogMerger.merge(phoneLogs, blockedCalls)
    }
    val visibleLogs = remember(logs, search, filter) {
        logs.filter {
            DialLogMerger.matchesFilter(it, filter) && DialLogMerger.matchesQuery(it, search)
        }
    }

    val navColors = NavigationBarItemDefaults.colors(
        selectedIconColor = Accent,
        selectedTextColor = Accent,
        unselectedIconColor = TextDim,
        unselectedTextColor = TextDim,
        indicatorColor = Fill
    )

    Box(modifier = Modifier.fillMaxSize().background(Bg)) {
        Scaffold(
            containerColor = Bg,
            bottomBar = {
                if (!showSettings) {
                    NavigationBar(containerColor = Surface, contentColor = TextMain) {
                        NavigationBarItem(
                            selected = tab == 0,
                            onClick = { tab = 0 },
                            icon = { Icon(Icons.Outlined.History, contentDescription = null) },
                            label = { Text("Recents") },
                            colors = navColors
                        )
                        NavigationBarItem(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            icon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                            label = { Text("Contacts") },
                            colors = navColors
                        )
                    }
                }
            }
        ) { padding ->
            if (showSettings) {
                SettingsScreen(
                    padding = padding,
                    screeningEnabled = screeningEnabled,
                    dialerEnabled = dialerEnabled,
                    blockingOn = blockingOn,
                    blockSavedOn = blockSavedOn,
                    phoneAccessEnabled = phoneAccessEnabled,
                    prefixInput = prefixInput,
                    onPrefixInputChange = onPrefixInputChange,
                    rules = rules,
                    totalBlockedCount = totalBlockedCount,
                    backLabel = settingsBack,
                    onBack = { showSettings = false },
                    onClearHistory = onClearHistory,
                    onEnableScreening = onEnableScreening,
                    onEnableDialer = onEnableDialer,
                    onToggleBlocking = onToggleBlocking,
                    onToggleBlockSaved = onToggleBlockSaved,
                    onGrantPhoneAccess = onGrantPhoneAccess,
                    onAddPrefix = onAddPrefix,
                    onTogglePrefix = onTogglePrefix,
                    onRemovePrefix = onRemovePrefix,
                    appVersion = appVersion,
                    appVersionCode = appVersionCode,
                    updateBusy = updateBusy,
                    updateRelease = updateRelease,
                    onCheckUpdate = onCheckUpdate
                )
            } else if (selectedLog != null) {
                CallDetailPage(
                    padding = padding,
                    entry = selectedLog!!,
                    logs = logs,
                    blockedBySeries = NumberMatcher.matchingPrefix(selectedLog!!.number, rules) != null,
                    onBack = { selectedLog = null },
                    onPlaceCall = onPlaceCall,
                    onCopyNumber = onCopyNumber,
                    onBlockNumber = onBlockNumber,
                    onUnblockNumber = onUnblockNumber,
                    onDeleteLog = {
                        onDeleteLog(listOf(it))
                        selectedLog = null
                    }
                )
            } else when (tab) {
                0 -> HomeRecentsTab(
                    padding = padding,
                    search = search,
                    onSearchChange = { search = it },
                    filter = filter,
                    onFilterChange = { filter = it },
                    logs = visibleLogs,
                    rules = rules,
                    canRead = canReadCallLog,
                    onGrantAccess = onGrantPhoneAccess,
                    onVoiceSearch = onVoiceSearch,
                    onOpenSettings = {
                        settingsBack = "Recents"
                        showSettings = true
                    },
                    onPlaceCall = onPlaceCall,
                    onCopyNumber = onCopyNumber,
                    onBlockNumber = onBlockNumber,
                    onUnblockNumber = onUnblockNumber,
                    onDeleteLog = onDeleteLog,
                    onOpenDetail = { selectedLog = it }
                )
                else -> ContactsTab(
                    padding = padding,
                    contacts = contacts,
                    accounts = contactAccounts,
                    canRead = canReadContacts,
                    onGrantAccess = onGrantPhoneAccess,
                    onPlaceCall = onPlaceCall,
                    onSave = onSaveContact,
                    onUpdate = onUpdateContact,
                    onDelete = onDeleteContact,
                    onToggleStar = onToggleContactStar,
                    onBlock = onBlockNumber,
                    onOpenSettings = {
                        settingsBack = "Contacts"
                        showSettings = true
                    }
                )
            }
        }

        if (!showSettings && tab == 0 && !showDialer && selectedLog == null) {
            FloatingActionButton(
                onClick = { showDialer = true },
                containerColor = Accent,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 84.dp)
            ) {
                Icon(Icons.Outlined.Dialpad, contentDescription = "Dial", modifier = Modifier.size(28.dp))
            }
        }

        AnimatedVisibility(
            visible = showDialer,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(160))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.32f))
                        .clickable { showDialer = false }
                )
                DialPadSheet(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    seedDigits = pendingDialDigits,
                    onSeedConsumed = onPendingDialConsumed,
                    contacts = contacts,
                    logs = phoneLogs,
                    onPlaceCall = { number ->
                        showDialer = false
                        onPlaceCall(number)
                    },
                    onClose = { showDialer = false }
                )
            }
        }

        if (showUpdateDialog && AppUpdate.isNewer(updateRelease, appVersionCode)) {
            AlertDialog(
                onDismissRequest = onDismissUpdate,
                containerColor = Surface,
                title = { Text("Software Update", color = TextMain, fontWeight = FontWeight.SemiBold) },
                text = {
                    Column {
                        Text(
                            "myPhone ${updateRelease?.versionName.orEmpty()} is available.",
                            color = TextMain,
                            fontSize = 15.sp
                        )
                        val notes = updateRelease?.notes.orEmpty()
                        if (notes.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(notes, color = TextDim, fontSize = 14.sp)
                        }
                        if (updateBusy) {
                            Spacer(Modifier.height(10.dp))
                            Text("Downloading…", color = Accent, fontSize = 14.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = onInstallUpdate, enabled = !updateBusy) {
                        Text(if (updateBusy) "Please wait" else "Download and Install", color = Accent)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissUpdate) { Text("Later", color = TextDim) }
                }
            )
        }
        if (showDefaultPrompt && !dialerEnabled && !showSettings) {
            AlertDialog(
                onDismissRequest = {
                    showDefaultPrompt = false
                    onSnoozeDialerPrompt()
                },
                containerColor = Surface,
                title = { Text("Set myPhone as default", color = TextMain) },
                text = {
                    Text(
                        "myPhone needs to be the default Phone app for accept, hang up, and in-call controls. If Google says the app was denied, open App info → ⋮ → Allow restricted settings, then try again.",
                        color = TextDim
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showDefaultPrompt = false
                        onSnoozeDialerPrompt()
                        onEnableDialer()
                    }) { Text("Set default", color = Accent) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDefaultPrompt = false
                        onSnoozeDialerPrompt()
                    }) { Text("Later", color = TextDim) }
                }
            )
        }
    }
}

@Composable
private fun RecentMenuItem(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(label, color = tint, fontSize = 16.sp) },
        leadingIcon = {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        },
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun HomeRecentsTab(
    padding: PaddingValues,
    search: String,
    onSearchChange: (String) -> Unit,
    filter: LogFilter,
    onFilterChange: (LogFilter) -> Unit,
    logs: List<DialLogEntry>,
    rules: List<PrefixRule>,
    canRead: Boolean,
    onGrantAccess: () -> Unit,
    onVoiceSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onPlaceCall: (String) -> Unit,
    onCopyNumber: (String) -> Unit,
    onBlockNumber: (String) -> Unit,
    onUnblockNumber: (String) -> Unit,
    onDeleteLog: (List<DialLogEntry>) -> Unit,
    onOpenDetail: (DialLogEntry) -> Unit
) {
    val nowMillis = remember(logs) { System.currentTimeMillis() }
    val bursts = remember(logs) { DialLogMerger.groupBursts(logs) }
    val grouped = remember(bursts) { BlockedTimeFormat.groupByDay(bursts, timeOf = { it.latest.atMillis }) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            "Recents",
            color = TextMain,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVoiceSearch) {
                Icon(Icons.Outlined.Mic, contentDescription = "Voice search", tint = Accent)
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Fill)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = TextDim, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = search,
                    onValueChange = onSearchChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(color = TextMain, fontSize = 15.sp),
                    cursorBrush = SolidColor(Accent),
                    decorationBox = { inner ->
                        if (search.isEmpty()) {
                            Text("Search calls", color = TextDim, fontSize = 15.sp)
                        }
                        inner()
                    }
                )
                if (search.isNotEmpty()) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Clear",
                        tint = TextDim,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onSearchChange("") }
                    )
                }
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = Accent)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LogFilter.entries.forEach { item ->
                val selected = filter == item
                FilterChip(
                    selected = selected,
                    onClick = { onFilterChange(item) },
                    label = {
                        Text(
                            when (item) {
                                LogFilter.ALL -> "All"
                                LogFilter.INCOMING -> "Incoming"
                                LogFilter.OUTGOING -> "Outgoing"
                                LogFilter.MISSED -> "Missed"
                                LogFilter.BLOCKED -> "Blocked"
                            }
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Surface,
                        labelColor = TextDim,
                        selectedContainerColor = Accent.copy(alpha = 0.22f),
                        selectedLabelColor = Accent
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = Line,
                        selectedBorderColor = Accent.copy(alpha = 0.5f)
                    )
                )
            }
        }

        if (!canRead) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Outlined.History, contentDescription = null, tint = TextDim, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(12.dp))
                Text("Call log permission needed", color = TextMain, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onGrantAccess,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White)
                ) { Text("Allow") }
            }
        } else if (logs.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Outlined.History, contentDescription = null, tint = TextDim, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(12.dp))
                Text("No calls", color = TextMain, fontWeight = FontWeight.Medium)
                Text("History shows up after calls", color = TextDim, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(
                    grouped,
                    key = { entry ->
                        when (entry) {
                            is DayListEntry.Header -> "h-${entry.label}"
                            is DayListEntry.Item -> entry.value.key
                        }
                    }
                ) { entry ->
                    when (entry) {
                        is DayListEntry.Header -> {
                            Text(
                                entry.label,
                                color = TextDim,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp)
                            )
                        }
                        is DayListEntry.Item -> {
                            val burst = entry.value
                            DialLogRow(
                                entry = burst.latest,
                                repeatCount = burst.count,
                                nowMillis = nowMillis,
                                blockedBySeries = NumberMatcher.matchingPrefix(burst.latest.number, rules) != null,
                                onPlaceCall = onPlaceCall,
                                onCopyNumber = onCopyNumber,
                                onBlockNumber = onBlockNumber,
                                onUnblockNumber = onUnblockNumber,
                                onDeleteLog = { onDeleteLog(burst.calls) },
                                onOpenDetail = onOpenDetail
                            )
                            HorizontalDivider(
                                color = Line.copy(alpha = 0.7f),
                                modifier = Modifier.padding(start = 76.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialLogRow(
    entry: DialLogEntry,
    repeatCount: Int = 1,
    nowMillis: Long,
    blockedBySeries: Boolean,
    onPlaceCall: (String) -> Unit,
    onCopyNumber: (String) -> Unit,
    onBlockNumber: (String) -> Unit,
    onUnblockNumber: (String) -> Unit,
    onDeleteLog: () -> Unit,
    onOpenDetail: (DialLogEntry) -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    val typeColor = when {
        entry.blocked -> Off
        entry.type == CallLog.Calls.MISSED_TYPE || entry.type == CallLog.Calls.REJECTED_TYPE -> Off
        entry.type == CallLog.Calls.OUTGOING_TYPE -> Accent
        else -> On
    }
    val typeIcon = when {
        entry.blocked -> Icons.Outlined.PhoneDisabled
        entry.type == CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Outlined.CallMade
        entry.type == CallLog.Calls.MISSED_TYPE -> Icons.AutoMirrored.Outlined.CallMissed
        entry.type == CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Outlined.CallReceived
        else -> Icons.Outlined.Call
    }
    val initial = entry.title.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
    val countOnTitle = repeatCount > 1 && (entry.name.isNullOrBlank() || entry.number.isBlank())
    val details = buildList {
        add(entry.typeLabel)
        entry.durationLabel?.let { add(it) }
        if (entry.blocked && !entry.matchedPrefix.isNullOrBlank()) {
            add("series ${entry.matchedPrefix}")
        } else if (!entry.name.isNullOrBlank() && entry.number.isNotBlank()) {
            add(entry.number)
        }
    }.joinToString("  ·  ")

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onOpenDetail(entry) },
                    onLongClick = { menu = true }
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (entry.blocked) Off.copy(alpha = 0.14f) else Accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(initial, color = if (entry.blocked) Off else Accent, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        entry.title,
                        color = if (entry.blocked || entry.type == CallLog.Calls.MISSED_TYPE) Off else TextMain,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (countOnTitle) {
                        Text(
                            " ($repeatCount)",
                            color = if (entry.blocked || entry.type == CallLog.Calls.MISSED_TYPE) Off else TextMain,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(typeIcon, contentDescription = null, tint = typeColor, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        details,
                        color = TextDim,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!countOnTitle && repeatCount > 1) {
                        Text(" ($repeatCount)", color = TextDim, fontSize = 12.sp)
                    }
                }
            }
            Text(
                BlockedTimeFormat.formatBlockedAt(entry.atMillis, nowMillis),
                color = TextDim,
                fontSize = 12.sp
            )
            IconButton(onClick = { if (entry.number.isNotBlank()) onPlaceCall(entry.number) }) {
                Icon(Icons.Outlined.Call, contentDescription = "Call", tint = Accent)
            }
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 16.dp)
        ) {
            DropdownMenu(
                expanded = menu,
                onDismissRequest = { menu = false },
                offset = DpOffset(0.dp, 6.dp),
                shape = RoundedCornerShape(14.dp),
                containerColor = Surface,
                shadowElevation = 12.dp
            ) {
                RecentMenuItem(
                    label = "Copy",
                    icon = Icons.Outlined.ContentCopy,
                    tint = TextMain
                ) {
                    menu = false
                    onCopyNumber(entry.number.ifBlank { entry.title })
                }
                RecentMenuItem(
                    label = if (blockedBySeries) "Unblock" else "Block",
                    icon = if (blockedBySeries) Icons.Outlined.Shield else Icons.Outlined.Block,
                    tint = if (blockedBySeries) On else Off
                ) {
                    menu = false
                    if (blockedBySeries) onUnblockNumber(entry.number) else onBlockNumber(entry.number)
                }
                RecentMenuItem(
                    label = "Delete",
                    icon = Icons.Outlined.Delete,
                    tint = Off
                ) {
                    menu = false
                    onDeleteLog()
                }
            }
        }
    }
}

@Composable
private fun CallDetailPage(
    padding: PaddingValues,
    entry: DialLogEntry,
    logs: List<DialLogEntry>,
    blockedBySeries: Boolean,
    onBack: () -> Unit,
    onPlaceCall: (String) -> Unit,
    onCopyNumber: (String) -> Unit,
    onBlockNumber: (String) -> Unit,
    onUnblockNumber: (String) -> Unit,
    onDeleteLog: (DialLogEntry) -> Unit
) {
    val nowMillis = remember(logs) { System.currentTimeMillis() }
    val related = remember(entry, logs) {
        logs.filter { sameCallerForDetails(entry, it) }.sortedByDescending { it.atMillis }
    }
    val lastCall = related.firstOrNull() ?: entry
    val totalDuration = related.sumOf { it.durationSec }
    val missedCount = related.count { !it.blocked && it.type == CallLog.Calls.MISSED_TYPE }
    val blockedCount = related.count { it.blocked }
    val subtitle = entry.number.ifBlank { entry.typeLabel }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(Bg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = Accent)
            }
            Text("Recents", color = Accent, fontSize = 17.sp)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(if (entry.blocked) Off.copy(alpha = 0.14f) else Accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    entry.title.firstOrNull()?.uppercaseChar()?.toString() ?: "#",
                    color = if (entry.blocked) Off else Accent,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                entry.title,
                color = if (entry.blocked) Off else TextMain,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(subtitle, color = TextDim, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DetailAction(
                    icon = Icons.Outlined.Call,
                    label = "call",
                    tint = On,
                    modifier = Modifier.weight(1f)
                ) { if (entry.number.isNotBlank()) onPlaceCall(entry.number) }
                DetailAction(
                    icon = Icons.Outlined.ContentCopy,
                    label = "copy",
                    tint = Accent,
                    modifier = Modifier.weight(1f)
                ) { onCopyNumber(entry.number.ifBlank { entry.title }) }
                DetailAction(
                    icon = if (blockedBySeries) Icons.Outlined.Shield else Icons.Outlined.Block,
                    label = if (blockedBySeries) "unblock" else "block",
                    tint = if (blockedBySeries) On else Off,
                    modifier = Modifier.weight(1f)
                ) {
                    if (blockedBySeries) onUnblockNumber(entry.number) else onBlockNumber(entry.number)
                }
            }
            Spacer(Modifier.height(18.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface)
            ) {
                InfoLine("Last call", "${lastCall.typeLabel} · ${BlockedTimeFormat.formatBlockedAt(lastCall.atMillis, nowMillis)}")
                HorizontalDivider(color = Line.copy(alpha = 0.7f), modifier = Modifier.padding(start = 16.dp))
                InfoLine("Calls", related.size.toString())
                if (totalDuration > 0) {
                    HorizontalDivider(color = Line.copy(alpha = 0.7f), modifier = Modifier.padding(start = 16.dp))
                    InfoLine("Total duration", formatDuration(totalDuration))
                }
                if (missedCount > 0) {
                    HorizontalDivider(color = Line.copy(alpha = 0.7f), modifier = Modifier.padding(start = 16.dp))
                    InfoLine("Missed", missedCount.toString(), valueColor = Off)
                }
                if (blockedCount > 0) {
                    HorizontalDivider(color = Line.copy(alpha = 0.7f), modifier = Modifier.padding(start = 16.dp))
                    InfoLine("Blocked", blockedCount.toString(), valueColor = Off)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Call History",
                color = TextMain,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface)
            ) {
                related.forEachIndexed { index, item ->
                    CallHistoryLine(
                        entry = item,
                        nowMillis = nowMillis,
                        onDelete = { onDeleteLog(item) }
                    )
                    if (index < related.lastIndex) {
                        HorizontalDivider(color = Line.copy(alpha = 0.7f), modifier = Modifier.padding(start = 54.dp))
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun DetailAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, color = Accent, fontSize = 12.sp)
    }
}

@Composable
private fun InfoLine(label: String, value: String, valueColor: Color = TextMain) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextDim, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(value, color = valueColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CallHistoryLine(
    entry: DialLogEntry,
    nowMillis: Long,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    val typeColor = when {
        entry.blocked -> Off
        entry.type == CallLog.Calls.MISSED_TYPE || entry.type == CallLog.Calls.REJECTED_TYPE -> Off
        entry.type == CallLog.Calls.OUTGOING_TYPE -> Accent
        else -> On
    }
    val typeIcon = when {
        entry.blocked -> Icons.Outlined.PhoneDisabled
        entry.type == CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Outlined.CallMade
        entry.type == CallLog.Calls.MISSED_TYPE -> Icons.AutoMirrored.Outlined.CallMissed
        entry.type == CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Outlined.CallReceived
        else -> Icons.Outlined.Call
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = { menu = true })
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(typeIcon, contentDescription = null, tint = typeColor, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.typeLabel, color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(
                listOfNotNull(
                    BlockedTimeFormat.formatBlockedAt(entry.atMillis, nowMillis),
                    entry.durationLabel
                ).joinToString(" · "),
                color = TextDim,
                fontSize = 13.sp
            )
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "More", tint = TextDim)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = Surface) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Delete, contentDescription = null, tint = Off, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Delete", color = Off)
                        }
                    },
                    onClick = {
                        menu = false
                        onDelete()
                    }
                )
            }
        }
    }
}

private fun sameCallerForDetails(seed: DialLogEntry, other: DialLogEntry): Boolean {
    val a = NumberMatcher.extractNumber(seed.number)
    val b = NumberMatcher.extractNumber(other.number)
    if (a.isNotBlank() && b.isNotBlank()) {
        if (a == b) return true
        val keep = minOf(10, a.length, b.length)
        if (keep >= 7 && a.takeLast(keep) == b.takeLast(keep)) return true
    }
    return seed.name?.takeIf { it.isNotBlank() } == other.name?.takeIf { it.isNotBlank() }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${secs}s"
        else -> "${secs}s"
    }
}

@Composable
private fun DialPadSheet(
    modifier: Modifier = Modifier,
    seedDigits: String,
    onSeedConsumed: () -> Unit,
    contacts: List<DeviceContact>,
    logs: List<PhoneLogEntry>,
    onPlaceCall: (String) -> Unit,
    onClose: () -> Unit
) {
    var digits by remember { mutableStateOf("") }
    LaunchedEffect(seedDigits) {
        if (seedDigits.isNotBlank()) {
            digits = seedDigits.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
            onSeedConsumed()
        }
    }
    val suggestions = remember(digits, contacts, logs) {
        ContactStore.suggestions(digits, contacts, logs)
    }
    val keys = listOf(
        Triple("1", "", ""),
        Triple("2", "ABC", ""),
        Triple("3", "DEF", ""),
        Triple("4", "GHI", ""),
        Triple("5", "JKL", ""),
        Triple("6", "MNO", ""),
        Triple("7", "PQRS", ""),
        Triple("8", "TUV", ""),
        Triple("9", "WXYZ", ""),
        Triple("*", "", ""),
        Triple("0", "+", ""),
        Triple("#", "", "")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Bg)
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = 8.dp)
                .size(width = 36.dp, height = 5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Fill)
                .clickable(onClick = onClose)
        )
        if (suggestions.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 168.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface)
            ) {
                suggestions.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlaceCall(item.primaryNumber) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.name,
                                color = TextMain,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(item.primaryNumber, color = TextDim, fontSize = 13.sp, maxLines = 1)
                        }
                        Icon(Icons.Outlined.Call, contentDescription = "Call", tint = On, modifier = Modifier.size(20.dp))
                    }
                    if (index < suggestions.lastIndex) {
                        HorizontalDivider(color = Line.copy(alpha = 0.55f), modifier = Modifier.padding(start = 14.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Text(
            text = digits.ifEmpty { " " },
            color = TextMain,
            fontSize = if (digits.length > 13) 26.sp else 34.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Spacer(Modifier.height(6.dp))
        keys.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { (digit, letters, _) ->
                    Box(
                        modifier = Modifier
                            .padding(vertical = 3.dp)
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Fill)
                            .combinedClickable(
                                onClick = { if (digits.length < 20) digits += digit },
                                onLongClick = {
                                    if (digit == "0") digits += "+" else if (digits.length < 20) digits += digit
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(digit, color = TextMain, fontSize = 26.sp, fontWeight = FontWeight.Medium)
                            if (letters.isNotEmpty()) {
                                Text(letters, color = TextDim, fontSize = 9.sp, letterSpacing = 1.sp)
                            }
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.size(68.dp))
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(On)
                    .clickable { if (digits.isNotBlank()) onPlaceCall(digits) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Call, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Box(
                modifier = Modifier.size(68.dp),
                contentAlignment = Alignment.Center
            ) {
                if (digits.isNotEmpty()) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Backspace,
                        contentDescription = "Delete",
                        tint = TextMain,
                        modifier = Modifier
                            .size(28.dp)
                            .combinedClickable(
                                onClick = { digits = digits.dropLast(1) },
                                onLongClick = { digits = "" }
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    padding: PaddingValues,
    screeningEnabled: Boolean,
    dialerEnabled: Boolean,
    blockingOn: Boolean,
    blockSavedOn: Boolean,
    phoneAccessEnabled: Boolean,
    prefixInput: String,
    onPrefixInputChange: (String) -> Unit,
    rules: List<PrefixRule>,
    totalBlockedCount: Int,
    backLabel: String,
    onBack: () -> Unit,
    onClearHistory: () -> Unit,
    onEnableScreening: () -> Unit,
    onEnableDialer: () -> Unit,
    onToggleBlocking: (Boolean) -> Unit,
    onToggleBlockSaved: (Boolean) -> Unit,
    onGrantPhoneAccess: () -> Unit,
    onAddPrefix: () -> Unit,
    onTogglePrefix: (String, Boolean) -> Unit,
    onRemovePrefix: (String) -> Unit,
    appVersion: String,
    appVersionCode: Long,
    updateBusy: Boolean,
    updateRelease: AppRelease?,
    onCheckUpdate: () -> Unit
) {
    val focus = LocalFocusManager.current
    var pendingDelete by remember { mutableStateOf<PrefixRule?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val activeRules = rules.count { it.enabled }
    val updateReady = AppUpdate.isNewer(updateRelease, appVersionCode)
    val blockingFooter = when {
        blockingOn && (screeningEnabled || dialerEnabled) ->
            "$activeRules active series · $totalBlockedCount blocked"
        blockingOn -> "Set the default phone app or call screening before blocking can run."
        else -> "Blocking is paused. Incoming calls are not filtered."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(Bg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = Accent)
            }
            Text(backLabel, color = Accent, fontSize = 17.sp)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Settings",
                color = TextMain,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            SettingsGroup(
                header = "Phone",
                footer = "myPhone needs to be the default phone app to place, answer, and silence calls."
            ) {
                SettingsNavRow(
                    icon = Icons.Outlined.Call,
                    iconBg = Accent,
                    title = "Default Phone App",
                    value = if (dialerEnabled) "On" else "Set Up",
                    onClick = if (dialerEnabled) null else onEnableDialer
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Outlined.Shield,
                    iconBg = On,
                    title = "Call Screening",
                    value = if (screeningEnabled) "On" else "Set Up",
                    onClick = if (screeningEnabled) null else onEnableScreening
                )
                if (!phoneAccessEnabled) {
                    SettingsDivider()
                    SettingsNavRow(
                        icon = Icons.Outlined.Notifications,
                        iconBg = Color(0xFFFF9500),
                        title = "Contacts, Calls and Alerts",
                        value = "Allow",
                        onClick = onGrantPhoneAccess
                    )
                }
            }
            SettingsGroup(header = "Blocking", footer = blockingFooter) {
                SettingsSwitchRow(
                    icon = Icons.Outlined.Security,
                    iconBg = if (blockingOn) Accent else TextDim,
                    title = "Protection",
                    checked = blockingOn,
                    onCheckedChange = onToggleBlocking
                )
                SettingsDivider()
                SettingsSwitchRow(
                    icon = Icons.Outlined.ContactPhone,
                    iconBg = Color(0xFFFF9500),
                    title = "Block Saved Contacts",
                    checked = blockSavedOn,
                    onCheckedChange = onToggleBlockSaved
                )
            }
            SettingsGroup(
                header = "Number Series",
                footer = "Only numbers that start with a series are blocked."
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = prefixInput,
                        onValueChange = onPrefixInputChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = TextStyle(color = TextMain, fontSize = 17.sp),
                        cursorBrush = SolidColor(Accent),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            onAddPrefix()
                            focus.clearFocus()
                        }),
                        decorationBox = { inner ->
                            Box {
                                if (prefixInput.isEmpty()) {
                                    Text("Add a series, e.g. 0300", color = TextDim, fontSize = 17.sp)
                                }
                                inner()
                            }
                        }
                    )
                    IconButton(onClick = {
                        onAddPrefix()
                        focus.clearFocus()
                    }) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add series", tint = Accent)
                    }
                }
                rules.forEach { rule ->
                    SettingsDivider(start = 16.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .padding(start = 16.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsIcon(Icons.Outlined.Tag, if (rule.enabled) Accent else TextDim)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            rule.prefix,
                            color = if (rule.enabled) TextMain else TextDim,
                            fontSize = 17.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = rule.enabled,
                            onCheckedChange = { onTogglePrefix(rule.prefix, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Accent
                            )
                        )
                        IconButton(onClick = { pendingDelete = rule }) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "Delete",
                                tint = Off,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            SettingsGroup(header = "General") {
                SettingsNavRow(
                    icon = Icons.Outlined.SystemUpdate,
                    iconBg = Accent,
                    title = "Software Update",
                    value = when {
                        updateBusy -> "Checking…"
                        updateReady -> updateRelease?.versionName ?: "Available"
                        else -> appVersion
                    },
                    valueColor = if (updateReady) Accent else TextDim,
                    onClick = onCheckUpdate
                )
            }
            SettingsGroup(header = "Recents") {
                Text(
                    "Clear Recents",
                    color = Off,
                    fontSize = 17.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { confirmClear = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                )
            }
            Text(
                "myPhone $appVersion",
                color = TextDim,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp, bottom = 28.dp)
            )
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
                }) { Text("Delete", color = Off) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel", color = TextDim) }
            }
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = Surface,
            title = { Text("Clear call history?", color = TextMain) },
            text = {
                Text(
                    "This removes phone call logs and blocked history on this device.",
                    color = TextDim
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onClearHistory()
                    confirmClear = false
                }) { Text("Clear", color = Off) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel", color = TextDim) }
            }
        )
    }
}

@Composable
private fun SettingsGroup(
    header: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (header != null) {
        Text(
            header.uppercase(),
            color = TextDim,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 22.dp, bottom = 7.dp)
        )
    } else {
        Spacer(Modifier.height(22.dp))
    }
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface),
        content = content
    )
    if (footer != null) {
        Text(
            footer,
            color = TextDim,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 7.dp)
        )
    }
}

@Composable
private fun SettingsDivider(start: androidx.compose.ui.unit.Dp = 58.dp) {
    HorizontalDivider(color = Line.copy(alpha = 0.7f), modifier = Modifier.padding(start = start))
}

@Composable
private fun SettingsIcon(icon: ImageVector, background: Color) {
    Box(
        modifier = Modifier
            .size(29.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    value: String,
    valueColor: Color = TextDim,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIcon(icon, iconBg)
        Spacer(Modifier.width(12.dp))
        Text(title, color = TextMain, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Text(value, color = valueColor, fontSize = 16.sp)
        if (onClick != null) {
            Spacer(Modifier.width(6.dp))
            Text("›", color = Color(0xFFC7C7CC), fontSize = 22.sp)
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(start = 16.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIcon(icon, iconBg)
        Spacer(Modifier.width(12.dp))
        Text(title, color = TextMain, fontSize = 17.sp, modifier = Modifier.weight(1f))
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
