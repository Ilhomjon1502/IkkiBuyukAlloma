package uz.mnsh.buyuklar.playback

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.PowerManager
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import uz.mnsh.buyuklar.data.model.SongModel
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class MediaPlayerHolder(private val mMusicService: MusicService?) :
        PlayerAdapter, MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {
    private val mContext: Context = mMusicService!!.applicationContext
    private val mAudioManager: AudioManager
    private var mMediaPlayer: MediaPlayer? = null
    private var mPlaybackInfoListener: PlaybackInfoListener? = null
    private var mExecutor: ScheduledExecutorService? = null
    private var mSeekBarPositionUpdateTask: Runnable? = null
    private var mSelectedSong: SongModel? = null
    private var mSongs: List<SongModel>? = null

    //faqat 1 ta audio yangrashi
    private var sReplaySong = false
    @PlaybackInfoListener.State
    private var mState: Int = 0
    private var mNotificationActionsReceiver: NotificationReceiver? = null
    private var mMusicNotificationManager: MusicNotificationManager? = null
    private var mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK
    private var mPlayOnFocusGain: Boolean = false

    //audio focusi o'zgarishni eshitish
    private val mOnAudioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> mCurrentAudioFocusState = AUDIO_FOCUSED
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                mCurrentAudioFocusState = AUDIO_NO_FOCUS_CAN_DUCK
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK
                mPlayOnFocusGain = isMediaPlayer() && mState == PlaybackInfoListener.State.PLAYING || mState == PlaybackInfoListener.State.RESUMED
            }
            AudioManager.AUDIOFOCUS_LOSS ->
                mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK
        }

        if (mMediaPlayer != null) {
            configurePlayerState()
        }
    }

    //constructor da audioManager obekti yaratilib olinmoqda
    init {
        mAudioManager = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    //MusicService ga notification uchun button click actionlar ni register qilish
    //bluetooth uchun ham bo'lishi mumkin
    private fun registerActionsReceiver() {
        mNotificationActionsReceiver = NotificationReceiver()
        val intentFilter = IntentFilter()

        intentFilter.addAction(MusicNotificationManager.PREV_ACTION)
        intentFilter.addAction(MusicNotificationManager.PLAY_PAUSE_ACTION)
        intentFilter.addAction(MusicNotificationManager.NEXT_ACTION)
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        intentFilter.addAction(Intent.ACTION_HEADSET_PLUG)
        intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)

        mMusicService!!.registerReceiver(mNotificationActionsReceiver, intentFilter)
    }

    //MusicServicedan notificationReceiver ni unregister qilish
    private fun unregisterActionsReceiver() {
        if (mMusicService != null && mNotificationActionsReceiver != null) {
            try {
                mMusicService.unregisterReceiver(mNotificationActionsReceiver)
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }

        }
    }

    //ture - MusicService ga NotificationReceiver ni register aks holda unregister
    override fun registerNotificationActionsReceiver(isRegister: Boolean) {

        if (isRegister) {
            registerActionsReceiver()
        } else {
            unregisterActionsReceiver()
        }
    }

    //SongModel ni qaytarish
    override fun getCurrentSong(): SongModel? {
        return mSelectedSong
    }


    //audioni tanlanganini o'rnatish song va audiolar ro'yhatini o'rnatish songs
    override fun setCurrentSong(song: SongModel, songs: List<SongModel>) {
        mSelectedSong = song
        mSongs = songs
    }

    //media eshituvchisiga tugallanganini aytish
    override fun onCompletion(mediaPlayer: MediaPlayer) {
        if (mPlaybackInfoListener != null) {
            mPlaybackInfoListener!!.onStateChanged(PlaybackInfoListener.State.COMPLETED)
        }

        if (sReplaySong) { // agar boshqa audio yo'q bo'lsa
            if (isMediaPlayer()) {//mediaPlayer null bo'lmasa (o'rnatilgan bo'lsa)
                resetSong() //ijrodagi audioni qayta ijro et
            }
            sReplaySong = false //qayta ijroni false qil
        } else { //aks holda
            skip(true) //keyingi audioga o't
        }
    }

    //activity onResume bo'lganda (seekbar progress o'zgarishini boshlash)
    override fun onResumeActivity() {
        startUpdatingCallbackWithPosition()
    }

    //activity onPause bo'lganda (seekbar progress to'xtatish)
    override fun onPauseActivity() {
        stopUpdatingCallbackWithPosition()
    }

    //audioni focusga olishga harakat
    private fun tryToGetAudioFocus() {

        val result = mAudioManager.requestAudioFocus(
                mOnAudioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN)
        mCurrentAudioFocusState = if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            AUDIO_FOCUSED
        } else {
            AUDIO_NO_FOCUS_NO_DUCK
        }
    }

    //audio focus ni yo'qotish
    private fun giveUpAudioFocus() {
        if (mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK
        }
    }

    //PlaybackInfoListener media ma'lumotlarini eshituvchisini yaratish
    override fun setPlaybackInfoListener(playbackInfoListener: PlaybackInfoListener) {
        mPlaybackInfoListener = playbackInfoListener
    }

    //PlaybackInfoListener media ma'lumotlari eshituvchisini holatini o'rnatish
    private fun setStatus(@PlaybackInfoListener.State state: Int) {

        mState = state
        if (mPlaybackInfoListener != null) {
            mPlaybackInfoListener!!.onStateChanged(state)
        }
    }

    //mediaPlayer ni play qilish
    private fun resumeMediaPlayer() {
        if (!isPlaying()) {
            mMediaPlayer!!.start()
            setStatus(PlaybackInfoListener.State.RESUMED)
            mMusicService!!.startForeground(MusicNotificationManager.NOTIFICATION_ID, mMusicNotificationManager!!.createNotification())
        }
    }

    //mediaPlayer ni pause qilish
    private fun pauseMediaPlayer() {
        setStatus(PlaybackInfoListener.State.PAUSED)
        mMediaPlayer!!.pause()
        mMusicService!!.stopForeground(false)
        mMusicNotificationManager!!.notificationManager.notify(MusicNotificationManager.NOTIFICATION_ID, mMusicNotificationManager!!.createNotification())
    }

    //audio ni boshidan ijro et
    private fun resetSong() {
        mMediaPlayer!!.seekTo(0)
        mMediaPlayer!!.start()
        setStatus(PlaybackInfoListener.State.PLAYING)
    }

    //asosan seekbar o'zgarishini boshlash
    private fun startUpdatingCallbackWithPosition() {
        if (mExecutor == null) {
            mExecutor = Executors.newSingleThreadScheduledExecutor()
        }

        //seekbar uchun oqim yangilash agar u null bo'lsa
        if (mSeekBarPositionUpdateTask == null) {
            mSeekBarPositionUpdateTask = Runnable {
                updateProgressCallbackTask()
            }
        }

        //sekbarni xususiyatlarini o'rnatish
        mExecutor!!.scheduleAtFixedRate(
            mSeekBarPositionUpdateTask,
            0,
            1000,
            TimeUnit.MILLISECONDS
        )
    }

    //asosan seekbar o'zgarishini to'xtatish
    private fun stopUpdatingCallbackWithPosition() {
        if (mExecutor != null) {
            mExecutor!!.shutdownNow()
            mExecutor = null
            mSeekBarPositionUpdateTask = null
        }
    }

    //asosan seekbar o'zgarishi yangilash
    private fun updateProgressCallbackTask() {
        try {
            if (isMediaPlayer() && mMediaPlayer!!.isPlaying) {//mediaPlayer yaratilgan bo'lsa va u ijro qilayotgan bo'lsa
                val currentPosition = mMediaPlayer!!.currentPosition //ijro positionni ol
                if (mPlaybackInfoListener != null) {//media ma'lumotlari tinglovchisi null bo'lmasa
                    mPlaybackInfoListener?.onPositionChanged(currentPosition) //position o'zgarish funksiyasini ishlat
                }
            }
        }catch (e: Exception){}
    }

    //keyingi audio yoki qayta tinglash
    override fun instantReset() {
        if (isMediaPlayer()) {//mediaPlayer yaratilganmi null emasmi
            if (mMediaPlayer!!.currentPosition < 5000) {
                skip(false)
            } else {
                resetSong()
            }
        }
    }

    //mediaPlayer ni yaratish
    override fun initMediaPlayer() {

        try {
            if (mMediaPlayer != null) {
                mMediaPlayer!!.reset()
            } else {
                mMediaPlayer = MediaPlayer()

                mMediaPlayer!!.setOnPreparedListener(this)
                mMediaPlayer!!.setOnCompletionListener(this)
                mMediaPlayer!!.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK)
                mMediaPlayer!!.setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                mMusicNotificationManager = mMusicService!!.musicNotificationManager
            }
            tryToGetAudioFocus()//qurilma mediasini focus olishga harakat
            mMediaPlayer!!.setDataSource(mSelectedSong?.songPath)//mediaPlayer yo'lini o'rsatish
            mMediaPlayer!!.prepare()
        } catch (e: Exception) {
            e.printStackTrace()
            skip(true)
        }

    }


    //mediaPlayer ni qaytarib berish
    override fun getMediaPlayer(): MediaPlayer? {
        return mMediaPlayer
    }

    //media tayyorlash, seekbar va media eshituvchisini o'rnatish
    override fun onPrepared(mediaPlayer: MediaPlayer) {
        startUpdatingCallbackWithPosition()
        setStatus(PlaybackInfoListener.State.PLAYING)
    }

    //media ni to'lqi to'xtatish, focus ni ham olib tashlash
    override fun release() {
        if (isMediaPlayer()) {
            mMediaPlayer!!.release()
            mMediaPlayer = null
            giveUpAudioFocus()
            unregisterActionsReceiver()
        }
    }

    //audio yangrayaptimi
    override fun isPlaying(): Boolean {
        return isMediaPlayer() && mMediaPlayer!!.isPlaying
    }

    //play pause qilish
    override fun resumeOrPause() {
        if (isPlaying()) {
            pauseMediaPlayer()
        } else {
            resumeMediaPlayer()
        }
    }

    //PlaybackInfoListener ning
    @PlaybackInfoListener.State
    override fun getState(): Int {
        return mState
    }

    //mediaPlayer o'rnatilmagan null bo'lsa false, o'rnatilgan bo'lsa true
    override fun isMediaPlayer(): Boolean {
        return mMediaPlayer != null
    }

    //audio qayta ijro etilishi
    override fun reset() {
        sReplaySong = !sReplaySong
    }

    //audio qayta ijro etilishi true bo'lsa qayta ijro etiladi
    override fun isReset(): Boolean {
        return sReplaySong
    }

    //keyingiga o'tkazish
    override fun skip(isNext: Boolean) {
        getSkipSong(isNext)
    }

    //audioni o'tkazish yoki qaytarish yoki boshiga o'tkazish
    private fun getSkipSong(isNext: Boolean) {
        val currentIndex = mSongs!!.indexOf(mSelectedSong)

        val index: Int

        try {//keyingiga o'tkazish yoki qaytarish
            index = if (isNext) currentIndex + 1 else currentIndex - 1
            mSelectedSong = mSongs!![index]
        } catch (e: IndexOutOfBoundsException) {
            try {//index topilmasa boshiga o'tkazish
                mSelectedSong =
                    if (currentIndex != 0)
                        mSongs!![0]
                    else
                        mSongs!![mSongs!!.size - 1]

            }catch (e:Exception){
                Toast.makeText(mContext, "Media ijrosida (list bo'shlgigida) Xatolik \n ${e.message}", Toast.LENGTH_SHORT).show()
            }
            e.printStackTrace()
        }
        initMediaPlayer()
    }

    //audioni ozgina o'tkazish
    override fun seekTo(position: Int) {
        if (isMediaPlayer()) {//mediaPlayer yaratilgan bo'lsa
            mMediaPlayer!!.seekTo(position)
        }
    }

    //mediaPlayer ni current positionini qaytarish (ijrodagi)
    override fun getPlayerPosition(): Int {
        return mMediaPlayer!!.currentPosition
    }

    //mediaPlayerni sozlash
    private fun configurePlayerState() {

        if (mCurrentAudioFocusState == AUDIO_NO_FOCUS_NO_DUCK) {
            pauseMediaPlayer()//agar media focus i olingan bo'lsa pause qilib qo'y
        } else {

            //media ovozini pasaytirib yoki ko'tarib davom etish
            if (mCurrentAudioFocusState == AUDIO_NO_FOCUS_CAN_DUCK) {
                mMediaPlayer!!.setVolume(VOLUME_DUCK, VOLUME_DUCK)
            } else {
                mMediaPlayer!!.setVolume(VOLUME_NORMAL, VOLUME_NORMAL)
            }

            if (mPlayOnFocusGain) {
                resumeMediaPlayer()//mediaPlayerni play qilib davom etish
                mPlayOnFocusGain = false
            }
        }
    }

    //notification actionlarini boshqarish
    private inner class NotificationReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            // TODO Auto-generated method stub
            val action = intent.action

            if (action != null) {

                when (action) {
                    MusicNotificationManager.PREV_ACTION -> instantReset()
                    MusicNotificationManager.PLAY_PAUSE_ACTION -> resumeOrPause()
                    MusicNotificationManager.NEXT_ACTION -> skip(true)

                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> if (mSelectedSong != null) {
                        pauseMediaPlayer()
                    }
                    BluetoothDevice.ACTION_ACL_CONNECTED -> if (mSelectedSong != null && !isPlaying()) {
                        resumeMediaPlayer()
                    }
                    Intent.ACTION_HEADSET_PLUG -> if (mSelectedSong != null) {
                        when (intent.getIntExtra("state", -1)) {
                            //0 means disconnected
                            0 -> pauseMediaPlayer()
                            //1 means connected
                            1 -> if (!isPlaying()) {
                                resumeMediaPlayer()
                            }
                        }
                    }
                    AudioManager.ACTION_AUDIO_BECOMING_NOISY -> if (isPlaying()) {
                        pauseMediaPlayer()
                    }
                }
            }
        }
    }

    companion object {
        private val VOLUME_DUCK = 0.2f
        private val VOLUME_NORMAL = 1.0f
        private val AUDIO_NO_FOCUS_NO_DUCK = 0
        private val AUDIO_NO_FOCUS_CAN_DUCK = 1
        private val AUDIO_FOCUSED = 2
    }
}
