package uz.mnsh.buyuklar.ui.fragment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import uz.mnsh.buyuklar.data.repository.AudiosRepository
import uz.mnsh.buyuklar.utils.lazyDeferred

class PageViewModel(
    private val audiosRepository: AudiosRepository
) : ViewModel() {

    private val _index = MutableLiveData<Int>()//1-alloma yoki 2-alloma
    val text: LiveData<Int> = Transformations.map(_index) {
        it
    }

    //index 1 bo'lsa 1-allloma topicId 10, 2 bo'lsa 2-alloma topicId 11
    fun setIndex(index: Int) {
        _index.value = index
    }

    fun getAudios(id: Int) = lazyDeferred {
        audiosRepository.getAudios(id)
    }
}