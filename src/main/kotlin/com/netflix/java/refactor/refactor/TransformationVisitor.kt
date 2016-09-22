package com.netflix.java.refactor.refactor

import com.netflix.java.refactor.ast.*

abstract class TransformationVisitor : AstVisitor<Tree?>({ it }) {

    /**
     * Top-down depth-first recursion
     */
    abstract fun <T: Tree> T.mutate(cursor: Cursor, childCopy: T.() -> T): T
    
    override fun visitImport(import: Tr.Import, cursor: Cursor): Tree =
            import.mutate(cursor) { copy(qualid = visit(qualid, cursor) as Tr.FieldAccess) }

    override fun visitSelect(field: Tr.FieldAccess, cursor: Cursor): Tree = 
            field.mutate(cursor) { copy(target = visit(field.target, cursor) as Expression) }

    override fun visitClassDecl(classDecl: Tr.ClassDecl, cursor: Cursor): Tree =
            classDecl.mutate(cursor) {
                copy(
                        fields = fields.map { visit(it, cursor) as Tr.VariableDecl },
                        implements = implements.map { visit(it, cursor) as Tree },
                        extends = visit(extends, cursor),
                        methods = methods.map { visit(it, cursor) as Tr.MethodDecl }
                )
            }

    override fun visitMethodInvocation(meth: Tr.MethodInvocation, cursor: Cursor): Tree =
            meth.mutate(cursor) { 
                copy(methodSelect = visit(meth.methodSelect, cursor) as Expression,
                        args = meth.args.map { visit(it, cursor) as Expression }) 
            }

    override fun visitVariable(variable: Tr.VariableDecl, cursor: Cursor): Tree =
            variable.mutate(cursor) {
                copy(varType = visit(varType, cursor) as Expression?,
                        nameExpr = visit(nameExpr, cursor) as Expression?,
                        initializer = visit(initializer, cursor) as Expression?)
            }

    override fun visitCompilationUnit(cu: Tr.CompilationUnit, cursor: Cursor): Tree {
        this.cu = cu
        return cu.mutate(cursor) {
            copy(imports = imports.map { visit(it, cursor) as Tr.Import },
                    packageDecl = visit(packageDecl, cursor) as Expression?,
                    classDecls = classDecls.map { visit(it, cursor) as Tr.ClassDecl })
        }
    }
            
    override fun visitIdentifier(ident: Tr.Ident, cursor: Cursor): Tree = ident.mutate(cursor) { this }

    override fun visitBlock(block: Tr.Block, cursor: Cursor): Tree = 
            block.mutate(cursor) { copy(statements = statements.map { visit(it, cursor) as Statement }) }

    override fun visitMethod(method: Tr.MethodDecl, cursor: Cursor): Tree =
            method.mutate(cursor) { 
                copy(params = params.map { visit(it, cursor) as Tr.VariableDecl },
                        thrown = thrown.map { visit(it, cursor) as Expression },
                        defaultValue = visit(defaultValue, cursor) as Expression?,
                        body = visit(body, cursor) as Tr.Block)
            }

    override fun visitNewClass(newClass: Tr.NewClass, cursor: Cursor): Tree =
            newClass.mutate(cursor) {
                copy(encl = visit(encl, cursor) as Expression?,
                        typeargs = typeargs.map { visit(it, cursor) as Expression },
                        args = args.map { visit(it, cursor) as Expression },
                        classBody = visit(classBody, cursor) as Tr.ClassDecl?)
            }

    override fun visitPrimitive(primitive: Tr.Primitive, cursor: Cursor): Tree = primitive.mutate(cursor) { this }

    override fun visitLiteral(literal: Tr.Literal, cursor: Cursor): Tree = literal.mutate(cursor) { this }

    override fun visitBinary(binary: Tr.Binary, cursor: Cursor): Tree = 
            binary.mutate(cursor) { 
                copy(left = visit(left, cursor) as Expression,
                        right = visit(right, cursor) as Expression)
            }
    
    override fun visitUnary(unary: Tr.Unary, cursor: Cursor): Tree =
            unary.mutate(cursor) { copy(expr = visit(expr, cursor) as Expression) }

    override fun visitForLoop(forLoop: Tr.ForLoop, cursor: Cursor): Tree =
            forLoop.mutate(cursor) {
                copy(init = forLoop.init.map { visit(it, cursor) as Statement },
                        condition = visit(forLoop.condition, cursor) as Expression?,
                        update = forLoop.update.map { visit(it, cursor) as Statement },
                        body = visit(body, cursor) as Statement)
            }

    override fun visitForEachLoop(forEachLoop: Tr.ForEachLoop, cursor: Cursor): Tree =
            forEachLoop.mutate(cursor) {
                copy(variable = visit(variable, cursor) as Tr.VariableDecl,
                        iterable = visit(iterable, cursor) as Expression,
                        body = visit(body, cursor) as Statement)
            }

    override fun visitIf(iff: Tr.If, cursor: Cursor): Tree =
            iff.mutate(cursor) {
                copy(ifCondition = visit(ifCondition, cursor) as Tr.Parentheses,
                        thenPart = visit(thenPart, cursor) as Statement,
                        elsePart = visit(elsePart, cursor) as Statement?)
            }

