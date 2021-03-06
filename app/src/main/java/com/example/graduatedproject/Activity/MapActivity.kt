package com.example.graduatedproject.Activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.graduatedproject.model.MapCode
import com.example.graduatedproject.model.MyLocation
import com.example.graduatedproject.model.Profile
import com.example.graduatedproject.R
import com.example.graduatedproject.Util.ServerUtil
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.activity_map.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.*

class MapActivity : AppCompatActivity() {
    private val TAG = MapActivity::class.java.simpleName
    lateinit var currentLatLng: Location
    val PERMISSIONS_REQUEST_CODE = 100
    var REQUIRED_PERMISSIONS = arrayOf<String>( Manifest.permission.ACCESS_FINE_LOCATION)
    var profile = Profile()
    var location : MyLocation? = null

    var locationManager : LocationManager? = null
    var latitudeY : Double? = null
    var longitudeX : Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        var pref = getSharedPreferences("login_sp", MODE_PRIVATE)
        var accessToken: String = "Bearer " + pref.getString("access_token", "").toString()

        val permissionCheck =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            try {
                if (intent.hasExtra("modify_item")) {
                    val new_locationId = intent.getIntExtra("modify_item",0)
                    modifyLocation(accessToken,new_locationId) }
                else { Mylocation(accessToken) }

                place_modify_btn.setOnClickListener {
                    //Activity ??????
                    val intent = Intent(this@MapActivity, MapSearchActivity::class.java)
                    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                }

                currentplace_search_btn.setOnClickListener {
                    findMyGPS()
                }

                place_seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                    //????????? seekbar??? ?????? ????????? ??????
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        var distance : Int = 0
                        if(progress == 0){ distance = 3 }
                        else if(progress == 1){ distance = 6 }
                        else { distance = 9 }

                        ServerUtil.retrofitService.requestDistance(accessToken, distance)
                            .enqueue(object : Callback<Profile> {
                                override fun onResponse(call: Call<Profile>, response: Response<Profile>) {
                                    if (response.isSuccessful) {
                                        Log.d(TAG, "?????? ???????????? ?????? ??????" + progress)
                                        user_distance.text = response.body()?.searchDistance.toString() + "km"
                                    }
                                }
                                override fun onFailure(call: Call<Profile>, t: Throwable) {
                                    Log.d(TAG, "?????? ???????????? ?????? ??????"+ progress)
                                    Toast.makeText(this@MapActivity, "?????? ???????????? ?????? ??????", Toast.LENGTH_LONG).show()
                                }
                            })
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })

            } catch (e: NullPointerException) {
                Log.e("LOCATION_ERROR", e.toString())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    ActivityCompat.finishAffinity(this)
                } else {
                    ActivityCompat.finishAffinity(this)
                }
                val intent = Intent(this, MapActivity::class.java)
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
    fun Mylocation(accessToken : String){
        ServerUtil.retrofitService.requestProfile(accessToken)
            .enqueue(object : Callback<Profile> {
                override fun onResponse(call: Call<Profile>, response: Response<Profile>) {
                    if (response.isSuccessful) {
                        //??????????????? ????????? locationId??? ?????????
                        profile = response.body()!!

                        if(profile.locationId == 0){
                            profile.locationId = 1
                        }
                        else{}
                        Log.d("?????????????????????", profile.locationId.toString())
                        ServerUtil.retrofitService.requestLocation(accessToken, profile.locationId)
                            .enqueue(object : Callback<MyLocation> {
                                override fun onResponse(call: Call<MyLocation>, response: Response<MyLocation>) {
                                    if (response.isSuccessful) {
                                        //????????? ??????????????? ?????????
                                        location  = response.body()!!
                                        Log.d("location ?????? ???????????? ??????", response.body().toString())

                                        my_place.setText(location!!.dong)
                                        user_distance.text = profile.searchDistance.toString() + "km"
                                        if(profile.searchDistance == 3){
                                            place_seekBar.setProgress(0)
                                        }
                                        else if(profile.searchDistance == 6){
                                            place_seekBar.setProgress(1)
                                        }
                                        else{
                                            place_seekBar.setProgress(2)
                                        }

                                        Log.d(TAG, "?????? ???????????? ?????? ??????")
                                    }
                                }
                                override fun onFailure(call: Call<MyLocation>, t: Throwable) {
                                    Log.d(TAG, "?????? ???????????? ?????? ??????")
                                    Toast.makeText(this@MapActivity, "?????? ???????????? ?????? ??????", Toast.LENGTH_LONG).show()
                                }
                            })
                    }
                }
                override fun onFailure(call: Call<Profile>, t: Throwable) {
                    Log.d(TAG, "?????? ??????Id?????? ??????")
                    Toast.makeText(this@MapActivity, "?????? ??????Id?????? ??????", Toast.LENGTH_LONG)
                        .show()
                }
            })

    }
    fun modifyLocation(accessToken : String,new_locationId:Int){
        Log.d("??????????????????",new_locationId.toString())
        ServerUtil.retrofitService.requestLocationmodify(accessToken,new_locationId)
            .enqueue(object : Callback<Profile> {
                override fun onResponse(call: Call<Profile>, response: Response<Profile>) {
                    if (response.isSuccessful) {
                        Log.d(TAG, "?????????????????? ??????")
                        Mylocation(accessToken)
                    }

                }
                override fun onFailure(call: Call<Profile>, t: Throwable) {
                    Log.d(TAG, "?????????????????? ??????")
                    Toast.makeText(this@MapActivity, "?????????????????? ??????", Toast.LENGTH_LONG).show()
                }
            })
    }

    fun findMyGPS(){
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        var userLocation: Location = getLatLng()
        if(userLocation != null){
            //??????, ????????? ?????????
            latitudeY = userLocation.latitude
            longitudeX = userLocation.longitude
            Log.d("CheckCurrentLocation", "?????? ??? ?????? ???: ${latitudeY}, ${longitudeX}")

            var mGeoCoder =  Geocoder(applicationContext, Locale.KOREAN)
            var mResultList: List<Address>? = null
            try{
                mResultList = mGeoCoder.getFromLocation(
                    latitudeY!!, longitudeX!!, 1
                )
            }catch(e: IOException){
                e.printStackTrace()
            }
            if(mResultList != null){
                Log.d("CheckCurrentLocation", mResultList[0].getAddressLine(0))
            }

        }
        findMyKakaoCode(longitudeX!!, latitudeY!!)
    }
    fun findMyKakaoCode(longitudeX : Double,latitudeY : Double){
        var kakaoToken = "KakaoAK 9a9a02eedbe752442e4a7550ec217f88"

        ServerUtil.kakaoService.requestCode(kakaoToken, longitudeX,latitudeY)
            .enqueue(object : Callback<MapCode> {
                override fun onResponse(call: Call<MapCode>, response: Response<MapCode>) {
                    if (response.isSuccessful) {
                        //GPS??? ?????? ??????/????????? ????????? ?????????


                        if(response.body()?.documents?.size == 2){
                            var code_one = response.body()?.documents?.get(1)?.code
                            findLocationId(code_one.toString())
                            Log.d( "?????? ??????CODE",code_one.toString())
                        }
                        else{
                            var code_zero = response.body()?.documents?.get(0)?.code
                            findLocationId(code_zero.toString())
                            Log.d( "?????? ??????CODE",code_zero.toString())
                        }
                    }
                }
                override fun onFailure(call: Call<MapCode>, t: Throwable) {
                    Log.d(TAG, "?????? ??????code ?????? ??????")
                    Toast.makeText(this@MapActivity, "?????? ????????????code ?????? ??????", Toast.LENGTH_LONG).show()
                }
            })
    }
    fun findLocationId(code : String){
        var pref = getSharedPreferences("login_sp", MODE_PRIVATE)
        var accessToken: String = "Bearer " + pref.getString("access_token", "").toString()
        val paramObject = JsonObject()
        paramObject.addProperty("code",code)

        ServerUtil.retrofitService.requestLocationId(accessToken,paramObject.get("code").asString)
            .enqueue(object : Callback<MyLocation> {
                override fun onResponse(call: Call<MyLocation>, response: Response<MyLocation>) {
                    if (response.isSuccessful) {
                        //?????????/????????? ????????? ?????? ????????? ????????????Id??? ?????????
                        val myLocation  = response.body()!!
                        val locationId = myLocation.id

                        modifyLocation(accessToken,locationId)

                    }
                }
                override fun onFailure(call: Call<MyLocation>, t: Throwable) {
                    Log.d(TAG, "?????? ????????????ID ?????? ??????")
                    Toast.makeText(this@MapActivity, "?????? ????????????ID ?????? ??????", Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun getLatLng(): Location{

        var hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION)
        var hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_COARSE_LOCATION)

        if(hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
            hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED){
            val locatioNProvider = LocationManager.GPS_PROVIDER
            currentLatLng = locationManager?.getLastKnownLocation(locatioNProvider)!!
        }else{
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
            currentLatLng = getLatLng()
        }
        return currentLatLng!!
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if(requestCode == PERMISSIONS_REQUEST_CODE && grantResults.size == REQUIRED_PERMISSIONS.size){
            var check_result = true
            for(result in grantResults){
                if(result != PackageManager.PERMISSION_GRANTED){
                    check_result = false;
                    break;
                }
            }
            if(check_result){
            }else{
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                    || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])){
                    Toast.makeText(this, "?????? ????????? ?????????????????????.\n?????? ?????????????????? ?????? ??????????????????.", Toast.LENGTH_SHORT).show()
                    finish()
                }else{
                    Toast.makeText(this, "?????? ????????? ?????????????????????.\n???????????? ????????? ???????????? ?????????..", Toast.LENGTH_SHORT).show()
                }
            }
            }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}