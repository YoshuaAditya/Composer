package com.example.composer

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.composer.data.Chat
import com.example.composer.data.ChatRepository
import com.example.composer.retrofit.Api
import com.example.composer.retrofit.Comment
import com.example.composer.retrofit.ApiInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainViewModel(private val repository: ChatRepository) : ViewModel() {
    var chats: LiveData<List<Chat>> = repository.getChats().asLiveData()
    fun insert(chat: Chat) = viewModelScope.launch {
        repository.insert(chat)
    }

    fun getComment(id: String, coroutineScope: CoroutineScope, lazyListState: LazyListState) {
        val client = Api().getRetrofitClient().create(ApiInterface::class.java)
        client.getComment(id).enqueue(object : Callback<Comment> {
            override fun onResponse(call: Call<Comment>, response: Response<Comment>) {
                if (response.isSuccessful) {
                    val comment = response.body()!!
                    val chat = Chat(comment.email, comment.body)
                    insert(chat)
                    coroutineScope.launch {
                        lazyListState.animateScrollBy(100f)//this one more smooth
//                                            listState.animateScrollToItem(chats.size)
                    }
                } else println(response.message())
            }

            override fun onFailure(call: Call<Comment>, t: Throwable) {
                print(t.message)
            }
        })
    }
}