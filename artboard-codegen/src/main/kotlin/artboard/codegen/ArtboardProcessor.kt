package artboard.codegen

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate
import java.io.OutputStreamWriter

/** Discovers eligible Compose `@Preview` functions and emits the gallery registry and report. */
class ArtboardProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    options: Map<String, String>,
) : SymbolProcessor {

    private val packageName = options[PACKAGE_OPTION] ?: "artboard.generated"
    private val projectPath = options[PROJECT_PATH_OPTION] ?: ":"
    private val seenFunctions = mutableSetOf<String>()
    private val unresolvedFunctions = linkedMapOf<String, String>()
    private val frames = linkedMapOf<String, FrameInfo>()
    private val excluded = mutableListOf<ExcludedInfo>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val candidates = PREVIEW_ANNOTATIONS
            .asSequence()
            .flatMap { resolver.getSymbolsWithAnnotation(it).asSequence() }
            .filterIsInstance<KSFunctionDeclaration>()
            .distinctBy { it.qualifiedName?.asString() ?: it.location.toString() }
            .toList()

        val deferred = mutableListOf<KSAnnotated>()
        for (function in candidates) {
            val key = function.qualifiedName?.asString() ?: function.location.toString()
            if (key in seenFunctions) continue
            if (!function.validate()) {
                unresolvedFunctions[key] = function.qualifiedName?.asString()
                    ?: function.simpleName.asString()
                deferred += function
                continue
            }

            unresolvedFunctions.remove(key)
            seenFunctions += key
            processFunction(function)
        }
        return deferred
    }

    override fun finish() {
        unresolvedFunctions.values.forEach { source ->
            excluded += ExcludedInfo(source, source.substringAfterLast('.'), "skipped", "unresolved")
            logger.warn("Artboard: skipping unresolved preview candidate $source")
        }
        val sortedFrames = frames.values.sortedBy(FrameInfo::id)
        val sortedExcluded = excluded.sortedWith(compareBy(ExcludedInfo::sourceFqName, ExcludedInfo::previewName))
        writeRegistry(sortedFrames)
        writeReport(sortedFrames, sortedExcluded)
        logger.info(
            "Artboard: generated ${sortedFrames.size} frame(s); " +
                "${sortedExcluded.size} candidate(s) excluded in $packageName",
        )
    }

    private fun processFunction(function: KSFunctionDeclaration) {
        val fqName = function.qualifiedName?.asString()
        val previewAnnotations = function.annotations
            .filter { it.qualifiedName() in PREVIEW_ANNOTATIONS }
            .toList()
        if (previewAnnotations.isEmpty()) return

        val reason = ineligibleReason(function, fqName)
        val ignored = function.hasArtboardSuppression() || function.containingFile?.hasArtboardSuppression() == true
        if (ignored || reason != null) {
            val status = if (ignored) "ignored" else "skipped"
            val exclusionReason = if (ignored) SUPPRESSION else reason.orEmpty()
            previewAnnotations.forEach { preview ->
                val previewName = preview.effectiveName(function.simpleName.asString())
                excluded += ExcludedInfo(
                    sourceFqName = fqName ?: function.simpleName.asString(),
                    previewName = previewName,
                    status = status,
                    reason = exclusionReason,
                )
            }
            if (!ignored) {
                logger.warn("Artboard: skipping ${fqName ?: function.simpleName.asString()}: $exclusionReason")
            }
            return
        }

        checkNotNull(fqName)
        previewAnnotations.forEach { preview ->
            val name = preview.effectiveName(function.simpleName.asString())
            val id = "$fqName::$name"
            if (frames.containsKey(id)) {
                logger.error(
                    "Artboard: duplicate frame id '$id'. Give repeatable @Preview annotations unique names.",
                    function,
                )
                excluded += ExcludedInfo(fqName, name, "skipped", "duplicateId")
                return@forEach
            }

            frames[id] = FrameInfo(
                id = id,
                name = name,
                group = preview.argString("group")?.takeIf(String::isNotBlank),
                kind = inferKind(function.simpleName.asString(), name),
                widthDp = preview.argInt("widthDp")?.takeIf { it > 0 },
                heightDp = preview.argInt("heightDp")?.takeIf { it > 0 },
                sourceFqName = fqName,
                callExpression = kotlinCallableReference(
                    packageName = function.packageName.asString(),
                    functionName = function.simpleName.asString(),
                ),
            )
        }
    }

    private fun ineligibleReason(function: KSFunctionDeclaration, fqName: String?): String? = when {
        fqName == null || function.parentDeclaration != null -> "notTopLevel"
        Modifier.PRIVATE in function.modifiers -> "private"
        function.parameters.isNotEmpty() -> "hasParameters"
        function.extensionReceiver != null -> "extensionFunction"
        Modifier.SUSPEND in function.modifiers -> "suspendFunction"
        function.typeParameters.isNotEmpty() -> "genericFunction"
        else -> null
    }

    private fun writeRegistry(sortedFrames: List<FrameInfo>) {
        val stream = codeGenerator.createNewFile(
            dependencies = Dependencies.ALL_FILES,
            packageName = packageName,
            fileName = "GeneratedArtboardRegistry",
        )
        OutputStreamWriter(stream, Charsets.UTF_8).use { out ->
            out.appendLine("// Generated by artboard-codegen — do not edit.")
            out.appendLine("package $packageName")
            out.appendLine()
            out.appendLine("import artboard.model.PreviewFrame")
            out.appendLine("import artboard.model.PreviewKind")
            out.appendLine("import artboard.registry.ArtboardRegistry")
            out.appendLine()
            out.appendLine("/** Auto-discovered Compose preview frames. */")
            out.appendLine("object GeneratedArtboardRegistry : ArtboardRegistry {")
            out.appendLine("    override val frames: List<PreviewFrame> = listOf(")
            sortedFrames.forEachIndexed { index, frame ->
                val comma = if (index == sortedFrames.lastIndex) "" else ","
                out.appendLine("        PreviewFrame(")
                out.appendLine("            id = ${frame.id.kotlinQuote()},")
                out.appendLine("            name = ${frame.name.kotlinQuote()},")
                out.appendLine("            group = ${frame.group?.kotlinQuote() ?: "null"},")
                out.appendLine("            kind = PreviewKind.${frame.kind},")
                out.appendLine("            widthDp = ${frame.widthDp ?: "null"},")
                out.appendLine("            heightDp = ${frame.heightDp ?: "null"},")
                out.appendLine("            sourceFqName = ${frame.sourceFqName.kotlinQuote()},")
                out.appendLine("            content = { ${frame.callExpression}() },")
                out.appendLine("        )$comma")
            }
            out.appendLine("    )")
            out.appendLine("}")
        }
    }

    private fun writeReport(sortedFrames: List<FrameInfo>, sortedExcluded: List<ExcludedInfo>) {
        val stream = codeGenerator.createNewFile(
            dependencies = Dependencies.ALL_FILES,
            packageName = "",
            fileName = REPORT_FILE_NAME,
            extensionName = "json",
        )
        OutputStreamWriter(stream, Charsets.UTF_8).use { out ->
            out.appendLine("{")
            out.appendLine("  \"schemaVersion\": 1,")
            out.appendLine("  \"projectPath\": ${projectPath.jsonQuote()},")
            out.appendLine("  \"generatedPackage\": ${packageName.jsonQuote()},")
            out.appendLine("  \"frames\": [")
            sortedFrames.forEachIndexed { index, frame ->
                val comma = if (index == sortedFrames.lastIndex) "" else ","
                out.append("    {\"id\": ${frame.id.jsonQuote()}, \"name\": ${frame.name.jsonQuote()}, ")
                out.append("\"group\": ${frame.group?.jsonQuote() ?: "null"}, \"kind\": ${frame.kind.jsonQuote()}, ")
                out.append("\"widthDp\": ${frame.widthDp ?: "null"}, \"heightDp\": ${frame.heightDp ?: "null"}, ")
                out.appendLine("\"sourceFqName\": ${frame.sourceFqName.jsonQuote()}}$comma")
            }
            out.appendLine("  ],")
            out.appendLine("  \"excluded\": [")
            sortedExcluded.forEachIndexed { index, candidate ->
                val comma = if (index == sortedExcluded.lastIndex) "" else ","
                out.append("    {\"sourceFqName\": ${candidate.sourceFqName.jsonQuote()}, ")
                out.append("\"previewName\": ${candidate.previewName.jsonQuote()}, ")
                out.append("\"status\": ${candidate.status.jsonQuote()}, ")
                out.appendLine("\"reason\": ${candidate.reason.jsonQuote()}}$comma")
            }
            out.appendLine("  ]")
            out.appendLine("}")
        }
    }

    private data class FrameInfo(
        val id: String,
        val name: String,
        val group: String?,
        val kind: String,
        val widthDp: Int?,
        val heightDp: Int?,
        val sourceFqName: String,
        val callExpression: String,
    )

    private data class ExcludedInfo(
        val sourceFqName: String,
        val previewName: String,
        val status: String,
        val reason: String,
    )

    private companion object {
        const val PACKAGE_OPTION = "artboard.package"
        const val PROJECT_PATH_OPTION = "artboard.projectPath"
        const val REPORT_FILE_NAME = "artboard-previews"
        const val SUPPRESSION = "ARTBOARD_PREVIEW"
        val PREVIEW_ANNOTATIONS = setOf(
            "androidx.compose.ui.tooling.preview.Preview",
            "org.jetbrains.compose.ui.tooling.preview.Preview",
        )
    }
}

