package com.example.composer.retrofit

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.random.Random

class Api {
    fun getRetrofitClient(): Retrofit {
        val baseURL= "https://jsonplaceholder.typicode.com/"
        val gson = GsonBuilder().setLenient().create()
        return Retrofit.Builder().baseUrl(baseURL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
}