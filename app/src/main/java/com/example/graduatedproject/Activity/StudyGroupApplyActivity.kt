package com.example.graduatedproject.Activity

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.graduatedproject.Fragment.GroupMember
import com.example.graduatedproject.model.Group
import com.example.graduatedproject.R
import com.example.graduatedproject.Util.ServerUtil
import kotlinx.android.synthetic.main.activity_study_group_apply.*
import net.daum.mf.map.api.MapPOIItem
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class StudyGroupApplyActivity : AppCompatActivity() {
    private val TAG = StudyGroupApplyActivity::class.java.simpleName
    val PERMISSIONS_REQUEST_CODE = 100
    var REQUIRED_PERMISSIONS = arrayOf<String>( Manifest.permission.ACCESS_FINE_LOCATION)
    var groupInfo = Group()
    var current_marker = MapPOIItem()
    var uLatitude : Double? = null
    var uLongitude : Double? = null
    lateinit var mapView : MapView
    lateinit var mapViewContainer : RelativeLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_study_group_apply)

        var pref = getSharedPreferences("login_sp", MODE_PRIVATE)
        var accessToken: String = "Bearer " + pref.getString("access_token", "").toString()
        val groupId = intent.getIntExtra("gatheringId",0)

        mapView = MapView(this)
        mapViewContainer = map_view
        mapViewContainer.addView(mapView)

        Log.d("gatheringId", groupId.toString())
        if (intent.hasExtra("gatheringId")) {
            val permissionCheck =
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                val lm: LocationManager =
                    getSystemService(Context.LOCATION_SERVICE) as LocationManager
                try {
                    ServerUtil.retrofitService.requestGroup(accessToken,groupId)
                        .enqueue(object : Callback<Group> {
                            override fun onResponse(call: Call<Group>, response: Response<Group>) {
                                if (response.isSuccessful) {
                                    groupInfo  = response.body()!!
                                    group_apply_on_off.setText(groupInfo!!.shape)
                                    current_people_number.setText(groupInfo.numberOfPeople.toString())

                                    val str = groupInfo.gatheringTime
                                    val idT: Int = str!!.indexOf("T")
                                    val date = str.substring(0,idT)
                                    val time = str.substring(idT+1)

                                    val dateList = date.split("-")
                                    val date_group = dateList[0] + "??? " + dateList[1] + "??? " + dateList[2] + "???"
                                    val timeList = time.split(":")
                                    val time_group = timeList[0] + "??? " + timeList[1] + "???"


                                    apply_group_date.setText(date_group)
                                    apply_group_time.setText(time_group)
                                    created_introduce_text.setText(groupInfo.content)
                                    if(groupInfo.place != null){
                                        location_detail_Info.setText(groupInfo.place!!.name)
                                        //?????? ????????????
                                        uLatitude = groupInfo.place!!.let
                                        uLongitude = groupInfo.place!!.len

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
                                    }
                                    else{
                                        location_detail_Info.visibility = View.GONE
                                        map_view.visibility = View.GONE
                                    }

                                    if(groupInfo.apply == true){
                                        //????????? ?????? -> "????????????" ?????? ?????????
                                        //"?????? ??????"?????? ?????????
                                        apply_group_btn.visibility = View.GONE
                                        apply_group_cancle_btn.visibility = View.VISIBLE
                                    }
                                    else if(groupInfo.apply == null){
                                        //?????? ????????? -> ???????????? ?????? ?????????
                                        apply_group_btn.visibility = View.GONE
                                        group_host_btn.visibility = View.VISIBLE
                                    }
                                    else{}//??????X,?????????????????? -> ???????????? ??????

                                    Log.d(TAG, "?????? ???????????? ?????? ??????")
                                }
                            }
                            override fun onFailure(call: Call<Group>, t: Throwable) {
                                Log.d(TAG, "?????? ???????????? ?????? ??????")
                                Toast.makeText(this@StudyGroupApplyActivity, "?????? ???????????? ?????? ??????", Toast.LENGTH_LONG).show()
                            }
                        })
                    current_people_number.setOnClickListener {
                        //?????? ????????? ?????? ??????
                        val dialog = GroupMember(groupId!!)
                        dialog.show(supportFragmentManager, "GroupMember")
                    }

                    apply_group_btn.setOnClickListener {
                        //?????? ??????
                        ServerUtil.retrofitService.requestGroupApply(accessToken,groupId)
                            .enqueue(object : Callback<Void> {
                                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                    if (response.isSuccessful) {
                                        Log.d(TAG, "???????????? ?????? ??????")
                                        Toast.makeText(this@StudyGroupApplyActivity, "?????? ??????????????? ????????????.", Toast.LENGTH_LONG).show()

                                        apply_group_btn.visibility = View.GONE
                                        apply_group_cancle_btn.visibility = View.VISIBLE
                                    }
                                }

                                override fun onFailure(call: Call<Void>, t: Throwable) {
                                    Log.d(TAG, "?????? ?????? ?????? ??????")
                                    Toast.makeText(this@StudyGroupApplyActivity, "???????????? ?????? ??????", Toast.LENGTH_LONG).show()
                                }
                            })
                    }

                    apply_group_cancle_btn.setOnClickListener {
                        //???????????? ??????
                        ServerUtil.retrofitService.requestGroupApplyCancel(accessToken,groupId)
                            .enqueue(object : Callback<Void> {
                                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                    if (response.isSuccessful) {
                                        Log.d(TAG, "?????? ???????????? ?????? ??????")
                                        Toast.makeText(this@StudyGroupApplyActivity, "?????? ??????????????? ??????????????????.", Toast.LENGTH_LONG).show()

                                        apply_group_cancle_btn.visibility = View.GONE
                                        apply_group_btn.visibility = View.VISIBLE
                                    }
                                }

                                override fun onFailure(call: Call<Void>, t: Throwable) {
                                    Log.d(TAG, "?????? ???????????? ?????? ??????")
                                    Toast.makeText(this@StudyGroupApplyActivity, "?????????????????? ?????? ??????", Toast.LENGTH_LONG).show()
                                }
                            })
                    }

                    group_modify_btn.setOnClickListener {
                        //Activity ??????
                        mapViewContainer.removeView(mapView)
                        val intent = Intent(this@StudyGroupApplyActivity, StudyGroupModifyActivity::class.java)
                        intent.putExtra("gatheringId",groupId)
                        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))

                        finish()
                    }


                    group_delete_btn.setOnClickListener {
                        //?????? ??????
                        var builder = AlertDialog.Builder(this)
                        builder.setTitle("??????")
                        builder.setMessage("?????? ????????? ?????????????????????????")
                        builder.setPositiveButton("??????", DialogInterface.OnClickListener { dialog, which ->
                            ServerUtil.retrofitService.requestDeleteGroup(accessToken,groupId)
                                .enqueue(object : Callback<Void> {
                                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                        if (response.isSuccessful) {
                                            Log.d(TAG, "?????? ?????? ??????")
                                            Toast.makeText(this@StudyGroupApplyActivity, "????????? ??????????????????.", Toast.LENGTH_LONG).show()

                                            mapViewContainer.removeView(mapView)
                                            finish()
                                        }
                                    }

                                    override fun onFailure(call: Call<Void>, t: Throwable) {
                                        Log.d(TAG, "?????? ?????? ??????")
                                        Toast.makeText(this@StudyGroupApplyActivity, "?????? ?????? ??????", Toast.LENGTH_LONG).show()
                                    }
                                })
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

    }
}
