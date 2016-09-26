package com.netflix.java.refactor.ast

class PrintVisitor : AstVisitor<String>("") {
    override fun reduce(r1: String, r2: String): String = r1 + r2

    override fun visitAnnotation(annotation: Tr.Annotation): String =
            annotation.fmt("@${visit(annotation.annotationType)}(${visit(annotation.args)})")

    override fun visitAssign(assign: Tr.Assign): String =
            assign.fmt("${visit(assign.variable)}${assign.operator.fmt("=")}${visit(assign.assignment)}")

    override fun visitClassDecl(classDecl: Tr.ClassDecl): String {
        return super.visitClassDecl(classDecl)
    }

    override fun visitFieldAccess(field: Tr.FieldAccess): String =
            field.fmt("${visit(field.target)}.${field.fieldName}")

    override fun visitForLoop(forLoop: Tr.ForLoop): String {
        val expr = forLoop.control.let { it.fmt("(${visit(it.init)};${visit(it.condition)};${visit(it.update)})") }
        return forLoop.fmt("for$expr${visit(forLoop.body)}")
    }

    override fun visitIdentifier(ident: Tr.Ident): String =
            ident.fmt(ident.name)

    override fun visitImport(import: Tr.Import): String =
            if (import.static)
                import.fmt("import static${visit(import.qualid)};")
            else
            import.fmt("import${visit(import.qualid)};")

    override fun visitLiteral(literal: Tr.Literal): String {
        val v = literal.value.toString()
        return literal.fmt(when(literal.typeTag) {
            Type.Tag.Boolean -> v
            Type.Tag.Byte -> v
            Type.Tag.Char -> v
            Type.Tag.Double -> "${v}d"
            Type.Tag.Float -> "${v}f"
            Type.Tag.Int -> v
            Type.Tag.Long -> "${v}L"
            Type.Tag.Short -> v
            Type.Tag.Void -> v
            Type.Tag.String -> "\"$v\""
            Type.Tag.None -> ""
            Type.Tag.Wildcard -> v
        })
    }

    override fun visitPackage(pkg: Tr.Package): String =
            pkg.fmt("package${visit(pkg.expr)};")

    override fun visitUnary(unary: Tr.Unary): String =
            unary.fmt(when(unary.operator) {
                is Tr.Unary.Operator.PreIncrement -> "++${visit(unary.expr)}"
                is Tr.Unary.Operator.PreDecrement -> "--${visit(unary.expr)}"
                is Tr.Unary.Operator.PostIncrement -> "${visit(unary.expr)}++"
                is Tr.Unary.Operator.PostDecrement -> "${visit(unary.expr)}--"
                is Tr.Unary.Operator.Positive -> "+${visit(unary.expr)}"
                is Tr.Unary.Operator.Negative -> "-${visit(unary.expr)}"
                is Tr.Unary.Operator.Complement -> "~${visit(unary.expr)}"
                is Tr.Unary.Operator.Not -> "!${visit(unary.expr)}"
            })
    
    private fun Tree?.fmt(code: String?): String =
            if (this == null || code == null)
                ""
            else when (formatting) {
                is Formatting.Reified ->
                    (formatting as Formatting.Reified).prefix + code
                is Formatting.Infer -> code
                is Formatting.None -> code
            }
}