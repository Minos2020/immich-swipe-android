package com.example.immichswipe.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.immichswipe.data.local.dao.SwipeDecisionDao
import com.example.immichswipe.data.local.entity.SwipeDecisionEntity

/**
 * La base de données principale de l'application.
 * Elle centralise les accès via les DAOs.
 */
@Database(
    entities = [SwipeDecisionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun swipeDecisionDao(): SwipeDecisionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "immich_swipe_database"
                )
                // Stratégie de migration simple : on détruit et on recrée si la version change
                // Utile pendant la phase de développement
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
