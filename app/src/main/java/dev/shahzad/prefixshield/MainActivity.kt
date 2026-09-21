package dev.shahzad.prefixshield

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.SharedPreferences
import android.net.Uri
import android.database.ContentObserver
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Settings
import android.speech.RecognizerIntent
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import dev.shahzad.prefixshield.incall.AppForeground
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val app get() = application as PrefixShieldApp

    private var screeningEnabled by mutableStateOf(false)
    private var dialerEnabled by mutableStateOf(false)
    private var blockingOn by mutableStateOf(true)
    private var blockSavedOn by mutableStateOf(true)
    private var phoneAccessEnabled by mutableStateOf(false)
    private var canReadCallLog by mutableStateOf(false)
    private var canReadContacts by mutableStateOf(false)
    private var promptDefaultDialer by mutableStateOf(false)
    private var contacts by mutableStateOf(listOf<DeviceContact>())
    private var contactAccounts by mutableStateOf(listOf<ContactAccount>())
    private var requestedPermsOnce = false
    private var prefixInput by mutableStateOf("")
    private var rules by mutableStateOf(listOf<PrefixRule>())
    private var blockedCalls by mutableStateOf(listOf<BlockedCall>())
    private var phoneLogs by mutableStateOf(listOf<PhoneLogEntry>())
    private var totalBlockedCount by mutableIntStateOf(0)
    private var spokenQuery by mutableStateOf("")
    private var pendingCallNumber: String? = null
    private var pendingDialDigits by mutableStateOf("")
    private var requestingDialerRole = false
    private var updateBusy by mutableStateOf(false)
    private var updateRelease by mutableStateOf<AppRelease?>(null)
    private var showUpdateDialog by mutableStateOf(false)
    private var checkedUpdateOnce = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private val refreshDebounced = Runnable { refresh() }

    private val roleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        refresh()
        if (isDialerHeld()) {
            requestingDialerRole = false
            Toast.makeText(this, "Default phone app on", Toast.LENGTH_SHORT).show()
        } else if (requestingDialerRole) {
            requestingDialerRole = false
            if (result.resultCode != Activity.RESULT_OK) {
                openDialerFallback()
            }
        } else if (isCallScreeningHeld()) {
            Toast.makeText(this, "Screening on", Toast.LENGTH_SHORT).show()
        }
    }

    private val logListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        scheduleRefresh()
    }

    private val callLogObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            scheduleRefresh()
        }
    }

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refresh() }

    private val callPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val number = pendingCallNumber
        pendingCallNumber = null
        if (number != null) placeCall(number, granted)
    }

    private val micPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchVoiceSearch()
        else Toast.makeText(this, "Mic permission needed", Toast.LENGTH_SHORT).show()
    }

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()
        if (spoken.isNotEmpty()) spokenQuery = spoken
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refresh()
        consumeTelIntent(intent)
        app.blockedLogStore.register(logListener)
        contentResolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, callLogObserver)
        contentResolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, callLogObserver)
        setContent {
            MaterialTheme(colorScheme = appColors) {
                DialerApp(
                    screeningEnabled = screeningEnabled,
                    dialerEnabled = dialerEnabled,
                    blockingOn = blockingOn,
                    blockSavedOn = blockSavedOn,
                    phoneAccessEnabled = phoneAccessEnabled,
                    prefixInput = prefixInput,
                    onPrefixInputChange = { prefixInput = it },
                    rules = rules,
                    blockedCalls = blockedCalls,
                    phoneLogs = phoneLogs,
                    contacts = contacts,
                    contactAccounts = contactAccounts,
                    canReadCallLog = canReadCallLog,
                    canReadContacts = canReadContacts,
                    promptDefaultDialer = promptDefaultDialer,
                    totalBlockedCount = totalBlockedCount,
                    spokenQuery = spokenQuery,
                    pendingDialDigits = pendingDialDigits,
                    onPendingDialConsumed = { pendingDialDigits = "" },
                    onSpokenQueryConsumed = { spokenQuery = "" },
                    onEnableScreening = ::requestCallScreeningRole,
                    onEnableDialer = ::requestDialerRole,
                    onSnoozeDialerPrompt = {
                        BlockSettings(this).snoozeDialerPrompt()
                        promptDefaultDialer = false
                    },
                    onToggleBlocking = ::toggleBlocking,
                    onToggleBlockSaved = ::toggleBlockSaved,
                    onGrantPhoneAccess = ::requestPhoneAccess,
                    onVoiceSearch = ::requestVoiceSearch,
                    onPlaceCall = ::requestPlaceCall,
                    onCopyNumber = ::copyNumber,
                    onBlockNumber = ::blockNumber,
                    onUnblockNumber = ::unblockNumber,
                    onDeleteLog = ::deleteLog,
                    onClearHistory = ::clearHistory,
                    onSaveContact = ::saveContact,
                    onUpdateContact = ::updateContact,
                    onDeleteContact = ::deleteContact,
                    onToggleContactStar = ::toggleContactStar,
                    onAddPrefix = ::addPrefix,
                    onTogglePrefix = { prefix, enabled ->
                        rules = app.prefixStore.toggle(prefix, enabled)
                    },
                    onRemovePrefix = { prefix ->
                        rules = app.prefixStore.remove(prefix)
                    },
                    appVersion = AppUpdate.installedName(this),
                    appVersionCode = AppUpdate.installedCode(this),
                    updateBusy = updateBusy,
                    updateRelease = updateRelease,
                    showUpdateDialog = showUpdateDialog,
                    onCheckUpdate = { checkForUpdate(false) },
                    onInstallUpdate = ::installUpdate,
                    onDismissUpdate = { showUpdateDialog = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        clearUnseenNotes()
        consumeTelIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        AppForeground.onResume()
        clearUnseenNotes()
        refresh()
        if (!checkedUpdateOnce) {
            checkedUpdateOnce = true
            checkForUpdate(true)
        }
    }

    override fun onPause() {
        AppForeground.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(refreshDebounced)
        app.blockedLogStore.unregister(logListener)
        contentResolver.unregisterContentObserver(callLogObserver)
        super.onDestroy()
    }

    private fun scheduleRefresh() {
        mainHandler.removeCallbacks(refreshDebounced)
        mainHandler.postDelayed(refreshDebounced, 300)
    }

    private fun refresh() {
        screeningEnabled = isCallScreeningHeld()
        dialerEnabled = isDialerHeld()
        blockingOn = BlockSettings(this).isBlockingEnabled()
        blockSavedOn = BlockSettings(this).blockSavedNumbers()
        phoneAccessEnabled = hasPhoneAccess()
        canReadCallLog = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED
        rules = app.prefixStore.list()
        blockedCalls = app.blockedLogStore.list()
        phoneLogs = PhoneLogStore.list(this)
        contacts = ContactStore.list(this)
        contactAccounts = ContactStore.accounts(this)
        canReadContacts = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        promptDefaultDialer = !dialerEnabled && BlockSettings(this).shouldPromptDialer()
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

    private fun isDialerHeld(): Boolean {
        val roleManager = getSystemService(RoleManager::class.java)
        return roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
    }

    private fun requestDialerRole() {
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
            Toast.makeText(this, "Already the default phone app", Toast.LENGTH_SHORT).show()
            return
        }
        requestPhoneAccess()
        if (!roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
            openDialerFallback()
            return
        }
        requestingDialerRole = true
        try {
            roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER))
        } catch (_: Exception) {
            requestingDialerRole = false
            openDialerFallback()
        }
    }

    private fun openDialerFallback() {
        try {
            startActivity(
                Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).putExtra(
                    TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                    packageName
                )
            )
            return
        } catch (_: Exception) {
        }
        try {
            startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        } catch (_: Exception) {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
            )
        }
        Toast.makeText(
            this,
            "If it was denied, App info → ⋮ → Allow restricted settings",
            Toast.LENGTH_LONG
        ).show()
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
            if (!isCallScreeningHeld() && !isDialerHeld()) {
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

    private fun requestPlaceCall(number: String) {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            placeCall(number, true)
        } else {
            pendingCallNumber = number
            callPermLauncher.launch(Manifest.permission.CALL_PHONE)
        }
    }

    private fun consumeTelIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme.equals("tel", ignoreCase = true)) {
            val number = data.schemeSpecificPart.orEmpty()
            if (number.isBlank()) return
            if (intent.action == Intent.ACTION_CALL) {
                requestPlaceCall(number)
            } else {
                pendingDialDigits = number
            }
        }
    }

    private fun placeCall(number: String, canCall: Boolean) {
        val digits = NumberMatcher.dialable(number).ifBlank {
            number.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
        }
        if (digits.isBlank()) {
            Toast.makeText(this, "No number to call", Toast.LENGTH_SHORT).show()
            return
        }
        val tel = Uri.fromParts("tel", digits, null)
        if (isDialerHeld()) {
            try {
                val telecom = getSystemService(TelecomManager::class.java)
                val extras = Bundle().apply {
                    putInt(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, VideoProfile.STATE_AUDIO_ONLY)
                }
                telecom.placeCall(tel, extras)
                return
            } catch (_: Exception) {
            }
        }
        val action = if (canCall) Intent.ACTION_CALL else Intent.ACTION_DIAL
        try {
            startActivity(Intent(action, tel))
        } catch (_: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_DIAL, tel))
            } catch (_: Exception) {
                Toast.makeText(this, "No phone app found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkForUpdate(silent: Boolean) {
        if (AppUpdate.isNewer(updateRelease, AppUpdate.installedCode(this)) && !silent) {
            showUpdateDialog = true
            return
        }
        if (updateBusy) return
        updateBusy = true
        AppUpdate.fetch { result ->
            updateBusy = false
            result.fold(
                onSuccess = { release ->
                    updateRelease = release
                    val newer = AppUpdate.isNewer(release, AppUpdate.installedCode(this))
                    if (newer) showUpdateDialog = true
                    else if (!silent) {
                        Toast.makeText(this, "You're up to date", Toast.LENGTH_SHORT).show()
                    }
                },
                onFailure = {
                    if (!silent) Toast.makeText(this, "Could not check for update", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun installUpdate() {
        val url = updateRelease?.apkUrl?.takeIf { it.isNotBlank() } ?: run {
            Toast.makeText(this, "No update file published yet", Toast.LENGTH_SHORT).show()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            Toast.makeText(this, "Allow installs from this app, then tap update again", Toast.LENGTH_LONG).show()
            runCatching {
                startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                )
            }.onFailure {
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                )
            }
            return
        }
        updateBusy = true
        AppUpdate.download(this, url) { result ->
            updateBusy = false
            result.fold(
                onSuccess = { file ->
                    runCatching { AppUpdate.install(this, file) }.onFailure {
                        Toast.makeText(this, "Could not open installer", Toast.LENGTH_SHORT).show()
                    }
                },
                onFailure = {
                    Toast.makeText(this, "Download failed", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun requestVoiceSearch() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) launchVoiceSearch() else micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun launchVoiceSearch() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Search calls")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Voice search not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyNumber(number: String) {
        val text = number.ifBlank { return }
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("number", text))
        Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
    }

    private fun blockNumber(number: String) {
        try {
            rules = app.prefixStore.add(number)
            Toast.makeText(this, "Blocked series added", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, "Need 3+ digits to block", Toast.LENGTH_SHORT).show()
        }
    }

    private fun unblockNumber(number: String) {
        val matched = NumberMatcher.matchingPrefix(number, rules)
        if (matched == null) {
            Toast.makeText(this, "No matching series", Toast.LENGTH_SHORT).show()
            return
        }
        rules = app.prefixStore.remove(matched)
        Toast.makeText(this, "Unblocked $matched", Toast.LENGTH_SHORT).show()
    }

    private fun saveContact(name: String, number: String, account: ContactAccount) {
        val ok = ContactStore.insert(this, name, number, account)
        Toast.makeText(this, if (ok) "Contact saved" else "Could not save contact", Toast.LENGTH_SHORT).show()
        refresh()
    }

    private fun updateContact(contact: DeviceContact, name: String, number: String) {
        val ok = ContactStore.update(this, contact.contactId, name, number)
        Toast.makeText(this, if (ok) "Contact updated" else "Could not update", Toast.LENGTH_SHORT).show()
        refresh()
    }

    private fun deleteContact(contact: DeviceContact) {
        val ok = ContactStore.delete(this, contact.contactId)
        Toast.makeText(this, if (ok) "Contact deleted" else "Could not delete", Toast.LENGTH_SHORT).show()
        refresh()
    }

    private fun toggleContactStar(contact: DeviceContact) {
        ContactStore.setStarred(this, contact.contactId, !contact.starred)
        refresh()
    }

    private fun deleteLog(entry: DialLogEntry) {
        entry.callLogId?.let { PhoneLogStore.delete(this, it) }
        app.blockedLogStore.removeMatching(entry.number, entry.atMillis)
        refresh()
    }

    private fun clearHistory() {
        PhoneLogStore.clear(this)
        app.blockedLogStore.clear()
        PendingNotifications(this).clear()
        BlockedNotifier.cancel(this)
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
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.GET_ACCOUNTS,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_NUMBERS
        )
        if (Build.VERSION.SDK_INT >= 33) {
            perms += Manifest.permission.POST_NOTIFICATIONS
        }
        return perms.toTypedArray()
    }
}
