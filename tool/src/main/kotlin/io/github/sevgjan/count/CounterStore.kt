package io.github.sevgjan.count

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class CounterStore(private val dataStore: DataStore<Preferences>) {
    private val serializer = ListSerializer(Counter.serializer())

    val counters: Flow<List<Counter>> = dataStore.data.map { decode(it[COUNTERS]) }

    fun counter(id: Long): Flow<Counter?> = counters.map { list -> list.firstOrNull { it.id == id } }

    suspend fun add(name: String): Counter {
        val counter = Counter(id = System.currentTimeMillis(), name = name.trim().ifEmpty { "Counter" })
        update { it + counter }
        return counter
    }

    suspend fun replace(counter: Counter) = update { list -> list.map { if (it.id == counter.id) counter else it } }

    suspend fun delete(id: Long) = update { list -> list.filterNot { it.id == id } }

    private suspend fun update(transform: (List<Counter>) -> List<Counter>) {
        dataStore.edit { prefs -> prefs[COUNTERS] = Json.encodeToString(serializer, transform(decode(prefs[COUNTERS]))) }
    }

    private fun decode(raw: String?): List<Counter> =
        raw?.let { runCatching { Json.decodeFromString(serializer, it) }.getOrNull() } ?: emptyList()

    private companion object {
        val COUNTERS = stringPreferencesKey("counters")
    }
}
