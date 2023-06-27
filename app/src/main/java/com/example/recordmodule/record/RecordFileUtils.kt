package com.example.recordmodule.record

import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
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

    fun delete(path: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            delete(File(path))
        } else {
            delete(Path(path))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun delete(path: Path) {
        if (path.exists()) {
            Files.walk(path).sorted(Comparator.reverseOrder()).forEach { it.deleteIfExists() }
        }
    }

    private fun delete(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { childFile ->
                delete(childFile)
            }

        }

        file.delete()
    }
}