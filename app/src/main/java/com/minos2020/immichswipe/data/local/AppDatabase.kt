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
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun swipeDecisionDao(): SwipeDecisionDao

    companion object {
        /**
         * Migration ROOM de la version 2 vers la version 3.
         * Ajoute la colonne 'fileSize' à la table 'swipe_decisions'.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE swipe_decisions ADD COLUMN fileSize INTEGER DEFAULT NULL")
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
                // On enregistre nos scripts de migration ici
                .addMigrations(MIGRATION_2_3)
                // On garde ceci par sécurité pour les versions très anciennes sans migration définie,
                // mais Room utilisera MIGRATION_2_3 s'il détecte une base en V2.
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
