package com.example.recordmodule

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.recordmodule.record.FloatingRecordButton
import com.example.recordmodule.record.RecordData
import com.example.recordmodule.record.RecordManager
import com.example.recordmodule.ui.theme.RecordModuleTheme
import java.io.File

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private val PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions(PERMISSIONS, 1)

        setContent {
            RecordModuleTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Button(onClick = { recordStart() }) {
                            Text(text = "녹취 서비스 시작")
                        }
                        Button(onClick = { recordCancel() }) {
                            Text(text = "녹취 서비스 종료")
                        }
                        Button(onClick = {
                            recordTempSave()
                            recordTempReStart()
                        }) {
                            Text(text = "녹취 임시저장")
                        }
                    }

                    FloatingRecordButton(
                        showFloatingRecordButton = record.showFloatingRecordButton,
                        recordState = record.recordState.value,
                        recordTime = record.recordTime.value,
                        recordFileList = recordList,
                        onRecord = { recordStart() },
                        onResume = { recordResume() },
                        onPause = { recordPause() },
                        onStop = { recordStop() },
                        onRecordList = { updateRecordList() }
                    )
                }
            }
        }
    }

    private fun checkPermissions(
        permissions: Array<String>,
        requestCode: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var result: Int
            val permissionList: MutableList<String> = ArrayList()
            for (pm in permissions) {
                result = ContextCompat.checkSelfPermission(this, pm)
                if (result != PackageManager.PERMISSION_GRANTED) {
                    permissionList.add(pm)
                }
            }
            if (permissionList.isNotEmpty()) {
                requestPermissions(permissionList.toTypedArray(), requestCode)
            }
        }
    }

    // 녹취
    var record = RecordManager(MainActivity::class.java)

    fun recordStart() {
        updateRecordList()
        val filePath = generateRecordFilePath(false)
        val fileTempPath = generateRecordFilePath(true)

        if (record.recordStart(
                context = this,
                filePath = filePath,
                fileTempPath = fileTempPath
            )
        ) {
            Toast.makeText(this, "녹취가 시작되었습니다.", Toast.LENGTH_SHORT).show()
        } else
            Toast.makeText(this, "녹취 시작 실패(record 정보 부족)", Toast.LENGTH_SHORT).show()
    }

    fun recordResume() {
        if (record.recordResume())
            Toast.makeText(this, "녹취가 재시작되었습니다.", Toast.LENGTH_SHORT).show()
    }

    fun recordPause() {
        if (record.recordPause())
            Toast.makeText(this, "녹취가 일시정지되었습니다.", Toast.LENGTH_SHORT).show()
    }

    fun recordStop() {
        if (record.recordStop(this)) {
            updateRecordList()
            Toast.makeText(this, "녹취 저장이 완료되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    fun recordTempSave() {
        if (record.recordTempSave()) {
            Toast.makeText(this, "녹취 임시저장이 완료되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    fun recordTempReStart() {
        if (record.recordTempReStart()) {
            Toast.makeText(this, "녹취 임시저장이 재시작되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    fun recordCancel() {
        if (record.recordCancel(this)) {
            Toast.makeText(this, "녹취 저장이 취소되었습니다.", Toast.LENGTH_SHORT).show()
            records.clear()
        }
    }


    // 녹취
    var selectedRecordCode by mutableStateOf("")
    val records = mutableStateMapOf<String, List<RecordData>>()
    private var _recordList = mutableStateListOf<RecordData>()
    val recordList: List<RecordData> = _recordList

    fun generateRecordFilePath(isTemp: Boolean): String {
        selectedRecordCode = "record"
        return if (!isTemp) File(getExternalFilesDir(null), "record/test_0_01.mp3").absolutePath
        else File(getExternalFilesDir(null), "temp/test_0_01.mp3").absolutePath
    }

    fun updateRecordList() {
        // TODO: recode code 여러개에 대한 처리가 필요함

        records.apply {
            clear()

            val recordFiles = File(getExternalFilesDir(null), "record").listFiles { _, name ->
                name.endsWith(".mp3")
            } ?: emptyArray()

            val records = recordFiles.map { recordFile ->
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(recordFile.absolutePath)
                } catch (e: Exception) {
                    Log.e(TAG, "e")
                }
                val duration = java.lang.Long.parseLong(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?: "0"
                )

                RecordData(
                    "녹취",
                    recordFile.absolutePath,
                    recordFile.name,
                    recordFile.length(),
                    duration
                )
            }

            if (records.isNotEmpty()) {
                this["record"] = records
            }
        }

        records[selectedRecordCode]?.let {
            _recordList.apply {
                clear()
                addAll(it)
            }
        }
    }
}