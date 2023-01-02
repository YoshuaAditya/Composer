package com.example.composer

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.composer.data.Chat
import com.example.composer.data.ChatRepository
import com.example.composer.retrofit.Comment
import com.example.composer.retrofit.ApiInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.Flow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val repository: ChatRepository,private val apiInterface: ApiInterface) : ViewModel() {
    var chats: LiveData<List<Chat>> = repository.getChats().asLiveData()
    var isLoadingChats : MutableLiveData<Boolean> = MutableLiveData(false)
    fun insert(chat: Chat) = viewModelScope.launch {
        repository.insert(chat)
    }

    fun getComment(id: String){
        apiInterface.getComment(id).enqueue(object : Callback<Comment> {
            override fun onResponse(call: Call<Comment>, response: Response<Comment>) {
                if (response.isSuccessful) {
                    val comment = response.body()!!
                    val chat = Chat(comment.email, comment.body)
                    insert(chat)
                } else {
                    println(response.errorBody()?.string())
                }
                isLoadingChats.value=false
            }

            override fun onFailure(call: Call<Comment>, t: Throwable) {
                print(t.message)
                isLoadingChats.value=false
            }
        })
    }
    fun deleteComment() = viewModelScope.launch {
        if(chats.value?.size!! >0) chats.value?.get(0)?.let { repository.delete(it.author) }
    }
}