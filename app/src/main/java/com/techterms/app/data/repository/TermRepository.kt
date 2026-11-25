package com.techterms.app.data.repository

import android.content.Context
import android.util.Log
import com.techterms.app.data.datastore.NotificationSettingsDataStore
import com.techterms.app.data.db.Term
import com.techterms.app.data.db.TermDao
import com.techterms.app.data.db.TermSeedDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.LocalDate

class TermRepository(
    private val context: Context,
    private val termDao: TermDao,
    private val settingsDataStore: NotificationSettingsDataStore
) {
    private val seedMutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Volatile
    private var isSeedChecked = false

    private val _seedFailed = MutableStateFlow(false)
    /** True when seeding from assets failed this launch; retried on next launch. */
    val seedFailed: StateFlow<Boolean> = _seedFailed

    suspend fun ensurePopulated() {
        if (isSeedChecked || _seedFailed.value) return
        seedMutex.withLock {
            if (isSeedChecked || _seedFailed.value) return
            withContext(Dispatchers.IO) {
                try {
                    val existingCount = termDao.count()
                    val storedDataVersion = settingsDataStore.getDataVersion()

                    when {
                        existingCount == 0 -> {
                            val seededTerms = loadTermsFromAssets()
                            if (seededTerms.isNotEmpty()) {
                                termDao.insertAll(seededTerms)
                                settingsDataStore.setDataVersion(CURRENT_DATA_VERSION)
                            }
                        }

                        storedDataVersion < CURRENT_DATA_VERSION -> {
                            val seededTerms = loadTermsFromAssets()
                            if (seededTerms.isNotEmpty()) {
                                termDao.upsertSeedTerms(seededTerms)
                                settingsDataStore.setDataVersion(CURRENT_DATA_VERSION)
                            }
                        }
                    }
                    isSeedChecked = true
                    _seedFailed.value = false
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to populate database from $TERMS_ASSET_FILE", e)
                    // Don't mark as checked: seeding is retried on the next launch.
                    // Avoid retry loops within this launch via the failure flag.
                    _seedFailed.value = true
                }
            }
        }
    }

    fun getAllTerms(): Flow<List<Term>> = termDao.getAll()

    fun searchTerms(
        query: String,
        category: String?,
        devOnly: Boolean = false,
        difficulty: String? = null
    ): Flow<List<Term>> {
        return termDao.search(
            query = query.trim(),
            category = category,
            devOnly = devOnly,
            difficulty = difficulty
        )
    }

    suspend fun getTermById(id: Int): Term? {
        if (id <= 0) return null
        ensurePopulated()
        return withContext(Dispatchers.IO) {
            try {
                termDao.getById(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load term by id=$id", e)
                null
            }
        }
    }

    suspend fun getTermOfTheDay(
        devTermsOnly: Boolean = false,
        date: LocalDate = LocalDate.now()
    ): Term? {
        ensurePopulated()
        return withContext(Dispatchers.IO) {
            try {
                if (devTermsOnly) {
                    val devTotal = termDao.countWithCode()
                    if (devTotal > 0) {
                        val offset = ((date.dayOfYear + date.year) % devTotal + devTotal) % devTotal
                        return@withContext termDao.getWithCodeByIndex(offset)
                    }
                }
                val total = termDao.count()
                if (total <= 0) {
                    null
                } else {
                    val offset = ((date.dayOfYear + date.year) % total + total) % total
                    termDao.getByIndex(offset)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load term of the day", e)
                null
            }
        }
    }

    private fun loadTermsFromAssets(): List<Term> {
        return run {
            val rawJson = context.assets.open(TERMS_ASSET_FILE)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            json.decodeFromString<List<TermSeedDto>>(rawJson)
                .map { it.toEntity() }
                .filter { it.term.isNotBlank() }
                .also { if (it.isEmpty()) error("$TERMS_ASSET_FILE contained no valid terms") }
        }
    }

    companion object {
        private const val TAG = "TechTerms"
        private const val TERMS_ASSET_FILE = "terms.json"
        const val CURRENT_DATA_VERSION = 3
    }
}
