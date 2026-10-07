package org.cawcs.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.cawcs.app.data.Repository
import org.cawcs.app.payments.PaymentGateway
import org.cawcs.app.security.AnonymousIdentity

object Routes {
    const val WELCOME = "welcome"
    const val HOME = "home"
    const val COUNSELLOR = "counsellor/{id}"
    const val PAY = "pay/{bookingId}"
    const val CHAT = "chat/{bookingId}"
    const val SESSIONS = "sessions"
    const val WALLET = "wallet"
    const val CRISIS = "crisis"
    const val SETTINGS = "settings"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CawcsApp(
    identity: AnonymousIdentity,
    repo: Repository,
    payments: PaymentGateway,
    unlocked: Boolean,
    onRequestUnlock: () -> Unit,
    onQuickExit: () -> Unit,
) {
    CawcsTheme {
        if (!unlocked) { LockScreen(onRequestUnlock); return@CawcsTheme }

        var hasIdentity by remember { mutableStateOf(identity.exists) }
        val nav = rememberNavController()
        val current = nav.currentBackStackEntryAsState().value?.destination?.route
        val topLevel = listOf(Routes.HOME, Routes.SESSIONS, Routes.WALLET, Routes.SETTINGS)

        Scaffold(
            topBar = {
                if (hasIdentity) TopAppBar(
                    title = { Text("CAWCS", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    actions = {
                        IconButton(onClick = { nav.navigate(Routes.CRISIS) }) {
                            Icon(Icons.Filled.Favorite, contentDescription = "Urgent help")
                        }
                        // Quick exit: closes the app instantly and removes it from recents.
                        IconButton(onClick = onQuickExit) {
                            Icon(Icons.Filled.ExitToApp, contentDescription = "Quick exit")
                        }
                    },
                )
            },
            bottomBar = {
                if (hasIdentity && current in topLevel) NavigationBar {
                    listOf(
                        Triple(Routes.HOME, "Counsellors", Icons.Filled.Favorite),
                        Triple(Routes.SESSIONS, "Sessions", Icons.Filled.Chat),
                        Triple(Routes.WALLET, "Wallet", Icons.Filled.AccountBalanceWallet),
                        Triple(Routes.SETTINGS, "Privacy", Icons.Filled.Settings),
                    ).forEach { (route, label, icon) ->
                        NavigationBarItem(
                            selected = current == route,
                            onClick = { nav.navigate(route) { popUpTo(Routes.HOME); launchSingleTop = true } },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                        )
                    }
                }
            },
        ) { pad ->
            NavHost(nav, startDestination = if (hasIdentity) Routes.HOME else Routes.WELCOME, modifier = Modifier.padding(pad)) {
                composable(Routes.WELCOME) {
                    WelcomeScreen(identity) {
                        hasIdentity = true
                        nav.navigate(Routes.HOME) { popUpTo(Routes.WELCOME) { inclusive = true } }
                    }
                }
                composable(Routes.HOME) { HomeScreen(repo) { nav.navigate("counsellor/$it") } }
                composable(Routes.COUNSELLOR) { e ->
                    CounsellorScreen(repo, e.arguments?.getString("id")!!) { bookingId -> nav.navigate("pay/$bookingId") }
                }
                composable(Routes.PAY) { e ->
                    PaymentScreen(repo, payments, identity, e.arguments?.getString("bookingId")!!,
                        onPaid = { id -> nav.navigate("chat/$id") { popUpTo(Routes.HOME) } },
                        onTopUp = { nav.navigate(Routes.WALLET) })
                }
                composable(Routes.CHAT) { e -> ChatScreen(repo, e.arguments?.getString("bookingId")!!) }
                composable(Routes.SESSIONS) { SessionsScreen(repo) { nav.navigate("chat/$it") } }
                composable(Routes.WALLET) { WalletScreen(payments, identity) }
                composable(Routes.CRISIS) { CrisisScreen() }
                composable(Routes.SETTINGS) {
                    PrivacyScreen(identity) {
                        identity.eraseEverything()
                        hasIdentity = false
                        nav.navigate(Routes.WELCOME) { popUpTo(0) }
                    }
                }
            }
        }
    }
}
