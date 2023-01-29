package uz.mnsh.buyuklar.ui.activity

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import com.github.florent37.runtimepermission.kotlin.askPermission
import com.google.android.exoplayer2.ui.BuildConfig
import com.google.gson.Gson
import com.mnsh.sayyidsafo.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.auto_mode.*
import kotlinx.android.synthetic.main.player_layout.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import org.kodein.di.android.kodein
import uz.mnsh.buyuklar.App
import uz.mnsh.buyuklar.data.model.SongModel
import uz.mnsh.buyuklar.data.provider.UnitProvider
import uz.mnsh.buyuklar.data.repository.AudiosRepository
import uz.mnsh.buyuklar.playback.MusicNotificationManager
import uz.mnsh.buyuklar.playback.MusicService
import uz.mnsh.buyuklar.playback.PlaybackInfoListener
import uz.mnsh.buyuklar.playback.PlayerAdapter
import uz.mnsh.buyuklar.ui.adapter.SectionsPagerAdapter
import uz.mnsh.buyuklar.utils.AboutUsDialog
import uz.mnsh.buyuklar.utils.Utils
import java.io.File

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), KodeinAware {

    override val kodein by kodein()
    private val unitProvider: UnitProvider by instance()
    private val audiosRepository: AudiosRepository by instance()

    //mashina rejimi
    private lateinit var audioTitle: TextView
    private lateinit var playButton: ImageView
    private lateinit var replayButton: ImageView
    private lateinit var forwardButton: ImageView

    //bizining MusicService imiz
    private var mMusicService: MusicService? = null

    //seekbar user tomonidan o'zgartirildimi
    private var mUserIsSeeking = false

    //media ma'lumotlarini eshitib turuvchi
    private var mPlaybackListener: PlaybackListener? = null

    private var mMusicNotificationManager: MusicNotificationManager? = null

    //media file (download dagi) lar ro'yhati
    private var listAudiosFile: ArrayList<SongModel> = ArrayList()

    //media MediaPlayer ijrosi uchun
    private var songModel: SongModel? = null

    //MusicService bind mi
    private var mIsBound: Boolean? = null

    companion object {
        var mPlayerAdapter: PlayerAdapter? = null
        var isSavedSong: Boolean = true
        var isSongPlay: MutableLiveData<Boolean> = MutableLiveData()
    }

    //MusicService bilan bog'lanish, listener
    private val mConnection = object : ServiceConnection {

        //service uzilganda
        override fun onServiceDisconnected(p0: ComponentName?) {
            mMusicService = null
        }

        //service bog'langanda
        override fun onServiceConnected(componentName: ComponentName?, iBinder: IBinder?) {
            mMusicService = (iBinder as MusicService.LocalBinder).instance
            mPlayerAdapter = mMusicService!!.mediaPlayerHolder
            mMusicNotificationManager = mMusicService!!.musicNotificationManager

            if (mPlaybackListener == null) {
                mPlaybackListener = PlaybackListener()
                mPlayerAdapter!!.setPlaybackInfoListener(mPlaybackListener!!)
            }
            if (mPlayerAdapter != null && mPlayerAdapter?.getCurrentSong()?.name == songModel?.name) {
                restorePlayerStatus()
            } else {
                if (songModel != null) {
                    onSongSelected(songModel!!)
                }
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        val sectionsPagerAdapter =
            SectionsPagerAdapter(
                this,
                supportFragmentManager
            )

        val viewPager: ViewPager = findViewById(R.id.view_pager)
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = findViewById(R.id.tabs)
        tabs.setupWithViewPager(viewPager)

        //mashina rejimi
        audioTitle = findViewById(R.id.audio_title)
        playButton = findViewById(R.id.play_button)
        replayButton = findViewById(R.id.replay)
        forwardButton = findViewById(R.id.forward)

        requestPermissions()
        initializeSeekBar()//seekbar ni changeLister i
        try {
            bindUI()//button clicklar va media davomiyligi
        } catch (e: Exception) {
            val dialog = AlertDialog.Builder(this)
            dialog.setTitle("Ogohlantirish")
            dialog.setMessage("Qurilmangiz (Telefoningiz) xotirasidan download papkasi ichidagi Ikki buyuk alloma papkasidagi ma'lumotlarni o'chirib tashlabsiz. Shuning uchun ilovaga qayta kirib audiolarni qayta yuklab olib keyin tinglashingiz mumkin")
            dialog.setPositiveButton("Xo'p", object :DialogInterface.OnClickListener{
                override fun onClick(p0: DialogInterface?, p1: Int) {
//                    finish()
                }
            })
            dialog.show()
            Toast.makeText(this, "File topilmadi \n ${e.message}", Toast.LENGTH_SHORT).show()
        }
        GlobalScope.launch(Dispatchers.IO) {
            if (unitProvider.isOnline()) {//agar qurilmada internet bo'lsa
                audiosRepository.fetchingAudios()// audiolarni API dan olib kel
            }
        }
    }

    private fun requestPermissions() {
//        ActivityCompat.requestPermissions(
//            this,
//            arrayOf(
//                Manifest.permission.READ_EXTERNAL_STORAGE,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE
//            ),
//            1
//        )
//        App.DIR_PATH =
//            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
//        App.DIR_PATH += "/Ikki buyuk alloma/"


        askPermission(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) {
            //all permissions already granted or just granted
            App.DIR_PATH =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
            App.DIR_PATH += "/Ikki buyuk alloma/"
        }.onDeclined { e ->
            if (e.hasDenied()) {

                AlertDialog.Builder(this)
                    .setMessage(
                        "Iltimos ilova to'g'ri ishlashi uchun barcha so'rovlarga ruxsat berin..." +
                                "\nAks holda ilova to'g'ri ishlamaydi"
                    )
                    .setPositiveButton("Xo'p") { dialog, which ->
                        e.askAgain();
                    } //ask again
                    .setNegativeButton("Yo'q") { dialog, which ->
                        dialog.dismiss();
                        finish()
                    }
                    .show();
            }

            if (e.hasForeverDenied()) {
                //the list of forever denied permissions, user has check 'never ask again'

                // you need to open setting manually if you really need it
                e.goToSettings();
            }
        }
    }

    var hasKesh = true
    //keshdan o'qish, button clicklar, media davomiyligi
    private fun bindUI() {
        listAudiosFile.clear()

        //keshdan listAudioFile ga songModel larni o'qib olish
        if (unitProvider.getSavedAudio().length > 5) {
            songModel = Gson().fromJson(unitProvider.getSavedAudio(), SongModel::class.java)
            File(App.DIR_PATH + "${songModel!!.topicID}/").walkTopDown().forEach { file ->
                if (file.name.endsWith(".mp3")) {
                    val sm = SongModel(
                        name = file.name.substring(0, file.name.length - 4),
                        songPath = file.path,
                        topicID = songModel!!.topicID
                    )
                    listAudiosFile.add(sm)
                }
            }
            tvSongName.text = songModel!!.name //audio nomini yozish
            tvEndTime.text = getFormattedTime(Utils.getDuration(songModel!!.songPath) / 1000)// audio davomiyligini yozish
        }

        //keyingiga o'tkazish
        imgNext.setOnClickListener {
            if (checkIsPlayer()) {
                mPlayerAdapter!!.skip(true)
            }
        }

        //play pause button click
        imgPlay.setOnClickListener {
            if (isSavedSong && songModel != null) {
                mPlayerAdapter!!.initMediaPlayer()
                isSavedSong = false
            }
            if (mPlayerAdapter?.isMediaPlayer() != null) {
                resumeOrPause()
            }
        }

        //oldingiga qaytarish
        imgPrevious.setOnClickListener {
            if (checkIsPlayer()) {
                mPlayerAdapter!!.instantReset()
            }
        }

        //30 sekund o'tkazish
        forwardButton.setOnClickListener {
            if (mPlayerAdapter != null) {
                if (mPlayerAdapter!!.getMediaPlayer()!!.currentPosition.plus(30000) < mPlayerAdapter!!.getMediaPlayer()!!.duration) {
                    mPlayerAdapter!!.seekTo(
                        mPlayerAdapter!!.getMediaPlayer()!!.currentPosition.plus(
                            30000
                        )
                    )
                } else {
                    mPlayerAdapter!!.skip(true)
                }
            } else {
                Toast.makeText(this, "242 mPlayerAdapter null", Toast.LENGTH_SHORT).show()
            }
        }

        //30 sekund qaytarish
        replayButton.setOnClickListener {
            if (mPlayerAdapter!!.getMediaPlayer()!!.currentPosition.minus(30000) > 0) {
                mPlayerAdapter!!.seekTo(
                    mPlayerAdapter!!.getMediaPlayer()!!.currentPosition.minus(
                        30000
                    )
                )
            } else {
                mPlayerAdapter!!.seekTo(0)
            }
        }

        //play pause mashina rejimi
        playButton.setOnClickListener {
            if (isSavedSong && songModel != null) {
                mPlayerAdapter!!.initMediaPlayer()
                isSavedSong = false
            }
            if (mPlayerAdapter?.isMediaPlayer() != null) {
                resumeOrPause()
            }
        }

        hasKesh = false
    }

    private fun restorePlayerStatus() {

        if (mPlayerAdapter != null && mPlayerAdapter!!.isMediaPlayer()) {

            mPlayerAdapter!!.onResumeActivity()
            updatePlayingInfo(restore = true, startPlay = false)
        }
    }

    //seekbar ni o'zgarganida mediani o'zgartirish
    private fun initializeSeekBar() {
        seekBar!!.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                var userSelectedPosition = 0

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    mUserIsSeeking = true
                }

                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    userSelectedPosition = progress
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    mUserIsSeeking = false
                    mPlayerAdapter!!.seekTo(userSelectedPosition)
                }
            })
    }

    //MusicService ni unBind qilish
    private fun doUnbindService() {
        if (mIsBound!!) {
            unbindService(mConnection)
            mIsBound = false
        }
    }

    //MusicService ni bind qilish
    private fun doBindService() {
        bindService(
            Intent(
                this,
                MusicService::class.java
            ), mConnection, Context.BIND_AUTO_CREATE
        )
        mIsBound = true

        val startNotStickyIntent = Intent(this, MusicService::class.java)
        startService(startNotStickyIntent)
    }

    //notification , seekbar o'zgarishi
    private fun updatePlayingInfo(restore: Boolean, startPlay: Boolean) {

        //notification
        if (startPlay) {
            mPlayerAdapter!!.getMediaPlayer()?.start()
            Handler().postDelayed({
                mMusicService!!.startForeground(
                    MusicNotificationManager.NOTIFICATION_ID,
                    mMusicNotificationManager!!.createNotification()
                )
            }, 200)
        }

        val selectedSong = mPlayerAdapter!!.getCurrentSong()

        tvSongName.text = selectedSong?.name
        audio_title.text = selectedSong?.name
        tvEndTime.text = getFormattedTime(Utils.getDuration(selectedSong!!.songPath) / 1000)
        seekBar?.max = Utils.getDuration(selectedSong.songPath).toInt()

        if (restore) {
            seekBar!!.progress = mPlayerAdapter!!.getPlayerPosition()
            updatePlayingStatus()

            Handler().postDelayed({
                if (mMusicService!!.isRestoredFromPause) {
                    mMusicService!!.stopForeground(false)
                    mMusicService!!.musicNotificationManager!!.notificationManager
                        .notify(
                            MusicNotificationManager.NOTIFICATION_ID,
                            mMusicService!!.musicNotificationManager!!.notificationBuilder!!.build()
                        )
                    mMusicService!!.isRestoredFromPause = false
                }
            }, 200)
        }
    }

    //play pause icon (mashina rejimi ham) va Media satusini o'zgartirish
    private fun updatePlayingStatus() {
        unitProvider.setSavedAudio(Gson().toJson(mPlayerAdapter!!.getCurrentSong()))
        if (hasKesh){
            bindUI()
        }
        //agar State.PAUSE bo'lmasa ic_stop ni drawable o'zgaruvchisiga tengla
        val drawable = if (mPlayerAdapter!!.getState() != PlaybackInfoListener.State.PAUSED)
            R.drawable.ic_stop
        else
            R.drawable.ic_play //pause bo'lgan bo'lsa bunga
        imgPlay!!.post { imgPlay!!.setImageResource(drawable) }

            //mashina rejimi
        if (mPlayerAdapter!!.getState() != PlaybackInfoListener.State.PAUSED) {
            playButton.setImageResource(R.drawable.ic_pause_circled)
        } else {
            playButton.setImageResource(R.drawable.ic_play_circled)
        }

    }

    private fun onSongSelected(song: SongModel) {
        try {
            mPlayerAdapter!!.setCurrentSong(song, listAudiosFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun resumeOrPause() {
        if (checkIsPlayer()) {
            mPlayerAdapter!!.resumeOrPause()
        } else {
            if (listAudiosFile.isNotEmpty()) {
                onSongSelected(songModel!!)
            }
        }
    }

    private fun checkIsPlayer(): Boolean {
        return mPlayerAdapter!!.isMediaPlayer()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.btn_rule -> {
                driving_mode_container.visibility = View.VISIBLE
            }
            R.id.tv_share -> {
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                var message = getString(R.string.about_us_text)
//                message = message + "\n" + getString(R.string.app_url) + BuildConfig.APPLICATION_ID + "\n\n"
                message = message + "\n" + getString(R.string.app_url) + "\n\n"
                intent.putExtra(Intent.EXTRA_TEXT, message)
                startActivity(Intent.createChooser(intent, "Улашиш"))
            }
            R.id.tv_telegram -> {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(getString(R.string.telegram_url))
                startActivity(intent)
            }
            R.id.tv_about -> {
                val dialog = AboutUsDialog()
                dialog.show(supportFragmentManager, "ABOUT_US")
            }
            R.id.tv_other_app -> {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(getString(R.string.our_app))
                startActivity(intent)
            }
            R.id.btn_question -> {
                if (mPlayerAdapter!!.isPlaying()) {
                    resumeOrPause()
                }
                startActivity(Intent(this@MainActivity, InfoActivity::class.java))
            }
        }
        return true
    }

    private fun getFormattedTime(seconds: Long): String {
        val minutes = seconds / 60
        return String.format("%d:%02d", minutes, seconds % 60)
    }

    //serverni unBind qiladi
    override fun onPause() {
        super.onPause()
        doUnbindService()
        if (mPlayerAdapter != null) {
            if (mPlayerAdapter!!.isMediaPlayer()) {
                mPlayerAdapter!!.onPauseActivity()
            }
        } else {
            Toast.makeText(this, "onPause 486-qator mPlayerAdapter null", Toast.LENGTH_SHORT).show()
        }
    }

    //serverni bind qiladi
    override fun onResume() {
        super.onResume()
        doBindService()
        try {
            bindUI()
        }catch (e:Exception){}
    }

    //SongModel ni keshga saqlab qo'yadi
    override fun onStop() {
        super.onStop()
        unitProvider.setSavedAudio(Gson().toJson(mPlayerAdapter!!.getCurrentSong()))
    }

    //media ma'lumotlarini o'zgarishini eshitib ish bajaradi
    internal inner class PlaybackListener : PlaybackInfoListener() {

        override fun onPositionChanged(position: Int) {
            seekBar.progress = position
            tvStartTime.text = getFormattedTime((position / 1000).toLong())
        }

        override fun onStateChanged(@State state: Int) {
            updatePlayingStatus()//play pause icon va Media Status o'zgartiradi
            if (mPlayerAdapter!!.getState() != State.PAUSED
                && mPlayerAdapter!!.getState() != State.PAUSED
            ) {
                isSongPlay.postValue(true)// media ijroda eknaligini liveData ga yozish
                updatePlayingInfo(restore = false, startPlay = true)
            } else isSongPlay.postValue(false)//media pause daligini liveData ga yozish
        }
    }

    //chiqish tugamsi bosilganda
    override fun onBackPressed() {
        if (driving_mode_container.visibility == View.VISIBLE) {//mashina rejimida bo'lsa
            driving_mode_container.visibility = View.GONE//uni yop
        } else {
            super.onBackPressed()//dasturdan chiqib ket
        }
    }
}