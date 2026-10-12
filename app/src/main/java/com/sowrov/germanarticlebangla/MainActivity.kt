package com.sowrov.germanarticlebangla

import android.os.Bundle
import org.json.JSONArray
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF120D09)
private val Surface = Color(0xFF211812)
private val Accent = Color(0xFFFFA43A)
private val Cream = Color(0xFFFFF0D7)
private val Muted = Color(0xFFB7A99A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val wordsJson = assets.open("words.json").bufferedReader().use { it.readText() }
        val wordsArray = JSONArray(wordsJson)
        val loadedWords = (0 until wordsArray.length()).map { i ->
            val item = wordsArray.getJSONObject(i)
            WordCard(
                german = item.optString("german"),
                english = item.optString("englishMeaning"),
                bengali = item.optString("bengaliMeaning"),
                article = item.optString("article"),
                level = item.optString("level"),
                category = item.optString("category")
            )
        }
        setContent { WortArtApp(loadedWords) }
    }
}

private data class WordCard(
    val german: String,
    val english: String,
    val bengali: String,
    val article: String,
    val level: String = "A1",
    val category: String = "Other"
)

@Composable
fun WortArtApp(allWords: List<WordCard>) {
    var tab by remember { mutableStateOf("Practice") }
    var index by remember { mutableIntStateOf(0) }
    var played by remember { mutableIntStateOf(173) }
    var correct by remember { mutableIntStateOf(79) }
    var selectedLevel by remember { mutableStateOf("All") }
    var levelMenuExpanded by remember { mutableStateOf(false) }
    var favorites by remember { mutableStateOf(setOf<String>()) }
    var lastAnswer by remember { mutableStateOf<String?>(null) }
    var run by remember { mutableIntStateOf(0) }
    val words = remember(selectedLevel, allWords) {
        if (selectedLevel == "All") allWords else allWords.filter { it.level == selectedLevel }
    }
    val word = words[index.mod(words.size.coerceAtLeast(1))]
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Background, surface = Surface, primary = Accent,
            onBackground = Cream, onSurface = Cream
        )
    ) {
        Scaffold(
            containerColor = Background,
            bottomBar = {
                NavigationBar(containerColor = Background, contentColor = Muted) {
                    val items = listOf(
                        Triple("Practice", Icons.Default.Layers, "Practice"),
                        Triple("Explore", Icons.Default.Explore, "Explore"),
                        Triple("Stats", Icons.Default.BarChart, "Stats"),
                        Triple("Premium", Icons.Default.Lock, "Premium")
                    )
                    items.forEach { (label, icon, route) ->
                        NavigationBarItem(
                            selected = tab == route,
                            onClick = { tab = route },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Accent,
                                selectedTextColor = Accent,
                                indicatorColor = Surface,
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted
                            )
                        )
                    }
                }
            }
        ) { padding ->
            when (tab) {
                "Practice" -> Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box {
                            Text(
                                text = "▱  ${if (selectedLevel == "All") "All words" else selectedLevel}  ▾",
                                color = Cream,
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(Surface)
                                    .clickable { levelMenuExpanded = true }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                            DropdownMenu(
                                expanded = levelMenuExpanded,
                                onDismissRequest = { levelMenuExpanded = false }
                            ) {
                                listOf("All", "A1", "A2", "B1").forEach { level ->
                                    DropdownMenuItem(
                                        text = { Text(if (level == "All") "All words" else level) },
                                        onClick = {
                                            selectedLevel = level
                                            index = 0
                                            lastAnswer = null
                                            levelMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Text("${words.size} words", color = Accent, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text("🔥  2", color = Accent, modifier = Modifier.clip(CircleShape).background(Surface).padding(horizontal = 16.dp, vertical = 12.dp))
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Metric("RUN", run.toString(), Modifier.weight(1f))
                        Metric("PLAYED", played.toString(), Modifier.weight(1f))
                        Metric("SUCCESS", "$correct%", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(22.dp))
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(30.dp))
                            .background(Color(0xFF302016)).border(1.dp, Color(0xFF39271B), RoundedCornerShape(30.dp)).padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Audio", tint = Cream)
                            Icon(if (favorites.contains(word.german)) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Favorite", tint = if (favorites.contains(word.german)) Accent else Cream,
                                modifier = Modifier.clickable { favorites = if (favorites.contains(word.german)) favorites - word.german else favorites + word.german })
                        }
                        Spacer(Modifier.height(60.dp))
                        Box(
                            modifier = Modifier.size(118.dp).clip(CircleShape).background(Color(0xFF17100B))
                                .border(3.dp, Accent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Text("🎁", fontSize = 56.sp) }
                        Spacer(Modifier.height(28.dp))
                        Text(word.german, color = Cream, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(18.dp))
                        Text(word.english, color = Muted, fontSize = 18.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(word.bengali, color = Cream, fontSize = 22.sp)
                        Spacer(Modifier.height(50.dp))
                        Text(lastAnswer ?: "Tap the correct article", color = if (lastAnswer == null) Muted else Accent, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("der", "die", "das").forEach { article ->
                            val color = when (article) {
                                "der" -> Color(0xFF63B9F2)
                                "die" -> Color(0xFFF27B9B)
                                else -> Color(0xFFFFD84D)
                            }
                            Column(
                                modifier = Modifier.weight(1f).height(76.dp).clip(RoundedCornerShape(24.dp))
                                    .background(Surface).border(1.dp, Color(0xFF30251D), RoundedCornerShape(24.dp))
                                    .clickable {
                                        played++
                                        val isCorrect = article == word.article
                                        lastAnswer = if (isCorrect) "Richtig! ${word.article} ${word.german}" else "Correct: ${word.article} ${word.german}"
                                        if (isCorrect) run++ else run = 0
                                        correct = (((correct * (played - 1)) + if (isCorrect) 100 else 0) / played).coerceIn(0, 100)
                                        index = (index + 1) % words.size.coerceAtLeast(1)
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(article, color = Cream, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Box(Modifier.width(28.dp).height(5.dp).clip(CircleShape).background(color))
                            }
                        }
                    }
                }
                "Explore" -> ExploreScreen(allWords)
                "Stats" -> SimpleScreen("Your Stats", "Practice progress", listOf("Words played: $played", "Success rate: $correct%", "Current streak: 2 days"))
                else -> SimpleScreen("WortArt Premium", "Premium activation will connect to Firebase in the next phase", listOf("26 categories in your vocabulary data", "Premium categories are marked in categories.json", "Google Sign-in and secure entitlement are not yet connected"))
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(24.dp)).background(Surface).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Muted, fontSize = 12.sp)
        Text(value, color = Cream, fontSize = 23.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SimpleScreen(title: String, subtitle: String, rows: List<String>) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(title, color = Cream, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Muted, fontSize = 15.sp)
        rows.forEach { row ->
            Text(row, color = Cream, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface).padding(18.dp))
        }
    }
}


@Composable
private fun ExploreScreen(allWords: List<WordCard>) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = remember(allWords) { listOf("All") + allWords.map { it.category }.filter { it.isNotBlank() }.distinct().sorted() }
    val results = remember(allWords, query, selectedCategory) {
        allWords.filter { word ->
            (selectedCategory == "All" || word.category == selectedCategory) &&
            (query.isBlank() || word.german.contains(query, ignoreCase = true) || word.english.contains(query, ignoreCase = true) || word.bengali.contains(query, ignoreCase = true))
        }.take(150)
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Explore", color = Cream, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search German / English / বাংলা") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Cream, unfocusedTextColor = Cream,
                focusedLabelColor = Accent, unfocusedLabelColor = Muted,
                focusedBorderColor = Accent, unfocusedBorderColor = Muted,
                cursorColor = Accent
            )
        )
        Text("Category", color = Muted, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("All", "Food & Drinks", "Daily Life").forEach { category ->
                if (category in categories) {
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) }
                    )
                }
            }
        }
        Text("${results.size} results shown", color = Accent, fontSize = 13.sp)
        androidx.compose.foundation.lazy.LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(results.size) { i ->
                val word = results[i]
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface).padding(14.dp)
                ) {
                    Text("${word.article} ${word.german}", color = Cream, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("${word.bengali} • ${word.english}", color = Muted, fontSize = 14.sp)
                    Text("${word.level} · ${word.category}", color = Accent, fontSize = 12.sp)
                }
            }
        }
    }
}
