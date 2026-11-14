package com.biskyt.mytvlauncher.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * An app's place on Home ([position], and whether it is [hidden]), with its [label] saved too: Home draws its first
 * frame from this table alone, before the launcher looks at the installed apps again (see AppRepository).
 */
@Entity(tableName = "app_place")
data class AppPlaceRow(@PrimaryKey val packageName: String, val label: String, val position: Int, val hidden: Boolean = false)

@Dao
interface AppPlaceDao {
    @Query("SELECT * FROM app_place ORDER BY position")
    fun observe(): Flow<List<AppPlaceRow>>

    @Query("SELECT * FROM app_place ORDER BY position")
    suspend fun all(): List<AppPlaceRow>

    @Query("DELETE FROM app_place")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(rows: List<AppPlaceRow>)

    /** Puts [rows] in place of every row, in one transaction, so Home never sees a half-written list. */
    @Transaction
    suspend fun replace(rows: List<AppPlaceRow>) {
        deleteAll()
        insertAll(rows)
    }
}

@Database(entities = [AppPlaceRow::class], version = 1, exportSchema = true)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun appPlaces(): AppPlaceDao

    companion object {
        fun open(context: Context): LauncherDatabase =
            Room.databaseBuilder(context, LauncherDatabase::class.java, "mytvlauncher.db").build()
    }
}
