package com.example.data

import kotlinx.coroutines.flow.Flow

class TranslationRepository(private val translationDao: TranslationDao) {
    val allTranslations: Flow<List<TranslationEntity>> = translationDao.getAllTranslations()

    suspend fun insert(translation: TranslationEntity) {
        translationDao.insertTranslation(translation)
    }

    suspend fun updateFavorite(id: Int, isFavorite: Boolean) {
        translationDao.updateFavoriteStatus(id, isFavorite)
    }

    suspend fun deleteById(id: Int) {
        translationDao.deleteTranslationById(id)
    }

    suspend fun clearAll() {
        translationDao.clearAllTranslations()
    }
}
