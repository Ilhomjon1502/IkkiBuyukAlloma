package uz.mnsh.buyuklar.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import uz.mnsh.buyuklar.data.db.model.AudioModel

@Dao
interface AudiosDao {

    //API dagi audiolarni databasega yozish bo'lsa yangilash
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAudios(audioModel: AudioModel)

    //topic bo'yicha saralab audiolarni qaytarib beradi
    @Query("select * from audios_table where topic == :id")
    fun getAudios(id :Int): LiveData<List<AudioModel>>

    //jadvalni o'chirish
    @Query("DELETE FROM audios_table")
    fun deleteAudios()

    //topic va rn bo'yicha audioni qaytarib beradi
    @Query("select * from audios_table where topic == :topID and rn == :index")
    fun getFirst(topID: String, index: Int): LiveData<AudioModel>
}