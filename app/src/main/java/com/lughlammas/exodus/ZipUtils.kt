package com.lughlammas.exodus

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipUtils {
    /**
     * Zips [sourceDir] into [destZip]. Entries are relative to the folder name
     * (clean single-root zip).
     */
    fun zipFolder(sourceDir: File, destZip: File, bufferSize: Int = 64 * 1024) {
        require(sourceDir.isDirectory) { "Not a directory: ${sourceDir.absolutePath}" }
        destZip.parentFile?.mkdirs()
        if (destZip.exists()) destZip.delete()

        ZipOutputStream(BufferedOutputStream(FileOutputStream(destZip), bufferSize)).use { zos ->
            val rootPath = sourceDir.canonicalFile.toPath()
            sourceDir.walkTopDown().forEach { file ->
                val rel = rootPath.relativize(file.canonicalFile.toPath()).toString().replace('\\', '/')
                if (rel.isEmpty()) return@forEach
                if (file.isDirectory) {
                    val dirName = if (rel.endsWith("/")) rel else "$rel/"
                    zos.putNextEntry(ZipEntry(dirName))
                    zos.closeEntry()
                } else {
                    zos.putNextEntry(ZipEntry(rel))
                    BufferedInputStream(FileInputStream(file), bufferSize).use { input ->
                        input.copyTo(zos, bufferSize)
                    }
                    zos.closeEntry()
                }
            }
        }
    }
}
