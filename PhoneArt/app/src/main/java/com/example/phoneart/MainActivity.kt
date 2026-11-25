package com.example.phoneart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoneart.ui.theme.PhoneArtTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private data class InkStroke(
    val points: List<Offset>,
    val color: Int,
    val width: Float
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhoneArtTheme {
                DrawingApp(
                    autosaveFile = File(filesDir, "current_drawing.json"),
                    artworkDirectory = File(filesDir, "artworks")
                )
            }
        }
    }
}

@Composable
private fun DrawingApp(autosaveFile: File, artworkDirectory: File) {
    val scope = rememberCoroutineScope()
    val strokes = remember { mutableStateListOf<InkStroke>() }
    var isLoading by remember { mutableStateOf(true) }
    val undone = remember { mutableStateListOf<InkStroke>() }
    var selectedColor by remember { mutableStateOf(Color(0xFF202124)) }
    var brushSize by remember { mutableStateOf(12f) }
    var eraser by remember { mutableStateOf(false) }
    var showGallery by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Your canvas is ready") }
    val palette = listOf(
        Color(0xFF202124), Color(0xFFE53935), Color(0xFFFF9800),
        Color(0xFFFFC107), Color(0xFF43A047), Color(0xFF00ACC1),
        Color(0xFF1E88E5), Color(0xFF8E24AA), Color(0xFFEC407A),
        Color(0xFF795548)
    )

    // Restore drawings off the UI thread so large saved files cannot stall app startup.
    LaunchedEffect(autosaveFile) {
        val restoredStrokes = withContext(Dispatchers.IO) { readDrawing(autosaveFile) }
        strokes.addAll(restoredStrokes)
        isLoading = false
    }

    // Keep a recoverable copy on private app storage whenever a stroke changes.
    LaunchedEffect(isLoading, strokes.size, strokes.lastOrNull()?.points?.size) {
        if (!isLoading) {
            val snapshot = strokes.toList()
            withContext(Dispatchers.IO) { writeDrawing(autosaveFile, snapshot) }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF4F1EC)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("PHONE ART", fontSize = 12.sp, letterSpacing = 2.sp,
                        color = Color(0xFF77716B), fontWeight = FontWeight.Bold)
                    Text("Make something", fontSize = 25.sp,
                        color = Color(0xFF25231F), fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = { showGallery = true }) { Text("My art") }
            }
            Surface(shape = RoundedCornerShape(20.dp), color = Color.White,
                shadowElevation = 2.dp, modifier = Modifier.weight(1f).fillMaxWidth()) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                        .padding(3.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .pointerInput(eraser, selectedColor, brushSize, isLoading) {
                            detectDragGestures(
                                onDragStart = { point ->
                                    if (isLoading) return@detectDragGestures
                                    undone.clear()
                                    strokes.add(InkStroke(listOf(point), if (eraser) Color.White.toArgbValue() else selectedColor.toArgbValue(), brushSize))
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val previous = strokes.lastOrNull() ?: return@detectDragGestures
                                    strokes[strokes.lastIndex] = previous.copy(
                                        points = previous.points + change.position
                                    )
                                }
                            )
                        }
                ) {
                    strokes.forEach { stroke ->
                        val points = stroke.points
                        if (points.size == 1) {
                            drawCircle(Color(stroke.color), stroke.width / 2f, points.first())
                        } else if (points.size > 1) {
                            val path = Path().apply {
                                moveTo(points.first().x, points.first().y)
                                points.drop(1).forEach { lineTo(it.x, it.y) }
                            }
                            drawPath(path, Color(stroke.color),
                                style = Stroke(width = stroke.width, cap = StrokeCap.Round))
                        }
                    }
                }
            }
            Text(
                if (isLoading) "Restoring your drawing…" else status,
                fontSize = 12.sp,
                color = Color(0xFF77716B)
            )
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("COLOR", fontSize = 11.sp, letterSpacing = 1.3.sp,
                    fontWeight = FontWeight.Bold, color = Color(0xFF77716B))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    palette.forEach { color ->
                        Box(
                            modifier = Modifier.size(if (selectedColor == color && !eraser) 34.dp else 29.dp)
                                .clip(CircleShape).background(color)
                                .then(if (selectedColor == color && !eraser)
                                    Modifier.border(2.dp, Color.White, CircleShape)
                                        .border(1.dp, Color(0xFF47423D), CircleShape)
                                else Modifier)
                                .clickable {
                                    selectedColor = color
                                    eraser = false
                                }
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("SIZE", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp, color = Color(0xFF77716B))
                Slider(value = brushSize, onValueChange = { brushSize = it },
                    valueRange = 3f..36f, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                Text("${brushSize.toInt()}", fontSize = 13.sp, color = Color(0xFF514C46))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = {
                    eraser = !eraser
                    status = if (eraser) "Eraser selected" else "Brush selected"
                }, modifier = Modifier.weight(1f)) { Text(if (eraser) "✎ Brush" else "⌫ Erase") }
                OutlinedButton(onClick = {
                    if (strokes.isNotEmpty()) undone.add(strokes.removeAt(strokes.lastIndex))
                    status = "Last stroke undone"
                }, modifier = Modifier.weight(1f)) { Text("↶ Undo") }
                OutlinedButton(onClick = {
                    strokes.clear()
                    undone.clear()
                    status = "Canvas cleared"
                }, modifier = Modifier.weight(1f)) { Text("Clear") }
                Button(onClick = {
                    artworkDirectory.mkdirs()
                    val file = File(artworkDirectory, "Artwork_${System.currentTimeMillis()}.json")
                    // Save a named copy so it remains available independently of the autosave.
                    scope.launch(Dispatchers.IO) { writeDrawing(file, strokes.toList()) }
                    status = "Artwork saved to My art"
                }, modifier = Modifier.weight(1.15f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6B55))) {
                    Text("Save")
                }
            }
            Spacer(Modifier.height(1.dp))
        }
    }

    if (showGallery) {
        val savedFiles = remember(showGallery, status) {
            artworkDirectory.listFiles { file -> file.extension == "json" }
                ?.sortedByDescending { it.lastModified() } ?: emptyList()
        }
        AlertDialog(
            onDismissRequest = { showGallery = false },
            title = { Text("My artwork") },
            text = {
                if (savedFiles.isEmpty()) {
                    Text("Saved drawings will appear here.")
                } else {
                    LazyColumn(modifier = Modifier.height(280.dp)) {
                        items(savedFiles, key = { it.name }) { file ->
                            Row(verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                                Text(file.nameWithoutExtension.replace('_', ' '),
                                    modifier = Modifier.weight(1f), fontSize = 14.sp)
                                TextButton(onClick = {
                                    scope.launch {
                                        val drawing = withContext(Dispatchers.IO) { readDrawing(file) }
                                        strokes.clear()
                                        strokes.addAll(drawing)
                                        showGallery = false
                                        status = "Opened ${file.nameWithoutExtension}"
                                    }
                                }) { Text("Open") }
                                TextButton(onClick = {
                                    file.delete()
                                    status = "Artwork deleted"
                                }) { Text("Delete") }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showGallery = false }) { Text("Done") } }
        )
    }
}

private fun Color.toArgbValue(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()
)

private fun writeDrawing(file: File, drawing: List<InkStroke>) {
    runCatching {
        file.parentFile?.mkdirs()
        val json = JSONArray()
        drawing.forEach { stroke ->
            val points = JSONArray()
            stroke.points.forEach { point ->
                points.put(JSONArray().put(point.x.toDouble()).put(point.y.toDouble()))
            }
            json.put(JSONObject().put("color", stroke.color)
                .put("width", stroke.width.toDouble()).put("points", points))
        }
        file.writeText(json.toString())
    }
}

private fun readDrawing(file: File): List<InkStroke> = runCatching {
    if (!file.exists()) return emptyList()
    val json = JSONArray(file.readText())
    buildList {
        for (i in 0 until json.length()) {
            val stroke = json.getJSONObject(i)
            val pointsJson = stroke.getJSONArray("points")
            val points = buildList {
                for (j in 0 until pointsJson.length()) {
                    val point = pointsJson.getJSONArray(j)
                    add(Offset(point.getDouble(0).toFloat(), point.getDouble(1).toFloat()))
                }
            }
            add(InkStroke(points, stroke.getInt("color"), stroke.getDouble("width").toFloat()))
        }
    }
}.getOrDefault(emptyList())
