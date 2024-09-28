package com.example.iot_object_reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AlertDialog
import android.widget.Toast


//웹소켓으로 받은 신호 처리
class CheckSignal(private val context: Context, private val webSocketManager: WebSocketManager) {

    private var espData: String? = null // 수신한 RFID 데이터를 저장할 변수
    private var espDialog: AlertDialog? = null // RFID 다이얼로그를 저장할 변수

    // BroadcastReceiver를 객체 생성 시 초기화
    private val rfidReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            espData = intent?.getStringExtra("rfidData")
            espData?.let {
                showRFIDData(it)
            }
        }
    }

    init {
        // BroadcastReceiver 등록
        val filter = IntentFilter("RFID_DATA")
        context.registerReceiver(rfidReceiver, filter)
    }

    // 수신한 RFID 데이터를 다이얼로그에 표시하는 함수
    private fun showRFIDData(data: String) {
        if (espDialog == null) {
            espDialog = AlertDialog.Builder(context)
                .setTitle("RFID Data")
                .setMessage(data)
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    espDialog = null // 다이얼로그 닫기
                }
                .create()
        }

        // 다이얼로그가 이미 열려있는 경우 메시지만 업데이트
        espDialog?.setMessage(data)
        if (espDialog?.isShowing == false) {
            espDialog?.show()
        }
    }

    fun unregisterReceiver() {
        // BroadcastReceiver 해제
        context.unregisterReceiver(rfidReceiver)
    }

    // Getter for espData
    fun getEspData(): String? {
        return espData
    }

    // Setter for espData
    fun setEspData(data: String) {
        espData = data
    }
}
