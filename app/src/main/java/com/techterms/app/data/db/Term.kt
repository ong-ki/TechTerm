package com.techterms.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "terms")
data class Term(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val term: String,
    val category: String,
    val difficulty: String,
    val meaningMy: String,
    val meaningEn: String,
    val example: String,
    val codeSnippet: String? = null,
    val codeLanguage: String? = null,
    val usageNote: String? = null
)

@Serializable
data class TermSeedDto(
    val term: String = "",
    val category: String = "",
    val difficulty: String = "",
    val meaningMy: String = "",
    val meaningEn: String = "",
    val example: String = "",
    val codeSnippet: String? = null,
    val codeLanguage: String? = null,
    val usageNote: String? = null
) {
    fun toEntity(): Term = Term(
        term = term.trim(),
        category = category.trim(),
        difficulty = difficulty.trim(),
        meaningMy = meaningMy.trim(),
        meaningEn = meaningEn.trim(),
        example = example.trim(),
        codeSnippet = codeSnippet?.trimEnd()?.takeIf { it.isNotBlank() },
        codeLanguage = codeLanguage?.trim()?.takeIf { it.isNotEmpty() },
        usageNote = usageNote?.trim()?.takeIf { it.isNotEmpty() }
    )
}

object TermCategories {
    val all: List<String> = listOf(
        "Programming",
        "Networking",
        "AI",
        "Security",
        "Hardware",
        "Web",
        "Mobile",
        "Database",
        "Cloud",
        "Git & Version Control",
        "Linux & Terminal",
        "DevOps",
        "Software Design",
        "Data & Formats"
    )
}
