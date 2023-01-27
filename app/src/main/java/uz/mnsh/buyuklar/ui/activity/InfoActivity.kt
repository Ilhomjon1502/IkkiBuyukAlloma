package uz.mnsh.buyuklar.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.mnsh.sayyidsafo.R
import uz.mnsh.buyuklar.ui.adapter.InfoPagerAdapter

//ulamolar haqida ma'lumot berish
class InfoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        val infoPagerAdapter =
            InfoPagerAdapter(
                this,
                supportFragmentManager
            )

        ///ikki ulamo uchun ma'lumot beruvchi fragment joylash
        val viewPager: ViewPager = findViewById(R.id.view_pager)
        viewPager.adapter = infoPagerAdapter
        val tabs: TabLayout = findViewById(R.id.tabs)
        tabs.setupWithViewPager(viewPager)
    }
}