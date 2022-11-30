package uz.mnsh.buyuklar.data.provider

interface UnitProvider {
    suspend fun isOnline(): Boolean

    fun getSavedAudio(): String

    fun setSavedAudio(audio: String)

    fun getSavedTime(): String

    fun setSavedTime(time: String)
}