package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "translations")
data class TranslationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalText: String,
    val translatedText: String,
    val sourceLangName: String,
    val targetLangName: String,
    val sourceLangCode: String,
    val targetLangCode: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