/** KSP entry point for [ArtboardProcessor]. */
class ArtboardProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        ArtboardProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
            options = environment.options,
        )
}

private fun KSAnnotation.qualifiedName(): String? =
    annotationType.resolve().declaration.qualifiedName?.asString()

private fun KSAnnotation.effectiveName(functionName: String): String =
    argString("name")?.takeIf(String::isNotBlank) ?: functionName

private fun KSAnnotated.hasArtboardSuppression(): Boolean = annotations
    .filter { it.qualifiedName() == "kotlin.Suppress" }
    .flatMap { annotation ->
        annotation.arguments.asSequence().flatMap { argument ->
            when (val value = argument.value) {
                is List<*> -> value.asSequence().filterIsInstance<String>()
                is String -> sequenceOf(value)
                else -> emptySequence()
            }
        }
    }
    .any { it == "ARTBOARD_PREVIEW" }

private fun KSAnnotation.argString(name: String): String? =
    arguments.findArg(name)?.value as? String

private fun KSAnnotation.argInt(name: String): Int? = when (val value = arguments.findArg(name)?.value) {
    is Int -> value
    is Short -> value.toInt()
    is Long -> value.toInt()
    is Byte -> value.toInt()
    else -> null
}

private fun List<KSValueArgument>.findArg(name: String): KSValueArgument? =
    firstOrNull { it.name?.asString() == name }

