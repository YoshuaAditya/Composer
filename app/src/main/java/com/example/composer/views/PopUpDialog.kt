package com.example.composer.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.composer.MainViewModel
import com.example.composer.data.Chat

class PopUpDialog {
    companion object {//this one cant use same viewmodel because of navigation?
        @Composable
        fun DialogBox(mainViewModel: MainViewModel= hiltViewModel(), onDismiss: () -> Unit) {
            val author = remember { mutableStateOf(TextFieldValue()) }
            val body = remember { mutableStateOf(TextFieldValue()) }
            Dialog(
                onDismissRequest = {
                    onDismiss()
                }
            ) {
                Surface(
                    modifier = Modifier,
                    elevation = 4.dp
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
//                        TextField(
//                            value = author.value,
//                            onValueChange = { author.value = it },
//                            maxLines = 1,
//                            modifier = Modifier.semantics { contentDescription = "author" },
//                            placeholder = { Text(text = "Enter author...") },
//                        )
                        TextField(
                            value = body.value,
                            onValueChange = { body.value = it },
                            modifier = Modifier.semantics { contentDescription = "body" },
                            placeholder = { Text(text = "Enter body...") },
                        )
                        Button(
                            modifier = Modifier,
                            onClick = {
                                val chat = Chat("author", body.value.text)
                                if(body.value.text.isNotBlank())mainViewModel.sendChat(chat)
                                onDismiss()
                            }) {
                            Text(
                                text = "Submit",
                            )
                        }
                    }
                }
            }
        }
    }
}