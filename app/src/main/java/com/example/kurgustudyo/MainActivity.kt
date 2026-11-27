package com.example.kurgustudyo

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    KurguStudyoApp()
                }
            }
        }
    }
}

private fun splitScenes(text: String, title: String, format: String): List<String> {
    val lines = text
        .split("\n", ".", "!", "?")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    if (lines.isNotEmpty()) return lines

    val safeTitle = title.trim().ifBlank { "Yeni YouTube Videosu" }
    return listOf(
        "$safeTitle ($format) başlıyor.",
        "İlk sahnede merak uyandıran olay gelişiyor.",
        "Kapanışta izleyiciden yorum ve beğeni isteniyor."
    )
}

private fun applyVoiceProfile(tts: TextToSpeech?, format: String, sceneIndex: Int) {
    if (tts == null) return
    val (rate, pitch) = when (format) {
        "Bilgi Shorts" -> 1.08f to (1.03f + (sceneIndex % 2) * 0.02f)
        "Duygusal Hikaye" -> 0.92f to (0.95f + (sceneIndex % 2) * 0.02f)
        else -> 1.02f to (1.04f + (sceneIndex % 3) * 0.02f)
    }
    tts.setSpeechRate(rate.coerceIn(0.75f, 1.35f))
    tts.setPitch(pitch.coerceIn(0.8f, 1.25f))
}

@Composable
private fun rememberBitmapFromUri(uri: Uri?): ImageBitmap? {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = uri) {
        value = if (uri == null) {
            null
        } else {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source).asImageBitmap()
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri).asImageBitmap()
                }
            }.getOrNull()
        }
    }
    return bitmap
}

