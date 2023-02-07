package com.example.composer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.*
import com.example.composer.data.Chat
import com.example.composer.data.ChatRepository
import com.example.composer.retrofit.ApiInterface
import com.example.composer.retrofit.Comment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.Socket
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val apiInterface: ApiInterface
) : ViewModel() {
    var chats: LiveData<List<Chat>> = repository.getChats().asLiveData()
    var isLoadingChats: MutableLiveData<Boolean> = MutableLiveData(false)
    var outputStream: DataOutputStream? = null
    var application: MainApplication? = null

    lateinit var nsdManager: NsdManager
    val TAG = "tag"
    var mServiceName = "NsdChat"
    val SERVICE_TYPE = "_nsdchat._tcp."
    var serviceIntent: Intent? = null
    var host: InetAddress? = null
    var clientSocket: Socket? = null
    var port:Int=0
    val receiver = ReceiveMessage()
    val receiverPort = ReceivePort()
    var isServiceStarted = false

    inner class ReceiveMessage : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val extra = intent.extras
            extra?.let {
                val chat =
                    Chat(host.toString(), it.getString("body").toString())
                insert(chat)
            }

        }
    }
    inner class ReceivePort : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val extra = intent.extras
            extra?.let {
                if(port == 0){
                    port = it.getInt("port")
                    println("Host listening on port:$port")
                    registerService(port)
                }
            }
        }
    }


    fun insert(chat: Chat) = viewModelScope.launch {
        repository.insert(chat)
    }

    fun sendChat(chat: Chat) = viewModelScope.launch(Dispatchers.IO) {
        outputStream?.let {
            it.writeUTF(chat.body+"\n")
            it.flush()
            Log.i("tag", "Send chat: $host $port")
            val yourChat=Chat("You",chat.body)
            repository.insert(yourChat)
        }
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
    val registrationListener = object : NsdManager.RegistrationListener {

        override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
            // Save the service name. Android may have changed it in order to
            // resolve a conflict, so update the name you initially requested
            // with the name Android actually used.
            mServiceName = NsdServiceInfo.serviceName
            Log.d(TAG, "Service Registered $mServiceName")
            nsdManager.discoverServices(
                SERVICE_TYPE,
                NsdManager.PROTOCOL_DNS_SD,
                discoveryListener
            )
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
//                service.serviceName == mServiceName -> // The name of the service tells the user what they'd be
//                    // connecting to. It could be "Bob's Chat App".
//                    Log.d(TAG, "Same machine: $mServiceName")
//                    nsdManager.resolveService(
//                        service,
//                        resolveListener
//                    )
                service.serviceName.contains("NsdChat") -> startResolveService(service)
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

    //to prevent resolver already used, make a new resolver each time discover service success
    private fun startResolveService(serviceInfo: NsdServiceInfo) {
        val resolveListener: NsdManager.ResolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Resolve Failed: $serviceInfo\tError Code: $errorCode")
                when (errorCode) {
                    NsdManager.FAILURE_ALREADY_ACTIVE -> {
                        Log.e(TAG, "FAILURE_ALREADY_ACTIVE")
                        // Just try again...
//                        startResolveService(serviceInfo)
                    }
                    NsdManager.FAILURE_INTERNAL_ERROR -> Log.e(TAG, "FAILURE_INTERNAL_ERROR")
                    NsdManager.FAILURE_MAX_LIMIT -> Log.e(TAG, "FAILURE_MAX_LIMIT")
                }
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.e(TAG, "Resolve Succeeded. $serviceInfo")

                if (serviceInfo.serviceName == mServiceName) {
                    Log.d(TAG, "Same IP.")
                    return
                }
                val mService = serviceInfo
                val port: Int = serviceInfo.port
                host = serviceInfo.host

                //connect to serversocket based on host/port information acquired from NSD discover
                Toast.makeText(application,"$host connected", Toast.LENGTH_SHORT).show()
                clientSocket = Socket(host, port)
                //outgoing stream redirect to socket
                outputStream = DataOutputStream(clientSocket?.getOutputStream())
            }
        }
        nsdManager.resolveService(serviceInfo, resolveListener)
    }

    fun getComment(id: String) {
        apiInterface.getComment(id).enqueue(object : Callback<Comment> {
            override fun onResponse(call: Call<Comment>, response: Response<Comment>) {
                if (response.isSuccessful) {
                    val comment = response.body()!!
                    val chat = Chat(comment.email, comment.body)
                    insert(chat)
                } else {
                    val chat = Chat("Error!", "ID not found")
                    insert(chat)
                    println(response.errorBody()?.string())
                }
                isLoadingChats.value = false
            }

            override fun onFailure(call: Call<Comment>, t: Throwable) {
                print(t.message)
                isLoadingChats.value = false
            }
        })
    }

    fun deleteComment(id: Int) = viewModelScope.launch {
        repository.delete(id)
    }
}