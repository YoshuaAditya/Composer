package com.example.composer

import android.app.Service
import android.content.*
import android.content.res.Configuration
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.composer.data.Chat
import com.example.composer.retrofit.TcpServerService
import com.example.composer.ui.theme.ComposerTheme
import com.example.composer.ui.theme.LightBlue
import com.example.composer.ui.theme.Red
import com.example.composer.ui.theme.Teal200
import com.example.composer.views.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.Socket
import kotlin.random.Random


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    //weird error? java.lang.IllegalArgumentException: CreationExtras must have a value by `SAVED_STATE_REGISTRY_OWNER_KEY`
    //https://stackoverflow.com/questions/73302605/creationextras-must-have-a-value-by-saved-state-registry-owner-key
    val mainViewModel: MainViewModel by viewModels()
    lateinit var nsdManager: NsdManager
    val TAG = "tag"
    var mServiceName = "NsdChat"
    val SERVICE_TYPE = "_nsdchat._tcp."
    var boundService: TcpServerService? = null
    private var serviceIntent: Intent? = null
    var host: InetAddress? = null
    val receiver = ReceiveMessage()

    inner class ReceiveMessage : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val extra = intent.extras
            extra?.let {
                val chat =
                    Chat(host.toString(), it.getString("body").toString())
                mainViewModel.insert(chat)
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager
        setContent {
            NavigationHost(this)
        }
    }

    fun doBindingService() {
        val intent = Intent(this, TcpServerService::class.java)
        serviceIntent = intent
        bindService(
            intent, object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder?) {
                    val binder = service as TcpServerService.LocalBinder
                    boundService = binder.service

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                    boundService?.let {
                        it.startMeForeground()
                        println("Host listening on port:${it.port}")
                        registerService(it.port)
                    }
                    unbindService(this)
                    registerReceiver(receiver, IntentFilter("TCPMessage"))
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        boundService?.stopForeground(Service.STOP_FOREGROUND_REMOVE)
                    } else {
                        boundService?.stopForeground(true)
                    }
                    boundService = null
                }
            },
            Context.BIND_AUTO_CREATE
        )
    }

    fun registerService(port: Int) {
        // Create the NsdServiceInfo object, and populate it.
        val serviceInfo = NsdServiceInfo().apply {
            // The name is subject to change based on conflicts
            // with other services advertised on the same network.
            serviceName = "NsdChat"
            serviceType = "_nsdchat._tcp"
            setPort(port)
        }
        try {
            nsdManager.apply {
                registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
            }
        } catch (e: java.lang.IllegalArgumentException) {
            println(e)
        }

    }

    private val registrationListener = object : NsdManager.RegistrationListener {

        override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
            // Save the service name. Android may have changed it in order to
            // resolve a conflict, so update the name you initially requested
            // with the name Android actually used.
            mServiceName = NsdServiceInfo.serviceName
            Log.d(TAG, "Service Registered $mServiceName")
            try {
                nsdManager.discoverServices(
                    SERVICE_TYPE,
                    NsdManager.PROTOCOL_DNS_SD,
                    discoveryListener
                )
            } catch (e: java.lang.IllegalArgumentException) {
                println(e)
            }

        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Registration failed! Put debugging code here to determine why.
            Log.d(TAG, "Register Failed")
        }

        override fun onServiceUnregistered(arg0: NsdServiceInfo) {
            // Service has been unregistered. This only happens when you call
            // NsdManager.unregisterService() and pass in this listener.
            Log.d(TAG, "Unregistered")
        }

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Unregistration failed. Put debugging code here to determine why.
            Log.d(TAG, "Unregistered Failed")
        }
    }
    val discoveryListener = object : NsdManager.DiscoveryListener {

        // Called as soon as service discovery begins.
        override fun onDiscoveryStarted(regType: String) {
            Log.d(TAG, "Service discovery started")
        }

        //heads up, seems like emulator doesnt able to connect with nsd, need real devices,
        //make sure router supports multicast https://stackoverflow.com/questions/36797008/android-nsd-onservicefound-not-getting-called
        //also, i think the way its intended is to run both registration+discovery listener,
        //this way, service name will be different, ex: there will be NsdChat and NsdChat (2)
        //discovering same machine/servicename will ignore it, while different servicenames will connect
        override fun onServiceFound(service: NsdServiceInfo) {
            // A service was found! Do something with it.
            Log.d(TAG, "Service discovery success $service")
            when {
                //for some reason service.serviceType adds . to the string
                service.serviceType != SERVICE_TYPE ->
                    Log.d(TAG, "Unknown Service Type: ${service.serviceType}")
                service.serviceName == mServiceName -> // The name of the service tells the user what they'd be
                    // connecting to. It could be "Bob's Chat App".
                    Log.d(TAG, "Same machine: $mServiceName")
//                    nsdManager.resolveService(
//                        service,
//                        resolveListener
//                    )
                service.serviceName.contains("NsdChat") -> nsdManager.resolveService(
                    service,
                    resolveListener
                )
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
            Log.e(TAG, "service lost: $service")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.i(TAG, "Discovery stopped: $serviceType")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery failed: Error code:$errorCode")
            nsdManager.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery failed: Error code:$errorCode")
            nsdManager.stopServiceDiscovery(this)
        }
    }
    private val resolveListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Called when the resolve fails. Use the error code to debug.
            Log.e(TAG, "Resolve failed: $errorCode")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Log.e(TAG, "Resolve Succeeded. $serviceInfo")

            if (serviceInfo.serviceName == mServiceName) {
                Log.d(TAG, "Same IP.")
//                return
            }
            val mService = serviceInfo
            val port: Int = serviceInfo.port
            host = serviceInfo.host

            //connect to serversocket based on host/port information acquired from NSD discover
            Toast.makeText(this@MainActivity,"$host connected",Toast.LENGTH_SHORT).show()
            val socket = Socket(host, port)

            //outgoing stream redirect to socket
            mainViewModel.outputStream = DataOutputStream(socket.getOutputStream())

//            out.writeUTF("This is the first type of message.\n")
//            out.flush()
//            out.writeUTF("This is the second type of message.")
//            out.writeUTF("This is the third type of message (Part 1).")
//            out.writeUTF("This is the third type of message (Part 2).")
//            out.flush() // Send off the data
//            out.close()
//
//            //Close connection
//            socket.close()
        }
    }

    override fun onPause() {
        tearDown()
        serviceIntent?.let {
            stopService(it)
        }
        super.onPause()
    }

    fun tearDown() {
        nsdManager.apply {
            try {
                unregisterService(registrationListener)
                stopServiceDiscovery(discoveryListener)
            } catch (e: java.lang.IllegalArgumentException) {
                println(e)
            }
        }
        try {
            unregisterReceiver(receiver)
        } catch (e: java.lang.IllegalArgumentException) {
            println(e)
        }
    }

    val requestLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            //not sure how to tell navcontroller to navigate from here
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && shouldShowRequestPermissionRationale(android.Manifest.permission.WRITE_CALENDAR)
        ) {
            AlertDialogView.buildAlert(this)
        } else {
            AlertDialogView.buildAlert(this)
        }
    }
}