@Composable
private fun GeneratedCharacterSceneCanvas(
    sceneText: String,
    sceneIndex: Int,
    format: String,
    modifier: Modifier = Modifier
) {
    val lower = sceneText.lowercase(Locale("tr", "TR"))
    val isNightOrMystery = lower.contains("gece") ||
        lower.contains("karanlık") ||
        lower.contains("geçmiş") ||
        lower.contains("gizem") ||
        lower.contains("korku")

    val palette = when {
        isNightOrMystery -> listOf(
            Color(0xFF101528),
            Color(0xFF28193B),
            Color(0xFF7CE8FF),
            Color(0xFF4B3F72)
        )
        format == "Bilgi Shorts" -> listOf(
            Color(0xFF0F2438),
            Color(0xFF1A4359),
            Color(0xFFFFC857),
            Color(0xFF2EC4B6)
        )
        format == "Duygusal Hikaye" -> listOf(
            Color(0xFF2B192E),
            Color(0xFF5C2A47),
            Color(0xFFFF9EBB),
            Color(0xFF9D4EDD)
        )
        else -> listOf(
            Color(0xFF1B263B),
            Color(0xFF3D348B),
            Color(0xFFFFB300),
            Color(0xFF4EA8DE)
        )
    }

    val bgTop = palette[0]
    val bgBottom = palette[1]
    val accentColor = palette[2]
    val outfitColor = palette[3]

    val skinTone = Color(0xFFF1C29B)
    val hairTone = when (abs(sceneText.hashCode()) % 4) {
        0 -> Color(0xFF2B1B17)
        1 -> Color(0xFF6B3E26)
        2 -> Color(0xFFD4A359)
        else -> Color(0xFF1C2541)
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.verticalGradient(colors = listOf(bgTop, bgBottom)),
            size = Size(w, h)
        )

        drawCircle(
            color = accentColor.copy(alpha = 0.14f),
            radius = minOf(w, h) * 0.42f,
            center = Offset(w * 0.5f, h * 0.44f)
        )

        for (i in 0 until 12) {
            val px = (((i * 97 + sceneIndex * 53) % 100) / 100f) * w
            val py = (((i * 61 + sceneIndex * 37) % 70) / 100f) * h
            val r = if (i % 2 == 0) 3.2f else 2.0f
            drawCircle(
                color = accentColor.copy(alpha = 0.45f),
                radius = r,
                center = Offset(px, py)
            )
        }

        drawRect(
            color = Color.Black.copy(alpha = 0.35f),
            topLeft = Offset(0f, h * 0.78f),
            size = Size(w, h * 0.22f)
        )

        val centerX = w * 0.5f
        val headCenterY = h * 0.36f
        val headRadius = minOf(w, h) * 0.15f

        val torsoPath = Path().apply {
            moveTo(centerX - headRadius * 1.9f, h * 0.85f)
            quadraticTo(
                centerX - headRadius * 1.6f,
                headCenterY + headRadius * 1.25f,
                centerX,
                headCenterY + headRadius * 1.25f
            )
            quadraticTo(
                centerX + headRadius * 1.6f,
                headCenterY + headRadius * 1.25f,
                centerX + headRadius * 1.9f,
                h * 0.85f
            )
            close()
        }
        drawPath(path = torsoPath, color = outfitColor)
        drawPath(
            path = torsoPath,
            color = accentColor.copy(alpha = 0.65f),
            style = Stroke(width = 2.5f)
        )

        drawRect(
            color = skinTone,
            topLeft = Offset(centerX - headRadius * 0.35f, headCenterY + headRadius * 0.82f),
            size = Size(headRadius * 0.7f, headRadius * 0.55f)
        )

        drawCircle(
            color = hairTone,
            radius = headRadius * 1.12f,
            center = Offset(centerX, headCenterY - headRadius * 0.08f)
        )

        drawCircle(
            color = skinTone,
            radius = headRadius,
            center = Offset(centerX, headCenterY)
        )

        val fringePath = Path().apply {
            moveTo(centerX - headRadius * 1.02f, headCenterY - headRadius * 0.1f)
            quadraticTo(
                centerX - headRadius * 0.35f,
                headCenterY - headRadius * 0.95f,
                centerX + headRadius * 0.25f,
                headCenterY - headRadius * 0.35f
            )
            quadraticTo(
                centerX + headRadius * 0.75f,
                headCenterY - headRadius * 0.85f,
                centerX + headRadius * 1.02f,
                headCenterY - headRadius * 0.1f
            )
            lineTo(centerX + headRadius * 1.02f, headCenterY - headRadius * 0.45f)
            lineTo(centerX - headRadius * 1.02f, headCenterY - headRadius * 0.45f)
            close()
        }
        drawPath(path = fringePath, color = hairTone)

        val eyeOffsetX = headRadius * 0.38f
        val eyeY = headCenterY + headRadius * 0.05f
        val eyeRadius = headRadius * 0.13f

        drawCircle(color = Color(0xFF1A1A1A), radius = eyeRadius, center = Offset(centerX - eyeOffsetX, eyeY))
        drawCircle(color = Color(0xFF1A1A1A), radius = eyeRadius, center = Offset(centerX + eyeOffsetX, eyeY))
        drawCircle(
            color = Color.White,
            radius = eyeRadius * 0.42f,
            center = Offset(centerX - eyeOffsetX - eyeRadius * 0.25f, eyeY - eyeRadius * 0.25f)
        )
        drawCircle(
            color = Color.White,
            radius = eyeRadius * 0.42f,
            center = Offset(centerX + eyeOffsetX - eyeRadius * 0.25f, eyeY - eyeRadius * 0.25f)
        )

        val mouthY = headCenterY + headRadius * 0.48f
        val mouthPath = Path().apply {
            moveTo(centerX - headRadius * 0.28f, mouthY)
            if (sceneIndex % 2 == 0) {
                quadraticTo(
                    centerX,
                    mouthY + headRadius * 0.22f,
                    centerX + headRadius * 0.28f,
                    mouthY
                )
            } else {
                quadraticTo(
                    centerX,
                    mouthY + headRadius * 0.08f,
                    centerX + headRadius * 0.28f,
                    mouthY
                )
            }
        }
        drawPath(
            path = mouthPath,
            color = Color(0xFF8D4A43),
            style = Stroke(width = 3f)
        )
    }
}

