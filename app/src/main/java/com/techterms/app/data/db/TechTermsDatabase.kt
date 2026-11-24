package com.techterms.app.data.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Term::class],
    version = 2,
    exportSchema = true
)
abstract class TechTermsDatabase : RoomDatabase() {
    abstract fun termDao(): TermDao

    companion object {
        private const val TAG = "TechTerms"
        private const val DATABASE_NAME = "techterms.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    val existingColumns = getTableColumns(db, "terms")
                    if ("codeSnippet" !in existingColumns) {
                        db.execSQL("ALTER TABLE terms ADD COLUMN codeSnippet TEXT")
                    }
                    if ("codeLanguage" !in existingColumns) {
                        db.execSQL("ALTER TABLE terms ADD COLUMN codeLanguage TEXT")
                    }
                    if ("usageNote" !in existingColumns) {
                        db.execSQL("ALTER TABLE terms ADD COLUMN usageNote TEXT")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Room migration 1->2 failed", e)
                    throw e
                }
            }
        }

        private fun getTableColumns(db: SupportSQLiteDatabase, tableName: String): Set<String> {
            val columns = mutableSetOf<String>()
            db.query("PRAGMA table_info($tableName)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex >= 0) {
                    while (cursor.moveToNext()) {
                        columns.add(cursor.getString(nameIndex))
                    }
                }
            }
            return columns
        }

        @Volatile
        private var instance: TechTermsDatabase? = null

        fun getInstance(context: Context): TechTermsDatabase {
            return instance ?: synchronized(this) {
                instance ?: try {
                    Room.databaseBuilder(
                        context.applicationContext,
                        TechTermsDatabase::class.java,
                        DATABASE_NAME
                    )
                        .addMigrations(MIGRATION_1_2)
                        .build()
                        .also { instance = it }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create TechTermsDatabase", e)
                    throw e
                }
            }
        }
    }
}
