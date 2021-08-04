package hr.dtakac.prognoza.database.dao

import androidx.room.*
import hr.dtakac.prognoza.database.entity.Place

@Dao
interface PlaceDao {
    @Query("SELECT * FROM Place WHERE id == :id")
    suspend fun get(id: String): Place?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(place: Place)

    @Query("SELECT * FROM Place ORDER BY fullName ASC")
    suspend fun getAll(): List<Place>

    @Delete
    suspend fun delete(place: Place)
}