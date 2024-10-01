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
    private var received1111 = false
    private var received2222 = false
    private var isCheckingForDevices = false
    private var checkTimer: Runnable? = null

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
                    // '0000' 메시지를 수신했을 때의 로직 (5초 타이머 시작)
                    startDeviceCheck()
                }
                "1111" -> {
                    received1111 = true
                    checkIfComplete()
                }
                "2222" -> {
                    received2222 = true
                    checkIfComplete()
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

    // '0000' 수신 후 5초간 '1111' 또는 '2222'가 수신되지 않으면 알림을 보냄
    private fun startDeviceCheck() {
        if (!isCheckingForDevices) {
            isCheckingForDevices = true
            received1111 = false
            received2222 = false

            checkTimer = Runnable {
                if (isCheckingForDevices) {
                    // 5초 후에도 isCheckingForDevices가 true라면 각 경우에 맞는 알림을 보냄
                    when {
                        !received1111 && !received2222 -> {
                            sendNotification("물건을 모두 두고 왔습니다.")
                        }
                        !received1111 -> {
                            sendNotification("1111을 두고 왔습니다.")
                        }
                        !received2222 -> {
                            sendNotification("2222를 두고 왔습니다.")
                        }
                    }
                    stopDeviceCheck()  // 타이머 종료
                }
            }
            // 5초 후에 확인 (메인 스레드의 핸들러 사용)
            android.os.Handler(mainLooper).postDelayed(checkTimer!!, 5000)
        }
    }

    // 두 신호가 모두 수신되었는지 확인하고 타이머 중지
    private fun checkIfComplete() { //두 신호가 모두 들어왔을 때 타이머를 중지하여 불필요한 알림이 발생하지 않도록 함
        if (received1111 && received2222) {
            stopDeviceCheck()
        }
    }

    // 타이머를 멈추고 플래그 초기화
    private fun stopDeviceCheck() {
        isCheckingForDevices = false
        checkTimer?.let {
            android.os.Handler(mainLooper).removeCallbacks(it)
        }
        checkTimer = null
    }

    // 알림을 보내는 함수
    private fun sendNotification(message: String) {
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
            .setContentText(message)
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
