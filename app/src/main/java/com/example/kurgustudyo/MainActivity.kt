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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Locale

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
    var statusInfo by remember { mutableStateOf("Hazır") }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImages = uris
            statusInfo = "${uris.size} görsel sahnelere bağlandı"
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
        statusInfo = "Sahne ${safe + 1}/${scenes.size} oynatılıyor"
        if (ttsReady && line.isNotBlank()) {
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
        val duration = (scenes[currentIndex].length * 65L).coerceIn(2500L, 8500L)
        delay(duration)
        if (isPlaying) {
            if (currentIndex + 1 < scenes.size) {
                currentIndex += 1
            } else {
                isPlaying = false
                statusInfo = "Tüm sahneler tamamlandı"
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
            text = "Senaryo & Sahne Hazırlayıcı",
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
            label = { Text("YouTube Video Başlığı") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = script,
            onValueChange = onScriptChange,
            label = { Text("Kurgunu yaz (her cümle otomatik sahne olur)") },
            minLines = 5,
            maxLines = 9,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Oluşan Sahne Kartları (${scenes.size})",
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
            Text("2. Sekmeye (Kurguya) Aktar →")
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
    val linkedUri = if (selectedImages.isNotEmpty()) {
        selectedImages[currentIndex % selectedImages.size]
    } else {
        null
    }
    val sceneBitmap = rememberBitmapFromUri(linkedUri)

    val targetScale = if (isPlaying) {
        if (currentIndex % 2 == 0) 1.12f else 1.06f
    } else {
        1.02f
    }
    val targetPanX = if (isPlaying) {
        if (currentIndex % 2 == 0) 24f else -24f
    } else {
        0f
    }

    val motionScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
        label = "motionScale"
    )
    val motionPanX by animateFloatAsState(
        targetValue = targetPanX,
        animationSpec = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
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
                text = "MiniKurgu Stüdyo",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            FilledTonalButton(onClick = onPickImages) {
                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Görsel Seç (${selectedImages.size})")
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.45f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.75f)
                                    )
                                )
                            )
                    )
                }

                Text(
                    text = "$format • Sahne ${currentIndex + 1}/${scenes.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = amber,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                )

                Text(
                    text = if (linkedUri != null) "🖼️ Görsel Bağlı (${(currentIndex % selectedImages.size) + 1}/${selectedImages.size})"
                    else "🎬 ${title.ifBlank { "Sahne Önizleme" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = scenes.getOrElse(currentIndex) { "" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Text(
            text = "Zaman Çizelgesi (Sahneler)",
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
