package dev.shahzad.prefixshield

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun ContactsTab(
    padding: PaddingValues,
    contacts: List<DeviceContact>,
    accounts: List<ContactAccount>,
    canRead: Boolean,
    onGrantAccess: () -> Unit,
    onPlaceCall: (String) -> Unit,
    onSave: (String, String, ContactAccount) -> Unit,
    onUpdate: (DeviceContact, String, String) -> Unit,
    onDelete: (DeviceContact) -> Unit,
    onToggleStar: (DeviceContact) -> Unit,
    onBlock: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var editor by remember { mutableStateOf<DeviceContact?>(null) }
    var creating by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<DeviceContact?>(null) }

    val visible = remember(contacts, query) {
        val q = query.trim()
        if (q.isEmpty()) contacts
        else contacts.filter {
            it.name.contains(q, ignoreCase = true) ||
                it.numbers.any { number -> number.contains(q) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Contacts",
                color = TextMain,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
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
                    onValueChange = { query = it },
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
            } else if (visible.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No contacts", color = TextMain, fontWeight = FontWeight.Medium)
                    Text("Add one or sync Google Contacts", color = TextDim, fontSize = 13.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(visible, key = { it.contactId.toString() + it.primaryNumber }) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { detail = contact }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Accent.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    contact.name.firstOrNull()?.uppercaseChar()?.toString() ?: "#",
                                    color = Accent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
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
                                        Icon(Icons.Filled.Star, contentDescription = null, tint = Accent, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Text(contact.primaryNumber, color = TextDim, fontSize = 13.sp)
                            }
                            IconButton(onClick = { onPlaceCall(contact.primaryNumber) }) {
                                Icon(Icons.Outlined.Call, contentDescription = "Call", tint = Accent)
                            }
                        }
                        HorizontalDivider(color = Line.copy(alpha = 0.7f), modifier = Modifier.padding(start = 72.dp))
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { creating = true },
            containerColor = Accent,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Outlined.Add, contentDescription = "Add contact")
        }
    }

    detail?.let { contact ->
        ContactDetailSheet(
            contact = contact,
            onDismiss = { detail = null },
            onCall = { onPlaceCall(it) },
            onEdit = {
                detail = null
                editor = contact
            },
            onDelete = {
                onDelete(contact)
                detail = null
            },
            onStar = { onToggleStar(contact) },
            onBlock = {
                onBlock(contact.primaryNumber)
                detail = null
            }
        )
    }

    if (creating || editor != null) {
        ContactEditorDialog(
            existing = editor,
            accounts = accounts,
            onDismiss = {
                creating = false
                editor = null
            },
            onSave = { name, number, account ->
                if (editor != null) onUpdate(editor!!, name, number) else onSave(name, number, account)
                creating = false
                editor = null
            }
        )
    }
}

@Composable
private fun ContactDetailSheet(
    contact: DeviceContact,
    onDismiss: () -> Unit,
    onCall: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStar: () -> Unit,
    onBlock: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Surface)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close", tint = TextDim)
                }
            }
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    contact.name.firstOrNull()?.uppercaseChar()?.toString() ?: "#",
                    color = Accent,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(contact.name, color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            contact.numbers.forEach { number ->
                TextButton(onClick = { onCall(number) }) {
                    Text(number, color = Accent, fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                ContactAction(Icons.Outlined.Call, "call", On) { onCall(contact.primaryNumber) }
                ContactAction(Icons.Outlined.Edit, "edit", Accent, onEdit)
                ContactAction(
                    if (contact.starred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    "favorite",
                    Accent,
                    onStar
                )
                ContactAction(Icons.Outlined.Block, "block", Off, onBlock)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDelete) { Text("Delete Contact", color = Off) }
        }
    }
}

@Composable
private fun ContactAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Fill)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint)
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Accent, fontSize = 12.sp)
    }
}

@Composable
private fun ContactEditorDialog(
    existing: DeviceContact?,
    accounts: List<ContactAccount>,
    onDismiss: () -> Unit,
    onSave: (String, String, ContactAccount) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var number by remember { mutableStateOf(existing?.primaryNumber.orEmpty()) }
    var account by remember { mutableStateOf(accounts.firstOrNull { it.isGoogle } ?: accounts.first()) }
    var accountMenu by remember { mutableStateOf(false) }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Accent,
        unfocusedBorderColor = Line,
        focusedTextColor = TextMain,
        unfocusedTextColor = TextMain,
        cursorColor = Accent,
        focusedLabelColor = TextDim,
        unfocusedLabelColor = TextDim
    )

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Surface)
                .padding(20.dp)
        ) {
            Text(
                if (existing == null) "New contact" else "Edit contact",
                color = TextMain,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name") },
                singleLine = true,
                colors = fieldColors
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = number,
                onValueChange = { number = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Number") },
                singleLine = true,
                colors = fieldColors
            )
            if (existing == null) {
                Spacer(Modifier.height(10.dp))
                Box(modifier = Modifier.clickable { accountMenu = true }) {
                    OutlinedTextField(
                        value = if (account.isGoogle) "Google · ${account.label}" else account.label,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        label = { Text("Save to") },
                        colors = fieldColors
                    )
                    DropdownMenu(
                        expanded = accountMenu,
                        onDismissRequest = { accountMenu = false },
                        containerColor = Surface
                    ) {
                        accounts.forEach { item ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (item.isGoogle) "Google · ${item.label}" else item.label,
                                        color = TextMain
                                    )
                                },
                                onClick = {
                                    account = item
                                    accountMenu = false
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel", color = TextDim) }
                TextButton(
                    onClick = { if (number.isNotBlank()) onSave(name, number, account) }
                ) { Text("Save", color = Accent) }
            }
        }
    }
}
