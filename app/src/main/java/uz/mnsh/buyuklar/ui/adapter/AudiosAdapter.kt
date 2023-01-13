package uz.mnsh.buyuklar.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.downloader.Error
import com.downloader.OnDownloadListener
import com.downloader.PRDownloader
import com.downloader.Status
import com.mnsh.sayyidsafo.R
import uz.mnsh.buyuklar.App
import uz.mnsh.buyuklar.App.Companion.BASE_URL
import uz.mnsh.buyuklar.data.db.model.AudioModel
import uz.mnsh.buyuklar.data.model.SongModel
import uz.mnsh.buyuklar.utils.FragmentAction

class AudiosAdapter(
    audiosModel: List<AudioModel>,
    private var fileList: ArrayList<SongModel>,
    private val fragmentAction: FragmentAction
) :
    RecyclerView.Adapter<AudiosAdapter.AudiosViewHolder>() {

    private val listModel: ArrayList<AudioModel> = ArrayList(audiosModel)
    var isPlay: Int = -1
    private var isStart: Boolean = true
    private var idList: HashMap<Int, Int> = HashMap()

    class AudiosViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.title)
        val tvDuration: TextView = view.findViewById(R.id.duration)
        val tvSize: TextView = view.findViewById(R.id.size)
        val progressBar: ProgressBar = view.findViewById(R.id.progress)
        val download: AppCompatImageView = view.findViewById(R.id.download)
        val constraintLayout: ConstraintLayout = view.findViewById(R.id.constraintLayout)
        val mContext: Context = view.context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudiosViewHolder {
        return AudiosViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_audios_container,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return listModel.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: AudiosViewHolder, position: Int) {
        holder.tvTitle.text = listModel[position].name
        holder.tvSize.text =  "${listModel[position].size} Мб"
        holder.tvDuration.text = listModel[position].duration

        holder.download.setImageResource(R.drawable.download)
        holder.download.visibility = View.VISIBLE
        holder.progressBar.visibility = View.GONE
        fileList.forEach {
            if (it.name == listModel[position].name) {
                holder.download.setImageResource(R.drawable.play)
                holder.download.visibility = View.VISIBLE
                holder.progressBar.visibility = View.GONE
                if (isPlay == position) {
                    holder.download.setImageResource(R.drawable.stop)
                }
            }
        }

        holder.constraintLayout.setOnClickListener {
            var isLoad = true
            fileList.forEach {
                if (it.name == listModel[position].name){
                    isLoad = false
                    fragmentAction.itemPlay(it)
                }
            }
            if (isLoad){
                startDownload(position, holder)
            }
        }
        holder.download.setOnClickListener {
            var isLoad = true
            fileList.forEach {
                if (it.name == listModel[position].name){
                    isLoad = false
                    fragmentAction.itemPlay(it)
                }
            }
            if (isLoad){
                startDownload(position, holder)
            }
        }
        holder.progressBar.setOnClickListener {
            startDownload(position, holder)
        }
    }

    private fun startDownload(index: Int, holder: AudiosViewHolder) {
        if (idList[index] != null && PRDownloader.getStatus(idList[index]!!) == Status.RUNNING) {
            PRDownloader.cancel(idList[index]!!)
            notifyItemChanged(index)
        } else {
            holder.progressBar.visibility = View.VISIBLE
            holder.download.setImageResource(R.drawable.cancel)
            idList[index] = PRDownloader.download(
                BASE_URL + listModel[index].location,
                App.DIR_PATH + listModel[index].topic + "/",
                listModel[index].getFileName()
            ).build()
                .setOnProgressListener {
                    holder.progressBar.progress = (it.currentBytes * 100 / it.totalBytes).toInt()
                }
                .start(object : OnDownloadListener {
                    override fun onDownloadComplete() {
                        fileList.add(
                            SongModel(
                                name = listModel[index].name,
                                songPath = "${App.DIR_PATH}${listModel[index].topic}/${listModel[index].getFileName()}",
                                topicID = listModel[index].rn.toInt()
                            )
                        )
                        Log.d("TAG", "onDownloadComplete: ${App.DIR_PATH}${listModel[index].rn}/${listModel[index].getFileName()}")
                        notifyItemChanged(index)
                        idList.remove(index)
                    }

                    override fun onError(error: Error?) {
                        Log.d("BAG", error?.responseCode.toString())
                        notifyItemChanged(index)
                    }
                })
        }

    }

//    private fun getFormattedTime(seconds: Long): String {
//        val minutes = seconds / 60
//        return String.format("%d:%02d", minutes, seconds % 60)
//    }
}