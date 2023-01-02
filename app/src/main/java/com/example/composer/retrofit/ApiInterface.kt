package com.example.composer.retrofit

import com.google.gson.GsonBuilder
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiInterface {
    @GET("comments/{id}")
    fun getComment(@Path("id") id:String): Call<Comment>

    companion object{
        fun create(): ApiInterface{
            val baseURL = "https://jsonplaceholder.typicode.com/"
            val gson = GsonBuilder().setLenient().create()
            return Retrofit.Builder().baseUrl(baseURL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(ApiInterface::class.java)
        }
    }
}