package kr.co.lion.android01.practice_lbs

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.gms.maps.MapsInitializer
import com.google.android.material.transition.MaterialSharedAxis
import kr.co.lion.android01.practice_lbs.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    lateinit var activityMainBinding: ActivityMainBinding

    //Activity 실행을 위한런쳐
    lateinit var cameraLauncher:ActivityResultLauncher<Intent>

    //촬영된 사진이 저장된 경로 정보를 가지고 있는 uri객체
    lateinit var contenturi:Uri

    //엘범을 가져오기 위한 런쳐
    lateinit var albumLauncher:ActivityResultLauncher<Intent>

    //확인할 권한 목록
    var permissionList = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_MEDIA_LOCATION,
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)
        setToolBar()
        setEvent()
        initView()
        setButtonEvent()

        requestPermissions(permissionList, 0)

    }

    //뷰 설정
    fun initView(){
        //사진 촬영을 위한 런쳐 생성
        var contract = ActivityResultContracts.StartActivityForResult()
        cameraLauncher = registerForActivityResult(contract){
            //사진을 사용하겠다고 한 뒤 돌아왔을 경우
            if (it.resultCode == RESULT_OK){
                //사진 객체를 생성한다
                var bitMap = BitmapFactory.decodeFile(contenturi.path)


                //회전 각도 값을 구한다
                var degree = getDegree(contenturi)
                //회전한 이미지를 구한다
                var bitmap2 = rotateBitMap(bitMap, degree.toFloat())
                //크기를 조정한 이미지를 구한다
                var bitmap3 = resizeBitMap(bitmap2, 1024)

                activityMainBinding.imageView.setImageBitmap(bitmap3)

                //사진 파일을 삭제한다
                var file = File(contenturi.path)
                file.delete()
            }
        }

        var contract2 = ActivityResultContracts.StartActivityForResult()
        albumLauncher = registerForActivityResult(contract2){
            //사진 선택을 완료한 후 돌아왔다면,,,
            if (it.resultCode == RESULT_OK){
                //선택한 이미지의 경로 데이터를 관리하는 uri객체를 추출한다
                var uri = it.data?.data

                //uri의 값이 있다면
                if (uri != null){
                    //버전 별로 분기한다
                    //안드로이드 Q이상이라면?
                    var bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                        //이미지를 생성할 수 있는 객체를 생성한다
                        var resource = ImageDecoder.createSource(contentResolver, uri)
                        //Bitmap을 생성한다
                        ImageDecoder.decodeBitmap(resource)
                    }else{
                        //컨텐트 프로바이더를 통해 이미지 데이터에 접근한다
                        var cursor = contentResolver.query(uri, null, null, null, null)
                        if (cursor != null){
                            cursor.moveToNext()

                            //이미지의 경로를 가져온다
                            var idx = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                            var source = cursor.getString(idx)

                            //이미지를 생성한다
                            BitmapFactory.decodeFile(source)
                        }else{
                            null
                        }
                    }
                    //회전 각도값을 가져온다
                    var degree = getDegree(uri)
                    var bitmap2 = rotateBitMap(bitmap!!, degree.toFloat())
                    var bitmap3 = resizeBitMap(bitmap2, 1024)

                    activityMainBinding.imageView.setImageBitmap(bitmap3)
                }
            }
        }
    }

    //이벤트 설정
    fun setButtonEvent(){
        activityMainBinding.apply {
            //사진 촬영 버튼을 눌렀을 때
            cameraButton.setOnClickListener {
                //촬영된 사진이 저장될 경로
                //외부 파일 저장소에서 어플의 경로를 가져온다
                var rootPath = getExternalFilesDir(null).toString()

                //이미지 파일명을 포함한 경로
                //얘가 이름
                var picpath = "${rootPath}/tempImage.jpg"

                //File 객체 생성
                var file = File(picpath)

                //사진이 저장된 위치를 관리할 Uri 생성
                //AndroidMenifest.xml에 등록한 provider의 authorities를 가져온다
                //얘가 위치?!
                var a1 = "kr.co.lion.android01.practice_lbs.file_provider"
                contenturi = FileProvider.getUriForFile(this@MainActivity, a1, file)

                if (contenturi != null){
                    //실행할 액티비티를 카메라 액티비티로 지정한다
                    //단말기에 설치되어있는 모든 어플이 가진 액티비티 중에 사진촬영이 가능한 액티비티가 실행된다
                    var cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    //이미지가 저장될 경로를 가진 Uri객체를 인텐트에 담아준다
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, contenturi)

                    //카메라 엑티비티 실행
                    cameraLauncher.launch(cameraIntent)
                }

            }
            albumButton.setOnClickListener {
                //앨범에서 사진을 선택할 수 있도록 세팅된 인텐트를 설정한다
                var albumIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                //실행할 액티비티의 타입을 설정(이미지를 선택할 수 있는 것이 뜨게한다)
                ///이건 그냥 뜨게 하는거
                albumIntent.setType("image/*")
                //선택할 수 있는 파일들의 MimeType을 설정한다
                //여기서 선택한 종류의 파일만 선택이 가능하다. 여기선 모든 파일로 선택함
                ///이건 선택하게 하는거
                var mimeType = arrayOf("image/*")
                albumIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeType)

                //액티비티를 실행한다
                albumLauncher.launch(albumIntent)
            }
        }
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
    // 사진의 회전 각도값을 반환하는 메서드
    // ExifInterface : 사진, 영상, 소리 등의 파일에 기록한 정보
    // 위치, 날짜, 조리개값, 노출 정도 등등 다양한 정보가 기록된다.
    // ExifInterface 정보에서 사진 회전 각도값을 가져와서 그만큼 다시 돌려준다.
    fun getDegree(uri: Uri) : Int {
        var exifInterface:ExifInterface? = null

        //버전별로 분기한다
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            //이미지 데이터를 가져올 수 있는 content Provider의 Uri를 추출한다
            //Exifinterface 정보를 읽어올 스트림을 추출한다
            var inputStream = contentResolver.openInputStream(uri)!!
            exifInterface = ExifInterface(inputStream)

        }else{
            exifInterface = ExifInterface(uri.path!!)
        }

        //즉 실행이 되면
        if (exifInterface != null){
            //반환할 각도값을 담을 변수
            var degree = 0
            //ExifInterface 객체에서 회전 각도값을 가져온다
            var ori = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)

            degree = when(ori){
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
            return degree
        }
        return 0
    }

    //화면을 회전시킨다
    fun rotateBitMap(bitMap:Bitmap, degree:Float) : Bitmap {
        //회전 이미지를 생성하기 위한 변환 행렬
        var matrix = Matrix()
        matrix.postRotate(degree)
        // 회전 행렬을 적용하여 회전된 이미지를 생성한다.
        // 첫 번째 : 원본 이미지
        // 두 번째와 세번째 : 원본 이미지에서 사용할 부분의 좌측 상단 x, y 좌표
        // 네번째와 다섯번째 : 원본 이미지에서 사용할 부분의 가로 세로 길이
        // 여기에서는 이미지데이터 전체를 사용할 것이기 때문에 전체 영역으로 잡아준다.
        // 여섯번째 : 변환행렬. 적용해준 변환행렬이 무엇이냐에 따라 이미지 변형 방식이 달라진다.
        var rotateBitMap = Bitmap.createBitmap(bitMap, 0, 0, bitMap.width, bitMap.height, matrix, false)
        return rotateBitMap

    }

    //이미지 사이즈를 조절한다
    fun resizeBitMap(bitMap: Bitmap, targetWidth:Int) : Bitmap {
        //이미지의 확대 축소 비율을 구한다
        var ratio = targetWidth.toDouble() / bitMap.width.toDouble()
        //세로 길이를 구한다
        var targetHeight = (bitMap.height * ratio).toInt()
        //크기 조정한 Bitmap을 생성한다
        var resizeBitmap = Bitmap.createScaledBitmap(bitMap, targetWidth, targetHeight, false)

        return resizeBitmap
    }
}




































