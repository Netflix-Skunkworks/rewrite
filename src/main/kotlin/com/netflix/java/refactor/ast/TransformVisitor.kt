package com.netflix.java.refactor.ast

class TransformVisitor(val transformations: Iterable<AstTransform<*>>) : AstVisitor<Tree?>({ it }) {
    private fun <T : Tree> T.transformIfNecessary(cursor: Cursor): T {
        return transformations
                .filterIsInstance<AstTransform<T>>()
                .filter { it.cursor == cursor }
                .map { it.mutation }
                .fold(this) { mutated, mut -> mut(mutated) }
    }
    
    private fun <T> List<T>.mapIfNecessary(transform: (T) -> T): List<T> {
        var changed = false
        val mapped = this.map {
            val mappedElem = transform(it)
            if(it !== mappedElem)
                changed = true
            mappedElem
        }
        
        return if(changed) mapped else this
    }
    
    override fun visitImport(import: Tr.Import): Tree {
        val qualid = visit(import.qualid) as Tr.FieldAccess
        return (if(qualid !== import.qualid) {
            import.copy(qualid = qualid)
        }
        else import).transformIfNecessary(cursor)
    }

    override fun visitFieldAccess(field: Tr.FieldAccess): Tree {
        val target = visit(field.target) as Expression
        return (if(target !== field.target) {
            field.copy(target = target)
        }
        else field).transformIfNecessary(cursor)
    }

    override fun visitClassDecl(classDecl: Tr.ClassDecl): Tree {
        val extends = visit(classDecl.extends)
        val implements = classDecl.implements.mapIfNecessary { visit(it) as Tree }
        val definitions = classDecl.definitions.mapIfNecessary { visit(it) as Tr.VariableDecl }
        
        return (if(definitions !== classDecl.definitions || implements !== classDecl.implements ||
                    extends !== classDecl.extends) {
           classDecl.copy(definitions = definitions, implements = implements, extends = extends) 
        } else classDecl).transformIfNecessary(cursor)
    }

    override fun visitMethodInvocation(meth: Tr.MethodInvocation): Tree {
        val methodSelect = visit(meth.methodSelect) as Expression
        val args = meth.args.mapIfNecessary { visit(it) as Expression }
        
        return (if(methodSelect !== meth.methodSelect || args !== meth.args) {
            meth.copy(methodSelect = methodSelect, args = args)
        } else meth).transformIfNecessary(cursor)
    }

    override fun visitVariable(variable: Tr.VariableDecl): Tree {
        val varType = visit(variable.varType) as Expression?
        val nameExpr = visit(variable.nameExpr) as Expression?
        val initializer = visit(variable.initializer) as Expression?
        
        return (if(varType !== variable.varType || nameExpr !== variable.nameExpr || initializer !== variable.initializer) {
            variable.copy(varType = varType, nameExpr = nameExpr, initializer = initializer)
        } else variable).transformIfNecessary(cursor)
    }

    override fun visitCompilationUnit(cu: Tr.CompilationUnit): Tree {
        this.cu = cu
        
        val imports = cu.imports.mapIfNecessary { visit(it) as Tr.Import }
        val packageDecl = visit(cu.packageDecl) as Tr.Package?
        val classDecls = cu.classDecls.mapIfNecessary { visit(it) as Tr.ClassDecl }
        
        return (if(imports !== cu.imports || packageDecl !== cu.packageDecl || classDecls !== cu.classDecls) {
            cu.copy(imports = imports, packageDecl = packageDecl, classDecls = classDecls)
        } else cu).transformIfNecessary(cursor)
    }

    override fun visitIdentifier(ident: Tr.Ident): Tree = ident.transformIfNecessary(cursor)

    override fun visitBlock(block: Tr.Block): Tree {
        val statements = block.statements.mapIfNecessary { visit(it) as Statement }
        
        return (if(statements !== block.statements) {
            block.copy(statements = statements)
        } else block).transformIfNecessary(cursor)
    }

    override fun visitMethod(method: Tr.MethodDecl): Tree {
        val params = method.params.mapIfNecessary { visit(it) as Tr.VariableDecl }
        val thrown = method.thrown.mapIfNecessary { visit(it) as Expression }
        val defaultValue = visit(method.defaultValue) as Expression?
        val body = visit(method.body) as Tr.Block
        
        return (if(params !== method.params || thrown !== method.thrown || defaultValue !== method.defaultValue ||
                    body !== method.body) {
            method.copy(params = params, thrown = thrown, defaultValue = defaultValue, body = body)
        } else method).transformIfNecessary(cursor)
    }

    override fun visitNewClass(newClass: Tr.NewClass): Tree {
        val encl = visit(newClass.encl) as Expression?
        val typeArgs = newClass.typeArgs.mapIfNecessary { visit(it) as Expression }
        val args = newClass.args.mapIfNecessary { visit(it) as Expression }
        val classBody = visit(newClass.classBody) as Tr.ClassDecl?
        
        return (if(encl !== newClass.encl || typeArgs !== newClass.typeArgs || args !== newClass.args || classBody !== newClass.classBody) {
            newClass.copy(encl = encl, typeArgs = typeArgs, args = args, classBody = classBody)
        } else newClass).transformIfNecessary(cursor)
    }

