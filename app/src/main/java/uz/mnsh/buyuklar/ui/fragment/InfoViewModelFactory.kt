package uz.mnsh.buyuklar.ui.fragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import uz.mnsh.buyuklar.data.repository.AudiosRepository

class InfoViewModelFactory(
    private val audiosRepository: AudiosRepository
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return InfoViewModel(audiosRepository) as T
    }
}