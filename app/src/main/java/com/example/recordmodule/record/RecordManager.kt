package com.example.recordmodule.record

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Timer
import kotlin.concurrent.timer

private const val TAG = "RecordManager"

class RecordManager(IntentNoti: Intent) : Record {
    // bindService
    private var recordService: RecordService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.i(TAG, "연결")
            this?.run {
                val b = service as RecordService.RecordServiceBinder
                recordService = b.getService(IntentNoti).apply {
                    startRecording()
                }
                timerStart()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.i(TAG, "연결끊김")
            recordService = null
        }
    }

    override fun recordStart(
        context: Context,
        filePath: String,
        fileTempPath: String
    ): Boolean {
        showFloatingRecordButton = true

        return if (filePath.isEmpty() || fileTempPath.isEmpty()) {
            Log.e(TAG, "fail start recording. record path is empty")
            false
        } else {
            this?.run {
                val intent = Intent(context, RecordService::class.java)
                    .putExtra(RecordService.INTENT_PATH, filePath)
                    .putExtra(RecordService.INTENT_TEMP_PATH, fileTempPath)
                context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            }
            true
        }
    }

    override fun recordResume(): Boolean {
        if (recordService?.resumeRecording() == true) {
            timerStart()
            return true
        } else
            return false
    }

    override fun recordPause(): Boolean {
        return if (recordService?.pauseRecording() == true) {
            timerTask?.cancel()

            _recordState.value = RecordState.Paused
            true
        } else {
            false
        }
    }

    override fun recordStop(context: Context): Boolean {
        return if (recordState.value != RecordState.None) {
            context.unbindService(serviceConnection)

            timerTask?.cancel()
            timeIncrement = 0
            _recordTime.value = "00:00"

            _recordState.value = RecordState.None
            true
        } else {
            false
        }
    }

    override fun recordTempSave(): Boolean {
        // 10초 지나지 않았다면 임시저장하지 않음.(임시저장 불러오기 과정에서 20kb 정도 파일로 저장되는 이슈 해결)
        return if (tempSaveCheckTime > 100) {
            tempSaveCheckTime = 0
            return recordService?.saveTempRecording() == true
        } else {
            false
        }
    }

    override fun recordTempReStart(): Boolean {
        return recordService?.reStartTempRecording() == true
    }

    override fun recordCancel(context: Context): Boolean {
        return if (showFloatingRecordButton) {
            if (recordState.value != RecordState.None) {
                context.unbindService(serviceConnection)
                recordDestroy()
            } else {
                showFloatingRecordButton = false
            }
            true
        } else {
            false
        }
    }

    override fun recordDestroy() {
        timerStop()
        _recordState.value = RecordState.None
        showFloatingRecordButton = false
    }

    //viewModel

    // 녹취
    var showFloatingRecordButton by mutableStateOf(false)
    private val _recordState = mutableStateOf<RecordState>(RecordState.None)
    val recordState: State<RecordState> = _recordState
    private val _recordTime = mutableStateOf("00:00")
    val recordTime: State<String> = _recordTime

    // timer
    private var timeIncrement = 0
    var tempSaveCheckTime = 0
    private var timerTask: Timer? = null

    @OptIn(DelicateCoroutinesApi::class)
    private fun timerStart() = GlobalScope.launch(Dispatchers.IO) {
        // 타이머 실행
        timerTask = timer(period = 100) { // 0.1초 마다 업데이트
            timeIncrement++
            tempSaveCheckTime++
            recordService?.apply { presentationTimeUs += 100000 }

            val formattedSeconds = (timeIncrement / 10 % 60).toString().padStart(2, '0')
            val formattedMinutes = (timeIncrement / 10 / 60).toString().padStart(2, '0')

            _recordTime.value = "$formattedMinutes : $formattedSeconds"
        }

        _recordState.value = RecordState.Recoding
    }

    private fun timerStop() {
        timerTask?.cancel()
        timeIncrement = 0
        _recordTime.value = "00 : 00"
    }
}