    override fun visitPrimitive(primitive: Tr.Primitive): Tree = primitive.transformIfNecessary(cursor)

    override fun visitLiteral(literal: Tr.Literal): Tree = literal.transformIfNecessary(cursor)

    override fun visitBinary(binary: Tr.Binary): Tree {
        val left = visit(binary.left) as Expression
        val right = visit(binary.right) as Expression
        
        return (if(left !== binary.left || right !== binary.right) {
            binary.copy(left = left, right = right, formatting = binary.formatting)
        } else binary).transformIfNecessary(cursor)
    }

    override fun visitUnary(unary: Tr.Unary): Tree {
        val expr = visit(unary.expr) as Expression
        
        return (if(expr !== unary.expr) {
            unary.copy(expr = expr)
        } else unary).transformIfNecessary(cursor)
    }

    override fun visitForLoop(forLoop: Tr.ForLoop): Tree {
        val control = forLoop.control.let {
            val init = it.init.mapIfNecessary { visit(it) as Statement }
            val condition = visit(it.condition) as Expression?
            val update = it.update.mapIfNecessary { visit(it) as Statement }
            
            if(init != it.init || condition != it.condition || update != it.update) {
                forLoop.control.copy(init = init, condition = condition, update = update)
            } else forLoop.control
        }

        val body = visit(forLoop.body) as Statement
        
        return (if(control !== forLoop.control || body !== forLoop.body) {
            forLoop.copy(control = control, body = body)
        } else forLoop).transformIfNecessary(cursor)
    }

    override fun visitForEachLoop(forEachLoop: Tr.ForEachLoop): Tree {
        val variable = visit(forEachLoop.variable) as Tr.VariableDecl
        val iterable = visit(forEachLoop.iterable) as Expression
        val body = visit(forEachLoop.body) as Statement
        
        return (if(variable !== forEachLoop.variable || iterable !== forEachLoop.iterable || body !== forEachLoop.body) {
            forEachLoop.copy(variable = variable, iterable = iterable, body = body)
        } else forEachLoop).transformIfNecessary(cursor)
    }

    override fun visitIf(iff: Tr.If): Tree {
        val ifCondition = visit(iff.ifCondition) as Tr.Parentheses
        val thenPart = visit(iff.thenPart) as Statement
        val elsePart = visit(iff.elsePart) as Statement?
        
        return (if(ifCondition !== iff.ifCondition || thenPart !== iff.thenPart || elsePart !== iff.elsePart) {
            iff.copy(ifCondition = ifCondition, thenPart = thenPart, elsePart = elsePart)
        } else iff).transformIfNecessary(cursor)
    }

    override fun visitTernary(ternary: Tr.Ternary): Tree {
        val condition = visit(ternary.condition) as Expression
        val truePart = visit(ternary.truePart) as Expression
        val falsePart = visit(ternary.falsePart) as Expression
     
        return (if(condition !== ternary.condition || truePart !== ternary.truePart || falsePart !== ternary.falsePart) {
            ternary.copy(condition = condition, truePart = truePart, falsePart = falsePart)
        } else ternary).transformIfNecessary(cursor)
    }

    override fun visitWhileLoop(whileLoop: Tr.WhileLoop): Tree {
        val condition = visit(whileLoop.condition) as Tr.Parentheses
        val body = visit(whileLoop.body) as Statement
        
        return (if(condition !== whileLoop.condition || body !== whileLoop.body) {
            whileLoop.copy(condition = condition, body = body)
        } else whileLoop).transformIfNecessary(cursor)
    }

    override fun visitDoWhileLoop(doWhileLoop: Tr.DoWhileLoop): Tree {
        val condition = visit(doWhileLoop.condition) as Tr.Parentheses
        val body = visit(doWhileLoop.body) as Statement
        
        return (if(condition !== doWhileLoop.condition || body !== doWhileLoop.body) {
            doWhileLoop.copy(condition = condition, body = body)
        } else doWhileLoop).transformIfNecessary(cursor)
    }

    override fun visitBreak(breakStatement: Tr.Break): Tree = breakStatement.transformIfNecessary(cursor)

    override fun visitContinue(continueStatement: Tr.Continue): Tree = continueStatement.transformIfNecessary(cursor)

    override fun visitLabel(label: Tr.Label): Tree {
        val statement = visit(label.statement) as Statement
        
        return (if(statement !== label.statement) {
            label.copy(statement = statement)
        } else label).transformIfNecessary(cursor)
    }

    override fun visitReturn(retrn: Tr.Return): Tree {
        val expr = visit(retrn.expr) as Expression?
        
        return (if(expr !== retrn.expr) {
            retrn.copy(expr = expr)
        } else retrn).transformIfNecessary(cursor)
    }

    override fun visitCase(case: Tr.Case): Tree {
        val pattern = visit(case.pattern) as Expression?
        val statements = case.statements.mapIfNecessary { visit(it) as Statement }
        
        return (if(pattern !== case.pattern || statements !== case.statements) {
            case.copy(pattern = pattern, statements = statements)
        } else case).transformIfNecessary(cursor)
    }

