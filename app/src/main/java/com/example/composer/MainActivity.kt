package com.example.composer

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.composer.data.Chat
import com.example.composer.ui.theme.ComposerTheme
import com.example.composer.ui.theme.Red
import com.example.composer.ui.theme.Teal200
import com.example.composer.views.PopUpDialog
import com.example.composer.views.WebViewJavascript
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.random.Random

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    //weird error? java.lang.IllegalArgumentException: CreationExtras must have a value by `SAVED_STATE_REGISTRY_OWNER_KEY`
    //https://stackoverflow.com/questions/73302605/creationextras-must-have-a-value-by-saved-state-registry-owner-key
    val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NavigationHost(mainViewModel, this)
        }
    }
}

@Composable
fun NavigationHost(mainViewModel: MainViewModel, mainActivity: MainActivity) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "main") {
        composable("main") { MainActivityContent(mainViewModel, mainActivity,navController ) }
        composable("dialog") { PopUpDialog.DialogBox (mainViewModel){navController.popBackStack()}}
        composable("javascript") { WebViewJavascript.IndexHTML ()}
    }
}

@Composable
fun MainActivityContent(mainViewModel: MainViewModel, mainActivity: MainActivity,navController: NavController) {
    ComposerTheme {
        Surface() {
            Box(modifier = Modifier.fillMaxSize()) {
                ConstraintLayout(modifier = Modifier.fillMaxSize()) {
                    //vals
                    val chatsState = mainViewModel.chats.observeAsState()
                    val isLoadingState = mainViewModel.isLoadingChats.observeAsState()
                    val coroutineScope = rememberCoroutineScope()
                    val lazyListState = rememberLazyListState()

                    //chats
                    chatsState.value?.let {
                        Conversation(it, lazyListState)
                    }
                    val button = createRef()
                    val loadingBar = createRef()
                    val deleteButton = createRef()
                    val searchButton = createRef()
                    val randomId = Random.nextInt(0, 400).toString()
                    //loading bar
                    if (isLoadingState.value!!) {
                        CircularProgressIndicator(Modifier.constrainAs(loadingBar) {
                            top.linkTo(parent.top)
                            start.linkTo(parent.start)
                            bottom.linkTo(parent.bottom)
                            end.linkTo(parent.end)
                        })
                    }
                    //new chat ExtendedFloatingActionButton
                    StatefulObject(Modifier
                        .constrainAs(button) {
                            bottom.linkTo(deleteButton.top)
                            end.linkTo(parent.end)
                        }
                        .padding(all = 8.dp)
                    ,"Random Chat", Teal200,Icons.Filled.Add) {
                        mainViewModel.isLoadingChats.value = true
                        mainViewModel.getComment(randomId)
                        //observe isLoadingChats, if it false(finished doing retrofit) then scroll
                        mainViewModel.isLoadingChats.observe(mainActivity) {
                            if (!it) {
                                coroutineScope.launch {
                                    lazyListState.animateScrollBy(100f)//this one more smooth
//                                            listState.animateScrollToItem(chats.size)
                                }
                            }
                        }
                    }
                    //delete chat
                    StatefulObject(Modifier
                        .constrainAs(deleteButton) {
                            bottom.linkTo(searchButton.top)
                            end.linkTo(parent.end)
                        }
                        .padding(all = 8.dp)
                        ,"Delete Chat", Red,Icons.Filled.Delete) {
                        mainViewModel.deleteComment()
                    }
                    //Get specific id comment
                    StatefulObject(Modifier
                        .constrainAs(searchButton) {
                            bottom.linkTo(parent.bottom)
                            end.linkTo(parent.end)
                        }
                        .padding(all = 8.dp)
                        ,"Create Chat", Color.Blue,Icons.Filled.Create) {
                        navController.navigate("dialog")
//                        navController.navigate("javascript")
                    }
                }
            }
        }
    }
}

@Composable
fun StatefulObject(modifier: Modifier,text:String, color: Color,icon:ImageVector, onClick: () -> Unit) {
    StatelessObject(modifier,text,color,icon) {
        onClick()
    }
}

@Composable
fun StatelessObject(modifier: Modifier,text:String, color: Color,icon: ImageVector, onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        modifier = modifier,
        text = { Text(text = text) },
        onClick = { onClick() },
        backgroundColor = color,
        contentColor = Color.White,
        icon = { Icon(icon, "") }
    )
}

@Composable
fun MessageCard(msg: Chat) {
    Row(modifier = Modifier.padding(all = 8.dp)) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colors.secondaryVariant, CircleShape)
        )
        //vertical space between image and text
        Spacer(modifier = Modifier.width(8.dp))

        // We keep track if the message is expanded or not
        var isExpanded by remember { mutableStateOf(false) }
        // surfaceColor will be updated gradually from one color to the other
        val surfaceColor by animateColorAsState(
            if (isExpanded) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
        )

        // We toggle the isExpanded variable when we click on this Column
        Column(modifier = Modifier.clickable { isExpanded = !isExpanded }) {
            Text(
                text = msg.author,
                color = MaterialTheme.colors.secondaryVariant,
                style = MaterialTheme.typography.subtitle2
            )

            //horizontal space between author and msgbody
            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = MaterialTheme.shapes.medium,
                elevation = 1.dp,
                // surfaceColor color will be changing gradually from primary to surface
                color = surfaceColor,
                // animateContentSize will change the Surface size gradually
                modifier = Modifier
                    .animateContentSize()
                    .padding(1.dp)
            ) {
                Text(
                    text = msg.body,
                    modifier = Modifier.padding(all = 4.dp),
                    // If the message is expanded, we display all its content
                    // otherwise we only display the first line
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    style = MaterialTheme.typography.body2
                )
            }
        }
    }
}

@Composable
fun Conversation(chats: List<Chat>, listState: LazyListState) {
    LazyColumn(state = listState) {
        items(chats) { chat ->
            MessageCard(chat)
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
fun PreviewMessageCard() {
    ComposerTheme(true) {
        Surface {
            MessageCard(
                msg = Chat("Colleague", "Take a look at Jetpack Compose, it's great!")
            )
        }
    }
}
