package com.example.recordmodule.record

import android.os.Build
import java.io.File
import java.io.IOException
import kotlin.io.path.Path
import kotlin.io.path.isDirectory

object RecordFileUtils {
    @Throws(IOException::class)
    fun mkdir(dstString: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            var dst = File(dstString)
            if (dst.isDirectory) {
                dst.mkdirs()
            } else {
                dst.parentFile?.mkdirs()
            }
        } else {
            var dst = Path(dstString)
            if (dst.isDirectory()) {
                dst.toFile().mkdirs()
            } else {
                dst.parent.toFile().mkdirs()
            }
        }
    }
}