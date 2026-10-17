package com.pyrzz.jside

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay

private val Bg = Color(0xFF111318)
private val Panel = Color(0xFF191C23)
private val Panel2 = Color(0xFF20242D)
private val Border = Color(0xFF303642)
private val TextMain = Color(0xFFE4E8EF)
private val TextMuted = Color(0xFF9AA4B2)
private val Accent = Color(0xFF80CBC4)
private val Purple = Color(0xFFC792EA)
private val Yellow = Color(0xFFFFCB6B)
private val Green = Color(0xFFC3E88D)
private val Blue = Color(0xFF82AAFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Accent,
                background = Bg,
                surface = Panel,
                onBackground = TextMain,
                onSurface = TextMain
            )) {
                Surface(modifier = Modifier.fillMaxSize(), color = Bg) { IdeShell() }
            }
        }
    }
}

@Composable
private fun IdeShell() {
    val context = LocalContext.current
    val projectDir = remember {
        File(context.filesDir, "projects/JSProject").apply {
            mkdirs()
            val index = File(this, "Index.js")
            if (!index.exists()) index.writeText(
                "// Welcome to JS/TS Node IDE\n" +
                "// Project files are saved in the app's private storage.\n\n" +
                "function greet(name) {\n  return `Hello, ${'$'}{name}!`;\n}\n\n" +
                "console.log(greet('JavaScript'));\n"
            )
            val packageJson = File(this, "package.json")
            if (!packageJson.exists()) packageJson.writeText(
                "{\n  \"name\": \"js-ts-project\",\n  \"version\": \"1.0.0\",\n  \"type\": \"commonjs\",\n  \"scripts\": {\n    \"start\": \"node Index.js\"\n  },\n  \"dependencies\": {}\n}\n"
            )
            val tsconfig = File(this, "tsconfig.json")
            if (!tsconfig.exists()) tsconfig.writeText(
                "{\n  \"compilerOptions\": {\n    \"target\": \"ES2022\",\n    \"module\": \"commonjs\",\n    \"strict\": true\n  }\n}\n"
            )
        }
    }
    var refreshFiles by remember { mutableIntStateOf(0) }
    val files = remember(refreshFiles) { projectDir.listFiles()?.filter { it.isFile }?.sortedBy { it.name } ?: emptyList() }
    var activeName by remember { mutableStateOf(files.firstOrNull()?.name ?: "Index.js") }
    var source by remember { mutableStateOf(File(projectDir, activeName).takeIf { it.exists() }?.readText().orEmpty()) }
    var terminalOpen by remember { mutableStateOf(true) }
    var explorerOpen by remember { mutableStateOf(true) }
    var newFileDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("NewFile.js") }
    var command by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Ready • local project") }
    val terminalLines = remember { mutableStateListOf("JS/TS Node IDE terminal", "Working directory: ${projectDir.absolutePath}", "This is the Android app's shell. Node.js is not bundled yet.") }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val shell = remember(projectDir.absolutePath) {
        ShellSession(projectDir) { line -> mainHandler.post {
            terminalLines.add(line)
            while (terminalLines.size > 300) terminalLines.removeAt(0)
        } }
    }
    DisposableEffect(shell) { onDispose { shell.close() } }
    LaunchedEffect(activeName, source) {
        delay(650)
        runCatching { File(projectDir, activeName).writeText(source) }
            .onSuccess { status = "Auto-saved $activeName" }
            .onFailure { status = "Auto-save failed: ${it.message}" }
    }

    fun saveCurrent() {
        runCatching { File(projectDir, activeName).writeText(source); status = "Saved ${activeName}" }
            .onFailure { status = "Save failed: ${it.message}" }
        refreshFiles++
    }
    fun openLocalFile(name: String) {
        saveCurrent()
        activeName = name
        source = runCatching { File(projectDir, name).readText() }.getOrElse { "" }
        status = "Opened $name"
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val displayName = runCatching {
                    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    }
                }.getOrNull()?.takeIf { it.isNotBlank() }
                    ?: uri.lastPathSegment?.substringAfterLast(':')?.takeIf { it.contains('.') }
                    ?: "Imported.js"
                val safeName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Could not read selected file")
                File(projectDir, safeName).writeText(text)
                refreshFiles++
                activeName = safeName
                source = text
                status = "Imported $safeName"
            }.onFailure { status = "Import failed: ${it.message}" }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri: Uri? ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(source) }
                ?: error("Could not write selected destination")
            status = "Exported $activeName"
        }.onFailure { status = "Export failed: ${it.message}" }
    }

    Column(Modifier.fillMaxSize().background(Bg).padding(WindowInsets.safeDrawing.asPaddingValues())) {
        Row(
            Modifier.fillMaxWidth().background(Panel).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text("JS / TS IDE", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("Android • Kotlin + Compose", color = TextMuted, fontSize = 10.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                SmallAction("Files") { explorerOpen = !explorerOpen }
                SmallAction("Save") { saveCurrent() }
                SmallAction("Run") {
                    saveCurrent()
                    shell.send("node ${shellQuote(activeName)}")
                    terminalOpen = true
                    status = "Sent to terminal (requires Node.js installed)"
                }
                SmallAction(if (terminalOpen) "Terminal −" else "Terminal +") { terminalOpen = !terminalOpen }
            }
        }
        Row(
            Modifier.fillMaxWidth().background(Panel2).horizontalScroll(rememberScrollState()).padding(horizontal = 7.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            files.forEach { file ->
                val selected = activeName == file.name
                Text(file.name, color = if (selected) Accent else TextMuted,
                    modifier = Modifier.padding(end = 5.dp).background(if (selected) Bg else Color.Transparent, RoundedCornerShape(6.dp))
                        .clickable { openLocalFile(file.name) }.padding(horizontal = 10.dp, vertical = 9.dp), fontSize = 11.sp)
            }
            Text("+", color = Accent, modifier = Modifier.clickable { newFileName = "NewFile.js"; newFileDialog = true }.padding(horizontal = 10.dp, vertical = 7.dp), fontSize = 18.sp)
        }
        if (explorerOpen) {
            Column(Modifier.fillMaxWidth().background(Panel).padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("EXPLORER  •  JSProject", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Import", color = Accent, fontSize = 11.sp, modifier = Modifier.clickable { importLauncher.launch(arrayOf("text/*", "application/json", "application/javascript")) })
                        Text("Export", color = Accent, fontSize = 11.sp, modifier = Modifier.clickable { saveCurrent(); exportLauncher.launch(activeName) })
                        Text("New", color = Accent, fontSize = 11.sp, modifier = Modifier.clickable { newFileName = "NewFile.js"; newFileDialog = true })
                    }
                }
                files.take(8).forEach { file ->
                    Text((if (file.name == activeName) "›  " else "   ") + file.name,
                        color = if (file.name == activeName) Accent else TextMain, fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth().clickable { openLocalFile(file.name) }.padding(top = 6.dp))
                }
                Text("Private app folder • import/export files to share", color = TextMuted, fontSize = 9.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
        Row(Modifier.fillMaxWidth().background(Bg).padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$activeName  •  ${languageLabel(activeName)}", color = TextMuted, fontSize = 10.sp)
            Text("UTF-8  •  LF", color = TextMuted, fontSize = 9.sp)
        }
        Row(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp)) {
            val lineCount = source.count { it == '\n' } + 1
            Column(Modifier.width(28.dp).padding(top = 4.dp), horizontalAlignment = Alignment.End) {
                repeat(lineCount.coerceAtMost(500)) { index ->
                    Text((index + 1).toString(), color = Color(0xFF606A78), fontSize = 11.sp, lineHeight = 19.sp, fontFamily = FontFamily.Monospace)
                }
            }
            BasicTextField(
                value = source,
                onValueChange = { source = it; status = "Modified • not saved" },
                modifier = Modifier.fillMaxSize().padding(start = 8.dp, top = 3.dp),
                textStyle = TextStyle(color = TextMain, fontSize = 12.sp, lineHeight = 19.sp, fontFamily = FontFamily.Monospace),
                cursorBrush = SolidColor(Accent),
                visualTransformation = CodeSyntaxTransformation,
                decorationBox = { inner -> Box { if (source.isEmpty()) Text("Start writing code…", color = TextMuted); inner() } }
            )
        }
        if (terminalOpen) {
            Divider(color = Border)
            Column(Modifier.fillMaxWidth().height(190.dp).background(Panel).padding(horizontal = 10.dp, vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("TERMINAL", color = Accent, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Text("/system/bin/sh", color = TextMuted, fontSize = 9.sp)
                }
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 6.dp)) {
                    SelectionContainer {
                        Text(terminalLines.takeLast(70).joinToString("\n"), color = Color(0xFFC5D0DD), fontSize = 10.sp, lineHeight = 15.sp, fontFamily = FontFamily.Monospace)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$", color = Accent, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(end = 6.dp))
                    BasicTextField(value = command, onValueChange = { command = it }, modifier = Modifier.weight(1f),
                        singleLine = true, textStyle = TextStyle(color = TextMain, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                        cursorBrush = SolidColor(Accent), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { if (command.isNotBlank()) { shell.send(command); command = "" } }))
                    Text("Run", color = Accent, modifier = Modifier.clickable { if (command.isNotBlank()) { shell.send(command); command = "" } }.padding(start = 12.dp, vertical = 6.dp), fontSize = 11.sp)
                }
            }
        }
        Row(Modifier.fillMaxWidth().background(Color(0xFF242A34)).padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(status, color = TextMuted, fontSize = 9.sp, maxLines = 1)
            Text("MVP • Node runtime pending", color = Yellow, fontSize = 9.sp)
        }
    }

    if (newFileDialog) {
        AlertDialog(
            onDismissRequest = { newFileDialog = false },
            title = { Text("Create file") },
            text = { OutlinedTextField(value = newFileName, onValueChange = { newFileName = it }, label = { Text("Filename") }, singleLine = true) },
            confirmButton = { TextButton(onClick = {
                val name = newFileName.trim().replace(Regex("[/\\\\:*?\"<>|]"), "_")
                if (name.isNotBlank()) {
                    val file = File(projectDir, name)
                    if (!file.exists()) file.writeText("")
                    refreshFiles++
                    activeName = name
                    source = runCatching { file.readText() }.getOrDefault("")
                    status = "Created $name"
                }
                newFileDialog = false
            }) { Text("Create") } },
            dismissButton = { TextButton(onClick = { newFileDialog = false }) { Text("Cancel") } }
        )
    }
}

private fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
private fun languageLabel(name: String): String = when {
    name.endsWith(".ts", true) || name.endsWith(".tsx", true) -> "TypeScript"
    name.endsWith(".json", true) -> "JSON"
    name.endsWith(".md", true) -> "Markdown"
    else -> "JavaScript"
}

@Composable
private fun SmallAction(label: String, onClick: () -> Unit) {
    Text(label, modifier = Modifier.background(Panel2, RoundedCornerShape(7.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 8.dp),
        color = TextMain, fontSize = 10.sp, maxLines = 1)
}

private object CodeSyntaxTransformation : VisualTransformation {
    private val keyword = Regex("\\b(const|let|var|function|return|if|else|for|while|class|new|import|from|export|default|async|await|try|catch|throw|typeof|instanceof|true|false|null|undefined|this)\\b")
    private val string = Regex("(\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|`(?:\\\\.|[^`\\\\])*`)")
    private val comment = Regex("//[^\\n]*|/\\*[\\s\\S]*?\\*/")
    private val number = Regex("\\b\\d+(?:\\.\\d+)?\\b")
    override fun filter(text: AnnotatedString): TransformedText {
        val out = buildAnnotatedString {
            append(text)
            fun paint(regex: Regex, color: Color, bold: Boolean = false) {
                regex.findAll(text).forEach { match ->
                    addStyle(androidx.compose.ui.text.SpanStyle(color = color, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal), match.range.first, match.range.last + 1)
                }
            }
            paint(keyword, Purple, true)
            paint(number, Yellow)
            paint(string, Green)
            paint(comment, Color(0xFF6A9955))
        }
        return TransformedText(out, OffsetMapping.Identity)
    }
}

/** A real Android shell process, scoped to this app's private project directory. It does not bundle Node.js. */
private class ShellSession(projectDir: File, private val onLine: (String) -> Unit) {
    private val alive = AtomicBoolean(true)
    private val process: Process?
    private val writer: OutputStreamWriter?

    init {
        var p: Process? = null
        var w: OutputStreamWriter? = null
        try {
            p = ProcessBuilder("/system/bin/sh").directory(projectDir).redirectErrorStream(true).apply {
                environment()["HOME"] = projectDir.absolutePath
                environment()["PWD"] = projectDir.absolutePath
                environment()["PATH"] = "/system/bin:/system/xbin"
            }.start()
            w = OutputStreamWriter(p.outputStream)
            val reader = BufferedReader(InputStreamReader(p.inputStream))
            Thread({
                try { while (alive.get()) { val line = reader.readLine() ?: break; onLine(line) } }
                catch (e: Exception) { if (alive.get()) onLine("Terminal read error: ${e.message}") }
            }, "ide-shell-reader").apply { isDaemon = true; start() }
            w.write("echo 'Shell ready'; pwd\n")
            w.flush()
        } catch (e: Exception) {
            onLine("Could not start shell: ${e.message}")
        }
        process = p
        writer = w
    }

    @Synchronized fun send(command: String) {
        if (command.isBlank()) return
        onLine("$ $command")
        try { writer?.write(command + "\n"); writer?.flush() }
        catch (e: Exception) { onLine("Command failed: ${e.message}") }
    }

    fun close() {
        alive.set(false)
        runCatching { writer?.write("exit\n"); writer?.flush(); writer?.close() }
        runCatching { process?.destroy() }
    }
}
