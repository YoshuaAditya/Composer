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

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.composer.worker.ChatDatabaseWorker
import com.example.composer.worker.ChatDatabaseWorker.Companion.KEY_FILENAME
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chats")
data class Chat constructor(val author: String, val body: String, @PrimaryKey(autoGenerate = true) val id: Int = 0)

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats")
    fun getChats(): Flow<List<Chat>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: Chat)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chats: List<Chat>)

    @get:Query("select * from chats where id = 0")
    val chatLiveData: LiveData<Chat?>

    @Query("DELETE FROM chats WHERE author = :author")
    suspend fun deleteChat(author: String)
}

@Database(entities = [Chat::class], version = 1, exportSchema = false)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    companion object {

        // For Singleton instantiation
        @Volatile private var instance: ChatDatabase? = null

        fun getInstance(context: Context): ChatDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): ChatDatabase {
            return Room.databaseBuilder(context, ChatDatabase::class.java, DATABASE_NAME)
                .addCallback(
                    object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            val request = OneTimeWorkRequestBuilder<ChatDatabaseWorker>()
                                .setInputData(workDataOf(KEY_FILENAME to CHAT_DATA_FILENAME))
                                .build()
                            //Make line below into comment if you want to launch unit test
                            //WorkManager not initialized properly on unit test environment
                            WorkManager.getInstance(context).enqueue(request)
                        }
                    }
                )
                .build()
        }
    }
}
