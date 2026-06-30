package com.minos2020.immichswipe.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.minos2020.immichswipe.data.local.dao.SwipeDecisionDao
import com.minos2020.immichswipe.data.local.entity.SwipeDecisionEntity

/**
 * La base de données principale de l'application.
 * Elle centralise les accès via les DAOs.
 */
@Database(
    entities = [SwipeDecisionEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun swipeDecisionDao(): SwipeDecisionDao

    companion object {
        /**
         * Migration ROOM de la version 2 vers la version 3.
         * - Ajoute la colonne 'fileSize'.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE swipe_decisions ADD COLUMN fileSize INTEGER DEFAULT NULL")
            }
        }

        /**
         * Migration ROOM de la version 3 vers la version 4.
         * - Ajoute la colonne 'userId' à la clé primaire.
         * - Comme on ne peut pas modifier la PK en SQLite, on recrée la table.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Créer la nouvelle table avec la nouvelle structure
                db.execSQL("""
                    CREATE TABLE swipe_decisions_new (
                        assetId TEXT NOT NULL,
                        albumId TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        decision TEXT NOT NULL,
                        fileSize INTEGER,
                        createdAt INTEGER NOT NULL,
                        isSynced INTEGER NOT NULL,
                        PRIMARY KEY(assetId, albumId, userId)
                    )
                """.trimIndent())

                // 2. Copier les données existantes.
                // On met 'legacy_user' temporairement, il sera mis à jour au premier lancement.
                db.execSQL("""
                    INSERT INTO swipe_decisions_new (assetId, albumId, userId, decision, fileSize, createdAt, isSynced)
                    SELECT assetId, albumId, 'legacy_user', decision, fileSize, createdAt, isSynced
                    FROM swipe_decisions
                """.trimIndent())

                // 3. Supprimer l'ancienne table et renommer la nouvelle
                db.execSQL("DROP TABLE swipe_decisions")
                db.execSQL("ALTER TABLE swipe_decisions_new RENAME TO swipe_decisions")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                                context.applicationContext,
                                AppDatabase::class.java,
                                "immich_swipe_database"
                            )
                    // On enregistre nos scripts de migration
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration(false)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
