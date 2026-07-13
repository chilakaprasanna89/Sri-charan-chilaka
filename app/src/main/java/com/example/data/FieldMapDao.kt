package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FieldMapDao {
    @Query("SELECT * FROM field_maps ORDER BY timestamp DESC")
    fun getAllFieldMaps(): Flow<List<FieldMapEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFieldMap(fieldMap: FieldMapEntity)

    @Query("DELETE FROM field_maps WHERE id = :id")
    suspend fun deleteFieldMapById(id: Int)

    @Query("DELETE FROM field_maps")
    suspend fun clearAllFieldMaps()
}
