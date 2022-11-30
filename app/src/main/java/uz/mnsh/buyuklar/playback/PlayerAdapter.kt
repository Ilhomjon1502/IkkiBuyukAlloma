package uz.mnsh.buyuklar.playback

import android.media.MediaPlayer
import androidx.lifecycle.LiveData
import uz.mnsh.buyuklar.data.model.SongModel

import uz.mnsh.buyuklar.playback.PlaybackInfoListener.*

interface PlayerAdapter {

    fun isMediaPlayer(): Boolean

    fun isPlaying(): Boolean

    fun isReset(): Boolean

    fun getCurrentSong(): SongModel?

    @State
    fun getState(): Int

    fun getPlayerPosition(): Int

    fun getMediaPlayer(): MediaPlayer?

    fun initMediaPlayer()

    fun release()

    fun resumeOrPause()

    fun reset()

    fun instantReset()

    fun skip(isNext: Boolean)

    fun seekTo(position: Int)

    fun setPlaybackInfoListener(playbackInfoListener: PlaybackInfoListener)

    fun registerNotificationActionsReceiver(isRegister: Boolean)


    fun setCurrentSong(song: SongModel, songs: List<SongModel>)

    fun onPauseActivity()

    fun onResumeActivity()

}
