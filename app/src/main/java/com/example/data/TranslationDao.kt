package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationDao {
    @Query("SELECT * FROM translations ORDER BY timestamp DESC")
    fun getAllTranslations(): Flow<List<TranslationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslation(translation: TranslationEntity)

    @Update
    suspend fun updateTranslation(translation: TranslationEntity)

    @Query("UPDATE translations SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Int, isFavorite: Boolean)

    @Query("DELETE FROM translations WHERE id = :id")
    suspend fun deleteTranslationById(id: Int)

    @Query("DELETE FROM translations")
    suspend fun clearAllTranslations()
}
