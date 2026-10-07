package org.cawcs.app.ui

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.cawcs.app.data.BookingStatus
import org.cawcs.app.data.CrisisDirectory
import org.cawcs.app.data.Repository
import org.cawcs.app.data.SessionLength
import org.cawcs.app.data.SessionMode
import org.cawcs.app.payments.PaymentGateway
import org.cawcs.app.payments.PaymentMethod
import org.cawcs.app.payments.PaymentResult
import org.cawcs.app.payments.Pricing
import org.cawcs.app.security.AnonymousIdentity

private fun usd(cents: Int) = "$" + String.format("%.2f", cents / 100.0)

@Composable
private fun SectionTitle(text: String) =
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))

// ---------------------------------------------------------------- Lock

@Composable
fun LockScreen(onUnlock: () -> Unit) {
    LaunchedEffect(Unit) { onUnlock() }
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.Lock, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Locked for your privacy", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onUnlock) { Text("Unlock") }
    }
}

// ---------------------------------------------------------------- Welcome / anonymous sign-up

@Composable
fun WelcomeScreen(identity: AnonymousIdentity, onDone: () -> Unit) {
    var alias by remember { mutableStateOf<String?>(null) }
    var agreed by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Spacer(Modifier.height(24.dp))
        Text("Confidential Anonymous Worldwide Counselling Services", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Talk to a verified counsellor from anywhere in the world — without giving your name, email or phone number.",
            style = MaterialTheme.typography.bodyLarge)

        SectionTitle("How we protect you")
        listOf(
            "No sign-up details. You get a random alias and a private recovery phrase.",
            "Messages are end-to-end encrypted between you and your counsellor.",
            "Screenshots and screen recording are blocked inside the app.",
            "Pay anonymously with vouchers, wallet credit, crypto or mobile money.",
            "One tap Quick Exit, and you can erase everything at any time.",
        ).forEach { Text("•  $it", Modifier.padding(vertical = 3.dp)) }

        Card(Modifier.fillMaxWidth().padding(top = 20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f))) {
            Text("This service is not an emergency service. If you are in immediate danger or thinking of ending your life, use the heart button at the top of the app for crisis lines, or contact local emergency services.",
                Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
        }

        Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = agreed, onCheckedChange = { agreed = it })
            Spacer(Modifier.width(12.dp))
            Text("I am 18 or over (or have consent where required) and accept the confidentiality terms.")
        }

        Spacer(Modifier.height(20.dp))
        if (alias == null) {
            Button(onClick = { alias = identity.create() }, enabled = agreed, modifier = Modifier.fillMaxWidth()) { Text("Create my anonymous profile") }
        } else {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Your alias", style = MaterialTheme.typography.labelLarge)
                    Text(alias!!, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Text("Recovery phrase — write it down somewhere safe. It's the only way to restore your wallet and history on a new phone.",
                        style = MaterialTheme.typography.bodySmall)
                    Text(identity.recoveryPhrase, Modifier.padding(top = 6.dp), fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("I've saved it — continue") }
        }
    }
}

// ---------------------------------------------------------------- Counsellor directory

