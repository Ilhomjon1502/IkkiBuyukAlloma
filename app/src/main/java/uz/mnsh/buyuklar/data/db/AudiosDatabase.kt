package uz.mnsh.buyuklar.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import uz.mnsh.buyuklar.data.db.model.AudioModel

@Database(
    entities = [AudioModel::class],
    version = 2
)
abstract class AudiosDatabase: RoomDatabase() {
    abstract fun audiosDao(): AudiosDao

    companion object{
        @Volatile private var instance: AudiosDatabase? = null
        private val LOCK = Any()

        operator fun invoke(context: Context) = instance ?: synchronized(LOCK) {
            instance ?: buildDatabase(context).also { instance = it }
        }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext,
                AudiosDatabase::class.java, "audios.db").fallbackToDestructiveMigration()
                .build()
    }
}