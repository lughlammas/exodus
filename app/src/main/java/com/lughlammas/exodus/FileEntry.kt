package com.lughlammas.exodus

import java.io.File

data class FileEntry(
    val file: File,
    val name: String = file.name,
    val isDirectory: Boolean = file.isDirectory,
    val length: Long = if (file.isFile) file.length() else 0L,
    val lastModified: Long = file.lastModified()
)