@Composable
fun HomeScreen(repo: Repository, onOpen: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var onlineOnly by remember { mutableStateOf(false) }
    val list = repo.counsellors.filter { c ->
        (!onlineOnly || c.online) && (query.isBlank() ||
            (c.specialties + c.languages + c.displayName).any { it.contains(query, ignoreCase = true) })
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            Text("Find a counsellor", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 12.dp))
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(top = 8.dp),
                placeholder = { Text("Topic or language, e.g. anxiety, Shona") }, singleLine = true)
            Row(Modifier.padding(vertical = 8.dp)) {
                FilterChip(selected = onlineOnly, onClick = { onlineOnly = !onlineOnly }, label = { Text("Available now") })
            }
        }
        items(list, key = { it.id }) { c ->
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onOpen(c.id) }) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(if (c.online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(c.displayName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        if (c.verified) Icon(Icons.Filled.Verified, "Verified", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    Text(c.specialties.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                    Text("${c.languages.joinToString(", ")}  ·  ★ ${c.rating}  ·  from ${usd((c.pricePerSessionUsdCents * 0.6).toInt())}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        if (list.isEmpty()) item { Text("No counsellors match. Try another topic or language.", Modifier.padding(24.dp)) }
    }
}

// ---------------------------------------------------------------- Counsellor detail + booking

@Composable
fun CounsellorScreen(repo: Repository, id: String, onBooked: (String) -> Unit) {
    val c = remember(id) { repo.counsellor(id) }
    var mode by remember { mutableStateOf(SessionMode.TEXT_CHAT) }
    var length by remember { mutableStateOf(SessionLength.STANDARD) }
    var tier by remember { mutableStateOf(Pricing.Tier.HIGH) }
    val quote = Pricing.quote(c, length, tier, "USD", 1.0)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(c.displayName, style = MaterialTheme.typography.headlineSmall)
        Text(c.specialties.joinToString(" · "), color = MaterialTheme.colorScheme.secondary)
        Text(c.bio, Modifier.padding(top = 12.dp))
        Text("Languages: ${c.languages.joinToString(", ")}   ·   Time zone: ${c.timezone}", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)

        SectionTitle("How would you like to talk?")
        SessionMode.entries.forEach { m ->
            val label = when (m) { SessionMode.TEXT_CHAT -> "Text chat (most private)"; SessionMode.VOICE -> "Voice call (voice can be masked)"; SessionMode.VIDEO -> "Video call (camera optional)" }
            RadioRow(label, mode == m) { mode = m }
        }

        SectionTitle("Session length")
        SessionLength.entries.forEach { l -> RadioRow("${l.minutes} minutes", length == l) { length = l } }

        SectionTitle("Fair pricing for your region")
        Text("Prices adjust to local income levels. Choose what applies — we never use your location.", style = MaterialTheme.typography.bodySmall)
        Pricing.Tier.entries.forEach { t ->
            val label = when (t) {
                Pricing.Tier.HIGH -> "High-income country"
                Pricing.Tier.UPPER_MIDDLE -> "Upper-middle income (−${t.discountPct}%)"
                Pricing.Tier.LOWER_MIDDLE -> "Lower-middle income (−${t.discountPct}%)"
                Pricing.Tier.LOW -> "Low-income country (−${t.discountPct}%)"
            }
            RadioRow(label, tier == t) { tier = t }
        }

        HorizontalDivider(Modifier.padding(vertical = 16.dp))
        Row { Text("Total", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium); Text(usd(quote.totalUsdCents), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        Text("Can't afford it? Choose \"Sponsored session\" at checkout.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onBooked(repo.createBooking(c.id, mode, length, quote.totalUsdCents).id) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (c.online) "Book and start now" else "Book — counsellor will confirm a time")
        }
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().selectable(selected, onClick = onClick).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label)
    }
}

// ---------------------------------------------------------------- Checkout

@Composable
fun PaymentScreen(repo: Repository, payments: PaymentGateway, identity: AnonymousIdentity, bookingId: String, onPaid: (String) -> Unit, onTopUp: () -> Unit) {
    val booking = repo.bookings.collectAsState().value.first { it.id == bookingId }
    val c = repo.counsellor(booking.counsellorId)
    var method by remember { mutableStateOf(PaymentMethod.WALLET) }
    var extra by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var balance by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { balance = payments.walletBalanceUsdCents(identity.accountId) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Checkout", style = MaterialTheme.typography.headlineSmall)
        Text("${booking.length.minutes}-min session with ${c.displayName}", Modifier.padding(top = 4.dp))
        Text(usd(booking.priceUsdCents), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
        Text("Your statement will show only \"CW Services\" and a random code.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)

        SectionTitle("Payment method")
        PaymentMethod.entries.forEach { m ->
            Card(
                Modifier.fillMaxWidth().padding(vertical = 4.dp).selectable(method == m) { method = m; error = null },
                colors = CardDefaults.cardColors(containerColor = if (method == m) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = method == m, onClick = { method = m })
                    Column(Modifier.weight(1f)) {
                        Text(m.label, fontWeight = FontWeight.Medium)
                        Text(if (m == PaymentMethod.WALLET) "Balance: ${usd(balance)}" else m.detail, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Privacy: ${m.anonymity}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        when (method) {
            PaymentMethod.VOUCHER -> OutlinedTextField(extra, { extra = it.uppercase() }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Voucher code") }, singleLine = true)
            PaymentMethod.MOBILE_MONEY -> OutlinedTextField(extra, { extra = it }, Modifier.fillMaxWidth().padding(top = 8.dp),
                label = { Text("Mobile money number (gets the payment prompt)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                supportingText = { Text("Used once to send the prompt, then discarded — never stored with your profile.") })
            PaymentMethod.SPONSORED -> Text("Sponsored sessions are funded by donors. You'll answer 3 short anonymous questions; up to 4 free sessions per month.",
                Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
            PaymentMethod.WALLET -> if (balance < booking.priceUsdCents) TextButton(onClick = onTopUp) { Text("Top up wallet") }
            else -> {}
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(16.dp))
        Button(
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                busy = true; error = null
                scope.launch {
                    val quote = Pricing.quote(c, booking.length, Pricing.Tier.HIGH, "USD", 1.0).copy(totalUsdCents = booking.priceUsdCents)
                    when (val r = payments.payForBooking(identity.accountId, bookingId, method, quote, extra.ifBlank { null })) {
                        is PaymentResult.Success -> { repo.setStatus(bookingId, BookingStatus.CONFIRMED); onPaid(bookingId) }
                        is PaymentResult.RequiresAction -> error = "Complete the payment in the window that opens."
                        is PaymentResult.Failed -> error = r.reason
                    }
                    busy = false
                }
            },
        ) {
            if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Pay ${usd(booking.priceUsdCents)}")
        }
        Text("Held in escrow until your session ends. Full refund if the counsellor doesn't show; 100% refund if cancelled 12+ hours before.",
            Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }
}

// ---------------------------------------------------------------- Chat

@Composable
fun ChatScreen(repo: Repository, bookingId: String) {
    val messages by repo.chat(bookingId).collectAsState()
    var draft by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex) }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Lock, null, Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("End-to-end encrypted · Auto-deletes after 30 days", style = MaterialTheme.typography.labelSmall)
        }
        LazyColumn(Modifier.weight(1f).padding(horizontal = 12.dp), state = listState) {
            items(messages, key = { it.id }) { m ->
                Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = if (m.fromClient) Alignment.CenterEnd else Alignment.CenterStart) {
                    Text(m.text,
                        Modifier.widthIn(max = 300.dp)
                            .background(if (m.fromClient) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        color = if (m.fromClient) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(draft, { draft = it }, Modifier.weight(1f), placeholder = { Text("Type privately…") })
            IconButton(onClick = {
                val t = draft.trim(); if (t.isEmpty()) return@IconButton
                draft = ""; scope.launch { repo.send(bookingId, t) }
            }) { Icon(Icons.Filled.Send, "Send") }
        }
    }
}

// ---------------------------------------------------------------- Sessions list

@Composable
fun SessionsScreen(repo: Repository, onOpen: (String) -> Unit) {
    val bookings by repo.bookings.collectAsState()
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item { Text("My sessions", style = MaterialTheme.typography.headlineSmall) }
        if (bookings.isEmpty()) item { Text("No sessions yet. Choose a counsellor to start.", Modifier.padding(top = 16.dp)) }
        items(bookings, key = { it.id }) { b ->
            val c = repo.counsellor(b.counsellorId)
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable(enabled = b.status == BookingStatus.CONFIRMED) { onOpen(b.id) }) {
                Column(Modifier.padding(16.dp)) {
                    Text(c.displayName, style = MaterialTheme.typography.titleMedium)
                    Text("${b.length.minutes} min · ${b.mode.name.replace('_', ' ').lowercase()} · ${usd(b.priceUsdCents)}", style = MaterialTheme.typography.bodySmall)
                    Text(b.status.name.replace('_', ' '), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Wallet

@Composable
fun WalletScreen(payments: PaymentGateway, identity: AnonymousIdentity) {
    var balance by remember { mutableIntStateOf(0) }
    var voucher by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    var method by remember { mutableStateOf(PaymentMethod.MOBILE_MONEY) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { balance = payments.walletBalanceUsdCents(identity.accountId) }

    fun handle(r: PaymentResult) {
        msg = when (r) { is PaymentResult.Success -> "Done. Receipt ${r.receiptCode}"; is PaymentResult.Failed -> r.reason; is PaymentResult.RequiresAction -> "Finish in the payment window." }
        scope.launch { balance = payments.walletBalanceUsdCents(identity.accountId) }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Wallet", style = MaterialTheme.typography.headlineSmall)
        Card(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text("Balance", style = MaterialTheme.typography.labelLarge)
                Text(usd(balance), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text("Credit is tied to your anonymous ID, not to you.", style = MaterialTheme.typography.bodySmall)
            }
        }

        SectionTitle("Redeem a voucher")
        OutlinedTextField(voucher, { voucher = it.uppercase() }, Modifier.fillMaxWidth(), label = { Text("Voucher code") }, singleLine = true)
        OutlinedButton(onClick = { scope.launch { handle(payments.redeemVoucher(identity.accountId, voucher)); voucher = "" } }, Modifier.padding(top = 8.dp)) { Text("Redeem") }

        SectionTitle("Top up")
        listOf(PaymentMethod.MOBILE_MONEY, PaymentMethod.CARD, PaymentMethod.CRYPTO).forEach { m -> RadioRow(m.label, method == m) { method = m } }
        if (method == PaymentMethod.MOBILE_MONEY)
            OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("Mobile money number") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1000, 2500, 5000).forEach { amt ->
                Button(onClick = { scope.launch { handle(payments.topUpWallet(identity.accountId, method, amt, phone.ifBlank { null })) } }) { Text(usd(amt)) }
            }
        }
        msg?.let { Text(it, Modifier.padding(top = 12.dp)) }
    }
}

// ---------------------------------------------------------------- Crisis

@Composable
fun CrisisScreen() {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Need help right now?", style = MaterialTheme.typography.headlineSmall)
            Text("If you are in immediate danger, contact your local emergency number. These free lines are staffed by trained people:",
                Modifier.padding(vertical = 8.dp))
        }
        items(CrisisDirectory.lines) { l ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text(l.region, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(l.name, fontWeight = FontWeight.Medium)
                    Text(l.contact)
                }
            }
        }
        item {
            Text("You can also start a free priority chat with the next available counsellor — no payment needed when you're in crisis.",
                Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ---------------------------------------------------------------- Privacy settings

@Composable
fun PrivacyScreen(identity: AnonymousIdentity, onErase: () -> Unit) {
    var lock by remember { mutableStateOf(identity.appLockEnabled) }
    var confirm by remember { mutableStateOf(false) }
    var showPhrase by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Privacy", style = MaterialTheme.typography.headlineSmall)
        Text("Signed in as ${identity.alias}", Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.secondary)

        Row(Modifier.fillMaxWidth().padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("App lock", fontWeight = FontWeight.Medium)
                Text("Fingerprint or device PIN every time the app opens", style = MaterialTheme.typography.bodySmall)
            }
            Switch(lock, { lock = it; identity.appLockEnabled = it })
        }
        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        TextButton(onClick = { showPhrase = !showPhrase }) { Text(if (showPhrase) "Hide recovery phrase" else "Show recovery phrase") }
        if (showPhrase) Text(identity.recoveryPhrase, Modifier.padding(horizontal = 12.dp))
        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        Text("We store no name, email, phone, location or contacts. Chats are end-to-end encrypted and auto-delete after 30 days. Counsellors see only your alias.",
            style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Erase everything from this device", color = MaterialTheme.colorScheme.error)
        }
    }

    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Erase everything?") },
        text = { Text("Your alias, wallet link and chats will be removed from this phone. Without your recovery phrase they cannot be restored.") },
        confirmButton = { TextButton(onClick = { confirm = false; onErase() }) { Text("Erase") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } },
    )
}
