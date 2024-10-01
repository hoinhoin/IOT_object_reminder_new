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
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class WebSocketForegroundService : Service() {

    private lateinit var webSocketManager: WebSocketManager
    private var received0000 = false
    private var received1111 = false
    private var received2222 = false
    private var isCheckingForDevices = false
    private var checkTimer: Runnable? = null

    private var belonging0:String = "물건0"
    private var belonging1:String = "물건1"
    private var belonging2:String = "물건2"

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
                "0000" -> { //0000신호 받으면
                    received0000 = true
                    startDeviceCheck() //1111,2222 신호 받았는지 확인
                }
                "1111" -> { //1111신호 받으면
                    received1111 = true
                    startDeviceCheck()
                }
                "2222" -> {
                    received2222 = true
                    startDeviceCheck()
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
            if (received0000){
                received1111 = false
                received2222 = false
            }
            else if (received1111){
                received0000 = false
                received2222 = false
            }
            else if (received2222){
                received0000 = false
                received1111 = false
            }

            checkTimer = Runnable {
                if (isCheckingForDevices) {
                    // 첫 번째 경우: 0000이 수신된 경우
                    if (received0000) {

                        if (!received1111 && !received2222) {
                            sendNotification(belonging1 + ", " + belonging2 + " 두고 왔습니다.")
                        }
                        else if (!received1111) {
                            // 1111가 수신되지 않았을 경우
                            sendNotification(belonging1 + " 두고 왔습니다.")
                        }
                        else if (!received2222) {
                            // 2222가 수신되지 않았을 경우
                            sendNotification(belonging2 + " 두고 왔습니다.")
                        }
                        stopDeviceCheck()
                        // 모든 신호가 수신되었으면 알림 X
                    }
                    // 두 번째 경우: 1111이 수신된 경우
                    else if (received1111) {
                        if (!received0000 && !received2222) {
                            sendNotification(belonging0 + ", " + belonging2 + " 두고 왔습니다.")

                        } else if (!received2222) {
                            // 2222가 수신되지 않았을 경우
                            sendNotification(belonging2 + " 두고 왔습니다.")
                        } else if (!received0000) {
                            // 0000이 수신되지 않았을 경우
                            sendNotification(belonging0 + " 두고 왔습니다.")
                        }
                        stopDeviceCheck()
                        // 모든 신호가 수신되었으면 알림 X

                    }
                    // 세 번째 경우: 2222가 수신된 경우
                    else if (received2222) {
                        if (!received0000 && !received1111) {
                            sendNotification(belonging0 + ", " + belonging1 + " 두고 왔습니다.")
                        } else if (!received1111) {
                            // 1111이 수신되지 않았을 경우
                            sendNotification(belonging1 + " 두고 왔습니다.")
                        } else if (!received0000) {
                            // 0000이 수신되지 않았을 경우
                            sendNotification(belonging0 + " 두고 왔습니다.")
                        }
                    }

                    // 타이머 종료
                    stopDeviceCheck()
                }
            }
            // 5초 후에 확인 (메인 스레드의 핸들러 사용)
            android.os.Handler(mainLooper).postDelayed(checkTimer!!, 5000)
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

    // 화면을 깨우는 함수
    private fun wakeApp() {
        val pm = applicationContext.getSystemService(POWER_SERVICE) as PowerManager
        val screenIsOn = pm.isInteractive // 화면이 켜져 있는지 확인
        if (!screenIsOn) {
            val wakeLockTag = packageName + "WAKELOCK"
            val wakeLock = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE, wakeLockTag
            )
            wakeLock.acquire() // 화면을 켬
            wakeLock.release() // 해제하면 화면은 기기 설정에 따라 다시 꺼짐
        }
    }

    // 알림을 보내는 함수
    private fun sendNotification(message: String) {
        wakeApp()//알림 보낼 때 화면 켜기

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
