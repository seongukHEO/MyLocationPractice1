package kr.co.lion.android01.practice_lbs

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kr.co.lion.android01.practice_lbs.databinding.ActivitySettingBinding
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class SettingActivity : AppCompatActivity() {

    lateinit var activitySettingBinding: ActivitySettingBinding

    //위치 정보를 관리하는 객체
    lateinit var locationManager:LocationManager

    //위치 측정이 성공하면 동작할 리스너
    var gpsLocationListener:MyLocationListener? = null
    var networkLocationListener:MyLocationListener? = null

    //구글 지도 객체를 담을 프로퍼티
    lateinit var mainGoogleMap: GoogleMap

    //현재 사용자 위치를 가지고 있는 객체
    var userLocation:Location? = null

    //확인할 권한 목록
    var permissionList = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    //서버로부터 받아온 데이터를 담을 리스트
    //위도
    var latitudeList = mutableListOf<Double>()
    //경도
    var longitudeList = mutableListOf<Double>()
    //이름
    var nameList = mutableListOf<String>()
    //대략적인 주소
    var vicinityList = mutableListOf<String>()

    //주변 장소를 표시한 마커들을 담을 리스트
    var markerList = mutableListOf<Marker>()

    //강사님이 만들어주신 데이터 가져오기
    var diaLogData = arrayOf(
        "all",
        "accounting", "airport", "amusement_park",
        "aquarium", "art_gallery", "atm", "bakery",
        "bank", "bar", "beauty_salon", "bicycle_store",
        "book_store", "bowling_alley", "bus_station",
        "cafe", "campground", "car_dealer", "car_rental",
        "car_repair", "car_wash", "casino", "cemetery",
        "church", "city_hall", "clothing_store", "convenience_store",
        "courthouse", "dentist", "department_store", "doctor",
        "drugstore", "electrician", "electronics_store", "embassy",
        "fire_station", "florist", "funeral_home", "furniture_store",
        "gas_station", "gym", "hair_care", "hardware_store", "hindu_temple",
        "home_goods_store", "hospital", "insurance_agency",
        "jewelry_store", "laundry", "lawyer", "library", "light_rail_station",
        "liquor_store", "local_government_office", "locksmith", "lodging",
        "meal_delivery", "meal_takeaway", "mosque", "movie_rental", "movie_theater",
        "moving_company", "museum", "night_club", "painter", "park", "parking",
        "pet_store", "pharmacy", "physiotherapist", "plumber", "police", "post_office",
        "primary_school", "real_estate_agency", "restaurant", "roofing_contractor",
        "rv_park", "school", "secondary_school", "shoe_store", "shopping_mall",
        "spa", "stadium", "storage", "store", "subway_station", "supermarket",
        "synagogue", "taxi_stand", "tourist_attraction", "train_station",
        "transit_station", "travel_agency", "university", "eterinary_care", "zoo"

    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapsInitializer.initialize(this, MapsInitializer.Renderer.LATEST, null)

        activitySettingBinding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(activitySettingBinding.root)

        settingGoogleMap()
        setToolBar()

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
                    when(it.itemId){
                        R.id.set_menu_location -> {
                            //현재 위치를 다시 측정한다
                            getMyLocation()
                        }
                        //주변 정보 가져와서 다이아로그에 띄우기
                        R.id.main_menu_place -> {
                            //다이알로그를 띄워준다
                            var materialAlertDialogBuilder = MaterialAlertDialogBuilder(this@SettingActivity)
                            materialAlertDialogBuilder.setTitle("장소 종류 선택")
                            materialAlertDialogBuilder.setNegativeButton("취소", null)

                            //람다를 사용해준다
                            materialAlertDialogBuilder.setItems(diaLogData){ dialogInterface: DialogInterface, i: Int ->
                                //주변 정보를 가져온다
                                //그니까 DiaLog를 클릭했을 떄 사용자가 누른 순서값을 가져온다
                                //매개변수에는 i번째(사용자가 선택한 항목의 순서값) 문자열을 전달해준다
                                getPlaceData(diaLogData[i])
                            }


                            materialAlertDialogBuilder.show()
                        }
                        //마커들을 초기화 하기
                        R.id.main_menu_clear -> {

                        }
                    }

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

            //구글 지도 객체를 가져온다
            mainGoogleMap = it

            //확대 축소 버튼 설정
            mainGoogleMap.uiSettings.isZoomControlsEnabled = true

            //현재 위치로 이동 시키는 버튼을 둔다
            mainGoogleMap.isMyLocationEnabled = true

            //그 아이콘을 지우고 싶다면?
            mainGoogleMap.uiSettings.isMyLocationButtonEnabled = false

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
                    locationManager.removeUpdates(networkLocationListener!!)
                    networkLocationListener = null

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

        //현재 사용자의 위치를 프로퍼티에 담아준다
        userLocation = location

        //위도와 경도를 관리하는 객체를 생성한다
        //위도 : location.latitude
        //경도 : location.longitude
        var userLocation = LatLng(location.latitude, location.longitude)

        //지도를 이동시키기 위한 객체를 생성한다
        //첫 번째 : 표시할 지도의 위치
        //두 번째 : 줌 배율!
        var cameraUpdate = CameraUpdateFactory.newLatLngZoom(userLocation, 18.0f)

        //카메라를 이동 시킨다
        mainGoogleMap.animateCamera(cameraUpdate)
    }

    //주변 정보를 가져온다
    fun getPlaceData(type:String){
        //매개변수로 받는 타입은 위에 변수로 저장해둔 diaLogData 내용들이다!
        //안드로이드는 네트워크에 대한 코드는 모두 Thread로 운영하는 것을 강제한다
        //모바일 네트워크는 오류가 발생할 가능성이 매우 매우 크기 때문에 오류가 발생 하더라도
        //어플이 종료되는 것을 방지하기 위함이다!

        //예외처리
        try {
            //Thread를 가동시킨다
            thread {
                //사용자의 위치를 받은 적이 있다면?!
                if (userLocation != null){
                    //접속할 서버의 주소
                    var site = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"

                    //사용자의 위치
                    //와 여기 띄어쓰기 조심하자,, ㅋㅋ 한참 해맷네
                    var location = "${userLocation?.latitude},${userLocation?.longitude}"

                    //반경
                    var radius = 5000

                    //언어
                    var language = "ko"

                    //ApiKey
                    var apiKey = "AIzaSyDrmc7u6pc_C_NnuO1Ok1ICCmiK6Vl0Xds"

                    //마지막까지 읽어온다
                    //더이상 읽어올 페이지가 없다면 null을 반환한다!
                    do {
                        //너무 많은 양의 데이터를 한 번에 받아오면 오류가 나기 때문에 딜레이 타임을 준다
                        SystemClock.sleep(2000)
                        //다음 페이지의 데이터를 요청하기 위한 토큰
                        var pagetoken: String? = null

                        //접속할 주소
                        // 매개변수로 전달되는 type이 all이 아니라면 type을 붙혀준다
                        var serverPath = if (type == "all") {
                            //type을 붙혀주지 않는다
                            "${site}?location=${location}&radius=${radius}&language=${language}&key=${apiKey}"
                        } else {
                            //type을 붙혀준다
                            "${site}?location=${location}&radius=${radius}&language=${language}&key=${apiKey}&type${type}"
                        }
                        //Log.d("test1234", serverPath)

                        //그니까 토큰이 있다면 위의 접속할 주소에 계속해서 다음 토큰을 넣어주고
                        //pagetoken이 null이라면 그냥 접속할 주소를 보여준다
                        var serverPath2 = if (pagetoken != null){
                            "${serverPath}&pagetoken=${pagetoken}"
                        }else{
                            serverPath
                        }

                        //서버에 접속한다
                        var url = URL(serverPath2)
                        var httpURLConnection = url.openConnection() as HttpURLConnection

                        //스트림을 생성한다
                        var inputStreamReader = InputStreamReader(httpURLConnection.inputStream)

                        //라인 단위로 문자열을 읽어오는 스트림
                        //장문의 문자열을 한 줄씩 읽어오는 것 --> BufferedReader
                        var bufferedReader = BufferedReader(inputStreamReader)

                        //읽어온 한 줄의 문자열을 담을 변수
                        var str: String? = null

                        //읽어온 문장들을 누적해서 담을 객체
                        var stringBuffer = StringBuffer()

                        //처음부터 마지막까지 읽어온다
                        //그러고 이 페이지가 더이상 읽어올 것이 없다면 null이 반환되고 반복문을 멈춘다
                        do {
                            //한줄의 문자열을 읽어온다
                            str = bufferedReader.readLine()

                            if (str != null) {
                                //stringBuffer에 누적 시켜준다
                                stringBuffer.append(str)
                            }

                        } while (str != null)

                        //전체가 {}이므로 JSONObject를 생성한다 --> 객체
                        //만약 전체가 []이면 JSONArray를 사용한다 --> 리스트
                        var root = JSONObject(stringBuffer.toString())

                        //status값이 OK인 경우 데이터를 가져온다
                        if (root.has("status")) {
                            var status = root.getString("status")

                            //status가 OK라면
                            if (status == "OK") {
                                //웹 사이트를 보면서하자!!
                                var resultArray = root.getJSONArray("results")

                                //처음부터 끝까지 반복한다
                                for (idx in 0 until resultArray.length()) {

                                    //idx번째 JSON 객체를 가져온다
                                    var resultsObject = resultArray[idx] as JSONObject

                                    //geometry 이름으로 객체를 가져온다
                                    var geometryObject = resultsObject.getJSONObject("geometry")

                                    //location 이름으로 객체를 가져온다
                                    var locationObject = geometryObject.getJSONObject("location")

                                    //위도를 가져온다
                                    var lat = locationObject.getDouble("lat")
                                    //경도를 가져온다
                                    var lng = locationObject.getDouble("lng")

                                    //이름을 가져온다
                                    var name = resultsObject.getString("name")
                                    //대략적 주소를 가져온다
                                    var vicinity = resultsObject.getString("vicinity")
                                }

                                //next_page_token이 있다면?!
                                if (root.has("next_page_token")){
                                    //토큰값을 담아준다
                                    pagetoken = root.getString("next_page_token")
                                }else{
                                    //null을 넣어준다
                                    //do while의 조건식이 토큰이 null이 아닐 경우 이므로
                                    //null을 넣어서 반복을 중단 시킨다
                                    pagetoken = null
                                }

                            } else {
                                showDataError()
                            }

                        } else {
                            showDataError()
                        }
                        //요기 요기가 null이어야 반복문이 멈춘다
                    }while (pagetoken != null)

                }
            }

        }catch (e:Exception){
            showDataError()
        }

    }

    //에러가 났을 때 SnackBar를 보여줄 함수를 하나 만든다
    fun showDataError(){
        Snackbar.make(activitySettingBinding.root, "일시적인 네트워크 오류입니다", Snackbar.LENGTH_SHORT).show()
    }

}





































