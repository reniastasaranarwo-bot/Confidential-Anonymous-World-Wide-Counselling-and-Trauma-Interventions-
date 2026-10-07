package org.cawcs.app.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import java.util.UUID

/**
 * Anonymous identity: no email, phone or real name is ever collected.
 * The client gets a random alias plus a random account ID and a recovery phrase.
 * Everything is stored in EncryptedSharedPreferences, keyed by the Android Keystore.
 */
class AnonymousIdentity(context: Context) {

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "cawcs_identity",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    val exists: Boolean get() = prefs.contains(KEY_ACCOUNT)
    val alias: String get() = prefs.getString(KEY_ALIAS, "") ?: ""
    val accountId: String get() = prefs.getString(KEY_ACCOUNT, "") ?: ""
    val recoveryPhrase: String get() = prefs.getString(KEY_PHRASE, "") ?: ""
    var appLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCK, true)
        set(v) = prefs.edit().putBoolean(KEY_LOCK, v).apply()

    fun create(): String {
        val alias = generateAlias()
        prefs.edit()
            .putString(KEY_ALIAS, alias)
            .putString(KEY_ACCOUNT, UUID.randomUUID().toString())
            .putString(KEY_PHRASE, generatePhrase())
            .apply()
        return alias
    }

    /** "Quick exit / erase": wipes the identity and all local data from this device. */
    fun eraseEverything() = prefs.edit().clear().apply()

    private fun generateAlias(): String {
        val r = SecureRandom()
        return "${ADJ[r.nextInt(ADJ.size)]} ${NOUN[r.nextInt(NOUN.size)]} ${100 + r.nextInt(900)}"
    }

    private fun generatePhrase(): String {
        val r = SecureRandom()
        return (1..8).joinToString(" ") { WORDS[r.nextInt(WORDS.size)] }
    }

    companion object {
        private const val KEY_ALIAS = "alias"
        private const val KEY_ACCOUNT = "account"
        private const val KEY_PHRASE = "phrase"
        private const val KEY_LOCK = "app_lock"
        private val ADJ = listOf("Calm", "Quiet", "Gentle", "Brave", "Steady", "Kind", "Bright", "Patient", "Hopeful", "Still")
        private val NOUN = listOf("River", "Cedar", "Harbor", "Meadow", "Lantern", "Sparrow", "Stone", "Willow", "Horizon", "Ember")
        private val WORDS = listOf(
            "amber", "basin", "candle", "delta", "echo", "fable", "garden", "hollow", "island", "jasmine",
            "kettle", "lagoon", "marble", "nectar", "orbit", "pebble", "quartz", "ribbon", "summit", "timber",
            "umber", "valley", "wander", "yonder", "zephyr", "acorn", "breeze", "cobalt", "dune", "fern",
        )
    }
}