    override fun visitSwitch(switch: Tr.Switch): Tree {
        val selector = visit(switch.selector) as Tr.Parentheses
        val cases = switch.cases.mapIfNecessary { visit(it) as Tr.Case }
        
        return (if(selector !== switch.selector || cases !== switch.cases) {
            switch.copy(selector = selector, cases = cases)
        } else switch).transformIfNecessary(cursor)
    }

    override fun visitAssign(assign: Tr.Assign): Tree {
        val variable = visit(assign.variable) as Expression
        val assignment = visit(assign.assignment) as Expression
        
        return (if(variable !== assign.variable || assignment !== assign.assignment) {
            assign.copy(variable = variable, assignment = assignment)
        } else assign).transformIfNecessary(cursor)
    }

    override fun visitAssignOp(assign: Tr.AssignOp): Tree {
        val variable = visit(assign.variable) as Expression
        val assignment = visit(assign.assignment) as Expression

        return (if(variable !== assign.variable || assignment !== assign.assignment) {
            assign.copy(variable = variable, assignment = assignment)
        } else assign).transformIfNecessary(cursor)
    }

    override fun visitThrow(thrown: Tr.Throw): Tree {
        val expr = visit(thrown.expr) as Expression
        
        return (if(expr !== thrown.expr) {
            thrown.copy(expr = expr)
        } else expr).transformIfNecessary(cursor)
    }

    override fun visitTry(tryable: Tr.Try): Tree {
        val resources = tryable.resources.mapIfNecessary { visit(it) as Tr.VariableDecl }
        val body = visit(tryable.body) as Tr.Block
        val catchers = tryable.catchers.mapIfNecessary { visit(it) as Tr.Catch }
        val finally = visit(tryable.finally) as Tr.Block?
        
        return (if(resources !== tryable.resources || body !== tryable.body || catchers !== tryable.catchers ||
                    finally !== tryable.finally) {
            tryable.copy(resources = resources, body = body, catchers = catchers, finally = finally)
        } else tryable).transformIfNecessary(cursor)
    }

    override fun visitCatch(catch: Tr.Catch): Tree {
        val param = visit(catch.param) as Tr.VariableDecl
        val body = visit(catch.body) as Tr.Block
     
        return (if(param !== catch.param || body !== catch.body) {
            catch.copy(param = param, body = body)
        } else catch).transformIfNecessary(cursor)
    }

    override fun visitSynchronized(synch: Tr.Synchronized): Tree {
        val lock = visit(synch.lock) as Tr.Parentheses
        val body = visit(synch.body) as Tr.Block
        
        return (if(lock !== synch.lock || body !== synch.body) {
            synch.copy(lock = lock, body = body)
        } else synch).transformIfNecessary(cursor)
    }

    override fun visitEmpty(empty: Tr.Empty): Tree = empty.transformIfNecessary(cursor)

    override fun visitParentheses(parens: Tr.Parentheses): Tree {
        val expr = visit(parens.expr) as Expression
        
        return (if(expr !== parens.expr) {
            parens.copy(expr = expr)
        } else parens).transformIfNecessary(cursor)
    }

    override fun visitInstanceOf(instanceOf: Tr.InstanceOf): Tree {
        val expr = visit(instanceOf.expr) as Expression
        val clazz = visit(instanceOf.clazz) as Tree
        
        return (if(expr !== instanceOf.expr || clazz !== instanceOf.clazz) {
            instanceOf.copy(expr = expr, clazz = clazz)
        } else instanceOf).transformIfNecessary(cursor)
    }

    override fun visitNewArray(newArray: Tr.NewArray): Tree {
        val typeExpr = visit(newArray.typeExpr) as Expression
        val dimensions = newArray.dimensions.mapIfNecessary { visit(it) as Expression }
        val elements = newArray.elements.mapIfNecessary { visit(it) as Expression }
        
        return (if(typeExpr !== newArray.typeExpr || dimensions !== newArray.dimensions || elements !== newArray.elements) {
            newArray.copy(typeExpr = typeExpr, dimensions = dimensions, elements = elements)
        } else newArray).transformIfNecessary(cursor)
    }
    
    override fun visitArrayAccess(arrayAccess: Tr.ArrayAccess): Tree {
        val indexed = visit(arrayAccess.index) as Expression
        val index = visit(arrayAccess.index) as Expression
     
        return (if(indexed !== arrayAccess.indexed || index !== arrayAccess.index) {
            arrayAccess.copy(indexed = indexed, index = index)
        } else arrayAccess).transformIfNecessary(cursor)
    }

    override fun visitLambda(lambda: Tr.Lambda): Tree {
        val params = lambda.params.mapIfNecessary { visit(it) as Tr.VariableDecl }
        val body = visit(lambda.body) as Tree
        
        return (if(params !== lambda.params || body !== lambda.body) {
            lambda.copy(params = params, body = body)
        } else lambda).transformIfNecessary(cursor)
    }
}