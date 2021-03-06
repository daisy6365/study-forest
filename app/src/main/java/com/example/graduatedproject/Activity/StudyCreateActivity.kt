package com.example.graduatedproject.Activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.graduatedproject.model.Category
import com.example.graduatedproject.model.MapCode
import com.example.graduatedproject.model.Study
import com.example.graduatedproject.R
import com.example.graduatedproject.Util.ServerUtil
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.android.synthetic.main.activity_study_create.*
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

class StudyCreateActivity : AppCompatActivity(), MapView.MapViewEventListener {
    private val TAG = StudyCreateActivity::class.java.simpleName
    val PERMISSIONS_REQUEST_CODE = 100
    var REQUIRED_PERMISSIONS = arrayOf<String>( Manifest.permission.ACCESS_FINE_LOCATION)
    var new_imageUrl : Uri? = null
    var new_imageUrlPath : String? = null
    var imageFile : File? = null
    var current_marker = MapPOIItem()
    lateinit var reverseGeoCoder : MapReverseGeoCoder
    var people_num : Int = 0
    var tagArray = arrayListOf<String>()
    var offline : Boolean = false
    var online : Boolean = false
    var uLatitude : Double? = null
    var uLongitude : Double? = null
    var locationCode : String? = null
    private lateinit var spinnerAdapterparent : SpinnerAdapter
    private lateinit var spinnerAdapterchild : SpinnerAdapter
    var categoryListParent : ArrayList<Category>? = null
    var categoryListChild : ArrayList<Category>? = null
    var categoryParent: MutableList<String> = mutableListOf("??? ????????????")
    var categoryChild : MutableList<String> =  mutableListOf("?????? ????????????")
    var categorychildSeletedItem : String? = null
    lateinit var mapView : MapView
    lateinit var mapViewContainer : RelativeLayout


