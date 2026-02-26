package com.pdrehab.handwritinglab.core.io

import java.io.File

object FileCopy {
    fun copyRecursively(src: File, dst: File) {
        if (!src.exists()) return
        if (src.isDirectory) {
            dst.mkdirs()
            src.listFiles()?.forEach { child ->
                copyRecursively(child, File(dst, child.name))
            }
        } else {
            dst.parentFile?.mkdirs()
            src.copyTo(dst, overwrite = true)
        }
    }
}