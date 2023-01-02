package com.example.composer.module

import android.content.Context
import com.example.composer.data.*
import com.example.composer.retrofit.ApiInterface
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module //for abstract/interface
@InstallIn(SingletonComponent::class)
class ViewModelModule {

    @Provides
    @Singleton // still need to use volatile companion to make worker do its job
    fun provideChatDatabase(@ApplicationContext context: Context) = ChatDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideApiInterface():ApiInterface{
        return ApiInterface.create()
    }

    @Provides
    fun provideChatDao(chatDatabase: ChatDatabase) = chatDatabase.chatDao()
}
