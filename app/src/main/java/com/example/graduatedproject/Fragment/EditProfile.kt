package com.example.graduatedproject.Fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.view.View.inflate
import android.widget.*
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.graduatedproject.R
import com.example.graduatedproject.Util.ServerUtil
import kotlinx.android.synthetic.main.fragment_edit_profile.*
import kotlinx.android.synthetic.main.fragment_edit_profile.view.*
import okhttp3.*
import org.jetbrains.anko.find
import java.io.File

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [EditProfile.newInstance] factory method to
 * create an instance of this fragment.
 */

class EditProfile() : DialogFragment() {
    val TAG = EditProfile::class.java.simpleName
    var PICK_IMAGE_FROM_ALBUM  = 0
    lateinit var new_imageUrl : Uri //맨처음! 넣어지는 이미지 경로
    lateinit var imageFile : File // 절대경로로 변환되어 파일형태의 이미지
    //DialogFragment를 호출한 부모 Fragment에 결과를 반환
    lateinit var imageUrl : String
    var deleteImage : Boolean = false
    lateinit var nickname : String


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view : View = inflater.inflate(R.layout.fragment_edit_profile, container, false)
        val args : Bundle? = getArguments()
        nickname = args?.getString("nickname").toString()
        imageUrl = args?.getString("imageUrl").toString()



        //cancel 버튼 누르면 다시 MYPAGE로 돌아가도록 함
        view.edit_profile_cancel.setOnClickListener {
            dismiss()
        }

        //apply 버튼 누르면 받아온 imageUrl과 nickname 서버로 보내기
        // 서버 통신
        // 다시 MYPAGE로 돌아가도록 함
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var edit_profile_img : ImageView = view.findViewById(R.id.edit_profile_img)
        var edit_profile_name : EditText = view.findViewById(R.id.edit_profile_name)
        var change_img : Button= view.findViewById(R.id.change_img)
        var change_default : Button= view.findViewById(R.id.change_default)
        var edit_profile_apply : Button = view.findViewById(R.id.edit_profile_apply)

        lateinit var requestImg: RequestBody
        lateinit var requestdelete : RequestBody
        lateinit var reqestnickname : RequestBody
        lateinit var Bmp :  MultipartBody.Part


        //그 사용자한테 저장된 이미지, 닉네임 불러옴
        // 변경전 사진 화면에 붙이기
        Glide.with(requireActivity())
            .load(imageUrl.toUri())
            .centerCrop()
            .error(R.drawable.profile_init)
            .into(edit_profile_img)
        // 변경전 닉네임 화면에 붙이기
        edit_profile_name.setText(nickname)

        //사진 변경
        change_img.setOnClickListener {
            //갤러리에 접근하도록 함 -> 갤러리를 엶
            var intent : Intent = Intent()
            intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.setType("images/*")
            intent.setAction(Intent.ACTION_GET_CONTENT)
            startActivityForResult(intent, PICK_IMAGE_FROM_ALBUM)

            // RequestBody로 변환 후 MultipartBody.Part로 파일 컨버전
            requestImg= RequestBody.create(MediaType.parse("multipart/form-data"),imageFile)
            Bmp = MultipartBody.Part
                .createFormData("IMG_FILE", imageFile.getName(), requestImg)


            //원하는 사진 누르면 edit_profile_img에 갖다 붙임
            //원하는 사진의 url 받아 놓기
            Glide.with(requireActivity())
                .load(new_imageUrl)
                .centerCrop()
                .error(imageUrl)
                .into(edit_profile_img)
        }


        //기본이미지로 변경
        change_default.setOnClickListener {
            Glide.with(requireActivity())
                .load(R.drawable.profile_init)
                .centerCrop()
                .error(imageUrl)
                .into(edit_profile_img)

            deleteImage = false
        }

        //확인 버튼
        edit_profile_apply.setOnClickListener {
            //accessToken을 가져옴
            val pref = requireActivity().getSharedPreferences("login_sp", Context.MODE_PRIVATE)
            var accessToken: String = "Bearer " + pref.getString("access_token", "").toString()

            //create(MediaType.parse("multipart/form-data"),deleteImage)
            reqestnickname = RequestBody.create(MediaType.parse("multipart/form-data"),nickname)

            //변경된이름을 EditText로부터 가져옴
//            ServerUtil.retrofitService.requestModifyProfile(accessToken,Bmp,requestdelete,requestdelete)
//                .enqueue(object : Callback<Void> {
//                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
//                        if (response.isSuccessful) {
//                            Log.d(TAG, "프로필변경 성공")
//                            Toast.makeText(getActivity(), "프로필변경 되었습니다.", Toast.LENGTH_SHORT).show();
//                            dismiss()
//                        }
//                    }
//
//                    override fun onFailure(call: Call<Void>, t: Throwable) {
//                        Log.d(TAG, "프로필변경 실패")
//                    }
//                })

//            dismiss()
        }
    }


    //저장한 선택사진의 경로를 절대경로로 바꿈
    fun absolutelyPath(new_imageUrl: Uri): String {
        var proj: Array<String> = arrayOf(MediaStore.Images.Media.DATA)
        var c: Cursor = activity?.contentResolver!!
            .query(new_imageUrl, proj, null, null, null)!!
        var index = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        c.moveToFirst()

        var result = c.getString(index)

        return result
    }

    //선택한 이미지를 받음
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_IMAGE_FROM_ALBUM) {
            if(resultCode == Activity.RESULT_OK){
                //결과값이 사진을 선택했을때
                new_imageUrl = data?.data!!
                //절대경로변환 함수 호출
                var new_imageUrlPath : String = absolutelyPath(new_imageUrl)
                imageFile = File(new_imageUrlPath)
            }
            else{
                //취소했을때
                Toast.makeText(activity, "사진 선택 취소", Toast.LENGTH_LONG).show()
            }
        }
    }


    fun getInstance(): EditProfile {
        return EditProfile()
    }
}