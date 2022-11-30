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

    private val _index = MutableLiveData<Int>()
    val text: LiveData<Int> = Transformations.map(_index) {
        it
    }

    fun setIndex(index: Int) {
        _index.value = index
    }

    fun getAudios(id: Int) = lazyDeferred {
        audiosRepository.getAudios(id)
    }
}