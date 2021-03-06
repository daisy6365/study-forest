package com.example.graduatedproject.Fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.graduatedproject.model.Profile
import com.example.graduatedproject.R
import com.example.graduatedproject.Util.ServerUtil
import com.example.graduatedproject.viewmodel.UserViewModel
import com.example.graduatedproject.databinding.FragmentEditProfileBinding
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.fragment_edit_profile.*
import kotlinx.android.synthetic.main.fragment_my_page.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.*


class EditProfile() : DialogFragment() {
    private lateinit var binding: FragmentEditProfileBinding
    private lateinit var user : UserViewModel
    //DialogFragment를 호출한 부모 Fragment에 결과를 반환
    var new_imageUrl : Uri? = null //맨처음! 넣어지는 이미지 경로
    var new_imageUrlPath: String? = null
    var imageFile : File? = null // 절대경로로 변환되어 파일형태의 이미지

    var deleteImage : Boolean = false
    lateinit var newNickname : String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        user = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)
        binding.viewModel = user

        user.users.observe(viewLifecycleOwner, Observer {
            if(it != null){
                binding.editProfileName.setText(it.nickName)
                binding.editProfileImg.clipToOutline = true
                edit_profile_img.clipToOutline = true
                if(it.image == null){
                    Glide.with(requireView().context)
                        .load(R.drawable.profile_init)
                        .into(edit_profile_img)
                }
                else{
                    MyPage.bindImage(binding.editProfileImg, it.image.profileImage)
                }
            }
        })

        return binding.root

    }

    private val getImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            //결과값이 사진을 선택했을때
            new_imageUrl = result.data?.data!!
            Glide.with(this)
                .load(new_imageUrl)
                .centerCrop()
                .into(binding.editProfileImg)
            //절대경로변환 함수 호출
            new_imageUrlPath= absolutelyPath(new_imageUrl!!)
            imageFile = File(new_imageUrlPath)
        } else {
            //취소했을때
            Toast.makeText(activity, "사진 선택 취소", Toast.LENGTH_LONG).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        lateinit var requestImg: RequestBody
        var imageBitmap : MultipartBody.Part? = null


//        //그 사용자한테 저장된 이미지, 닉네임 불러옴
//        // 변경전 사진 화면에 붙이기
//        if(imageUrl.equals("null")){
//            Glide.with(this)
//                .load("https://project-lambda-bucket.s3.ap-northeast-2.amazonaws.com/KakaoTalk_Image_2021-07-19-03-04-43.png")
//                .centerCrop()
//                .into(binding.editProfileImg)
//        }
//        else{
//            Glide.with(view)
//                .load(imageUrl.toUri())
//                .centerCrop()
//                .into(binding.editProfileImg)
//        }
//
//        // 변경전 닉네임 화면에 붙이기
//        binding.editProfileName.setText(nickname)


        binding.changeImg.setOnClickListener {
            openGalley()
        }
        //원하는 사진 누르면 edit_profile_img에 갖다 붙임
        //원하는 사진의 url 받아 놓기

        //기본이미지로 변경
        binding.changeDefault.setOnClickListener {
            Glide.with(this)
                .load(R.drawable.profile_init)
                .centerCrop()
                //.error(imageUrl)
                .into(binding.editProfileImg)
            deleteImage = true
        }

        //cancel 버튼 누르면 다시 MYPAGE로 돌아가도록 함
        binding.editProfileCancel.setOnClickListener {
            dismiss()
        }

        //확인 버튼
        binding.editProfileApply.setOnClickListener {
            //accessToken을 가져옴
            val pref = requireActivity().getSharedPreferences("login_sp", Context.MODE_PRIVATE)
            var accessToken: String = "Bearer " + pref.getString("access_token", "").toString()

            newNickname = binding.editProfileName.text.toString()

            // RequestBody로 변환 후 MultipartBody.Part로 파일 컨버전
            if(imageFile != null){
                requestImg= RequestBody.create(MediaType.parse("image/*"),imageFile)
                imageBitmap = MultipartBody.Part.createFormData("image", imageFile?.getName(), requestImg)

            }else{}

            //deleteImage여부, 새로운 닉네임
            val paramObject = JsonObject()
            paramObject.addProperty("deleteImage", deleteImage)
            paramObject.addProperty("nickName", newNickname)

            val request = RequestBody.create(MediaType.parse("application/json"),paramObject.toString())

//            requestdelete = RequestBody.create(MediaType.parse("deleteImage"),deleteImage.toString())
//            reqestnickname = RequestBody.create(MediaType.parse("nickName"),newNickname)



            //변경된이름을 EditText로부터 가져옴
            ServerUtil.retrofitService.requestModifyProfile(accessToken,imageBitmap,request)
                .enqueue(object : retrofit2.Callback<Profile> {
                    @SuppressLint("ResourceType")
                    override fun onResponse(call: retrofit2.Call<Profile>, response: retrofit2.Response<Profile>) {
                        if (response.isSuccessful) {
                            val profileInfo = response.body()

                            user.setData(profileInfo!!)

                            Log.d("EditProfile", "프로필변경 성공")
                            Toast.makeText(getActivity(), "프로필변경 되었습니다.", Toast.LENGTH_SHORT).show()
                            dismiss()
                        }
                    }
                    override fun onFailure(call: retrofit2.Call<Profile>, t: Throwable) {
                        Log.d("EditProfile", "프로필변경 실패")
                    }
                })
        }
    }

    private fun openGalley(){
        //갤러리에 접근하도록 함 -> 갤러리를 엶
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        getImage.launch(intent)
    }


    //저장한 선택사진의 경로를 절대경로로 바꿈
    fun absolutelyPath(new_imageUrl: Uri): String {
        val contentResolver = requireContext()!!.contentResolver?: return null.toString()

        val filePath = requireContext()!!.applicationInfo.dataDir + File.separator +
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



    fun getInstance(): EditProfile {
        return EditProfile()
    }
}


