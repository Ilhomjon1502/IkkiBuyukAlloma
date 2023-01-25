package uz.mnsh.buyuklar.playback

import android.media.MediaPlayer
import androidx.lifecycle.LiveData
import uz.mnsh.buyuklar.data.model.SongModel

import uz.mnsh.buyuklar.playback.PlaybackInfoListener.*

//mediaPlayer uchun funksiyalar rejasi
interface PlayerAdapter {

    //mediaPlayer yaratilganmi
    fun isMediaPlayer(): Boolean

    //audi ijro etilyaptimi
    fun isPlaying(): Boolean

    //media qayta yangradimi
    fun isReset(): Boolean

    //o'sha paytdagi songModel ni qaytarish
    fun getCurrentSong(): SongModel?

    @State
    fun getState(): Int

    //audio ijrodagi current positionini qaytarish
    fun getPlayerPosition(): Int

    //mediaPlayer ni qaytarib berish
    fun getMediaPlayer(): MediaPlayer?

    //mediaPlayer ni yaratish
    fun initMediaPlayer()

    //mediaPlayer ni tugatish
    fun release()

    //play yoki pause
    fun resumeOrPause()

    //qayta ijro etish
    fun reset()

    //keyingi audio yoki qayta tinglash
    fun instantReset()

    //keyingiga o'tkazish va qaytarish. true - o'tkazish, false - qaytarish
    fun skip(isNext: Boolean)

    //audioni currentPosition iga o'tkazish
    fun seekTo(position: Int)

    //media o'zgarishini positon va holatni eshitib turuvchi
    fun setPlaybackInfoListener(playbackInfoListener: PlaybackInfoListener)

    //notificationReceiver ni register qilish yoki olib tashlash. true - register, false - unregister
    fun registerNotificationActionsReceiver(isRegister: Boolean)

    //tanlangan audio SongModel ni va tanlangan SongModelList ni o'rnatish
    fun setCurrentSong(song: SongModel, songs: List<SongModel>)

    //activity onPause ga tushganda
    fun onPauseActivity()

    //activity onResume ga tushganda
    fun onResumeActivity()

}