    override fun visitTernary(ternary: Tr.Ternary, cursor: Cursor): Tree =
            ternary.mutate(cursor) {
                copy(condition = visit(condition, cursor) as Expression,
                        truePart = visit(truePart, cursor) as Expression,
                        falsePart = visit(falsePart, cursor) as Expression)
            }

    override fun visitWhileLoop(whileLoop: Tr.WhileLoop, cursor: Cursor): Tree =
            whileLoop.mutate(cursor) {
                copy(condition = visit(condition, cursor) as Tr.Parentheses,
                        body = visit(body, cursor) as Statement)
            }

    override fun visitDoWhileLoop(doWhileLoop: Tr.DoWhileLoop, cursor: Cursor): Tree =
            doWhileLoop.mutate(cursor) {
                copy(condition = visit(condition, cursor) as Tr.Parentheses,
                        body = visit(body, cursor) as Statement)
            }

    override fun visitBreak(breakStatement: Tr.Break, cursor: Cursor): Tree = breakStatement.mutate(cursor) { this }

    override fun visitContinue(continueStatement: Tr.Continue, cursor: Cursor): Tree = continueStatement.mutate(cursor) { this }

    override fun visitLabel(label: Tr.Label, cursor: Cursor): Tree = 
            label.mutate(cursor) { copy(statement = visit(statement, cursor) as Statement) }

    override fun visitReturn(retrn: Tr.Return, cursor: Cursor): Tree = 
            retrn.mutate(cursor) { copy(expr = visit(expr, cursor) as Expression?) }

    override fun visitCase(case: Tr.Case, cursor: Cursor): Tree = 
            case.mutate(cursor) { 
                copy(pattern = visit(pattern, cursor) as Expression?,
                        statements = statements.map { visit(it, cursor) as Statement })
            }

    override fun visitSwitch(switch: Tr.Switch, cursor: Cursor): Tree = 
            switch.mutate(cursor) {
                copy(selector = visit(selector, cursor) as Tr.Parentheses,
                        cases = cases.map { visit(it, cursor) as Tr.Case })
            }

    override fun visitAssign(assign: Tr.Assign, cursor: Cursor): Tree = 
            assign.mutate(cursor) {
                copy(variable = visit(variable, cursor) as Expression,
                        assignment = visit(assignment, cursor) as Expression)
            }

    override fun visitAssignOp(assign: Tr.AssignOp, cursor: Cursor): Tree = 
            assign.mutate(cursor) {
                copy(variable = visit(variable, cursor) as Expression,
                        assignment = visit(assignment, cursor) as Expression)
            }

    override fun visitThrow(thrown: Tr.Throw, cursor: Cursor): Tree =
            thrown.mutate(cursor) { copy(expr = visit(thrown.expr, cursor) as Expression) }

    override fun visitTry(tryable: Tr.Try, cursor: Cursor): Tree =
            tryable.mutate(cursor) {
                copy(resources = resources.map { visit(it, cursor) as Tr.VariableDecl },
                        body = visit(body, cursor) as Tr.Block,
                        catchers = catchers.map { visit(it, cursor) as Tr.Catch },
                        finally = visit(finally, cursor) as Tr.Block?)
            }
 
    override fun visitCatch(catch: Tr.Catch, cursor: Cursor): Tree = 
            catch.mutate(cursor) { 
                copy(param = visit(param, cursor) as Tr.VariableDecl,
                        body = visit(body, cursor) as Tr.Block)
            }

    override fun visitSynchronized(synch: Tr.Synchronized, cursor: Cursor): Tree = 
            synch.mutate(cursor) {
                copy(lock = visit(lock, cursor) as Tr.Parentheses,
                        body = visit(body, cursor) as Tr.Block)
            }

    override fun visitEmpty(empty: Tr.Empty, cursor: Cursor): Tree = empty.mutate(cursor) { this }

    override fun visitParentheses(parens: Tr.Parentheses, cursor: Cursor): Tree = 
            parens.mutate(cursor) { copy(expr = visit(expr, cursor) as Expression) }

    override fun visitInstanceOf(instanceOf: Tr.InstanceOf, cursor: Cursor): Tree = 
            instanceOf.mutate(cursor) {
                copy(expr = visit(expr, cursor) as Expression,
                        clazz = visit(clazz, cursor) as Tree)
            }

    override fun visitNewArray(newArray: Tr.NewArray, cursor: Cursor): Tree =
            newArray.mutate(cursor) {
                copy(typeExpr = visit(typeExpr, cursor) as Expression,
                        dimensions = dimensions.map { visit(it, cursor) as Expression },
                        elements = elements.map { visit(it, cursor) as Expression })
            }

    override fun visitArrayAccess(arrayAccess: Tr.ArrayAccess, cursor: Cursor): Tree =
            arrayAccess.mutate(cursor) {
                copy(indexed = visit(arrayAccess.index, cursor) as Expression,
                        index = visit(arrayAccess.index, cursor) as Expression)
            }

    override fun visitLambda(lambda: Tr.Lambda, cursor: Cursor): Tree =
            lambda.mutate(cursor) {
                copy(params = params.map { visit(it, cursor) as Tr.VariableDecl },
                        body = visit(body, cursor) as Tree)
            }

    override fun visitToken(token: Tr.Token, cursor: Cursor): Tree =
            token.mutate(cursor) { this }
}