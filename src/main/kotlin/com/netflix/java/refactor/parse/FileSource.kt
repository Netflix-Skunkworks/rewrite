package com.netflix.java.refactor.parse

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class FileSource(val file: File): Source {
    override val text = file.readText()
    override val path = file.path
    
    override fun fix(newSource: String) {
        Files.write(file.toPath(), newSource.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    }

    object Builder {
        fun fromPath(path: Path): FileSource = FileSource(path.toFile())
    }
}