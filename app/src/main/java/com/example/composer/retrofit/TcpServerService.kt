package com.example.composer.retrofit

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.composer.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicBoolean

@AndroidEntryPoint
class TcpServerService : Service() {
    private var serverSocket = ServerSocket(0)
    var port: Int = serverSocket.localPort
    private val working = AtomicBoolean(true)
    private val runnable = GlobalScope.launch(Dispatchers.IO) {
        try {
            while (working.get()) {
                println("while loop")
                val socket = serverSocket.accept()
                println("New client: $socket")
                val dataInputStream = BufferedReader(socket.getInputStream().reader())
                val dataOutputStream = DataOutputStream(socket.getOutputStream())

                while(true){
                    if (dataInputStream.ready()) {
                        //use lambda function implements closable interface to automatically close the stream
                        val body=dataInputStream.readLine()
                        Log.i(TAG, "Received: $body")
                        val intent = Intent("TCPMessage")
                        intent.putExtra("body", body)
                        sendBroadcast(intent)
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            try {
                serverSocket.close()
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
        }
    }

    //this method leaks, find how next
    //https://stackoverflow.com/questions/5072469/android-binder-leaks
    inner class LocalBinder : Binder() {
        val service: TcpServerService
            get() = this@TcpServerService
    }

    // Create the instance on the service.
    private val binder = LocalBinder()

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        startMeForeground()
        runnable
    }

    override fun onDestroy() {
        working.set(false)
        runnable.cancel()
        serverSocket.close()
    }

    //5 second time limit to start a service? it gives ANR when failed to start at that time
    //https://stackoverflow.com/questions/44425584/context-startforegroundservice-did-not-then-call-service-startforeground
    fun startMeForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val NOTIFICATION_CHANNEL_ID = packageName
            val channelName = "Tcp Server Background Service"
            val chan = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_NONE
            )
            chan.lightColor = Color.BLUE
            chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val manager = (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            manager.createNotificationChannel(chan)
            val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            val notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Tcp Server is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build()
            startForeground(2, notification)
        } else {
            startForeground(1, Notification())
        }
    }

    companion object {
        private val TAG = TcpServerService::class.java.simpleName
    }
}