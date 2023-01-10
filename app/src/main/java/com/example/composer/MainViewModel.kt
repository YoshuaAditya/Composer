package com.example.composer

import androidx.lifecycle.*
import com.example.composer.data.Chat
import com.example.composer.data.ChatRepository
import com.example.composer.retrofit.ApiInterface
import com.example.composer.retrofit.Comment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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
                    val chat = Chat("Error!", "Something went wrong")
                    insert(chat)
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
        chats.value?.let {
            if(it.isNotEmpty()) repository.delete(it[0].author)
        }
    }
}