package com.streamdeck.control.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val columns: Int = 4,
    val rows: Int = 4,
    val isFavorite: Boolean = false,
)

@Entity(tableName = "deck_buttons")
data class DeckButtonEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val title: String,
    val subtitle: String = "",
    val emoji: String = "●",
    val actionType: String,
    val actionValue: String,
    val position: Int,
    val confirmationRequired: Boolean = false,
    @ColumnInfo(defaultValue = "'#FFFFFF'") val backgroundColor: String = "#FFFFFF",
    @ColumnInfo(defaultValue = "'#183D32'") val textColor: String = "#183D32",
    @ColumnInfo(defaultValue = "'#183D32'") val iconColor: String = "#183D32",
    @ColumnInfo(defaultValue = "'#E8E5DC'") val borderColor: String = "#E8E5DC",
    @ColumnInfo(defaultValue = "0.0") val borderWidth: Float = 0f,
    @ColumnInfo(defaultValue = "20") val cornerRadius: Int = 20,
    @ColumnInfo(defaultValue = "2") val shadowElevation: Int = 2,
    @ColumnInfo(defaultValue = "12") val fontSize: Int = 12,
    @ColumnInfo(defaultValue = "23") val iconSize: Int = 23,
    @ColumnInfo(defaultValue = "1") val spanWidth: Int = 1,
    @ColumnInfo(defaultValue = "1") val spanHeight: Int = 1,
    @ColumnInfo(defaultValue = "1") val haptic: Boolean = true,
    @ColumnInfo(defaultValue = "1") val pressAnimation: Boolean = true,
    @ColumnInfo(defaultValue = "''") val imageUri: String = "",
    @ColumnInfo(defaultValue = "'dashboard'") val iconName: String = "dashboard",
    @ColumnInfo(defaultValue = "'auto'") val iconMode: String = "auto",
)

@Entity(tableName = "macros")
data class MacroEntity(
    @PrimaryKey val id: String,
    val name: String,
    val actionsJson: String,
)

@Dao
interface DeckDao {
    @Query("SELECT * FROM profiles ORDER BY name")
    fun observeProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun profile(id: String): ProfileEntity?

    @Query("SELECT * FROM profiles ORDER BY name")
    suspend fun allProfiles(): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE isFavorite = 1 ORDER BY name")
    fun observeFavorites(): Flow<List<ProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteProfile(id: String)

    @Query("SELECT * FROM deck_buttons WHERE profileId = :profileId ORDER BY position")
    fun observeButtons(profileId: String): Flow<List<DeckButtonEntity>>

    @Query("SELECT * FROM deck_buttons ORDER BY title")
    suspend fun allButtons(): List<DeckButtonEntity>

    @Query("SELECT * FROM deck_buttons WHERE id = :id LIMIT 1")
    suspend fun button(id: String): DeckButtonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveButton(button: DeckButtonEntity)

    @Query("DELETE FROM deck_buttons WHERE id = :id")
    suspend fun deleteButton(id: String)

    @Query("DELETE FROM deck_buttons WHERE profileId = :profileId")
    suspend fun deleteButtonsForProfile(profileId: String)

    @Query("SELECT * FROM macros ORDER BY name")
    fun observeMacros(): Flow<List<MacroEntity>>

    @Query("SELECT * FROM macros ORDER BY name")
    suspend fun allMacros(): List<MacroEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMacro(macro: MacroEntity)

    @Query("DELETE FROM macros WHERE id = :id")
    suspend fun deleteMacro(id: String)
}

@Database(
    entities = [ProfileEntity::class, DeckButtonEntity::class, MacroEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class DeckDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao

    companion object {
        @Volatile private var instance: DeckDatabase? = null

        fun get(context: Context): DeckDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                DeckDatabase::class.java,
                "stream-deck.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
        }

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE deck_buttons ADD COLUMN backgroundColor TEXT NOT NULL DEFAULT '#FFFFFF'")
                db.execSQL("ALTER TABLE deck_buttons ADD COLUMN textColor TEXT NOT NULL DEFAULT '#183D32'")
                db.execSQL("ALTER TABLE deck_buttons ADD COLUMN iconColor TEXT NOT NULL DEFAULT '#183D32'")
                db.execSQL("ALTER TABLE deck_buttons ADD COLUMN borderColor TEXT NOT NULL DEFAULT '#E8E5DC'")
                db.execSQL("ALTER TABLE deck_buttons ADD COLUMN borderWidth REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE deck_buttons ADD COLUMN cornerRadius INTEGER NOT NULL DEFAULT 20")
                db.execSQL("ALTER TABLE deck_buttons ADD COLUMN shadowElevation INTEGER NOT NULL DEFAULT 2")
                db.execSQL("ALTER TABLE deck_buttons ADD COLUMN fontSize INTEGER NOT NULL DEFAULT 12")
                db.execSQL("ALTER TABLE deck_buttons ADD COLUMN iconSize INTEGER NOT NULL DEFAULT 23")
                db.execSQL("ALTER TABLE deck_buttons ADD COLUMN spanWidth INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE deck_buttons ADD COLUMN spanHeight INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE deck_buttons ADD COLUMN haptic INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE deck_buttons ADD COLUMN pressAnimation INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE deck_buttons ADD COLUMN imageUri TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE deck_buttons ADD COLUMN iconName TEXT NOT NULL DEFAULT 'dashboard'")
                db.execSQL("ALTER TABLE deck_buttons ADD COLUMN iconMode TEXT NOT NULL DEFAULT 'auto'")
            }
        }
    }
}
