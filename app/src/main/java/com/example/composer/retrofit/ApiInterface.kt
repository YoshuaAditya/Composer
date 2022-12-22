package com.example.composer.retrofit

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiInterface {
    @GET("comments/{id}")
    fun getComment(@Path("id") id:String): Call<Comment>
}