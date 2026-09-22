package dev.shahzad.prefixshield

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun ContactsTab(
    padding: PaddingValues,
    contacts: List<DeviceContact>,
    accounts: List<ContactAccount>,
    canRead: Boolean,
    onGrantAccess: () -> Unit,
    onPlaceCall: (String) -> Unit,
    onSave: (ContactDraft) -> Unit,
    onUpdate: (DeviceContact, ContactDraft) -> Unit,
    onDelete: (DeviceContact) -> Unit,
    onToggleStar: (DeviceContact) -> Unit,
    onBlock: (String) -> Unit,
    prefillNumber: String = "",
    onPrefillConsumed: () -> Unit = {},
    onOpenSettings: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var editor by remember { mutableStateOf<DeviceContact?>(null) }
    var creating by remember { mutableStateOf(false) }
    var seedNumber by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf<DeviceContact?>(null) }

    LaunchedEffect(prefillNumber) {
        if (prefillNumber.isNotBlank()) {
            seedNumber = prefillNumber
            creating = true
            editor = null
            onPrefillConsumed()
        }
    }

    LaunchedEffect(contacts) {
        val current = detail ?: return@LaunchedEffect
        if (current.contactId == 0L) return@LaunchedEffect
        val fresh = contacts.find { it.contactId == current.contactId }
        if (fresh != null) detail = fresh
        else if (contacts.isNotEmpty()) detail = null
    }

    BackHandler(enabled = detail != null && editor == null && !creating) { detail = null }
    BackHandler(enabled = creating || editor != null) {
        creating = false
        editor = null
    }

    val visible = remember(contacts, query) {
        val q = query.trim()
        if (q.isEmpty()) contacts
        else contacts.filter {
            it.name.contains(q, ignoreCase = true) ||
                it.company.contains(q, ignoreCase = true) ||
                it.email.contains(q, ignoreCase = true) ||
                it.numbers.any { number -> number.contains(q) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        when {
            creating || editor != null -> ContactEditorPage(
                existing = editor,
                initialNumber = if (editor == null) seedNumber else "",
                accounts = accounts,
                onDismiss = {
                    creating = false
                    editor = null
                },
                onSave = { draft ->
                    val current = editor
                    if (current != null) onUpdate(current, draft) else onSave(draft)
                    creating = false
                    editor = null
                }
            )
            detail != null -> ContactDetailPage(
                contact = detail!!,
                onBack = { detail = null },
                onCall = onPlaceCall,
                onEdit = { editor = detail },
                onDelete = {
                    onDelete(detail!!)
                    detail = null
                },
                onStar = { onToggleStar(detail!!) },
                onBlock = { onBlock(detail!!.primaryNumber) }
            )
            else -> ContactList(
                query = query,
                onQueryChange = { query = it },
                contacts = visible,
                canRead = canRead,
                onGrantAccess = onGrantAccess,
                onPlaceCall = onPlaceCall,
                onOpen = { detail = it },
                onOpenSettings = onOpenSettings,
                onCreate = { creating = true }
            )
        }
    }
}

@Composable
private fun ContactList(
    query: String,
    onQueryChange: (String) -> Unit,
    contacts: List<DeviceContact>,
    canRead: Boolean,
    onGrantAccess: () -> Unit,
    onPlaceCall: (String) -> Unit,
    onOpen: (DeviceContact) -> Unit,
    onOpenSettings: () -> Unit,
    onCreate: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Contacts",
                    color = TextMain,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = Accent)
                }
            }
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Fill)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = TextDim, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(color = TextMain, fontSize = 15.sp),
                    cursorBrush = SolidColor(Accent),
                    decorationBox = { inner ->
                        if (query.isEmpty()) Text("Search name or number", color = TextDim, fontSize = 15.sp)
                        inner()
                    }
                )
            }

            if (!canRead) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = TextDim, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Contacts permission needed", color = TextMain, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onGrantAccess,
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White)
                    ) { Text("Allow") }
                }
            } else if (contacts.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(if (query.isBlank()) "No contacts" else "No results", color = TextMain, fontWeight = FontWeight.Medium)
                    Text(
                        if (query.isBlank()) "Add one or sync Google Contacts" else "Try a different name or number",
                        color = TextDim,
                        fontSize = 13.sp
                    )
                }
            } else {
                ContactIndexList(
                    contacts = contacts,
                    onOpen = onOpen,
                    onPlaceCall = onPlaceCall,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }

        FloatingActionButton(
            onClick = onCreate,
            containerColor = Accent,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Outlined.Add, contentDescription = "Add contact")
        }
    }
}

