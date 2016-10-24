package com.netflix.java.refactor.ast

object TreeBuilder {

    fun buildName(cache: TypeCache, fullyQualifiedName: String, fmt: Formatting = Formatting.Reified.Empty): NameTree {
        val parts = fullyQualifiedName.split('.')

        val expr = parts.foldIndexed(Tr.Empty(Formatting.None) as Expression to "") { i, acc, part ->
            val (target, subpackage) = acc
            if (target is Tr.Empty) {
                Tr.Ident(part, Type.Package.build(cache, part), Formatting.Reified.Empty) to part
            } else {
                val fullName = "$subpackage.$part"
                val partFmt = if (i == parts.size - 1) {
                    fmt
                } else {
                    Formatting.Reified("", "\\s*[^\\s]+(\\s*)".toRegex().matchEntire(part)!!.groupValues[1])
                }

                val identFmt = Formatting.Reified("^\\s*".toRegex().find(part)!!.groupValues[0])

                if (part[0].isUpperCase() || i == parts.size - 1) {
                    Tr.FieldAccess(target, Tr.Ident(part.trim(), null, identFmt), Type.Class.build(cache, fullName), partFmt) to fullName
                } else {
                    Tr.FieldAccess(target, Tr.Ident(part.trim(), null, identFmt), Type.Package.build(cache, fullName), partFmt) to fullName
                }
            }
        }

        return expr.first as NameTree
    }
}