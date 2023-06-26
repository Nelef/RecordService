package com.example.recordmodule.record

import android.content.Context

interface Record {

    fun recordStart(
        context: Context,
        filePath: String,
        fileTempPath: String
    ): Boolean

    fun recordResume(): Boolean

    fun recordPause(): Boolean

    fun recordStop(context: Context): Boolean // 일반 저장(중지)

    fun recordTempSave(): Boolean
    fun recordTempReStart(): Boolean
    fun recordCancel(context: Context): Boolean
    fun recordDestroy()
}