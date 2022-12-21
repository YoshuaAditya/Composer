package com.example.composer

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.composer.data.Chat
import com.example.composer.data.ChatRepository
import kotlinx.coroutines.launch

class MainViewModel (private val repository: ChatRepository) : ViewModel(){
    var chats: LiveData<List<Chat>> = repository.getChats().asLiveData()
    fun insert(chat: Chat) = viewModelScope.launch {
        repository.insert(chat)
    }
}