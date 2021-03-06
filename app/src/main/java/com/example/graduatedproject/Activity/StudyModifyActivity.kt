package com.example.graduatedproject.Activity

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import androidx.activity.result.ActivityResult
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.graduatedproject.model.MapCode
import com.example.graduatedproject.model.Study
import com.example.graduatedproject.R
import com.example.graduatedproject.Util.ServerUtil
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.android.synthetic.main.activity_study_apply.*
import kotlinx.android.synthetic.main.activity_study_modify.*
import kotlinx.android.synthetic.main.activity_study_modify.big_category
import kotlinx.android.synthetic.main.activity_study_modify.small_category
import net.daum.mf.map.api.MapPOIItem
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapReverseGeoCoder
import net.daum.mf.map.api.MapView
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class StudyModifyActivity : AppCompatActivity(), MapView.MapViewEventListener {
    private val TAG = StudyModifyActivity::class.java.simpleName
    var studyInfo = Study()
    val PERMISSIONS_REQUEST_CODE = 100
    var REQUIRED_PERMISSIONS = arrayOf<String>( Manifest.permission.ACCESS_FINE_LOCATION)
    var new_imageUrl : Uri? = null
    var new_imageUrlPath : String? = null
    var imageFile : File? = null
    var deleteImage : Boolean = false
    var current_marker = MapPOIItem()
    lateinit var reverseGeoCoder : MapReverseGeoCoder
    var people_num : Int = 0
    var tagArray = arrayListOf<String>()
    var status : String = "OPEN"
    var offline : Boolean = false
    var online : Boolean = false
    var uLatitude : Double? = null
    var uLongitude : Double? = null
    var locationCode : String? = null
    lateinit var mapView : MapView
    lateinit var mapViewContainer : RelativeLayout


    private val getImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            //???????????? ????????? ???????????????
            new_imageUrl = result.data?.data!!
            Glide.with(this)
                .load(new_imageUrl)
                .centerCrop()
                .error(R.drawable.cover)
                .into(modify_study_cover_img)
            //?????????????????? ?????? ??????
            new_imageUrlPath= absolutelyPath( this ,new_imageUrl!!)
            imageFile = File(new_imageUrlPath)
        } else {
            //???????????????
            Toast.makeText(this@StudyModifyActivity, "?????? ?????? ??????", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_study_modify)

        var pref = getSharedPreferences("login_sp", MODE_PRIVATE)
        var accessToken: String = "Bearer " + pref.getString("access_token", "").toString()

        //??? ?????? ??????
        var chipGroup: ChipGroup = modify_chip_group
        var inflater: LayoutInflater = LayoutInflater.from(chipGroup.context)
        modify_study_cover_img.clipToOutline = true

        mapView = MapView(this)
        mapViewContainer = modify_map_view
        mapViewContainer.addView(mapView)

        val permissionCheck =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            try {
                //?????? ???????????? ????????? ????????? ?????????
                val studyRoomId = intent.getIntExtra("studyRoomId",0)
                val studyId = studyRoomId
                ServerUtil.retrofitService.requestStudy(accessToken,studyId)
                    .enqueue(object : Callback<Study> {
                        override fun onResponse(call: Call<Study>, response: Response<Study>) {
                            if (response.isSuccessful) {
                                studyInfo  = response.body()!!

                                if(studyInfo!!.image?.profileImage != null){
                                    Glide.with(this@StudyModifyActivity)
                                        .load(studyInfo!!.image?.profileImage)
                                        .centerCrop()
                                        .into(modify_study_cover_img)
                                } else if(studyInfo!!.image == null){
                                    Glide.with(this@StudyModifyActivity)
                                        .load(R.drawable.applogo_gray)
                                        .centerCrop()
                                        .into(modify_study_cover_img)
                                }
                                modify_study_name.setText(studyInfo!!.name)
                                people_num = studyInfo!!.numberOfPeople
                                modify_people_number.setText(people_num.toString())
                                tagArray = studyInfo!!.studyTags!!
                                for(i in 0 .. tagArray.size-1){
                                    var oldchip : Chip = inflater.inflate(R.layout.item_liketopic, chipGroup, false) as Chip
                                    oldchip.text = tagArray[i]
                                    chipGroup.addView(oldchip)

                                    oldchip.setOnCloseIconClickListener {
                                        for(i in 0 .. 6) {
                                            tagArray.remove((it as TextView).text.toString())
                                            chipGroup.removeView(it)
                                            Log.d("???????????? ?????????", tagArray.toString())
                                        }
                                    }
                                }

                                modify_introduce_text.setText(studyInfo.content)
                                if(studyInfo.status == "OPEN"){ study_close.isChecked = false }
                                else{ study_close.isChecked = true }

                                if(studyInfo!!.online == true){
                                    modify_check_online.isChecked = true
                                }else{modify_check_online.isChecked = false}

                                if(studyInfo!!.offline == true){
                                    modify_check_offline.isChecked = true
                                }else{modify_check_offline.isChecked = false}

                                big_category.setText(studyInfo.parentCategory!!.name)
                                small_category.setText(studyInfo.childCategory!!.name)

                                //???????????? ???????????? ????????? ????????? ?????? ??????
                                uLatitude = studyInfo.location!!.let
                                uLongitude = studyInfo.location!!.len
                                locationCode = studyInfo.location!!.code

                                val uNowPosition = MapPoint.mapPointWithGeoCoord(uLatitude!!, uLongitude!!)
                                mapView.setMapCenterPoint(uNowPosition, true)

                                //????????????(??????, ??????)??? ???????????? ?????? ??????
                                current_marker.itemName = "?????? ??????"
                                current_marker.tag = 0
                                current_marker.mapPoint = MapPoint.mapPointWithGeoCoord(uLatitude!!, uLongitude!!)
                                current_marker.isMoveToCenterOnSelect
                                current_marker.markerType = MapPOIItem.MarkerType.BluePin
                                current_marker.selectedMarkerType = MapPOIItem.MarkerType.RedPin
                                mapView.addPOIItem(current_marker)


                                Log.d(TAG, "????????? ?????? ?????? ??????")
                            }
                        }
                        override fun onFailure(call: Call<Study>, t: Throwable) {
                            Log.d(TAG, "????????? ?????? ?????? ??????")
                            Toast.makeText(this@StudyModifyActivity, "????????? ?????? ?????? ??????", Toast.LENGTH_LONG).show()
                        }
                    })
                //???????????? ????????? ??? ????????????
                //????????? ?????? ???????????? ????????? ?????? ????????????
                //????????? ????????? ??????, ?????? ????????????
                mapView.setMapViewEventListener(this)

                //?????? ??????
                modify_study_cover_btn.setOnClickListener {
                    //????????? ????????? ??????url ?????? -> ??????????????? ??????
                    openGalley()
                }

                modify_number_minus.setOnClickListener {
                    if(people_num > 2){
                        people_num--
                        modify_people_number.text = people_num.toString()
                    }
                    else{
                        Toast.makeText(this@StudyModifyActivity, "?????????????????? ?????? 2???????????? ?????????.", Toast.LENGTH_LONG).show()
                    }
                }
                modify_number_plus.setOnClickListener {
                    people_num++
                    modify_people_number.text = people_num.toString()
                }
                add_tag_btn.setOnClickListener {
                    //????????? ???????????? ?????? ?????? ??? ?????????
                    addTag(chipGroup,inflater)
                    modify_edit_tag.setText(null)
                    modify_edit_tag.setHint("???????????? ??????")
                }

                study_close.setOnCheckedChangeListener { buttonView, isChecked ->
                    if(isChecked){ status = "CLOSE" }
                    else{ status = "OPEN" }
                }
                modify_check_offline.setOnCheckedChangeListener { buttonView, isChecked ->
                    if(isChecked){ offline = true }
                    else{ offline = false }
                }
                modify_check_online.setOnCheckedChangeListener { buttonView, isChecked ->
                    if(isChecked){ online = true }
                    else{ online = false }
                }

                //???????????? ????????? ????????? ??? ???????????? ????????? ?????????
                modify_study_btn.setOnClickListener {
                    sendModifiedInfo(accessToken, studyId)
                }

                //???????????? ????????? ????????? -> ?????? ??????
                delete_study.setOnClickListener {
                    var builder = AlertDialog.Builder(this)
                    builder.setTitle("??????")
                    builder.setMessage("?????? ???????????? ?????????????????????????")
                    builder.setPositiveButton("??????", DialogInterface.OnClickListener { dialog, which ->
                        sendDeleteStudy(accessToken,studyId)
                    })
                    builder.setNegativeButton("??????", DialogInterface.OnClickListener { dialog, which ->
                        Log.d(TAG, "??????")
                    })
                    builder.show()
                }

            } catch (e: NullPointerException) {
                Log.e("LOCATION_ERROR", e.toString())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    ActivityCompat.finishAffinity(this)
                } else {
                    ActivityCompat.finishAffinity(this)
                }
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)

                System.exit(0)

            }
        } else {
            Toast.makeText(this, "?????? ????????? ????????????.", Toast.LENGTH_SHORT).show()
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                PERMISSIONS_REQUEST_CODE
            )
        }
    }

    fun sendModifiedInfo(accessToken : String,studyId : Int){
        var close : Boolean = false

        //???????????????,????????????, ???????????? ?????????, ????????????2???, ????????????, ???/???????????? ??????, locationId ?????? ????????????
        var studyName = modify_study_name.text.toString()
        var numberOfPeople = modify_people_number.text.toString()
        var content = modify_introduce_text.text.toString()
        if(status == "CLOSE"){
            close = true
        }
        else{
            close = false
        }
        lateinit var requestImg: RequestBody
        var imageBitmap : MultipartBody.Part? = null
        Log.d(TAG, imageFile.toString())
        if(imageFile != null){
            requestImg= RequestBody.create(MediaType.parse("image/*"),imageFile)
            imageBitmap = MultipartBody.Part.createFormData("image", imageFile?.getName(), requestImg)

        }else{deleteImage = true}

        var jsonArray = JSONArray(tagArray)
        val paramObject = JSONObject()
        paramObject.put("name", studyName)
        paramObject.put("numberOfPeople", numberOfPeople.toInt())
        paramObject.put("content", content)
        paramObject.put("tags",jsonArray)
        paramObject.put("deleteImage", deleteImage)
        paramObject.put("close",close)
        paramObject.put("online", online)
        paramObject.put("offline", offline)
        paramObject.put("locationCode", locationCode)
        paramObject.put("categoryId", studyInfo.childCategory!!.id)
        val request = RequestBody.create(MediaType.parse("application/json"),paramObject.toString())

        ServerUtil.retrofitService.requestModifyStudy(accessToken,studyId,imageBitmap,request)
            .enqueue(object : Callback<Study> {
                override fun onResponse(call: Call<Study>, response: Response<Study>) {
                    if (response.isSuccessful) {
                        var studyId : Int = response.body()!!.id

                        Log.d(TAG, "????????????????????? ?????? ??????")

                        mapViewContainer.removeView(mapView)
                        finish()
                        val intent = Intent(this@StudyModifyActivity, StudyRoomActivity::class.java)
                        intent.putExtra("studyRoomId",studyId)
                        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))

                    }
                }
                override fun onFailure(call: Call<Study>, t: Throwable) {
                    Log.d(TAG, "????????????????????? ?????? ??????")
                    Toast.makeText(this@StudyModifyActivity, "????????????????????? ?????? ??????", Toast.LENGTH_LONG).show()
                }
            })
    }

    fun sendDeleteStudy(accessToken : String, studyId : Int){
        ServerUtil.retrofitService.requestDeleteStudy(accessToken,studyId)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        val intent = Intent(this@StudyModifyActivity, MainActivity::class.java)
                        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))

                        Log.d(TAG, "????????? ?????? ??????")
                    }
                }
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.d(TAG, "????????? ?????? ??????")
                    Toast.makeText(this@StudyModifyActivity, "????????? ?????? ??????", Toast.LENGTH_LONG).show()
                }
            })
    }

    //???????????? ??????
    private fun openGalley() {
        //???????????? ??????????????? ??? -> ???????????? ???
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        getImage.launch(intent)
    }
    //????????? ??????????????? ????????? ??????????????? ??????
    fun absolutelyPath(context : Context, new_imageUrl: Uri): String {
        val contentResolver = context!!.contentResolver?: return null.toString()
        val filePath = context!!.applicationInfo.dataDir + File.separator + System.currentTimeMillis()
        val file = File(filePath)
        try {
            val inputStream = contentResolver.openInputStream(new_imageUrl) ?: return null.toString()
            val outputStream: OutputStream = FileOutputStream(file)
            val buf = ByteArray(1024)
            var len: Int
            while (inputStream.read(buf).also { len = it } > 0) outputStream.write(buf, 0, len)
            outputStream.close()
            inputStream.close()
        } catch (e: IOException) { return null.toString() }
        val result = file.absolutePath
        return result
    }

    //?????? ??????
    fun addTag(chipGroup : ChipGroup, inflater: LayoutInflater): ArrayList<String> {
        var chip : Chip = inflater.inflate(R.layout.item_liketopic, chipGroup, false) as Chip

        tagArray.add(modify_edit_tag.text.toString())
        chip.text = modify_edit_tag.text.toString()
        chipGroup.addView(chip)

        Log.d("???????????? ?????????", tagArray.toString())
        chip.setOnCloseIconClickListener {
            for(i in 0 .. 6) {
                tagArray.remove((it as TextView).text.toString())
                chipGroup.removeView(it)
                Log.d("???????????? ?????????", tagArray.toString())
            }
        }
        return tagArray
    }

    override fun onMapViewInitialized(p0: MapView?) {}
    override fun onMapViewCenterPointMoved(p0: MapView?, p1: MapPoint?) {}
    override fun onMapViewZoomLevelChanged(p0: MapView?, p1: Int) {}
    override fun onMapViewSingleTapped(p0: MapView?, p1: MapPoint?) {}
    override fun onMapViewDoubleTapped(p0: MapView?, p1: MapPoint?) {}
    override fun onMapViewLongPressed(p0: MapView?, p1: MapPoint?){
        if (p1 != null) {
            p0?.removeAllPOIItems()           // ?????? ?????? ????????????
            // ????????? ????????? ??????
            uLatitude = p1!!.mapPointGeoCoord.latitude
            uLongitude = p1!!.mapPointGeoCoord.longitude
            Log.d("?????? ?????? ??????uLatitude", uLatitude.toString())
            Log.d("?????? ?????? ??????uLongitude", uLongitude.toString())
            var kakaoToken = "KakaoAK 9a9a02eedbe752442e4a7550ec217f88"

            // ????????? ????????? ????????? ?????? ?????????
            reverseGeoCoder = MapReverseGeoCoder("35e1ae8dad57b1dfb6a8f38f6903c184", p1!!,
                object : MapReverseGeoCoder.ReverseGeoCodingResultListener {
                    override fun onReverseGeoCoderFoundAddress(mapReverseGeoCoder: MapReverseGeoCoder, s: String) {
                        current_marker.itemName = s
                    }

                    override fun onReverseGeoCoderFailedToFindAddress(mapReverseGeoCoder: MapReverseGeoCoder) {
                        Log.e("Err", "GeoCoding")
                        Toast.makeText(this@StudyModifyActivity, "????????? ?????? ??????????????????.", Toast.LENGTH_LONG).show()
                    }
                }, this
            )
            current_marker.mapPoint = MapPoint.mapPointWithGeoCoord(uLatitude!!, uLongitude!!)
            p0!!.addPOIItem(current_marker)

            Log.d("?????? ?????? ??????", p1.toString())

            //LocationCode ????????????
            ServerUtil.kakaoService.requestCode(kakaoToken, uLongitude!!, uLatitude!!)
                .enqueue(object : Callback<MapCode> {
                    override fun onResponse(call: Call<MapCode>, response: Response<MapCode>) {
                        if (response.isSuccessful) {
                            //?????????
                            if(response.body()?.documents?.size == 2){
                                locationCode = response.body()?.documents?.get(1)?.code
                                Log.d( "?????? ??????CODE",locationCode.toString())
                            }
                            //?????????
                            else{
                                locationCode = response.body()?.documents?.get(0)?.code
                                Log.d( "?????? ??????CODE",locationCode.toString())
                            }
                        }
                    }
                    override fun onFailure(call: Call<MapCode>, t: Throwable) {
                        Log.d(TAG, "?????? ??????code ?????? ??????")
                        Toast.makeText(this@StudyModifyActivity, "?????? ????????????code ?????? ??????", Toast.LENGTH_LONG).show()
                    }
                })
        }
    }
    override fun onMapViewDragStarted(p0: MapView?, p1: MapPoint?) {}
    override fun onMapViewDragEnded(p0: MapView?, p1: MapPoint?) {}
    override fun onMapViewMoveFinished(p0: MapView?, p1: MapPoint?) {}

}