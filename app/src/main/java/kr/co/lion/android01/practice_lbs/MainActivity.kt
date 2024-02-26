package kr.co.lion.android01.practice_lbs

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.gms.maps.MapsInitializer
import com.google.android.material.transition.MaterialSharedAxis
import kr.co.lion.android01.practice_lbs.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    lateinit var activityMainBinding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)
        setToolBar()
        setEvent()

    }

    //툴바 설정
    fun setToolBar(){
        activityMainBinding.apply {
            materialToolbar2.apply {
                title = "도전입니다!!"
            }
        }
    }

    fun setEvent(){
        activityMainBinding.apply {
            mapsButton.setOnClickListener {
                var newIntent = Intent(this@MainActivity, SettingActivity::class.java)
                startActivity(newIntent)

            }
        }
    }
}




































