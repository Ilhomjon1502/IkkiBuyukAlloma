package uz.mnsh.buyuklar.data.db.model


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey


//AudioModel table - dasturda asosiy ma'lumotlar
@Keep
@Entity(tableName = "audios_table")
data class AudioModel(
    @PrimaryKey(autoGenerate = false)
    @SerializedName("id")
    val id: Int,
    @SerializedName("duration")
    val duration: String,
    @SerializedName("location")
    val location: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("rn")
    val rn: Int,
    @SerializedName("topic")
    val topic: Int,
    @SerializedName("size")
    val size: String
){
    fun getFileName(): String {
        return "$name.mp3"
    }
}