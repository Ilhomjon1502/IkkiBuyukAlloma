package uz.mnsh.buyuklar.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.ybq.android.spinkit.SpinKitView
import com.mnsh.sayyidsafo.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance
import uz.mnsh.buyuklar.App
import uz.mnsh.buyuklar.data.db.model.AudioModel
import uz.mnsh.buyuklar.data.model.SongModel
import uz.mnsh.buyuklar.ui.activity.MainActivity.Companion.isSavedSong
import uz.mnsh.buyuklar.ui.activity.MainActivity.Companion.isSongPlay
import uz.mnsh.buyuklar.ui.activity.MainActivity.Companion.mPlayerAdapter
import uz.mnsh.buyuklar.ui.adapter.AudiosAdapter
import uz.mnsh.buyuklar.utils.FragmentAction
import java.io.File
import kotlin.coroutines.CoroutineContext

//recycleViewni o'z ichiga olgan audiolar ro'yhatini ko'rsatadigan viewPager itemi fragmentidir
class PlaceholderFragment : Fragment(R.layout.fragment_main), CoroutineScope, KodeinAware, FragmentAction {

    override val kodein by closestKodein()
    private val viewModelFactory: PageViewModelFactory by instance()
    private lateinit var pageViewModel: PageViewModel
    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private lateinit var recyclerView: RecyclerView
    private var spinKitView: SpinKitView? = null
    private var listAudioFile: ArrayList<SongModel> = ArrayList()
    private var mAdapter: AudiosAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        job = Job()

        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        spinKitView = view.findViewById(R.id.spin_kit)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        pageViewModel = viewModelFactory.create(PageViewModel::class.java).apply {
            setIndex(arguments?.getInt(ARG_SECTION_NUMBER) ?: 1)
        }
        pageViewModel.text.observe(viewLifecycleOwner, Observer {
            loadData(it)
        })
    }

    private fun loadData(index: Int) = launch {
        if (index == 1){//1-alloma
            pageViewModel.getAudios(10).value.await().observe(viewLifecycleOwner, Observer {
                if (it == null) return@Observer
                bindUI(it)
            })
        }else{//2-alloma
            pageViewModel.getAudios(11).value.await().observe(viewLifecycleOwner, Observer {
                if (it == null) return@Observer
                bindUI(it)
            })
        }
    }

    private fun bindUI(audioModel: List<AudioModel>){
        if (audioModel.isNotEmpty()) {
            listAudioFile.clear()
            File(App.DIR_PATH + "${audioModel[0].topic}/").walkTopDown().forEach { file ->
                if (file.name.endsWith(".mp3")) {
                    val sm = SongModel(
                        name = file.name.substring(0, file.name.length - 4),
                        songPath = file.path,
                        topicID = audioModel[0].topic
                    )
                    listAudioFile.add(sm)//media uchun listga file ma'lumotlarini o'qib olmoqda, fayllar download ichida 10 yoki 11 papakasi ichida name bilan saqlanadi
                }
            }
            mAdapter = AudiosAdapter(audioModel, listAudioFile, this)
            recyclerView.adapter = mAdapter
            recyclerView.visibility = View.VISIBLE
            spinKitView?.visibility = View.GONE

            isSongPlay.observe(viewLifecycleOwner, Observer {
                if (it == null) return@Observer
                var play = true
                audioModel.forEachIndexed { i, model ->
                    if (model.name == mPlayerAdapter!!.getCurrentSong()?.name){
                        if (it){
                            mAdapter?.isPlay = i
                        }else{
                            mAdapter?.isPlay = -1
                        }
                        mAdapter?.notifyItemChanged(i)
//                        play = false
                    }
                }
//                if (play) mAdapter?.isPlay = -1
//                mAdapter?.notifyDataSetChanged()
                /*
                pause yoki play button bosilganda
                isSongPlay liveData da audio play yoki pause ligi keladi
                agar ijrodagi media audioModeldagi
                 */
            })
        }
    }

    companion object {
        private const val ARG_SECTION_NUMBER = "section_number"

        @JvmStatic
        fun newInstance(sectionNumber: Int): PlaceholderFragment {
            return PlaceholderFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun itemPlay(model: SongModel) {
        if (mPlayerAdapter != null){
            isSavedSong = false
            if (mPlayerAdapter!!.getCurrentSong()?.name == model.name){
                if (mPlayerAdapter!!.getMediaPlayer() != null){
                    mPlayerAdapter!!.resumeOrPause()
                }else{
                    mPlayerAdapter!!.initMediaPlayer()
                }
            }else{
                mPlayerAdapter!!.setCurrentSong(model, listAudioFile)
                mPlayerAdapter!!.initMediaPlayer()
            }
        }
    }
}