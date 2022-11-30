package uz.mnsh.buyuklar.data.repository

import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.mnsh.buyuklar.data.db.AudiosDao
import uz.mnsh.buyuklar.data.db.model.AudioModel
import uz.mnsh.buyuklar.data.network.ApiService

class AudiosRepositoryImpl(
    private val audiosDao: AudiosDao,
    private val apiService: ApiService
) : AudiosRepository {

    override suspend fun getAudios(id: Int): LiveData<List<AudioModel>> {
        return withContext(Dispatchers.IO){
            return@withContext audiosDao.getAudios(id)
        }
    }

    override suspend fun getFirst(topID: String, index: Int): LiveData<AudioModel> {
        return withContext(Dispatchers.IO){
            return@withContext audiosDao.getFirst(topID, index)
        }
    }

    override suspend fun fetchingAudios() {
        val response = apiService.getAudios(10)
        if (response.isSuccessful && response.body()!!.data.isNotEmpty()){
            val response2 = apiService.getAudios(11)
            if (response2.isSuccessful && response2.body()!!.data.isNotEmpty()){
                audiosDao.deleteAudios()
                response.body()!!.data.forEach {
                    audiosDao.upsertAudios(it)
                }
                response2.body()!!.data.forEach {
                    audiosDao.upsertAudios(it)
                }
            }
        }
    }

}