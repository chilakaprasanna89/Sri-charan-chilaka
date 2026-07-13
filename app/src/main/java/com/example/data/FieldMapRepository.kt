package com.example.data

import kotlinx.coroutines.flow.Flow

class FieldMapRepository(private val fieldMapDao: FieldMapDao) {
    val allFieldMaps: Flow<List<FieldMapEntity>> = fieldMapDao.getAllFieldMaps()

    suspend fun insert(fieldMap: FieldMapEntity) {
        fieldMapDao.insertFieldMap(fieldMap)
    }

    suspend fun deleteById(id: Int) {
        fieldMapDao.deleteFieldMapById(id)
    }

    suspend fun clearAll() {
        fieldMapDao.clearAllFieldMaps()
    }
}
