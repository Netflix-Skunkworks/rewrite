package com.netflix.java.refactor.refactor.op

//class ChangeMethodInvocation(signature: String, val tx: RefactorTransaction) : RefactorTreeVisitor() {
////    override fun scanner(): AstScanner<List<RefactorFix>> =
////            if (refactorTargetToStatic is String) {
////                IfThenScanner(ifFixesResultFrom = ChangeMethodInvocationScanner(this),
////                        then = arrayOf(
////                                AddImport(refactorTargetToStatic!!).scanner()
////                        ))
////            } else {
////                ChangeMethodInvocationScanner(this)
////            }
//
//    internal val matcher = MethodMatcher(signature)
//
//    internal var refactorName: String? = null
//    internal var refactorArguments: RefactorArguments? = null
//    internal var refactorTargetToStatic: String? = null
//    internal var refactorTargetToVariable: String? = null
//
//    fun changeName(name: String): ChangeMethodInvocation {
//        refactorName = name
//        return this
//    }
//
//    fun changeArguments(): RefactorArguments {
//        refactorArguments = RefactorArguments(this)
//        return refactorArguments!!
//    }
//
//    /**
//     * Change to a static method invocation on clazz
//     */
//    fun changeTarget(clazz: String): ChangeMethodInvocation {
//        refactorTargetToStatic = clazz
//        return this
//    }
//
//    /**
//     * Change to a static method invocation on clazz
//     */
//    fun changeTarget(clazz: Class<*>) = changeTarget(clazz.name)
//
//    /**
//     * Change the target to a named variable
//     */
//    fun changeTargetToVariable(variable: String): ChangeMethodInvocation {
//        refactorTargetToVariable = variable
//        return this
//    }
//
//    fun done() = tx
//
//    override fun visitMethodInvocation(meth: Tr.MethodInvocation): List<RefactorFix> {
//        if (matcher.matches(meth)) {
//            return refactorMethod(meth)
//        }
//        return emptyList()
//    }
//
//    fun refactorMethod(invocation: Tr.MethodInvocation): List<RefactorFix> {
//        val meth = invocation.select
//        val fixes = ArrayList<RefactorFix>()
//
//        if (refactorName is String) {
//            when (meth) {
//                is Tr.FieldAccess -> {
////                    val nameStart = meth.target.pos.endInclusive + 1
////                    fixes.add(RefactorFix(nameStart..nameStart + meth.name.length, refactorName!!, source))
//                }
//                is Tr.Ident -> meth.replace(refactorName!!)
//            }
//        }
//
//        if (refactorArguments is RefactorArguments) {
//            if (refactorArguments?.reorderArguments != null) {
//                val reorders = refactorArguments!!.reorderArguments!!
//                val paramNames = when(invocation.type) {
//                    is Type.Method -> invocation.type.paramTypes.map { it.asClass() }.filterNotNull().map { it.fullyQualifiedName }
//                    else -> null
//                }
//
//                if(paramNames != null) {
//                    var argPos = 0
//                    reorders.forEachIndexed { paramPos, reorder ->
//                        if (invocation.args.args.size <= argPos) {
//                            // this is a weird case, there are not enough arguments in the invocation to satisfy the reordering specification
//                            // TODO what to do?
//                            return@forEachIndexed
//                        }
//
//                        if (paramNames[paramPos] != reorder) {
//                            var swaps = invocation.args.args.filterIndexed { j, swap -> paramNames[Math.min(j, paramNames.size-1)] == reorder }
//
//                            // when no source is attached, we must define names first
//                            if(swaps.isEmpty()) {
//                                val pos = refactorArguments?.argumentNames?.indexOf(reorder) ?: -1
//                                if (pos >= 0 && pos < invocation.args.args.size) {
//                                    swaps = if(pos < refactorArguments!!.argumentNames!!.size - 1) {
//                                        listOf(invocation.args.args[pos])
//                                    } else {
//                                        // this is a varArgs argument, grab them all
//                                        invocation.args.args.drop(pos)
//                                    }
//                                }
//                            }
//
//                            swaps.forEach { swap: Expression ->
////                                fixes.add(invocation.args[argPos].replace(swap.changesToArgument(argPos) ?: swap.source()))
//                                argPos++
//                            }
//                        }
//                        else argPos++
//                    }
//                }
//                else {
//                    // TODO what do we do when the method symbol is not present?
//                }
//            } else {
//                invocation.args.args.forEachIndexed { i, arg ->
//                    arg.changesToArgument(i)?.let { changes ->
//                        fixes.add(arg.replace(changes))
//                    }
//                }
//            }
//
//            refactorArguments?.insertions?.forEach { insertion ->
////                if(invocation.args.isEmpty()) {
////                    val argStart = source.text.indexOf('(', invocation.select.pos.endInclusive) + 1
////                    fixes.add(insertAt(argStart, "${if(insertion.pos > 0) ", " else ""}${insertion.value}"))
////                }
////                else if(invocation.args.size <= insertion.pos) {
////                    fixes.add(insertAt(invocation.args.last().pos.endInclusive, ", ${insertion.value}"))
////                }
////                else {
////                    fixes.add(insertAt(invocation.args[insertion.pos].pos.start, "${insertion.value}, "))
////                }
//            }
//        }
//
//        if (refactorTargetToStatic is String) {
//            when (meth) {
//                is Tr.FieldAccess -> fixes.add(meth.target.replace(className(refactorTargetToStatic!!)))
//                is Tr.Ident -> fixes.add(meth.insertBefore(className(refactorTargetToStatic!! + ".")))
//            }
//        }
//
//        if (refactorTargetToVariable is String) {
//            when (meth) {
//                is Tr.FieldAccess -> fixes.add(meth.target.replace(refactorTargetToVariable!!))
//                is Tr.Ident -> fixes.add(meth.insertBefore(refactorTargetToVariable!! + "."))
//            }
//        }
//
//        return fixes
//    }
//
//    fun Expression.changesToArgument(pos: Int): String? {
//        val refactor = refactorArguments?.individualArgumentRefactors?.find { it.posConstraint == pos } ?:
//                refactorArguments?.individualArgumentRefactors?.find { this.type?.matches(it.typeConstraint) ?: false }
//
////        return if (refactor is RefactorArgument) {
////            val fixes = ChangeArgumentScanner(refactor).visit(this)
////
////            // aggregate all the fixes to this argument into one "change" replacement rule
////            return if (fixes.isNotEmpty()) {
////                val sortedFixes = fixes.sortedBy { it.position.last }.sortedBy { it.position.start }
////                var fixedArg = sortedFixes.foldIndexed("") { i, src, fix ->
////                    val prefix = if (i == 0)
////                        source.text.substring(this.pos.start, fix.position.start)
////                    else source.text.substring(sortedFixes[i - 1].position.last, fix.position.start)
////                    src + prefix + (fix.changes ?: "")
////                }
////                if (sortedFixes.last().position.last < source.text.length) {
////                    fixedArg += source.text.substring(sortedFixes.last().position.last, this.pos.endInclusive)
////                }
////
////                fixedArg
////            } else null
////        } else null
//
//        return null
//    }
//}
//
//class ChangeArgumentScanner(val refactor: RefactorArgument) : RefactorTreeVisitor() {
//
//    override fun visitLiteral(literal: Tr.Literal): List<RefactorFix> {
//        val value = literal.value.toString()
//
//        // prefix and suffix hold the special characters surrounding the values of primitive-ish types,
//        // e.g. the "" around String, the L at the end of a long, etc.
//        val valueMatcher = "(.*)${Pattern.quote(value)}(.*)".toRegex().find(literal.printTrimmed().replace("\\", ""))
//        return when(valueMatcher) {
//            is MatchResult -> {
//                val (prefix, suffix) = valueMatcher.groupValues.drop(1)
//
//                val transformed = refactor.refactorLiterals?.invoke(value) ?: value
//                if (transformed != value) listOf(literal.replace("$prefix$transformed$suffix")) else emptyList()
//            }
//            else -> {
//                // this should never happen
//                emptyList()
//            }
//        }
//    }
//}
//
//class RefactorArguments(val op: ChangeMethodInvocation) {
//    internal val individualArgumentRefactors = ArrayList<RefactorArgument>()
//    internal var reorderArguments: List<String>? = null
//    internal var argumentNames: List<String>? = null
//    internal val insertions = ArrayList<InsertArgument>()
//
//    fun arg(clazz: String): RefactorArgument {
//        val arg = RefactorArgument(this, typeConstraint = clazz)
//        individualArgumentRefactors.add(arg)
//        return arg
//    }
//
//    fun arg(clazz: Class<*>) = arg(clazz.name)
//
//    fun arg(pos: Int): RefactorArgument {
//        val arg = RefactorArgument(this, posConstraint = pos)
//        individualArgumentRefactors.add(arg)
//        return arg
//    }
//
//    fun whereArgNamesAre(vararg name: String): RefactorArguments {
//        this.argumentNames = name.toList()
//        return this
//    }
//
//    fun reorderByArgName(vararg name: String): RefactorArguments {
//        reorderArguments = name.toList()
//        return this
//    }
//
//    fun insert(pos: Int, value: String): RefactorArguments {
//        insertions.add(InsertArgument(pos, value))
//        return this
//    }
//
//    fun done() = op
//}
//
//data class InsertArgument(val pos: Int, val value: String)
//
//open class RefactorArgument(val op: RefactorArguments,
//                            val typeConstraint: String? = null,
//                            val posConstraint: Int? = null) {
//    internal var refactorLiterals: ((Any) -> Any)? = null
//
//    fun changeLiterals(transform: (Any) -> Any): RefactorArgument {
//        this.refactorLiterals = transform
//        return this
//    }
//
//    fun done() = op
//}