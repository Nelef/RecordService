package com.example.recordmodule.record

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.media.*
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.recordmodule.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "RecordService"

class RecordService : Service() {

    companion object {
        // Notification
        private const val NOTI_ID = 1

        const val INTENT_PATH = "path"
        const val INTENT_TEMP_PATH = "temp_path"

        private const val SAMPLE_RATE = 44100
        private const val BIT_RATE = 128000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    // 현재상태 (녹취ON or 녹취OFF)
    private var isRecording = false
    private var isPaused = false

    // bindService 설정
    private val binder = RecordServiceBinder()
    private lateinit var intentClass: Class<*>

    private lateinit var filePath: String
    private lateinit var fileTempPath: String // 임시저장

    // AudioRecord
    private val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
    private lateinit var audioRecord: AudioRecord
    private lateinit var writeAudioDataToFileJob: Job

    // MediaCodec 및 MediaMuxer 객체 선언
    private lateinit var codec: MediaCodec
    private lateinit var mediaMuxer: MediaMuxer
    private var audioTrackIndex: Int = -1
    private var outputFile: File? = null
    private var bufferInfo = MediaCodec.BufferInfo()

    // 임시저장
    private lateinit var mediaMuxer2: MediaMuxer
    private lateinit var backupFormat: MediaFormat // 안드로이드 7.1.1 이하
    private var isTempRecording = false

    // timer
    var presentationTimeUs = 0L

    @SuppressLint("MissingPermission")
    override fun onBind(intent: Intent): IBinder {
        filePath = intent.getStringExtra(INTENT_PATH) ?: ""
        fileTempPath = intent.getStringExtra(INTENT_TEMP_PATH) ?: ""

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        // MediaCodec 초기화
        val format = MediaFormat.createAudioFormat("audio/mp4a-latm", SAMPLE_RATE, 1)
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize)
        codec = MediaCodec.createEncoderByType("audio/mp4a-latm")
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        // MediaMuxer 초기화
        mediaMuxer = initMediaMuxer(filePath = filePath, isTemp = false)
        mediaMuxer.start()

        // 오리지널 설정 값 백업 (안드로이드 7.1.1 이하 문제 해결)
        backupFormat = codec.outputFormat

        // TEMP MediaMuxer 초기화
        mediaMuxer2 = initMediaMuxer(filePath = fileTempPath, isTemp = true)
        mediaMuxer2.start()

        return binder
    }

    private fun initMediaMuxer(filePath: String, isTemp: Boolean): MediaMuxer {
        RecordFileUtils.delete(filePath)
        RecordFileUtils.mkdir(filePath)
        outputFile = File(filePath)
        val mediaMuxer = MediaMuxer(
            outputFile!!.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
        )
        // 안드로이드 버전에 따른 임시저장 시 오류 발생하므로 분기
        // (7.1.1 이하 헤더 128bytes 제한으로 backupFormat으로 가능. 13의 경우 originalFormat으로만 가능.)
        audioTrackIndex = mediaMuxer.addTrack(
            if (isTemp) {
                if (Build.VERSION.SDK_INT < 28) backupFormat
                else codec.outputFormat
            } else
                codec.outputFormat
        )
        return mediaMuxer
    }

    override fun onRebind(intent: Intent?) {
        filePath = intent?.getStringExtra(INTENT_PATH) ?: ""
        fileTempPath = intent?.getStringExtra(INTENT_TEMP_PATH) ?: ""
        super.onRebind(intent)
    }

    inner class RecordServiceBinder : Binder() {
        fun getService(intentClass_: Class<*>): RecordService {
            intentClass = intentClass_
            createNotification()
            return this@RecordService
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "RecordService onCreate")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopRecording()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
        Log.i(TAG, "RecordService onDestroy")
    }

    @SuppressLint("NotificationPermission")
    private fun createNotification() {
        val builder = NotificationCompat.Builder(this, "default")
        builder.setSmallIcon(R.mipmap.ic_launcher)
        builder.setContentTitle("IBKS 녹취 중")
        builder.setContentText("IBKS 녹취 중입니다.")
        builder.color = Color.RED
        val notificationIntent = Intent(this, intentClass)
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        // API 31 부터 pendingIntent에 FLAG 설정 필수
        val pendingIntent: PendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        builder.setContentIntent(pendingIntent) // 알림 클릭 시 이동

        // 알림 표시
        // API 26 부터 NotificationChannel 객체 사용
        // 고유한 채널 ID, 사용자가 볼 수 있는 이름, 중요도 수준을 사용
        val notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    "default",
                    "기본 채널",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
        notificationManager.notify(NOTI_ID, builder.build()) // id : 정의해야하는 각 알림의 고유한 int값
        val notification = builder.build()
        startForeground(NOTI_ID, notification)
    }