@Composable
fun KurguStudyoApp() {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val formats = listOf("Çizgi Hikaye", "Bilgi Shorts", "Duygusal Hikaye")
    var selectedFormat by remember { mutableStateOf(formats.first()) }
    var videoTitle by remember { mutableStateOf("") }
    var rawScript by remember { mutableStateOf("") }

    val scenes by remember(rawScript, videoTitle, selectedFormat) {
        derivedStateOf { splitScenes(rawScript, videoTitle, selectedFormat) }
    }

    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var ttsReady by remember { mutableStateOf(false) }
    var statusInfo by remember { mutableStateOf("Hazir - Otomatik karakter aktif") }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImages = uris
            statusInfo = "${uris.size} ozel gorsel baglandi"
        }
    }

    val tts = remember {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = engine?.setLanguage(Locale("tr", "TR"))
                if (res == TextToSpeech.LANG_MISSING_DATA ||
                    res == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    engine?.language = Locale.getDefault()
                }
                ttsReady = true
            }
        }
        engine
    }

    DisposableEffect(tts) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    fun playSceneAt(index: Int) {
        val safe = index.coerceIn(0, (scenes.size - 1).coerceAtLeast(0))
        currentIndex = safe
        val line = scenes.getOrElse(safe) { "" }
        statusInfo = "Sahne ${safe + 1}/${scenes.size} oynatiliyor"
        if (ttsReady && line.isNotBlank()) {
            applyVoiceProfile(tts, selectedFormat, safe)
            tts?.speak(line, TextToSpeech.QUEUE_FLUSH, null, "kurgu_$safe")
        }
    }

    fun stopAll() {
        isPlaying = false
        tts?.stop()
        statusInfo = "Durduruldu"
    }

    LaunchedEffect(isPlaying, currentIndex, scenes) {
        if (!isPlaying || scenes.isEmpty()) return@LaunchedEffect
        playSceneAt(currentIndex)
        val duration = (scenes[currentIndex].length * 68L).coerceIn(2600L, 9000L)
        delay(duration)
        if (isPlaying) {
            if (currentIndex + 1 < scenes.size) {
                currentIndex += 1
            } else {
                isPlaying = false
                statusInfo = "Tum sahneler tamamlandi"
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = {
                        stopAll()
                        selectedTab = 0
                    },
                    icon = { Icon(Icons.Rounded.EditNote, contentDescription = null) },
                    label = { Text("1. Senaryo") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        stopAll()
                        selectedTab = 1
                    },
                    icon = { Icon(Icons.Rounded.MovieCreation, contentDescription = null) },
                    label = { Text("2. Kurgu") }
                )
            }
        }
    ) { innerPadding ->
        if (selectedTab == 0) {
            SenaryoTab(
                modifier = Modifier.padding(innerPadding),
                formats = formats,
                selectedFormat = selectedFormat,
                onFormatSelect = { selectedFormat = it },
                title = videoTitle,
                onTitleChange = { videoTitle = it },
                script = rawScript,
                onScriptChange = { rawScript = it },
                scenes = scenes,
                onSendToEditor = {
                    currentIndex = 0
                    selectedTab = 1
                }
            )
        } else {
            KurguTab(
                modifier = Modifier.padding(innerPadding),
                title = videoTitle,
                format = selectedFormat,
                scenes = scenes,
                selectedImages = selectedImages,
                currentIndex = currentIndex,
                isPlaying = isPlaying,
                statusInfo = statusInfo,
                onPickImages = { imagePicker.launch("image/*") },
                onPlayAll = {
                    currentIndex = 0
                    isPlaying = true
                },
                onNext = {
                    isPlaying = false
                    val next = if (currentIndex + 1 < scenes.size) currentIndex + 1 else 0
                    playSceneAt(next)
                },
                onStop = { stopAll() },
                onSelectScene = { idx ->
                    isPlaying = false
                    playSceneAt(idx)
                }
            )
        }
    }
}

