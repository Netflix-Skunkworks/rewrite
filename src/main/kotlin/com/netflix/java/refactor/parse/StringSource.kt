package com.netflix.java.refactor.parse

import java.io.Serializable
import java.nio.file.Path

class StringSource(override var text: String,
                   override val path: String): RawSourceCode(), Serializable {
     
    override fun fix(newSource: String) {
        text = newSource
    }
    
    object Builder {
        fun fromPath(path: Path): StringSource = StringSource(path.toFile().readText(), path.toString())
    }
}