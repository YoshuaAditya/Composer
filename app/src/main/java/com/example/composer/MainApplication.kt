package com.example.composer

import android.app.Application
import com.example.composer.data.ChatDatabase
import com.example.composer.data.ChatRepository

class MainApplication : Application() {
    val database by lazy { ChatDatabase.getInstance(this) }
    val repository by lazy { ChatRepository(database.chatDao()) }
}