internal fun inferKind(functionName: String, previewName: String): String {
    val screenTokens = setOf("screen", "page", "route", "scaffold")
    val segments = splitNameSegments(functionName) + splitNameSegments(previewName)
    return if (segments.any { it in screenTokens }) "Screen" else "Component"
}

private fun splitNameSegments(raw: String): List<String> = raw
    .split(Regex("[^A-Za-z]+"))
    .filter(String::isNotEmpty)
    .flatMap { token ->
        token.replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
            .replace(Regex("([A-Z]+)([A-Z][a-z])"), "$1 $2")
            .split(' ')
    }
    .map(String::lowercase)
    .filter { it.isNotEmpty() && it != "preview" }

internal fun String.kotlinQuote(): String = escapedQuote(escapeDollar = true)

internal fun String.jsonQuote(): String = escapedQuote(escapeDollar = false)

internal fun kotlinCallableReference(packageName: String, functionName: String): String =
    (packageName.split('.').filter(String::isNotEmpty) + functionName)
        .joinToString(".") { it.kotlinIdentifier() }

private fun String.kotlinIdentifier(): String =
    if (matches(KOTLIN_IDENTIFIER) && this !in KOTLIN_KEYWORDS) this else "`$this`"

private fun String.escapedQuote(escapeDollar: Boolean): String = buildString {
    append('"')
    for (character in this@escapedQuote) {
        when (character) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            '\b' -> append("\\b")
            '\u000C' -> append(if (escapeDollar) "\\u000c" else "\\f")
            '$' -> if (escapeDollar) append("\\$") else append(character)
            else -> if (character.code < 0x20) {
                append("\\u")
                append(character.code.toString(16).padStart(4, '0'))
            } else {
                append(character)
            }
        }
    }
    append('"')
}

private val KOTLIN_IDENTIFIER = Regex("[A-Za-z_][A-Za-z0-9_]*")

private val KOTLIN_KEYWORDS = setOf(
    "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in",
    "interface", "is", "null", "object", "package", "return", "super", "this", "throw", "true",
    "try", "typealias", "typeof", "val", "var", "when", "while",
)
