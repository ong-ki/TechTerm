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

    suspend fun ensurePopulated() {
        if (isSeedChecked) return
        seedMutex.withLock {
            if (isSeedChecked) return
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
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to populate database from $TERMS_ASSET_FILE", e)
                    // Mark checked to prevent infinite retry loops during this launch;
                    // callers fall back to an empty state safely.
                    isSeedChecked = true
                }
            }
        }
    }

    fun getAllTerms(): Flow<List<Term>> = termDao.getAll()

    fun searchTerms(query: String, category: String?, devOnly: Boolean = false): Flow<List<Term>> {
        return termDao.search(
            query = query.trim(),
            category = category,
            devOnly = devOnly
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
        return try {
            val rawJson = context.assets.open(TERMS_ASSET_FILE)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            json.decodeFromString<List<TermSeedDto>>(rawJson)
                .map { it.toEntity() }
                .filter { it.term.isNotBlank() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse $TERMS_ASSET_FILE", e)
            emptyList()
        }
    }

    companion object {
        private const val TAG = "TechTerms"
        private const val TERMS_ASSET_FILE = "terms.json"
        const val CURRENT_DATA_VERSION = 2
    }
}
