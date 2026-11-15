package com.biskyt.mytvlauncher.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.biskyt.mytvlauncher.core.watchnext.CardShape
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

    @Query("UPDATE app_place SET hidden = :hidden WHERE packageName = :packageName")
    suspend fun setHidden(packageName: String, hidden: Boolean)

    @Insert
    suspend fun insertAll(rows: List<AppPlaceRow>)

    /** Puts [rows] in place of every row, in one transaction, so Home never sees a half-written list. */
    @Transaction
    suspend fun replace(rows: List<AppPlaceRow>) {
        deleteAll()
        insertAll(rows)
    }
}

/**
 * The last Watch Next items read from the TV, so that Home draws Continue watching at once, before the next read (see
 * WatchNextRepository). The columns are core's WatchNextItem.
 */
@Entity(tableName = "watch_next_item")
data class WatchNextRow(
    @PrimaryKey val key: String,
    val packageName: String,
    val title: String,
    val subtitle: String?,
    val pictureUri: String?,
    val shape: CardShape,
    val progress: Float?,
    val lastEngagementMs: Long,
    val intentUri: String?,
    val season: String? = null,
    val episode: String? = null,
    val leftMs: Long? = null
)

@Dao
interface WatchNextDao {
    @Query("SELECT * FROM watch_next_item")
    fun observe(): Flow<List<WatchNextRow>>

    @Query("SELECT * FROM watch_next_item")
    suspend fun all(): List<WatchNextRow>

    @Query("DELETE FROM watch_next_item")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(rows: List<WatchNextRow>)

    /** Puts [rows] in place of every row, in one transaction, so Home never sees a half-written row. */
    @Transaction
    suspend fun replace(rows: List<WatchNextRow>) {
        deleteAll()
        insertAll(rows)
    }
}

/**
 * The last app channels read from the TV, one row per item, so that Home draws them at once (see ChannelRepository).
 * [channelPosition] keeps the TV's order of the channels, [position] each channel's order of its items.
 */
@Entity(tableName = "channel_item")
data class ChannelItemRow(
    @PrimaryKey val key: String,
    val channelId: Long,
    val channelName: String,
    val channelPosition: Int,
    val packageName: String,
    val title: String,
    val subtitle: String?,
    val pictureUri: String?,
    val shape: CardShape,
    val progress: Float?,
    val position: Int,
    val intentUri: String?,
    /** Whether the TV shows the item's channel on its own home screen (see AppChannel.browsable). */
    @ColumnInfo(defaultValue = "1") val channelBrowsable: Boolean = true
)

@Dao
interface ChannelItemDao {
    @Query("SELECT * FROM channel_item ORDER BY channelPosition, position")
    fun observe(): Flow<List<ChannelItemRow>>

    @Query("SELECT * FROM channel_item ORDER BY channelPosition, position")
    suspend fun all(): List<ChannelItemRow>

    @Query("DELETE FROM channel_item")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(rows: List<ChannelItemRow>)

    /** Puts [rows] in place of every row, in one transaction, so Home never sees half-written rows. */
    @Transaction
    suspend fun replace(rows: List<ChannelItemRow>) {
        deleteAll()
        insertAll(rows)
    }
}

/**
 * Something the user took off Home: [kind] says what ([HiddenKind]), and [key] which one (a package name, or a Watch
 * Next item's key).
 */
@Entity(tableName = "hidden", primaryKeys = ["kind", "key"])
data class HiddenRow(val kind: String, val key: String)

/**
 * The kinds of [HiddenRow]: saved by name, so never rename one. An app hidden from Home is not here: that is
 * `app_place.hidden`, with the app's place.
 */
object HiddenKind {
    /** All of an app's Watch Next items (Settings › Home rows). */
    const val WATCH_NEXT_APP = "watch_next_app"

    /** One Watch Next item (hold OK on it › Hide this item). */
    const val WATCH_NEXT_ITEM = "watch_next_item"

    /** An app channel's row, by the channel's id (Settings › Home rows, or hold OK on an item › Hide this channel). */
    const val CHANNEL = "channel"

    /** Not hidden: an app channel the TV does not show that the user turned on (Settings › Home rows). */
    const val CHANNEL_ON = "channel_on"
}

@Dao
interface HiddenDao {
    @Query("SELECT `key` FROM hidden WHERE kind = :kind")
    fun observe(kind: String): Flow<List<String>>

    @Query("SELECT * FROM hidden WHERE kind IN (:kinds)")
    fun observeKinds(kinds: List<String>): Flow<List<HiddenRow>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun hide(row: HiddenRow)

    @Query("DELETE FROM hidden WHERE kind = :kind AND `key` = :key")
    suspend fun show(kind: String, key: String)

    @Query("DELETE FROM hidden WHERE kind = :kind")
    suspend fun showAll(kind: String)

    @Query("SELECT * FROM hidden")
    suspend fun all(): List<HiddenRow>

    /** Replaces every row: a restore from a backup. */
    @Transaction
    suspend fun replace(rows: List<HiddenRow>) {
        deleteAll()
        insertAll(rows)
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rows: List<HiddenRow>)

    @Query("DELETE FROM hidden")
    suspend fun deleteAll()
}

@Database(
    entities = [AppPlaceRow::class, WatchNextRow::class, HiddenRow::class, ChannelItemRow::class],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5)
    ]
)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun appPlaces(): AppPlaceDao

    abstract fun watchNext(): WatchNextDao

    abstract fun hidden(): HiddenDao

    abstract fun channelItems(): ChannelItemDao

    companion object {
        fun open(context: Context): LauncherDatabase =
            Room.databaseBuilder(context, LauncherDatabase::class.java, "mytvlauncher.db").build()
    }
}
