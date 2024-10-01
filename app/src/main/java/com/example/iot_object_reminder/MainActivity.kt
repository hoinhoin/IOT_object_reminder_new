package com.example.iot_object_reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class MainActivity : AppCompatActivity() {

    private lateinit var webSocketManager: WebSocketManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ForegroundService 시작
        val serviceIntent = Intent(this, WebSocketForegroundService::class.java)
        startService(serviceIntent)

        // 웹소켓 매니저 초기화 및 연결 설정
        webSocketManager = WebSocketManager(this, webSocketListener)
        webSocketManager.initWebSocket("ws://192.168.4.1:8080")
    }

    private val webSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "웹소켓 연결 성공!", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d("WebSocket", "Received message: $text")
            showNotification("New WebSocket message", text) // 메시지 수신 시 알림 표시
            runOnUiThread {
                // CheckSignal.updateRFIDData(text)
            }
        }

        private fun showNotification(title: String, message: String) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "webSocketMessageChannel"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "WebSocket Message Channel",
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }

            // Use this@MainActivity to refer to the context of the outer class
            val notification = NotificationCompat.Builder(this@MainActivity, channelId)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_notification)
                .build()

            notificationManager.notify(1, notification)
        }



        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "웹소켓 오류: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(1000, null)
            runOnUiThread {
                Toast.makeText(this@MainActivity, "웹소켓 연결 종료", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // ForegroundService 종료
        val stopIntent = Intent(this, WebSocketForegroundService::class.java)
        stopService(stopIntent)

        // 액티비티 종료 시 웹소켓 연결 닫기
        webSocketManager.closeConnection()
    }
}
