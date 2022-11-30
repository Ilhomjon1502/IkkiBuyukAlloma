package uz.mnsh.buyuklar.data.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep
import uz.mnsh.buyuklar.data.db.model.AudioModel

@Keep
data class AudiosResponse(
    @SerializedName("data")
    val data: List<AudioModel>
)