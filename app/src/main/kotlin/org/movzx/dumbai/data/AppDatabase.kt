package org.movzx.dumbai.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.movzx.dumbai.model.FavoriteImage

@Database(entities = [FavoriteImage::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteImageDao(): FavoriteImageDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE
                ?: synchronized(this) {
                    val instance =
                        Room.databaseBuilder(
                                context.applicationContext,
                                AppDatabase::class.java,
                                "dumbai_database",
                            )
                            .fallbackToDestructiveMigration(true)
                            .build()

                    INSTANCE = instance

                    instance
                }
        }
    }
}
