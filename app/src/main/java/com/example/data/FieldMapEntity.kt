package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

data class PointD(val x: Double, val y: Double)

data class LatLngD(val latitude: Double, val longitude: Double)

@Entity(tableName = "field_maps")
data class FieldMapEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val timestamp: Long = System.currentTimeMillis(),
    val distance: Double,
    val areaLocal: Double,
    val areaGps: Double,
    val pulseCount: Int,
    val localPointsJson: String,
    val gpsPointsJson: String,
    val isGpsUsed: Boolean
)

object CoordinateSerializer {
    fun serializeLocal(points: List<PointD>): String {
        return points.joinToString(";") { "${it.x},${it.y}" }
    }

    fun deserializeLocal(data: String): List<PointD> {
        if (data.isEmpty()) return emptyList()
        return data.split(";").mapNotNull {
            val parts = it.split(",")
            if (parts.size == 2) {
                val x = parts[0].toDoubleOrNull()
                val y = parts[1].toDoubleOrNull()
                if (x != null && y != null) PointD(x, y) else null
            } else null
        }
    }

    fun serializeGps(points: List<LatLngD>): String {
        return points.joinToString(";") { "${it.latitude},${it.longitude}" }
    }

    fun deserializeGps(data: String): List<LatLngD> {
        if (data.isEmpty()) return emptyList()
        return data.split(";").mapNotNull {
            val parts = it.split(",")
            if (parts.size == 2) {
                val lat = parts[0].toDoubleOrNull()
                val lon = parts[1].toDoubleOrNull()
                if (lat != null && lon != null) LatLngD(lat, lon) else null
            } else null
        }
    }
}
