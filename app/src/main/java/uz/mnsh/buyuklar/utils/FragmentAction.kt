package uz.mnsh.buyuklar.utils

import uz.mnsh.buyuklar.data.model.SongModel

//recycleview item i bosilishini adapter dan fragment ga olib o'tish
interface FragmentAction {

    fun itemPlay(model: SongModel)
}