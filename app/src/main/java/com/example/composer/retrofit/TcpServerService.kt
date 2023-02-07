package com.example.composer.retrofit

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.example.composer.R
import dagger.hilt.android.AndroidEntryPoint
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicBoolean

@AndroidEntryPoint
class TcpServerService : LifecycleService() {
    private var serverSocket = ServerSocket(0)
    var port: Int = serverSocket.localPort
    private val working = AtomicBoolean(true)

    //maybe global scope leaks?
    private val runnable = Runnable {
        try {
            println("while loop")

            val socket = serverSocket.accept()
            println("New client: $socket")
            val dataInputStream = BufferedReader(socket.getInputStream().reader())
            val dataOutputStream = DataOutputStream(socket.getOutputStream())

            while (working.get()) {
                if (dataInputStream.ready()) {
                    //use lambda function implements closable interface to automatically close the stream
                    val body = dataInputStream.readLine()
                    Log.i(TAG, "Received: $body")
                    Log.i(TAG, "Received: $socket")
                    val intent = Intent("TCPMessage")
                    intent.putExtra("body", body)
                    sendBroadcast(intent)
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

    override fun onCreate() {
        val intentPort = Intent("ServerPort")
        intentPort.putExtra("port", port)
        sendBroadcast(intentPort)
        startMeForeground()
        super.onCreate()
        Thread(runnable).start()
    }


    override fun onDestroy() {
        working.set(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(Service.STOP_FOREGROUND_REMOVE)
        } else {
            stopSelf()
        }
        serverSocket.close()
        super.onDestroy()
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