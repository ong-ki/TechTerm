package com.techterms.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TermDao {
    @Query("SELECT * FROM terms ORDER BY term COLLATE NOCASE ASC")
    fun getAll(): Flow<List<Term>>

    @Query(
        """
        SELECT * FROM terms
        WHERE (:category IS NULL OR category = :category)
          AND (:difficulty IS NULL OR difficulty = :difficulty)
          AND (:devOnly = 0 OR (codeSnippet IS NOT NULL AND codeSnippet != ''))
          AND (
            :query = ''
            OR term LIKE '%' || :query || '%' COLLATE NOCASE
            OR meaningMy LIKE '%' || :query || '%' COLLATE NOCASE
            OR meaningEn LIKE '%' || :query || '%' COLLATE NOCASE
            OR COALESCE(codeSnippet, '') LIKE '%' || :query || '%' COLLATE NOCASE
          )
        ORDER BY term COLLATE NOCASE ASC
        """
    )
    fun search(
        query: String,
        category: String?,
        devOnly: Boolean = false,
        difficulty: String? = null
    ): Flow<List<Term>>

    @Query("SELECT * FROM terms WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Term?

    @Query("SELECT * FROM terms WHERE term = :termName COLLATE NOCASE LIMIT 1")
    suspend fun getByTermName(termName: String): Term?

    @Query("SELECT COUNT(*) FROM terms")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM terms WHERE codeSnippet IS NOT NULL AND codeSnippet != ''")
    suspend fun countWithCode(): Int

    @Query("SELECT * FROM terms ORDER BY id ASC LIMIT 1 OFFSET :offset")
    suspend fun getByIndex(offset: Int): Term?

    @Query(
        """
        SELECT * FROM terms
        WHERE codeSnippet IS NOT NULL AND codeSnippet != ''
        ORDER BY id ASC
        LIMIT 1 OFFSET :offset
        """
    )
    suspend fun getWithCodeByIndex(offset: Int): Term?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(terms: List<Term>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(term: Term): Long

    @Query(
        """
        UPDATE terms
        SET category = :category,
            difficulty = :difficulty,
            meaningMy = :meaningMy,
            meaningEn = :meaningEn,
            example = :example,
            codeSnippet = :codeSnippet,
            codeLanguage = :codeLanguage,
            usageNote = :usageNote
        WHERE id = :id
        """
    )
    suspend fun updateById(
        id: Int,
        category: String,
        difficulty: String,
        meaningMy: String,
        meaningEn: String,
        example: String,
        codeSnippet: String?,
        codeLanguage: String?,
        usageNote: String?
    )

    @Transaction
    suspend fun upsertSeedTerms(incoming: List<Term>) {
        for (term in incoming) {
            val existing = getByTermName(term.term)
            if (existing != null) {
                updateById(
                    id = existing.id,
                    category = term.category,
                    difficulty = term.difficulty,
                    meaningMy = term.meaningMy,
                    meaningEn = term.meaningEn,
                    example = term.example,
                    codeSnippet = term.codeSnippet,
                    codeLanguage = term.codeLanguage,
                    usageNote = term.usageNote
                )
            } else {
                insert(term)
            }
        }
    }
}
