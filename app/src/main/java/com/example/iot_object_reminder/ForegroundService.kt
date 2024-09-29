package com.example.iot_object_reminder

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class WebSocketForegroundService : Service() {

    private lateinit var webSocketManager: WebSocketManager

    override fun onCreate() {
        super.onCreate()

        // 웹소켓 매니저 초기화 및 연결 설정
        webSocketManager = WebSocketManager(this, webSocketListener)
        webSocketManager.initWebSocket("ws://192.168.4.1:8080")

        // 포그라운드 서비스 시작
        startForegroundService()
    }

    @SuppressLint("ForegroundServiceType")
    private fun startForegroundService() {
        // 알림 채널 생성 (Android O 이상에서 필요)
        val channelId = "WebSocketServiceChannel"
        val channel = NotificationChannel(
            channelId, "WebSocket Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)

        // 알림 생성
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "WebSocketServiceChannel")
            .setContentTitle("WebSocket Service")
            .setContentText("ESP32 신호 수신 중...")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .build()

        // 포그라운드 서비스 시작
        startForeground(1, notification)
    }

    private val webSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            // WebSocket 연결 성공 시 처리
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            // WebSocket으로 받은 메시지 직접 처리
            when (text) {
                "0000" -> {
                    // '0000' 메시지를 수신했을 때의 로직
                    startDeviceCheck()  // 여기서 5초 타이머 시작
                }
                "1111", "2222" -> {
                    // '1111' 또는 '2222' 수신 시 로직
                    stopDeviceCheck()  // 타이머가 동작 중이면 멈춤
                }
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            // 오류 처리
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(1000, null)
        }
    }

    private var isCheckingForDevices = false
    private var checkTimer: Runnable? = null

    // '0000' 수신 후 5초간 '1111' 또는 '2222'가 수신되지 않으면 알림을 보냄
    private fun startDeviceCheck() {
        if (!isCheckingForDevices) {
            isCheckingForDevices = true
            checkTimer = Runnable {
                if (isCheckingForDevices) {
                    sendMissingDeviceNotification()
                    stopDeviceCheck()
                }
            }
            // 5초 후에 확인 (메인 스레드의 핸들러 사용)
            android.os.Handler(mainLooper).postDelayed(checkTimer!!, 5000)
        }
    }

    // '1111' 또는 '2222'가 수신되면 타이머 멈춤
    private fun stopDeviceCheck() {
        isCheckingForDevices = false
        checkTimer?.let {
            android.os.Handler(mainLooper).removeCallbacks(it)
        }
        checkTimer = null
    }

    private fun sendMissingDeviceNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "missingDeviceChannel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Missing Device Channel", NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Missing Device")
            .setContentText("5초 내에 연결되지 않은 기기가 있습니다.")
            .setSmallIcon(R.drawable.ic_notification)
            .build()

        notificationManager.notify(2, notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // 서비스 바인딩이 필요 없으면 null 반환
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocketManager.closeConnection()
    }
}