@Composable
private fun SenaryoTab(
    modifier: Modifier = Modifier,
    formats: List<String>,
    selectedFormat: String,
    onFormatSelect: (String) -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
    script: String,
    onScriptChange: (String) -> Unit,
    scenes: List<String>,
    onSendToEditor: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Senaryo & Sahne Hazirlayici",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(formats) { item ->
                FilterChip(
                    selected = selectedFormat == item,
                    onClick = { onFormatSelect(item) },
                    label = { Text(item) }
                )
            }
        }

        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("YouTube Video Basligi") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = script,
            onValueChange = onScriptChange,
            label = { Text("Kurgunu yaz (her cumle otomatik sahne olur)") },
            minLines = 5,
            maxLines = 9,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Olusan Sahne Kartlari (${scenes.size})",
            style = MaterialTheme.typography.titleMedium
        )

        scenes.forEachIndexed { index, line ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Sahne ${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFFFB300)
                    )
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Button(
            onClick = onSendToEditor,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("2. Sekmeye (Kurguya) Aktar")
        }
    }
}

@Composable
private fun KurguTab(
    modifier: Modifier = Modifier,
    title: String,
    format: String,
    scenes: List<String>,
    selectedImages: List<Uri>,
    currentIndex: Int,
    isPlaying: Boolean,
    statusInfo: String,
    onPickImages: () -> Unit,
    onPlayAll: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit,
    onSelectScene: (Int) -> Unit
) {
    val amber = Color(0xFFFFB300)
    val currentLine = scenes.getOrElse(currentIndex) { "" }
    val linkedUri = if (selectedImages.isNotEmpty()) {
        selectedImages[currentIndex % selectedImages.size]
    } else {
        null
    }
    val sceneBitmap = rememberBitmapFromUri(linkedUri)

    val targetScale = if (isPlaying) {
        if (currentIndex % 2 == 0) 1.14f else 1.07f
    } else {
        1.03f
    }
    val targetPanX = if (isPlaying) {
        if (currentIndex % 2 == 0) 26f else -26f
    } else {
        0f
    }

    val motionScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
        label = "motionScale"
    )
    val motionPanX by animateFloatAsState(
        targetValue = targetPanX,
        animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
        label = "motionPanX"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MiniKurgu Studyo",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            FilledTonalButton(onClick = onPickImages) {
                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Gorsel Sec (${selectedImages.size})")
            }
        }

        Card(
            border = BorderStroke(1.dp, amber),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF181A20)),
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (sceneBitmap != null) {
                    Image(
                        bitmap = sceneBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = motionScale
                                scaleY = motionScale
                                translationX = motionPanX
                            }
                    )
                } else {
                    GeneratedCharacterSceneCanvas(
                        sceneText = currentLine,
                        sceneIndex = currentIndex,
                        format = format,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = motionScale
                                scaleY = motionScale
                                translationX = motionPanX
                            }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.78f)
                                )
                            )
                        )
                )

                Text(
                    text = "$format - Sahne ${currentIndex + 1}/${scenes.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = amber,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                ) {
                    if (linkedUri == null) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    val badgeLabel = if (linkedUri != null) {
                        "Gorsel Bagli (${(currentIndex % selectedImages.size) + 1}/${selectedImages.size})"
                    } else {
                        "Karakter Uretildi - ${title.ifBlank { format }}"
                    }
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.74f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = currentLine,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Text(
            text = "Zaman Cizelgesi (Sahneler)",
            style = MaterialTheme.typography.titleMedium
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(scenes) { index, _ ->
                FilterChip(
                    selected = index == currentIndex,
                    onClick = { onSelectScene(index) },
                    label = { Text("Sahne ${index + 1}") }
                )
            }
        }

        Text(
            text = "Durum: $statusInfo",
            style = MaterialTheme.typography.bodySmall,
            color = amber
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onPlayAll,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Oynat")
            }

            OutlinedButton(
                onClick = onNext,
                modifier = Modifier.height(50.dp)
            ) {
                Icon(Icons.Rounded.SkipNext, contentDescription = null)
                Spacer(Modifier.width(2.dp))
                Text("Sonraki")
            }

            OutlinedButton(
                onClick = onStop,
                modifier = Modifier.height(50.dp)
            ) {
                Icon(Icons.Rounded.Stop, contentDescription = null)
                Spacer(Modifier.width(2.dp))
                Text("Durdur")
            }
        }
    }
}
