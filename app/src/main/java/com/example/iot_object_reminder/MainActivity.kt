package com.example.iot_object_reminder

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class MainActivity : AppCompatActivity() {

    private lateinit var webSocketManager: WebSocketManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 웹소켓 매니저 초기화 및 연결 설정
        webSocketManager = WebSocketManager(this, webSocketListener)
        webSocketManager.initWebSocket("ws://192.168.4.1:8080")


    }

    private val webSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "웹소켓 연결 성공", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) { //웹소켓(WebSocket) 통신을 통해 서버로부터 받은 메시지를 처리
            runOnUiThread {
                //CheckSignal.updateRFIDData(text)
            }
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
        // 액티비티 종료 시 웹소켓 연결 닫기
        webSocketManager.closeConnection()
    }
}