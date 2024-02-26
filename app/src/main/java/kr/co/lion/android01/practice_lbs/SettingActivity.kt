package kr.co.lion.android01.practice_lbs

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ReportFragment.Companion.reportFragment
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.material.snackbar.Snackbar
import kr.co.lion.android01.practice_lbs.databinding.ActivitySettingBinding

class SettingActivity : AppCompatActivity() {

    lateinit var activitySettingBinding: ActivitySettingBinding

    //위치 정보를 관리하는 객체
    lateinit var locationManager:LocationManager

    //위치 측정이 성공하면 동작할 리스너
    var gpsLocationListener:MyLocationListener? = null
    var networkLocationListener:MyLocationListener? = null

    //확인할 권한 목록
    var permissionList = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapsInitializer.initialize(this, MapsInitializer.Renderer.LATEST, null)

        activitySettingBinding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(activitySettingBinding.root)

        //권한을 확인받는다
        requestPermissions(permissionList, 0)
    }

    fun setToolBar(){
        activitySettingBinding.apply {
            materialToolbar.apply {
                title = "나의 지도"
                //메뉴
                inflateMenu(R.menu.set_menu)
                //메뉴를 클릭했을 떄
                setOnMenuItemClickListener {

                    true
                }
            }
        }
    }

    //구글지도 세팅
    fun settingGoogleMap(){
        //MapFragment의 객체를 가져온다 --> xml파일에 만든 fragment
        var supportMapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        //리스너를 설정한다
        //구글 지도 사용이 완료되면 동작한다
        supportMapFragment.getMapAsync {

            //위치 정보를 관리하는 객체를 가져온다 그니까 즉 초기화도 해준다
            locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

            //단말기에 저장되어있는 위치값을 가져온다
            // 둘 중 하나라도 허용일 경우
            //허용되어 있으면 하고 아니면 만다
            var a1 = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            var a2 = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

            if (a1 && a2){
                //저장되어 있는 위치값을 가져온다 (없으면 null이 반환된다)
                var location1 = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                var location2 = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                //현재 위치를 지도에 표현한다
                if (location1 != null){
                    setMyLocation(location1)
                }else if (location2 != null){
                    setMyLocation(location2)
                }

                //현재 위치를 측정한다
                getMyLocation()
            }

        }
    }

    //현재 위치를 가져오는 메서드
    //gps도 돌리고 netWork도 돌린다
    fun getMyLocation(){
        //위치 정보 사용 권한 허용 여부 확인
        // 어플이 실행되면 처음 뜨는 notification에서 허용을 눌렀는가?
        var a1 = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED
        var a2 = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED

        if (a1 || a2){
            //둘 중에 하나라도 denied라면
            return
        }

        //혹은 gps 프로바이더 사용이 가능하다면
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) == true){
            if (gpsLocationListener == null){
                gpsLocationListener = MyLocationListener()
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0.0f, gpsLocationListener!!)
            }
        }
        //만약 network 프로바이더가 사용이 가능하다면
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true)
            //리스너를 멈춘다
            if (networkLocationListener == null){
                networkLocationListener = MyLocationListener()
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0.0f, networkLocationListener!!)
            }
    }

    //위치 측정이 성공하면 동작하는 리스너
    inner class MyLocationListener : LocationListener{
        override fun onLocationChanged(location: Location) {

            //사용한 위치 정보 프로바이더로 분기한다
            when(location.provider){
                //GPS 프로바이더 라면?
                LocationManager.GPS_PROVIDER -> {
                    //Gps 리스너 연결을 해제해준다
                    locationManager.removeUpdates(gpsLocationListener!!)
                    gpsLocationListener = null

                }
                //NetWork 프로바이더라면?
                LocationManager.NETWORK_PROVIDER -> {
                    //newWork 리스너 연결을 해제 해준다

                }
            }

            //측정된 위치로 지도를 움직인다
            setMyLocation(location)
        }
    }

    //지도의 위치를 설정하는 메서드
    fun setMyLocation(location: Location){
        //위도와 경도를 출력한다
        Snackbar.make(activitySettingBinding.root, "위도 : ${location.latitude}, 경도 : ${location.longitude}", Snackbar.LENGTH_SHORT).show()
    }
}





































