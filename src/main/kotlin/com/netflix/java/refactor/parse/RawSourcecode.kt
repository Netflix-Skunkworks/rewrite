package com.netflix.java.refactor.parse

import java.util.*
import java.util.concurrent.atomic.AtomicInteger

typealias LineNumber = Int
typealias AbsolutePosition = Int

abstract class RawSourceCode {
    abstract val text: String
    abstract val path: String
    
    abstract fun fix(newSource: String)
    
    @delegate:Transient
    val lineStarts: TreeMap<LineNumber, AbsolutePosition> by lazy {
        val line = AtomicInteger(1)
        text.foldIndexed(TreeMap<LineNumber, AbsolutePosition>() to false) { i, acc, c ->
            val (positionsOfLines, lastCharWasNewline) = acc
            if(lastCharWasNewline)
                positionsOfLines.put(line.andIncrement, i)
            positionsOfLines to (c == '\n')
        }.first
    }
    
    fun lineIndent(pos: AbsolutePosition): String {
        var start = lineStarts.lowerKey(pos)
        var i = start
        while(text[i] == ' ' || text[i] == '\t') i++
        return text.substring(start, i)
    }
    
    @delegate:Transient
    val defaultIndentation by lazy {
//        lineStarts.forEach { i, i -> 
//            
//        }
    }
}