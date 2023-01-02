/*
 * Copyright (C) 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.composer.data

import android.net.Network
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.example.composer.MainViewModel
import dagger.Module
import dagger.hilt.InstallIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(private val chatDao: ChatDao) {

    fun getChats() = chatDao.getChats()

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: ChatRepository? = null

        fun getInstance(chatDao: ChatDao) =
            instance ?: synchronized(this) {
                instance ?: ChatRepository(chatDao).also { instance = it }
            }
    }
    @WorkerThread
    suspend fun insert(chat: Chat) {
        chatDao.insertChat(chat)
    }
    @WorkerThread
    suspend fun delete(author:String) {
        chatDao.deleteChat(author)
    }
}
