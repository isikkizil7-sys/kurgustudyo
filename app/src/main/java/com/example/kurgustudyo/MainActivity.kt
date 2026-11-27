package com.example.cizgistudyo

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.sin

data class StoryDraft(
    val category: String,
    val title: String,
    val characterName: String,
    val emotion: String,
    val scenes: List<String>
)

data class CartoonCharacter(
    val id: String,
    val name: String,
    val emoji: String,
    val voicePitch: Float,
    val voiceRate: Float,
    val accentColor: Color
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CartoonStudioApp()
                }
            }
        }
    }
}

private val sampleDrafts = listOf(
    StoryDraft(
        category = "Yunan Mitolojisi",
        title = "Hermes ve Kanatlı Ayakkabılar",
        characterName = "Hermes 🪽",
        emotion = "Heyecanlı",
        scenes = listOf(
            "Olimpos'un bulutları arasında rüzgar esiyor ve Hermes kanatlı ayakkabılarıyla gökyüzüne süzülüyor.",
            "Haberi vaktinde ulaştırmak için fırtınalı vadinin üzerinden hızla geçiyor.",
            "Görevini başarıyla tamamlayıp yıldızların altında neşeyle duruyor."
        )
    ),
    StoryDraft(
        category = "Dini Kıssalar",
        title = "Sabır ve Bereket Bahçesi",
        characterName = "Bilge 👤",
        emotion = "Bilgece",
        scenes = listOf(
            "Kurak bir vadide küçük bir fidan toprağa emanet ediliyor ve sabırla bekleniyor.",
            "Yağmur damlaları toprağı besledikçe umut yeşermeye başlıyor.",
            "Zamanla gölge veren bereketli bir ağaç herkesi bir araya getiriyor."
        )
    ),
    StoryDraft(
        category = "Çocuk Çizgi Film",
        title = "Minik Tilki ve Kayıp Yıldız",
        characterName = "Tilki 🦊",
        emotion = "Neşeli",
        scenes = listOf(
            "Minik Tilki ormanda parlayan kayıp bir yıldız parçası buluyor.",
            "Arkadaşlarıyla birlikte yıldızı gökyüzüne geri götürmek için patikada ilerliyor.",
            "Yıldız yerine kavuşunca bütün orman ışıl ışıl parlıyor."
        )
    )
)

private fun buildResearchScenes(topic: String, character: String): List<String> {
    val clean = topic.trim().ifBlank { "Gizemli Yolculuk" }
    return listOf(
        "$clean araştırması başlıyor: $character ilk ipucunu inceliyor.",
        "$clean hakkında yeni bir keşif yapılıyor ve heyecan artıyor.",
        "$clean yolculuğu unutulmaz bir dersle sona eriyor."
    )
}

private fun saveVideoProjectSummaryToMovies(context: Context, title: String, script: String): Uri? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
    val values = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, "${title.ifBlank { "cizgi_film" }}.mp4")
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CizgiStudyom")
    }
    val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
    uri?.let { targetUri ->
        context.contentResolver.openOutputStream(targetUri)?.use { out ->
            val info = "CizgiStudyom Projesi\nBaslik: $title\n\n$script".toByteArray()
            out.write(info)
        }
    }
    return uri
}

