package com.example.whatsappalarm

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class WatchedContactsPrefs(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("watched_contacts", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CONTACTS = "contacts"
        private const val SEPARATOR = "||"
    }

    fun getContacts(): Set<String> {
        val raw = prefs.getString(KEY_CONTACTS, "") ?: ""
        return if (raw.isBlank()) emptySet()
        else raw.split(SEPARATOR).filter { it.isNotBlank() }.toSet()
    }

    fun addContact(name: String) {
        val current = getContacts().toMutableSet()
        current.add(name)
        save(current)
    }

    fun removeContact(name: String) {
        val current = getContacts().toMutableSet()
        current.remove(name)
        save(current)
    }

    private fun save(contacts: Set<String>) {
        prefs.edit().putString(KEY_CONTACTS, contacts.joinToString(SEPARATOR)).apply()
    }

    // Exposes changes as a Flow for Compose
    val contactsFlow: Flow<Set<String>> = callbackFlow {
        trySend(getContacts())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_CONTACTS) trySend(getContacts())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}