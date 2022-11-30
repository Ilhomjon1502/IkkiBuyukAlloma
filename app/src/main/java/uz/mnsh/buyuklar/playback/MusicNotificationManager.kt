package uz.mnsh.buyuklar.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.session.MediaSessionManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import com.mnsh.sayyidsafo.R
import uz.mnsh.buyuklar.data.model.SongModel
import uz.mnsh.buyuklar.ui.activity.MainActivity
import uz.mnsh.buyuklar.utils.Utils


class MusicNotificationManager internal constructor(private val mMusicService: MusicService) {
    private val CHANNEL_ID = "action.CHANNEL_ID"
    private val REQUEST_CODE = 100
    val notificationManager: NotificationManager = mMusicService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    var notificationBuilder: NotificationCompat.Builder? = null
        private set
    private var mediaSession: MediaSessionCompat? = null
    private var mediaSessionManager: MediaSessionManager? = null
    private var transportControls: MediaControllerCompat.TransportControls? = null
    private val context: Context = mMusicService.baseContext

    private fun playerAction(action: String): PendingIntent {

        val pauseIntent = Intent()
        pauseIntent.action = action

//        return PendingIntent.getBroadcast(mMusicService, REQUEST_CODE, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        //PendingIntent.FLAG_IMMUTABLE - versiya 31 dan yuqorida
        return PendingIntent.getBroadcast(mMusicService, REQUEST_CODE, pauseIntent, PendingIntent.FLAG_IMMUTABLE)
    }

    fun createNotification(): Notification {

        val song = mMusicService.mediaPlayerHolder?.getCurrentSong()

        notificationBuilder = NotificationCompat.Builder(mMusicService, CHANNEL_ID)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        val openPlayerIntent = Intent(mMusicService, MainActivity::class.java)
        openPlayerIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val contentIntent = PendingIntent.getActivity(mMusicService, REQUEST_CODE,
                openPlayerIntent, PendingIntent.FLAG_IMMUTABLE)

        val songTitle = song?.name

        initMediaSession(song!!)

        notificationBuilder!!
                .setShowWhen(false)
                .setSmallIcon(R.drawable.ic_music_player)
                .setLargeIcon(Utils.songArt(song.songPath, mMusicService.baseContext))
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setContentTitle(songTitle)
                .setContentIntent(contentIntent)
                .addAction(notificationAction(PREV_ACTION))
                .addAction(notificationAction(PLAY_PAUSE_ACTION))
                .addAction(notificationAction(NEXT_ACTION))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        notificationBuilder!!.setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession!!.sessionToken)
                .setShowActionsInCompactView(0, 1, 2))
        return notificationBuilder!!.build()
    }

    private fun notificationAction(action: String): NotificationCompat.Action {

        val icon: Int = when (action) {
            PREV_ACTION -> R.drawable.ic_skip_previous_notif
            PLAY_PAUSE_ACTION ->

                if (mMusicService.mediaPlayerHolder?.getState() != PlaybackInfoListener.State.PAUSED)
                    R.drawable.stop_notif
                else
                    R.drawable.play_notif
            NEXT_ACTION -> R.drawable.ic_skip_next_notif
            else -> R.drawable.ic_skip_previous_notif
        }
        return NotificationCompat.Action.Builder(icon, action, playerAction(action)).build()
    }

    @RequiresApi(26)
    private fun createNotificationChannel() {

        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val notificationChannel = NotificationChannel(CHANNEL_ID,
                    mMusicService.getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_LOW)

            notificationChannel.description = mMusicService.getString(R.string.app_name)

            notificationChannel.enableLights(false)
            notificationChannel.enableVibration(false)
            notificationChannel.setShowBadge(false)

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun initMediaSession(song: SongModel) {
        mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        mediaSession = MediaSessionCompat(context, "AudioPlayer")
        transportControls = mediaSession!!.controller.transportControls
        mediaSession!!.isActive = true
        mediaSession!!.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        updateMetaData(song)
    }

    private fun updateMetaData(song: SongModel) {
        mediaSession!!.setMetadata(MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, Utils.songArt(song.songPath, context))
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.name)
                .build())
    }

    companion object {
        val NOTIFICATION_ID = 101
        internal val PLAY_PAUSE_ACTION = "action.PLAYPAUSE"
        internal val NEXT_ACTION = "action.NEXT"
        internal val PREV_ACTION = "action.PREV"
    }

}