@Composable
fun NavigationHost(mainActivity: MainActivity) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "main") {
        composable("main") { MainActivityContent(mainActivity, navController) }
        composable("dialog") { PopUpDialog.DialogBox(mainActivity.mainViewModel) { navController.popBackStack() } }
        composable("javascript") { WebViewJavascript.IndexHTML() }
        composable("settings") { SettingsView.SettingsContent() }
    }
}

@Composable
fun MainActivityContent(
    mainActivity: MainActivity,
    navController: NavController
) {
    ComposerTheme {
        Surface() {
            Box(modifier = Modifier.fillMaxSize()) {
                ConstraintLayout(modifier = Modifier.fillMaxSize()) {
                    //vals
                    val mainViewModel = mainActivity.mainViewModel
                    val chatsState = mainViewModel.chats.observeAsState()
                    val isLoadingState = mainViewModel.isLoadingChats.observeAsState()
                    val coroutineScope = rememberCoroutineScope()
                    val lazyListState = rememberLazyListState()

                    //chats
                    chatsState.value?.let {
                        Conversation(it, lazyListState)
                    }
                    //refs for constrain layout
                    val button = createRef()
                    val loadingBar = createRef()
                    val deleteButton = createRef()
                    val searchButton = createRef()
                    val settingsButton = createRef()
                    val hostButton = createRef()
                    val clientButton = createRef()
                    //loading bar, if it finished doing retrofit then scroll
                    isLoadingState.value?.let { isLoading ->
                        if (isLoading) {
                            CircularProgressIndicator(Modifier.constrainAs(loadingBar) {
                                top.linkTo(parent.top)
                                start.linkTo(parent.start)
                                bottom.linkTo(parent.bottom)
                                end.linkTo(parent.end)
                            })
                        } else {
                            LaunchedEffect(true) {
                                coroutineScope.launch {
//                                lazyListState.animateScrollBy(100f)//this one more smooth
                                    chatsState.value?.let {//scroll to last item
                                        lazyListState.animateScrollToItem(it.size)
                                    }
                                }
                            }
                        }
                    }
                    //random chat ExtendedFloatingActionButton
                    StatefulObject(Modifier
                        .constrainAs(button) {
                            bottom.linkTo(deleteButton.top)
                            end.linkTo(parent.end)
                        }
                        .padding(all = 8.dp), "Random Chat", Teal200, Icons.Filled.Add) {
                        val randomId = Random.nextInt(0, 502)
                            .toString()//the API max id is 500, if you get 501 it will show error chat instead
                        mainViewModel.isLoadingChats.value = true
                        mainViewModel.getComment(randomId)
                    }
                    //delete chat
                    StatefulObject(Modifier
                        .constrainAs(deleteButton) {
                            bottom.linkTo(searchButton.top)
                            end.linkTo(parent.end)
                        }
                        .padding(all = 8.dp), "Delete Chat", Red, Icons.Filled.Delete) {
                        mainViewModel.deleteComment()
                    }
                    //Get specific id comment
                    StatefulObject(Modifier
                        .constrainAs(searchButton) {
                            bottom.linkTo(parent.bottom)
                            end.linkTo(parent.end)
                        }
                        .padding(all = 8.dp), "Create Chat", Color.Blue, Icons.Filled.Create) {
                        navController.navigate("dialog")
//                        if (ContextCompat.checkSelfPermission(
//                                mainActivity,
//                                android.Manifest.permission.WRITE_CALENDAR
//                            )
//                            == PackageManager.PERMISSION_GRANTED
//                        ) {
//                            CalendarPrompt.pushAppointmentsToCalender(
//                                mainActivity,
//                                "Title",
//                                "Information",
//                                "Location",
//                                1,
//                                System.currentTimeMillis(),
//                                needReminder = true,
//                                needMailService = true
//                            )
//                        } else {
//                            mainActivity.requestLauncher.launch(android.Manifest.permission.WRITE_CALENDAR)
//                        }
                    }
                    //settings button
                    StatefulObject(
                        Modifier
                            .constrainAs(settingsButton) {
                                top.linkTo(parent.top)
                                end.linkTo(parent.end)
                            }
                            .padding(all = 8.dp),
                        text = "",
                        color = LightBlue,
                        icon = Icons.Filled.Settings
                    ) {
                        navController.navigate("settings")
                    }
                    //Connect button
                    StatefulObject(
                        Modifier
                            .constrainAs(hostButton) {
                                bottom.linkTo(parent.bottom)
                                end.linkTo(searchButton.start)
                            }
                            .padding(all = 8.dp),
                        text = "Connect",
                        color = Color.DarkGray,
                        icon = Icons.Filled.Home
                    ) {
                        mainActivity.doBindingService()
                    }
                }
            }
        }
    }
}

@Composable
fun StatefulObject(
    modifier: Modifier,
    text: String,
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    StatelessObject(modifier, text, color, icon) {
        onClick()
    }
}

@Composable
fun StatelessObject(
    modifier: Modifier,
    text: String,
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ExtendedFloatingActionButton(
        modifier = modifier,
        text = { Text(text = text) },
        onClick = { onClick() },
        backgroundColor = color,
        contentColor = Color.White,
        icon = { Icon(icon, "") }
    )
}

@OptIn(ExperimentalGlideComposeApi::class)
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
            if (msg.body.startsWith("http://") || msg.body.startsWith("https://")) {
                GlideImage(
                    model = msg.body,
                    modifier = Modifier.padding(all = 4.dp),
                    contentDescription = ""
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