private sealed class ContactLine {
    abstract val letter: Char

    data class Header(override val letter: Char) : ContactLine()
    data class Person(val contact: DeviceContact, override val letter: Char) : ContactLine()
}

private fun sectionLetter(name: String): Char {
    val letter = name.trim().firstOrNull()?.uppercaseChar() ?: return '#'
    return if (letter in 'A'..'Z') letter else '#'
}

@Composable
private fun ContactIndexList(
    contacts: List<DeviceContact>,
    onOpen: (DeviceContact) -> Unit,
    onPlaceCall: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lines = remember(contacts) {
        buildList {
            var previous = '\u0000'
            for (contact in contacts) {
                val letter = sectionLetter(contact.name)
                if (letter != previous) {
                    add(ContactLine.Header(letter))
                    previous = letter
                }
                add(ContactLine.Person(contact, letter))
            }
        }
    }
    val letterIndex = remember(lines) {
        buildMap {
            lines.forEachIndexed { index, line ->
                if (line is ContactLine.Header) put(line.letter, index)
            }
        }
    }
    val present = letterIndex.keys
    val alphabet = remember(present) {
        buildList {
            if ('#' in present) add('#')
            for (letter in 'A'..'Z') add(letter)
        }
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var dragged by remember { mutableStateOf<Char?>(null) }
    val scrolled by remember {
        derivedStateOf { lines.getOrNull(listState.firstVisibleItemIndex)?.letter }
    }
    val active = dragged ?: scrolled

    fun targetFor(letter: Char): Int {
        letterIndex[letter]?.let { return it }
        val ordered = letterIndex.keys.sortedWith(compareBy { if (it == '#') -1 else it.code })
        val next = ordered.firstOrNull { candidate ->
            candidate != '#' && letter != '#' && candidate > letter
        } ?: ordered.lastOrNull()
        return next?.let { letterIndex[it] } ?: 0
    }

    Box(modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            items(
                lines,
                key = { line ->
                    when (line) {
                        is ContactLine.Header -> "h-${line.letter}"
                        is ContactLine.Person -> line.contact.contactId.toString() + line.contact.primaryNumber
                    }
                }
            ) { line ->
                when (line) {
                    is ContactLine.Header -> Text(
                        line.letter.toString(),
                        color = TextDim,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 28.dp, bottom = 2.dp)
                    )
                    is ContactLine.Person -> {
                        val contact = line.contact
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpen(contact) }
                                .padding(start = 16.dp, end = 28.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            InitialAvatar(contact.name, 44.dp, 16.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        contact.name,
                                        color = TextMain,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (contact.starred) {
                                        Spacer(Modifier.width(6.dp))
                                        Icon(
                                            Icons.Filled.Star,
                                            contentDescription = null,
                                            tint = Accent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    contact.primaryNumber,
                                    color = TextDim,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { onPlaceCall(contact.primaryNumber) }) {
                                Icon(Icons.Outlined.Call, contentDescription = "Call", tint = Accent)
                            }
                        }
                        HorizontalDivider(
                            color = Line.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 72.dp, end = 28.dp)
                        )
                    }
                }
            }
        }
        if (dragged != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Accent.copy(alpha = 0.94f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    dragged.toString(),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(top = 2.dp, bottom = 72.dp, end = 1.dp)
                .width(16.dp)
                .pointerInput(alphabet, letterIndex) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        fun pick(y: Float): Char {
                            val fraction = (y / size.height.toFloat().coerceAtLeast(1f)).coerceIn(0f, 0.999f)
                            return alphabet[(fraction * alphabet.size).toInt()]
                        }
                        fun go(letter: Char) {
                            dragged = letter
                            scope.launch { listState.scrollToItem(targetFor(letter)) }
                        }
                        go(pick(down.position.y))
                        drag(down.id) { change ->
                            val letter = pick(change.position.y)
                            if (letter != dragged) go(letter)
                            change.consume()
                        }
                        dragged = null
                    }
                },
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            alphabet.forEach { letter ->
                val selected = letter == active
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (selected) Accent else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        letter.toString(),
                        color = when {
                            selected -> Color.White
                            letter in present -> TextDim
                            else -> TextDim.copy(alpha = 0.35f)
                        },
                        fontSize = 8.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        lineHeight = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactDetailPage(
    contact: DeviceContact,
    onBack: () -> Unit,
    onCall: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStar: () -> Unit,
    onBlock: () -> Unit
) {
    var share by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        PageBackRow("Contacts", onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))
            InitialAvatar(contact.name, 92.dp, 36.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                contact.name,
                color = TextMain,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            if (contact.company.isNotBlank() && contact.company != contact.name) {
                Text(contact.company, color = TextDim, fontSize = 15.sp, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ContactPageAction(Icons.Outlined.Call, "call", Accent, Modifier.weight(1f)) {
                    if (contact.primaryNumber.isNotBlank()) onCall(contact.primaryNumber)
                }
                ContactPageAction(Icons.Outlined.Edit, "edit", Accent, Modifier.weight(1f), onEdit)
                ContactPageAction(
                    if (contact.starred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    "favorite",
                    Accent,
                    Modifier.weight(1f),
                    onStar
                )
                ContactPageAction(Icons.Outlined.Block, "block", Off, Modifier.weight(1f), onBlock)
            }
            Spacer(Modifier.height(22.dp))
            if (contact.numbers.isNotEmpty()) {
                InsetGroup {
                    contact.numbers.forEachIndexed { index, number ->
                        DetailInfoRow(
                            icon = Icons.Outlined.Phone,
                            label = "mobile",
                            value = number,
                            valueColor = Accent,
                            onClick = { onCall(number) }
                        )
                        if (index < contact.numbers.lastIndex) {
                            HorizontalDivider(color = Line.copy(alpha = 0.55f), modifier = Modifier.padding(start = 52.dp))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            val extras = buildList {
                if (contact.email.isNotBlank()) add(Triple(Icons.Outlined.Email, "email", contact.email))
                if (contact.company.isNotBlank()) add(Triple(Icons.Outlined.Business, "company", contact.company))
                if (contact.note.isNotBlank()) add(Triple(Icons.AutoMirrored.Outlined.Notes, "notes", contact.note))
            }
            if (extras.isNotEmpty()) {
                InsetGroup {
                    extras.forEachIndexed { index, (icon, label, value) ->
                        DetailInfoRow(icon = icon, label = label, value = value)
                        if (index < extras.lastIndex) {
                            HorizontalDivider(color = Line.copy(alpha = 0.55f), modifier = Modifier.padding(start = 52.dp))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            if (contact.primaryNumber.isNotBlank()) {
                InsetGroup {
                    Text(
                        "Share Contact",
                        color = Accent,
                        fontSize = 17.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { share = true }
                            .padding(vertical = 14.dp),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
            InsetGroup {
                Text(
                    "Delete Contact",
                    color = Off,
                    fontSize = 17.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDelete)
                        .padding(vertical = 14.dp),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(28.dp))
        }
    }
    if (share && contact.primaryNumber.isNotBlank()) {
        ShareQrDialog(title = contact.name, payload = contact.primaryNumber, onDismiss = { share = false })
    }
}

@Composable
private fun ContactEditorPage(
    existing: DeviceContact?,
    initialNumber: String = "",
    accounts: List<ContactAccount>,
    onDismiss: () -> Unit,
    onSave: (ContactDraft) -> Unit
) {
    val context = LocalContext.current
    val detected = remember { DialCountries.detect(context) }
    val initialNames = remember(existing?.contactId) { editorNames(existing) }
    val initialPhone = remember(existing?.contactId, initialNumber) {
        DialCountries.split(existing?.primaryNumber ?: initialNumber, detected)
    }
    var given by remember(existing?.contactId) { mutableStateOf(initialNames.first) }
    var family by remember(existing?.contactId) { mutableStateOf(initialNames.second) }
    var company by remember(existing?.contactId) { mutableStateOf(existing?.company.orEmpty()) }
    var email by remember(existing?.contactId) { mutableStateOf(existing?.email.orEmpty()) }
    var note by remember(existing?.contactId) { mutableStateOf(existing?.note.orEmpty()) }
    var country by remember(existing?.contactId) { mutableStateOf(initialPhone.first) }
    var number by remember(existing?.contactId) { mutableStateOf(initialPhone.second) }
    var account by remember {
        mutableStateOf(
            accounts.firstOrNull { it.isGoogle }
                ?: accounts.firstOrNull()
                ?: ContactAccount("", ContactAccount.TYPE_PHONE, "Phone")
        )
    }
    var pickingCountry by remember { mutableStateOf(false) }
    val e164 = remember(country, number) { DialCountries.toE164(country, number) }
    val phoneToSave = if (
        existing != null &&
        country.iso == initialPhone.first.iso &&
        number.filter { it.isDigit() } == initialPhone.second.filter { it.isDigit() }
    ) {
        existing.primaryNumber
    } else {
        e164
    }
    val canSave = phoneToSave.count { it.isDigit() } >= 4
    BackHandler(enabled = pickingCountry) { pickingCountry = false }

    if (pickingCountry) {
        CountryPickerPage(
            selected = country,
            onBack = { pickingCountry = false },
            onPick = {
                country = it
                pickingCountry = false
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss, modifier = Modifier.width(88.dp)) {
                Text("Cancel", color = Accent, fontSize = 17.sp)
            }
            Text(
                if (existing == null) "New Contact" else "Edit Contact",
                color = TextMain,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = {
                    onSave(
                        ContactDraft(
                            givenName = given.trim(),
                            familyName = family.trim(),
                            company = company.trim(),
                            phone = phoneToSave,
                            email = email.trim(),
                            note = note.trim(),
                            account = account
                        )
                    )
                },
                enabled = canSave,
                modifier = Modifier.width(88.dp)
            ) {
                Text(
                    "Done",
                    color = if (canSave) Accent else TextDim,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(bottom = 28.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val seed = given.ifBlank { family }.ifBlank { company }
                InitialAvatar(seed, 96.dp, 36.sp)
            }
            Spacer(Modifier.height(18.dp))
            InsetGroup {
                PlainField(given, { given = it }, "First name")
                GroupDivider()
                PlainField(family, { family = it }, "Last name")
                GroupDivider()
                PlainField(company, { company = it }, "Company")
            }
            Spacer(Modifier.height(22.dp))
            InsetGroup {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { pickingCountry = true }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(country.flag, fontSize = 20.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(country.dial, color = Accent, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(
                        modifier = Modifier
                            .padding(vertical = 6.dp)
                            .width(1.dp)
                            .height(22.dp)
                            .background(Line)
                    )
                    BasicTextField(
                        value = number,
                        onValueChange = { raw ->
                            if (raw.trim().startsWith("+")) {
                                val split = DialCountries.split(raw, country)
                                country = split.first
                                number = split.second.filter { it.isDigit() }.take(16)
                            } else {
                                number = raw.filter { it.isDigit() }.take(16)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                        singleLine = true,
                        textStyle = TextStyle(color = TextMain, fontSize = 17.sp),
                        cursorBrush = SolidColor(Accent),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        decorationBox = { inner ->
                            Box {
                                if (number.isBlank()) Text("Phone number", color = TextDim, fontSize = 17.sp)
                                inner()
                            }
                        }
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            InsetGroup {
                PlainField(
                    email,
                    { email = it.filter { ch -> !ch.isWhitespace() }.take(80) },
                    "Email",
                    keyboardType = KeyboardType.Email,
                    capitalization = KeyboardCapitalization.None
                )
                GroupDivider()
                PlainField(
                    note,
                    { note = it.take(280) },
                    "Notes",
                    singleLine = false,
                    capitalization = KeyboardCapitalization.Sentences,
                    modifier = Modifier.heightIn(min = 72.dp)
                )
            }
            if (existing == null && account.isSim) {
                Text(
                    "SIM contacts only keep the name and phone number.",
                    color = TextDim,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 8.dp)
                )
            }
            if (existing == null && accounts.isNotEmpty()) {
                Spacer(Modifier.height(22.dp))
                Text(
                    "SAVE TO",
                    color = TextDim,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 32.dp, bottom = 7.dp)
                )
                InsetGroup {
                    accounts.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { account = item }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (item.isGoogle) "Google · ${item.label}" else item.label,
                                color = TextMain,
                                fontSize = 17.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (item == account) {
                                Icon(Icons.Outlined.Check, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp))
                            }
                        }
                        if (index < accounts.lastIndex) GroupDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun CountryPickerPage(
    selected: DialCountry,
    onBack: () -> Unit,
    onPick: (DialCountry) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val visible = remember(query) {
        val q = query.trim()
        if (q.isEmpty()) DialCountries.all
        else DialCountries.all.filter {
            it.name.contains(q, ignoreCase = true) ||
                it.dial.contains(q) ||
                it.code.contains(q.removePrefix("+"))
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        PageBackRow("Contact", onBack)
        Text(
            "Country",
            color = TextMain,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Fill)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = TextDim, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(color = TextMain, fontSize = 16.sp),
                cursorBrush = SolidColor(Accent),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("Search countries", color = TextDim, fontSize = 16.sp)
                    inner()
                }
            )
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(visible, key = { it.iso + it.code }) { country ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(country) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(country.flag, fontSize = 22.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        country.name,
                        color = TextMain,
                        fontSize = 17.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(country.dial, color = TextDim, fontSize = 16.sp)
                    if (country.iso == selected.iso) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Outlined.Check, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp))
                    }
                }
                HorizontalDivider(color = Line.copy(alpha = 0.45f), modifier = Modifier.padding(start = 56.dp))
            }
        }
    }
}

@Composable
private fun PageBackRow(label: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = Accent)
        }
        Text(label, color = Accent, fontSize = 17.sp)
    }
}

@Composable
private fun InitialAvatar(name: String, size: androidx.compose.ui.unit.Dp, fontSize: androidx.compose.ui.unit.TextUnit) {
    val letter = name.firstOrNull()?.uppercaseChar()?.toString()
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Accent.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        if (letter == null) {
            Icon(Icons.Outlined.Person, contentDescription = null, tint = Accent, modifier = Modifier.size(size * 0.46f))
        } else {
            Text(letter, color = Accent, fontSize = fontSize, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun InsetGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface),
        content = content
    )
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(color = Line.copy(alpha = 0.55f), modifier = Modifier.padding(start = 16.dp))
}

@Composable
private fun PlainField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Words,
    singleLine: Boolean = true,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        singleLine = singleLine,
        textStyle = TextStyle(color = TextMain, fontSize = 17.sp),
        cursorBrush = SolidColor(Accent),
        keyboardOptions = KeyboardOptions(capitalization = capitalization, keyboardType = keyboardType),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(placeholder, color = TextDim, fontSize = 17.sp)
                inner()
            }
        }
    )
}

@Composable
private fun DetailInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = TextMain,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = TextDim, fontSize = 12.sp)
            Text(value, color = valueColor, fontSize = 17.sp)
        }
    }
}

@Composable
private fun ContactPageAction(
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
        Text(label, color = Accent, fontSize = 12.sp, maxLines = 1)
    }
}

private fun editorNames(contact: DeviceContact?): Pair<String, String> {
    if (contact == null) return "" to ""
    if (contact.givenName.isNotBlank() || contact.familyName.isNotBlank()) {
        return contact.givenName to contact.familyName
    }
    if (contact.name.isBlank() || contact.name == contact.primaryNumber) return "" to ""
    val parts = contact.name.trim().split(Regex("\\s+"), limit = 2)
    return if (parts.size == 2) parts[0] to parts[1] else parts[0] to ""
}