private fun shareToYouTube(context: Context, videoUri: Uri?, title: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "video/*"
        if (videoUri != null) {
            putExtra(Intent.EXTRA_STREAM, videoUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, "$title - Yeni çizgi film bölümü!")
        setPackage("com.google.android.youtube")
    }
    val fallbackIntent = Intent.createChooser(
        Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            if (videoUri != null) {
                putExtra(Intent.EXTRA_STREAM, videoUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            putExtra(Intent.EXTRA_TEXT, title)
        },
        "YouTube'da Paylaş"
    )
    runCatching { context.startActivity(intent) }.getOrElse { context.startActivity(fallbackIntent) }
}

@Composable
fun CartoonStudioApp() {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val characters = remember {
        mutableStateListOf(
            CartoonCharacter("c1", "Hermes 🪽", "🪽", 1.08f, 1.06f, Color(0xFFFFB300)),
            CartoonCharacter("c2", "Bilge 👤", "👤", 0.94f, 0.92f, Color(0xFF4FC3F7)),
            CartoonCharacter("c3", "Tilki 🦊", "🦊", 1.12f, 1.05f, Color(0xFFFF7043)),
            CartoonCharacter("c4", "Ejderha 🐉", "🐉", 0.88f, 0.96f, Color(0xFFAB47BC))
        )
    }

    val emotions = listOf("Neşeli", "Heyecanlı", "Hüzünlü", "Şaşkın", "Kızgın", "Bilgece", "Gizemli")

    var selectedCategory by remember { mutableStateOf("Tümü") }
    var researchQuery by remember { mutableStateOf("") }
    var storyTitle by remember { mutableStateOf(sampleDrafts.last().title) }
    var selectedCharacter by remember { mutableStateOf(characters[2]) }
    var selectedEmotion by remember { mutableStateOf(sampleDrafts.last().emotion) }
    var scenes by remember { mutableStateOf(sampleDrafts.last().scenes) }

    // Süre ayarları (her sahne kaç saniye sürsün)
    var sceneDurationSec by remember { mutableFloatStateOf(5f) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentSceneIndex by remember { mutableIntStateOf(0) }
    var elapsedInSceneMs by remember { mutableLongStateOf(0L) }
    var savedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var statusBanner by remember { mutableStateOf("Hazır taslak seçebilir veya sahne süresini ayarlayabilirsin.") }

    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale("tr", "TR")
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

    fun speakCurrentScene(index: Int) {
        val line = scenes.getOrElse(index) { "" }
        if (ttsReady && line.isNotBlank()) {
            tts?.setPitch(selectedCharacter.voicePitch)
            tts?.setSpeechRate(selectedCharacter.voiceRate)
            tts?.speak(line, TextToSpeech.QUEUE_FLUSH, null, "scene_$index")
        }
    }

    // Süre akışı döngüsü
    LaunchedEffect(isPlaying, currentSceneIndex, sceneDurationSec, scenes) {
        if (!isPlaying || scenes.isEmpty()) return@LaunchedEffect
        val totalMs = (sceneDurationSec * 1000L).toLong().coerceAtLeast(1500L)
        if (elapsedInSceneMs == 0L) {
            speakCurrentScene(currentSceneIndex)
        }
        while (isPlaying && elapsedInSceneMs < totalMs) {
            delay(50L)
            elapsedInSceneMs += 50L
        }
        if (isPlaying) {
            elapsedInSceneMs = 0L
            if (currentSceneIndex + 1 < scenes.size) {
                currentSceneIndex += 1
            } else {
                isPlaying = false
                statusBanner = "Tüm sahneler tamamlandı."
            }
        }
    }

    val sceneDurationMs = (sceneDurationSec * 1000f).coerceAtLeast(1500f)
    val progressFraction = (elapsedInSceneMs / sceneDurationMs).coerceIn(0f, 1f)
    val totalVideoSec = scenes.size * sceneDurationSec
    val currentTotalSec = (currentSceneIndex * sceneDurationSec) + (elapsedInSceneMs / 1000f)

    // Zamanla akan hareket ve dudak uyumu hesapları
    val timeSec = elapsedInSceneMs / 1000f
    val walkX = sin(timeSec * 2.2f) * if (isPlaying) 65f else 15f
    val hopY = kotlin.math.abs(sin(timeSec * 4.5f)) * if (isPlaying) -18f else -4f
    val mouthOpenFactor = if (isPlaying) ((sin(timeSec * 14f) + 1f) / 2f) else 0f
    val mouthLabel = when {
        !isPlaying -> "Kapalı (Sessiz)"
        mouthOpenFactor > 0.65f -> "Açık (Hece Vurgusu)"
        mouthOpenFactor > 0.25f -> "Yarı Açık (Konuşuyor)"
        else -> "Hafif Kapalı"
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Rounded.MenuBook, null) },
                    label = { Text("Taslak") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Rounded.Face, null) },
                    label = { Text("Karakter") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Rounded.Movie, null) },
                    label = { Text("Sahne") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Rounded.Share, null) },
                    label = { Text("YouTube") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "ÇizgiStüdyom • ${selectedCharacter.name}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(text = statusBanner, style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFB300))

            when (selectedTab) {
                0 -> {
                    val categories = listOf("Tümü", "Yunan Mitolojisi", "Dini Kıssalar", "Çocuk Çizgi Film")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = researchQuery,
                        onValueChange = { researchQuery = it },
                        label = { Text("Araştırılacak konu (mitoloji, kıssa, masal)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            storyTitle = researchQuery.ifBlank { "Yeni Araştırma Bölümü" }
                            scenes = buildResearchScenes(researchQuery, selectedCharacter.name)
                            currentSceneIndex = 0
                            elapsedInSceneMs = 0L
                            statusBanner = "'$storyTitle' için sahneler oluşturuldu."
                            selectedTab = 2
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Araştır ve Sahne Taslağı Üret")
                    }

                    val filtered = if (selectedCategory == "Tümü") sampleDrafts
                    else sampleDrafts.filter { it.category == selectedCategory }

                    filtered.forEach { draft ->
                        Card(
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(draft.category, style = MaterialTheme.typography.labelMedium, color = Color(0xFFFFB300))
                                Text(draft.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${draft.scenes.size} sahne • Duygu: ${draft.emotion}", style = MaterialTheme.typography.bodySmall)
                                Button(onClick = {
                                    storyTitle = draft.title
                                    selectedEmotion = draft.emotion
                                    scenes = draft.scenes
                                    characters.find { it.name == draft.characterName }?.let { selectedCharacter = it }
                                    currentSceneIndex = 0
                                    elapsedInSceneMs = 0L
                                    statusBanner = "'${draft.title}' taslağı yüklendi."
                                    selectedTab = 2
                                }) {
                                    Text("Taslağı Kullan")
                                }
                            }
                        }
                    }
                }

                1 -> {
                    Text("Sabit Karakter Kimlikleri (Sahneler Arası Bozulmaz)", style = MaterialTheme.typography.titleMedium)
                    characters.forEach { charItem ->
                        Card(
                            border = BorderStroke(
                                width = if (charItem.id == selectedCharacter.id) 2.dp else 1.dp,
                                color = if (charItem.id == selectedCharacter.id) charItem.accentColor else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("${charItem.name} (ID: ${charItem.id})", fontWeight = FontWeight.Bold)
                                    Text("Doğal Ses Profili • Ton: ${charItem.voicePitch}x • Hız: ${charItem.voiceRate}x", style = MaterialTheme.typography.bodySmall)
                                }
                                Button(onClick = {
                                    selectedCharacter = charItem
                                    statusBanner = "${charItem.name} aktif karakter olarak seçildi."
                                }) {
                                    Text(if (charItem.id == selectedCharacter.id) "Seçili" else "Seç")
                                }
                            }
                        }
                    }
                }

                2 -> {
                    Text("Sahne Süresi: ${sceneDurationSec.toInt()} sn • Toplam: ${totalVideoSec.toInt()} sn", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = sceneDurationSec,
                        onValueChange = {
                            sceneDurationSec = it
                            elapsedInSceneMs = 0L
                        },
                        valueRange = 2f..12f,
                        steps = 9
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(emotions) { emo ->
                            FilterChip(
                                selected = selectedEmotion == emo,
                                onClick = { selectedEmotion = emo },
                                label = { Text(emo) }
                            )
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, selectedCharacter.accentColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF1A237E), selectedCharacter.accentColor.copy(alpha = 0.45f))
                                    )
                                )
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Süre: ${String.format(Locale.US, "%.1f", currentTotalSec)}s / ${String.format(Locale.US, "%.1f", totalVideoSec)}s",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .graphicsLayer {
                                        translationX = walkX
                                        translationY = hopY
                                    }
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(selectedCharacter.emoji, style = MaterialTheme.typography.displaySmall)
                            }

                            Text("Dudak Durumu: $mouthLabel • Duygu: $selectedEmotion", style = MaterialTheme.typography.bodySmall, color = Color.White)
                            Text("Sahne ${currentSceneIndex + 1}/${scenes.size}", fontWeight = FontWeight.Bold, color = Color(0xFFFFE082))
                            Text(scenes.getOrElse(currentSceneIndex) { "" }, color = Color.White)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (!isPlaying) {
                                    isPlaying = true
                                    statusBanner = "Çizgi film oynatılıyor..."
                                } else {
                                    isPlaying = false
                                    tts?.stop()
                                    statusBanner = "Duraklatıldı."
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null)
                            Spacer(Modifier.width(4.dp))
                            Text(if (isPlaying) "Duraklat" else "Oynat")
                        }

                        OutlinedButton(
                            onClick = {
                                isPlaying = false
                                tts?.stop()
                                elapsedInSceneMs = 0L
                                currentSceneIndex = (currentSceneIndex + 1) % scenes.size.coerceAtLeast(1)
                            }
                        ) {
                            Icon(Icons.Rounded.SkipNext, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Sonraki")
                        }
                    }
                }

                3 -> {
                    Text("Videoyu Kaydet ve YouTube'da Paylaş", style = MaterialTheme.typography.titleMedium)
                    Text("Başlık: $storyTitle", style = MaterialTheme.typography.bodyMedium)
                    Text("Toplam Süre: ${totalVideoSec.toInt()} saniye (${scenes.size} sahne)", style = MaterialTheme.typography.bodySmall)

                    Button(
                        onClick = {
                            val uri = saveVideoProjectSummaryToMovies(context, storyTitle, scenes.joinToString("\n"))
                            savedVideoUri = uri
                            statusBanner = if (uri != null) {
                                "Dosya Movies/CizgiStudyom klasörüne kaydedildi."
                            } else {
                                "Kayıt tamamlanamadı."
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Save, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Cihaza Kaydet (.mp4)")
                    }

                    Button(
                        onClick = {
                            val uri = savedVideoUri ?: saveVideoProjectSummaryToMovies(context, storyTitle, scenes.joinToString("\n"))
                            savedVideoUri = uri
                            shareToYouTube(context, uri, storyTitle)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Share, null)
                        Spacer(Modifier.width(6.dp))
                        Text("YouTube Kanalımda Paylaş")
                    }
                }
            }
        }
    }
}
