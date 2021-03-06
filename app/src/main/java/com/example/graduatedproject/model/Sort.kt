package com.example.graduatedproject.model

import com.google.gson.annotations.SerializedName

data class Sort(
    @SerializedName("sorted")
    var sorted: Boolean = true,

    @SerializedName("unsorted")
    var unsorted: Boolean = true,

//값이 비 었는지 여부
    @SerializedName("empty")
    var empty: Boolean = true
)