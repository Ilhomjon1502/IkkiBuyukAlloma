package uz.mnsh.buyuklar.data.repository

import androidx.lifecycle.LiveData
import uz.mnsh.buyuklar.data.db.model.AudioModel

//funksiyalarni bir joyga yig'ishni rejasi, ro'yhati
interface AudiosRepository {
    suspend fun getAudios(id: Int): LiveData<List<AudioModel>>
    suspend fun getFirst(topID: String, index: Int): LiveData<AudioModel>
    suspend fun fetchingAudios()
}