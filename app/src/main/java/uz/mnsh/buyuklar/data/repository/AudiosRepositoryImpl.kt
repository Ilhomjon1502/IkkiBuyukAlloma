package uz.mnsh.buyuklar.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.mnsh.buyuklar.data.db.AudiosDao
import uz.mnsh.buyuklar.data.db.model.AudioModel
import uz.mnsh.buyuklar.data.network.ApiService

//API dagi audios ro'yhatini database bilan yangilash, databasedan audios ro'yhat qaytarib berish
private const val TAG = "AudiosRepositoryImpl"
class AudiosRepositoryImpl(
    private val audiosDao: AudiosDao,
    private val apiService: ApiService
) : AudiosRepository {

    //topic bo'yicha db dan AudiosModellarni qaytarib berish
    override suspend fun getAudios(id: Int): LiveData<List<AudioModel>> {
        return withContext(Dispatchers.IO) {
            return@withContext audiosDao.getAudios(id)
        }
    }

    //topic va rn bo'yucha db dan o'qib berish
    override suspend fun getFirst(topID: String, index: Int): LiveData<AudioModel> {
        return withContext(Dispatchers.IO) {
            return@withContext audiosDao.getFirst(topID, index)
        }
    }

    //API dan topic 10 va 11 ma'lumotlarni olib kelib db ga yozish
    override suspend fun fetchingAudios() {
        try {
            val response = apiService.getAudios(10)
            if (response.isSuccessful && response.body()!!.data.isNotEmpty()) {
                val response2 = apiService.getAudios(11)
                if (response2.isSuccessful && response2.body()!!.data.isNotEmpty()) {
                    audiosDao.deleteAudios()
                    response.body()!!.data.forEach {
                        audiosDao.upsertAudios(it)
                    }
                    response2.body()!!.data.forEach {
                        audiosDao.upsertAudios(it)
                    }
                }
            }
        }catch (e:Exception){
            Log.e(TAG, "getAudio: ${e.message}")
        }
    }

}