package com.example.iot_object_reminder

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class WebSocketManager(private val context: Context, private val listener: WebSocketListener) { // listener는 nullable하지 않도록 수정
    private lateinit var webSocket: WebSocket
    private var espData: String? = null // 수신한 RFID 데이터를 저장할 변수

    fun initWebSocket(url: String) {
        val client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()

        val request = Request.Builder()
            .url(url) // 웹소켓 서버 주소
            .build()

        webSocket = client.newWebSocket(request, listener) // listener는 nullable하지 않음
    }

    fun sendMessage(message: String) {
        webSocket.send(message)
    }

    fun closeConnection() {
        webSocket.close(1000, "앱 종료")
    }
}
