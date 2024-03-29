package uz.mnsh.buyuklar.data.provider

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

//keshga yozish ota classi
abstract class PreferenceProvider(context: Context) {
    private val appContext = context.applicationContext

    protected val preferences: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(appContext)
}