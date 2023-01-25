package uz.mnsh.buyuklar.playback

import androidx.annotation.IntDef

//media ma'lumotlari o'zgarishnini eshitib turuvchi
abstract class PlaybackInfoListener {

    //position o'zgarganda
    open fun onPositionChanged(position: Int) {}

    //holati o'zgarganda
    open fun onStateChanged(@State state: Int) {}

    @IntDef(State.INVALID, State.PLAYING, State.PAUSED, State.COMPLETED, State.RESUMED)
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class State {
        companion object {
            const val INVALID = -1 //xato
            const val PLAYING = 0 //ijroda
            const val PAUSED = 1 //pausada
            const val COMPLETED = 2 //muvaffaqiyatli yaratildi (ijro boshlandi)
            const val RESUMED = 3 //ijro davom  etmoqda (play)
        }
    }
}