    // 녹취 시작
    fun startRecording() {
        if (isRecording) {
            stopRecording()
        }
        isPaused = false
        isRecording = true
        isTempRecording = true

        audioRecord.startRecording()
        writeAudioDataToFileJob = GlobalScope.launch {
            writeAudioDataToFile()
        }
    }

    // 녹취 종료
    private fun stopRecording() {
        if (isRecording) {
            filePath = ""
            isRecording = false
            isPaused = false

            audioRecord.stop()

            // 코루틴 작업이 정상 종료되면 stop 호출.
            writeAudioDataToFileJob.invokeOnCompletion {
                // MediaCodec 및 MediaMuxer 해제
                codec.stop()
                codec.release()
                mediaMuxer.stop()
                mediaMuxer.release()

                // Temp MediaMuxer 해제
                mediaMuxer2.stop()
                mediaMuxer2.release()
            }
        }
    }

    fun saveTempRecording(): Boolean {
        return if (isRecording) {
            isTempRecording = false
            // Temp MediaMuxer 해제
            mediaMuxer2.stop()
            mediaMuxer2.release()
            true
        } else {
            false
        }
    }

    fun reStartTempRecording(): Boolean {
        return if (isRecording && !isTempRecording) {
            // 파일 이름 올린 뒤 다시 시작
            fileTempPath = increaseFileName(fileTempPath)
            // TEMP MediaMuxer 초기화
            mediaMuxer2 = initMediaMuxer(filePath = fileTempPath, isTemp = true)
            mediaMuxer2.start()
            isTempRecording = true
            true
        } else {
            false
        }
    }

    /**
     * "...test_000_09.mp3" -> "...test_000_10.mp3" 형식으로 변환하는 함수.
     * */
    private fun increaseFileName(filePath: String): String {
        val parts = filePath.split("_")
        return if (parts.size < 3) "" else "${parts.dropLast(1).joinToString("_")}_${
            String.format("%02d", (parts.last().substringBeforeLast(".").toIntOrNull() ?: 0) + 1)
        }.${parts.last().substringAfterLast(".")}"
    }

    private fun writeAudioDataToFile() {
        val audioData = ByteArray(bufferSize)

        // 타임스탬프 에러 확인 용 변수
        var isErrorTimeStamp = false
        var preTimeStamp = 0L

        while (isRecording) {
            val numberOfBytes = audioRecord.read(audioData, 0, bufferSize)
            if (numberOfBytes != AudioRecord.ERROR_INVALID_OPERATION) {
                val inputBufferId = codec.dequeueInputBuffer(-1)
                if (inputBufferId >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputBufferId)
                    inputBuffer?.clear()
                    inputBuffer?.put(audioData)
                    codec.queueInputBuffer(inputBufferId, 0, numberOfBytes, 0, 0)
                }

                var outputBufferId = codec.dequeueOutputBuffer(bufferInfo, 0)
                while (outputBufferId >= 0) {

                    // 타임스탬프 에러나는지 확인
                    if (!isErrorTimeStamp) {
                        if (preTimeStamp > bufferInfo.presentationTimeUs) {
                            isErrorTimeStamp = true
                        }
                        preTimeStamp = bufferInfo.presentationTimeUs
                    }
                    // 타임스탬프 에러날 때 직접 타임스탬프 작성.
                    if (isErrorTimeStamp) bufferInfo.presentationTimeUs = presentationTimeUs

                    val outputBuffer = codec.getOutputBuffer(outputBufferId)
                    val encodedData = ByteArray(bufferInfo.size)
                    outputBuffer?.get(encodedData)
                    outputBuffer?.clear() // 이전 데이터는 중복되니 삭제 후 새 정보만 기록
                    mediaMuxer.writeSampleData(audioTrackIndex, outputBuffer!!, bufferInfo)
                    if (isTempRecording)
                        mediaMuxer2.writeSampleData(audioTrackIndex, outputBuffer!!, bufferInfo)
                    codec.releaseOutputBuffer(outputBufferId, false)
                    outputBufferId = codec.dequeueOutputBuffer(bufferInfo, 0)
                }
            }
        }
    }

    // 녹취 일시정지
    fun pauseRecording(): Boolean {
        return if (isRecording && !isPaused) {
            isPaused = true
            audioRecord.stop()
            true
        } else {
            false
        }
    }

    // 녹취 재개
    fun resumeRecording(): Boolean {
        return if (isRecording && isPaused) {
            isRecording = true
            isPaused = false
            audioRecord.startRecording()
            true
        } else {
            false
        }
    }
}