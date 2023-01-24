package uz.mnsh.buyuklar.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import com.mnsh.sayyidsafo.R
import java.io.ByteArrayInputStream
import java.io.InputStream


object Utils {

    //notification uchun dizayn chizib berish
    fun songArt(path: String, context: Context): Bitmap {
        val retriever = MediaMetadataRetriever()
        val inputStream: InputStream
        retriever.setDataSource(path)
        return if (retriever.embeddedPicture != null) {
            inputStream = ByteArrayInputStream(retriever.embeddedPicture)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            retriever.release()
            bitmap
        } else {
            getLargeIcon(context)
        }
    }

//    media davomiyligini qaytarish
    fun getDuration(path: String): Long {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        return retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()
    }

    //katta icon qaytarish
    private fun getLargeIcon(context: Context): Bitmap {
        return BitmapFactory.decodeResource(context.resources, R.drawable.profile)
    }
}