    private val getImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            //???????????? ????????? ???????????????
            new_imageUrl = result.data?.data!!
            Glide.with(this)
                .load(new_imageUrl)
                .centerCrop()
                .error(com.example.graduatedproject.R.drawable.cover)
                .into(create_study_cover_img)
            //?????????????????? ?????? ??????
            new_imageUrlPath= absolutelyPath( this ,new_imageUrl!!)
            imageFile = File(new_imageUrlPath)
        } else {
            //???????????????
            Toast.makeText(this@StudyCreateActivity, "?????? ?????? ??????", Toast.LENGTH_LONG).show()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.example.graduatedproject.R.layout.activity_study_create)

        //??? ?????? ??????
        var chipGroup: ChipGroup = create_chip_group
        var inflater: LayoutInflater = LayoutInflater.from(chipGroup.context)

        mapView = MapView(this)
        mapViewContainer = map_view
        mapViewContainer.addView(mapView)
        create_study_cover_img.clipToOutline = true

        val permissionCheck =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            val lm: LocationManager =
                getSystemService(Context.LOCATION_SERVICE) as LocationManager
            try {

                val userNowLocation: Location? =
                    lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                //???????????? ????????????
                uLatitude = userNowLocation!!.latitude
                uLongitude = userNowLocation!!.longitude
                val uNowPosition = MapPoint.mapPointWithGeoCoord(uLatitude!!, uLongitude!!)
                mapView.setMapCenterPoint(uNowPosition, true)

                //????????????(??????, ??????)??? ???????????? ?????? ??????

                current_marker.itemName = "?????? ????????? ??????????????? ??????"
                current_marker.tag = 0
                current_marker.mapPoint = MapPoint.mapPointWithGeoCoord(uLatitude!!, uLongitude!!)
                current_marker.isMoveToCenterOnSelect
                current_marker.markerType = MapPOIItem.MarkerType.BluePin
                current_marker.selectedMarkerType = MapPOIItem.MarkerType.RedPin
                mapView.addPOIItem(current_marker)

                //???????????? ????????? ??? ????????????
                //????????? ?????? ???????????? ????????? ?????? ????????????
                //????????? ????????? ??????, ?????? ????????????
                mapView.setMapViewEventListener(this)

                //????????? ??????????????? ???????????? -> ?????????
                change_study_cover_btn.setOnClickListener {
                    //????????? ????????? ??????url ?????? -> ??????????????? ??????
                    openGalley()
                }
                number_minus.setOnClickListener {
                    if(people_num > 2){
                        people_num--
                        people_number.text = people_num.toString()
                    }
                    else{
                        Toast.makeText(this@StudyCreateActivity, "?????????????????? ?????? 2???????????? ?????????.", Toast.LENGTH_LONG).show()
                    }
                }
                number_plus.setOnClickListener {
                    people_num++
                    people_number.text = people_num.toString()
                }
                add_tag_btn.setOnClickListener{
                    //????????? ???????????? ?????? ?????? ??? ?????????
                    addTag(chipGroup,inflater)
                    edit_tag.setText(null)
                    edit_tag.setHint("???????????? ??????")
                }

                getCategoryList()
                big_category.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if(big_category.getItemAtPosition(position).equals("??? ????????????")){ }
                        for(i in 1..categoryParent.size-1){
                            categoryChild =  mutableListOf("?????? ????????????")

                            Log.d("???????????? ?????? ", categoryChild.toString())

                            setCategoryChild()

                            if(big_category.getItemAtPosition(position).equals(categoryParent[i])){
                                ServerUtil.retrofitService.requestCategoryChild(i)
                                    .enqueue(object : Callback<ArrayList<Category>> {
                                        override fun onResponse(call: Call<ArrayList<Category>>, response: Response<ArrayList<Category>>) {
                                            if (response.isSuccessful) {
                                                categoryListChild = response.body()!!
                                                for(i in 0..categoryListChild!!.size-1){
                                                    categoryChild?.add(categoryListChild!!.get(i).name)
                                                }
                                                Log.d(TAG, "???????????? ???????????????????????? ?????? ??????")

                                                setCategoryChild()

                                            }
                                        }
                                        override fun onFailure(call: Call<ArrayList<Category>>, t: Throwable) {
                                            Log.d(TAG, "???????????? ???????????????????????? ?????? ??????")
                                            Toast.makeText(this@StudyCreateActivity, "???????????? ???????????????????????? ?????? ??????", Toast.LENGTH_LONG).show()
                                        }
                                    })

                            }
                        }

//                        categoryparentSeletedItem = categoryParent!![position]
//                        Log.d("categoryparentSeletedItem", categoryparentSeletedItem!!.toString())

                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {
                    }

                }
                small_category.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if(small_category.getItemAtPosition(position).equals("?????? ????????????")) {

                        }
                        else{
                            categorychildSeletedItem = categoryChild!![position]
                            Log.d("categorychildSeletedItem", categorychildSeletedItem!!.toString())
                        }

                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {
                    }
                }


                check_offline.setOnCheckedChangeListener { buttonView, isChecked ->
                    if(isChecked){
                        offline = true
                    }
                    else{
                        offline = false
                    }
                }
                check_online.setOnCheckedChangeListener { buttonView, isChecked ->
                    if(isChecked){
                        online = true
                    }
                    else{
                        online = false
                    }
                }

                register_study.setOnClickListener {
                    if(offline == false && online == false){
                        Toast.makeText(this, "ON/OFFLINE??? ??????????????????.", Toast.LENGTH_SHORT).show()
                    }
                    else{
                        sendStudyInfo()
                    }
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
    //????????????
    private fun sendStudyInfo(){
        var pref = getSharedPreferences("login_sp", MODE_PRIVATE)
        var accessToken: String = "Bearer " + pref.getString("access_token", "").toString()
        var categorychildSeletedId : Int? = null


        //???????????????,????????????, ???????????? ?????????, ????????????2???, ????????????, ???/???????????? ??????, locationId ?????? ????????????
        var studyName = edit_study_name.text.toString()
        var numberOfPeople = people_number.text.toString()
        var content = create_introduce_text.text.toString()
        for(i in 0..categoryListChild!!.size-1){
            if(categoryListChild!!.get(i).name == categorychildSeletedItem){
                categorychildSeletedId = categoryListChild!!.get(i).id
            }
        }


        lateinit var requestImg: RequestBody
        var imageBitmap : MultipartBody.Part? = null
        if(imageFile != null){
            requestImg= RequestBody.create(MediaType.parse("image/*"),imageFile)
            imageBitmap = MultipartBody.Part.createFormData("image", imageFile?.getName(), requestImg)

        }else{}

        var jsonArray = JSONArray(tagArray)
        val paramObject = JSONObject()
        paramObject.put("name", studyName)
        paramObject.put("numberOfPeople", numberOfPeople.toInt())
        paramObject.put("content", content)
        paramObject.put("tags",jsonArray)
        paramObject.put("online", online)
        paramObject.put("offline", offline)
        paramObject.put("locationCode", locationCode)
        paramObject.put("categoryId", categorychildSeletedId!!.toInt())
        val request = RequestBody.create(MediaType.parse("application/json"),paramObject.toString())

        ServerUtil.retrofitService.SendCreateStudyInfo(accessToken, imageBitmap, request)
            .enqueue(object : Callback<Study> {
                override fun onResponse(call: Call<Study>, response: Response<Study>) {
                    if (response.isSuccessful) {
                        var studyId : Int = response.body()!!.id

                        Log.d(TAG, "????????????????????? ?????? ??????")

                        mapViewContainer.removeView(mapView)


                        val intent = Intent(this@StudyCreateActivity, StudyApplyActivity::class.java)
                        intent.putExtra("studyId",studyId)
                        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))

                        finish()
                    }
                }
                override fun onFailure(call: Call<Study>, t: Throwable) {
                    Log.d(TAG, "????????????????????? ?????? ??????")
                    Toast.makeText(this@StudyCreateActivity, "????????????????????? ?????? ??????", Toast.LENGTH_LONG).show()

                }
            })

    }
    //???????????? ????????? ????????????
    fun getCategoryList(){
        ServerUtil.retrofitService.requestCategoryParent()
            .enqueue(object : Callback<ArrayList<Category>> {
                override fun onResponse(call: Call<ArrayList<Category>>, response: Response<ArrayList<Category>>) {
                    if (response.isSuccessful) {
                        categoryListParent = response.body()
                        for(i in 0 .. categoryListParent!!.size-1){
                            categoryParent?.add(categoryListParent!!.get(i).name)

                        }
                        Log.d(TAG, "???????????? ???????????????????????? ?????? ??????")

                    }
                }
                override fun onFailure(call: Call<ArrayList<Category>>, t: Throwable) {
                    Log.d(TAG, "???????????? ???????????????????????? ?????? ??????")
                    Toast.makeText(this@StudyCreateActivity, "???????????? ???????????????????????? ?????? ??????", Toast.LENGTH_LONG).show()
                }
            })

        spinnerAdapterparent = ArrayAdapter(this,R.layout.item_spinner, categoryParent)
        big_category.adapter = spinnerAdapterparent

    }
    //?????????????????? ??????
    private fun setCategoryChild(){
        Log.d("???????????? ?????? ", categoryChild.toString())
        spinnerAdapterchild = ArrayAdapter(this,R.layout.item_spinner, categoryChild)
        small_category.adapter = spinnerAdapterchild
    }
    //?????? ??????
    fun addTag(chipGroup : ChipGroup, inflater: LayoutInflater): ArrayList<String> {
        var chip : Chip = inflater.inflate(R.layout.item_liketopic, chipGroup, false) as Chip

        tagArray.add(edit_tag.text.toString())
        chip.text = edit_tag.text.toString()
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

        val filePath = context!!.applicationInfo.dataDir + File.separator +
                System.currentTimeMillis()
        val file = File(filePath)

        try {
            val inputStream = contentResolver.openInputStream(new_imageUrl) ?: return null.toString()
            val outputStream: OutputStream = FileOutputStream(file)
            val buf = ByteArray(1024)
            var len: Int

            while (inputStream.read(buf).also { len = it } > 0) outputStream.write(buf, 0, len)

            outputStream.close()
            inputStream.close()

        } catch (e: IOException) {
            return null.toString()
        }
        val result = file.absolutePath
        return result
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
                        Toast.makeText(this@StudyCreateActivity, "????????? ?????? ??????????????????.", Toast.LENGTH_LONG).show()
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
                        Toast.makeText(this@StudyCreateActivity, "?????? ????????????code ?????? ??????", Toast.LENGTH_LONG).show()
                    }
                })

        }
    }
    override fun onMapViewDragStarted(p0: MapView?, p1: MapPoint?) {}
    override fun onMapViewDragEnded(p0: MapView?, p1: MapPoint?) {}
    override fun onMapViewMoveFinished(p0: MapView?, p1: MapPoint?) {}
}