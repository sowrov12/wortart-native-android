package com.sowrov.germanarticlebangla

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import org.json.JSONArray
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

private val Background = Color(0xFF120D09)
private val Surface = Color(0xFF211812)
private val Accent = Color(0xFFFFA43A)
private val Cream = Color(0xFFFFF0D7)
private val Muted = Color(0xFFB7A99A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val wordsArray = JSONArray(assets.open("words.json").bufferedReader().use { it.readText() })
        val words = (0 until wordsArray.length()).map { i ->
            val item = wordsArray.getJSONObject(i)
            WordCard(item.optString("german"), item.optString("englishMeaning"),
                item.optString("bengaliMeaning"), item.optString("article"),
                item.optString("level", "A1"), item.optString("category", "Other"))
        }
        val categories = try {
            val array = JSONArray(assets.open("categories.json").bufferedReader().use { it.readText() })
            (0 until array.length()).map { i ->
                val item = array.getJSONObject(i)
                CategoryInfo(item.optString("name"), item.optString("backgroundImage"),
                    item.optString("iconImage"), item.optString("access", "Free"))
            }
        } catch (_: Exception) { emptyList() }
        setContent { WortArtApp(words, categories) }
    }
}

data class WordCard(
    val german: String, val english: String, val bengali: String,
    val article: String, val level: String = "A1", val category: String = "Other"
)
data class CategoryInfo(val name: String, val backgroundImage: String, val iconImage: String, val access: String)

@Composable
fun WortArtApp(allWords: List<WordCard>, categoryInfo: List<CategoryInfo>) {
    var tab by remember { mutableStateOf("Practice") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedLevel by remember { mutableStateOf("All") }
    var levelMenu by remember { mutableStateOf(false) }
    var categoryMenu by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    var premiumCategory by remember { mutableStateOf("") }
    var index by remember { mutableIntStateOf(0) }
    var played by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var run by remember { mutableIntStateOf(0) }
    var answerMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("wortart_preferences", Context.MODE_PRIVATE) }
    var favorites by remember { mutableStateOf(prefs.getStringSet("favorites", emptySet())?.toSet() ?: emptySet()) }
    fun toggleFavorite(german: String) {
        val updated = if (german in favorites) favorites - german else favorites + german
        favorites = updated
        prefs.edit().putStringSet("favorites", updated.toSet()).apply()
    }

    val categoryNames = remember(allWords, categoryInfo) {
        (categoryInfo.map { it.name } + allWords.map { it.category }).filter { it.isNotBlank() }.distinct().sorted()
    }
    val filteredWords = remember(selectedLevel, selectedCategory, allWords, favorites) {
        allWords.filter { w ->
            (selectedLevel == "All" || w.level.equals(selectedLevel, true)) &&
                when (selectedCategory) {
                    "All" -> true
                    "Favorites" -> w.german in favorites
                    else -> w.category.equals(selectedCategory, true)
                }
        }
    }
    var shuffledWords by remember { mutableStateOf(filteredWords.shuffled()) }
    LaunchedEffect(selectedLevel, selectedCategory, allWords, favorites) {
        shuffledWords = filteredWords.shuffled()
        index = 0
        answerMessage = null
    }
    val word = shuffledWords.getOrNull(index)
    val activeCategory = categoryInfo.firstOrNull { it.name == selectedCategory }

    fun selectCategory(name: String) {
        val info = categoryInfo.firstOrNull { it.name == name }
        if (info?.access.equals("Premium", ignoreCase = true)) {
            premiumCategory = name
            showPremiumDialog = true
        } else {
            selectedCategory = name
            index = 0
            answerMessage = null
            tab = "Practice"
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Background, surface = Surface, primary = Accent,
        onBackground = Cream, onSurface = Cream
    )) {
        Scaffold(
            containerColor = Background,
            bottomBar = {
                NavigationBar(containerColor = Background, contentColor = Muted) {
                    listOf(
                        Triple("Practice", Icons.Default.Layers, "Practice"),
                        Triple("Explore", Icons.Default.Explore, "Explore"),
                        Triple("Stats", Icons.Default.BarChart, "Stats"),
                        Triple("Premium", Icons.Default.Lock, "Premium")
                    ).forEach { (label, icon, route) ->
                        NavigationBarItem(
                            selected = tab == route, onClick = { tab = route },
                            icon = { Icon(icon, contentDescription = label) }, label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Accent, selectedTextColor = Accent,
                                indicatorColor = Surface, unselectedIconColor = Muted, unselectedTextColor = Muted
                            )
                        )
                    }
                }
            }
        ) { padding ->
            when (tab) {
                "Practice" -> Column(
                    Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            Text("▱ ${if (selectedLevel == "All") "All levels" else selectedLevel} ▾",
                                color = Cream, modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(Surface)
                                    .clickable { levelMenu = true }.padding(12.dp))
                            DropdownMenu(expanded = levelMenu, onDismissRequest = { levelMenu = false }) {
                                listOf("All", "A1", "A2", "B1").forEach { level ->
                                    DropdownMenuItem(text = { Text(if (level == "All") "All levels" else level) },
                                        onClick = { selectedLevel = level; levelMenu = false })
                                }
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Box {
                            Text("▦ ${categoryLabel(selectedCategory)} ▾", color = Cream,
                                modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(Surface)
                                    .clickable { categoryMenu = true }.padding(12.dp))
                            DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                                (listOf("All", "Favorites") + categoryNames).distinct().forEach { name ->
                                    DropdownMenuItem(text = { Text(categoryLabel(name)) },
                                        onClick = { categoryMenu = false; selectCategory(name) })
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Metric("RUN", run.toString(), Modifier.weight(1f))
                        Metric("PLAYED", played.toString(), Modifier.weight(1f))
                        Metric("SUCCESS", if (played == 0) "—" else "${correctCount * 100 / played}%", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(14.dp))
                    if (word == null) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No words found for this selection.", color = Muted)
                        }
                    } else {
                        Column(
                            Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(28.dp))
                                .background(Color(0xFF302016)).border(1.dp, Color(0xFF39271B), RoundedCornerShape(28.dp)).padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Audio", tint = Cream)
                                Icon(if (word.german in favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite", tint = if (word.german in favorites) Accent else Cream,
                                    modifier = Modifier.clickable { toggleFavorite(word.german) })
                            }
                            Spacer(Modifier.height(20.dp))
                            Box(Modifier.size(100.dp).clip(CircleShape).background(Color(0xFF17100B)).border(3.dp, Accent, CircleShape),
                                contentAlignment = Alignment.Center) {
                                CategoryImage(activeCategory?.iconImage.orEmpty(), Modifier.size(78.dp), "📚")
                            }
                            Spacer(Modifier.height(20.dp))
                            Text(word.german, color = Cream, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.height(10.dp))
                            Text(word.english, color = Muted, fontSize = 18.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(word.bengali, color = Cream, fontSize = 21.sp)
                            Spacer(Modifier.height(24.dp))
                            Text(answerMessage ?: "Tap the correct article", color = if (answerMessage == null) Muted else Accent)
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("der", "die", "das").forEach { article ->
                                val barColor = when (article) {
                                    "der" -> Color(0xFF63B9F2)
                                    "die" -> Color(0xFFF27B9B)
                                    else -> Color(0xFFFFD84D)
                                }
                                Column(
                                    Modifier.weight(1f).height(72.dp).clip(RoundedCornerShape(22.dp))
                                        .background(Surface).border(1.dp, Color(0xFF30251D), RoundedCornerShape(22.dp))
                                        .clickable {
                                            played++
                                            val right = article.equals(word.article, true)
                                            if (right) { correctCount++; run++; answerMessage = "Richtig! ${word.article} ${word.german}" }
                                            else { run = 0; answerMessage = "Correct: ${word.article} ${word.german}" }
                                            if (index + 1 >= shuffledWords.size) {
                                                shuffledWords = filteredWords.shuffled()
                                                index = 0
                                            } else index++
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                                ) {
                                    Text(article, color = Cream, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    Box(Modifier.width(28.dp).height(5.dp).clip(CircleShape).background(barColor))
                                }
                            }
                        }
                    }
                }
                "Explore" -> ExploreScreen(allWords, favorites, categoryInfo, ::toggleFavorite, ::selectCategory)
                "Stats" -> SimpleScreen("Your Stats", "Practice progress",
                    listOf("Words played: $played", "Success rate: ${if (played == 0) 0 else correctCount * 100 / played}%", "Current run: $run"))
                else -> PremiumScreen()
            }
        }

        if (showPremiumDialog) {
            AlertDialog(
                onDismissRequest = { showPremiumDialog = false },
                containerColor = Surface,
                icon = { Icon(Icons.Default.Star, contentDescription = null, tint = Accent, modifier = Modifier.size(36.dp)) },
                title = { Text("Unlock Premium category", color = Cream) },
                text = { Text("$premiumCategory is locked. Get Premium to unlock this category and practice it.", color = Muted) },
                confirmButton = {
                    Button(onClick = { showPremiumDialog = false; tab = "Premium" },
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Background)) {
                        Text("Get Premium")
                    }
                },
                dismissButton = { TextButton(onClick = { showPremiumDialog = false }) { Text("OK", color = Cream) } }
            )
        }
    }
}

private fun categoryLabel(name: String) = when (name) {
    "All" -> "All Categories"
    "Favorites" -> "Favorites"
    else -> name
}

@Composable
private fun ExploreScreen(
    allWords: List<WordCard>,
    favorites: Set<String>,
    categoryInfo: List<CategoryInfo>,
    onToggleFavorite: (String) -> Unit,
    onCategoryClick: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val categories = remember(allWords, categoryInfo) {
        categoryInfo.ifEmpty { allWords.map { CategoryInfo(it.category, "", "", "Free") }.distinctBy { it.name } }
    }
    val results = remember(allWords, query) {
        allWords.filter { w -> query.isNotBlank() &&
            (w.german.contains(query, true) || w.english.contains(query, true) || w.bengali.contains(query, true))
        }.take(80)
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Explore", color = Cream, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Search German / English / বাংলা") }, singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Cream, unfocusedTextColor = Cream, focusedLabelColor = Accent,
                unfocusedLabelColor = Muted, focusedBorderColor = Accent, unfocusedBorderColor = Muted,
                cursorColor = Accent
            )
        )
        Text("Categories", color = Muted)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2), modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { CategoryTile(CategoryInfo("All", "", "", "Free")) { onCategoryClick("All") } }
            item { CategoryTile(CategoryInfo("Favorites", "", "", "Free")) { onCategoryClick("Favorites") } }
            items(categories) { category -> CategoryTile(category) { onCategoryClick(category.name) } }
        }
        if (query.isNotBlank()) {
            Text("Search results", color = Accent, fontWeight = FontWeight.Bold)
            LazyColumn(Modifier.heightIn(max = 180.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(results) { word ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${word.article} ${word.german}", color = Cream, fontWeight = FontWeight.Bold)
                            Text("${word.bengali} • ${word.english}", color = Muted, fontSize = 13.sp)
                        }
                        Icon(if (word.german in favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Toggle favorite", tint = if (word.german in favorites) Accent else Muted,
                            modifier = Modifier.clickable { onToggleFavorite(word.german) }.padding(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTile(category: CategoryInfo, onClick: () -> Unit) {
    val premium = category.access.equals("Premium", true)
    Box(
        Modifier.fillMaxWidth().height(138.dp).clip(RoundedCornerShape(20.dp))
            .background(Surface).border(1.dp, if (premium) Accent else Color(0xFF39271B), RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        CategoryImage(category.backgroundImage, Modifier.fillMaxSize(), "")
        Box(Modifier.fillMaxSize().background(Color(0xAA120D09)))
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                CategoryImage(category.iconImage, Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)), categoryEmoji(category.name))
                if (premium) Icon(Icons.Default.Lock, contentDescription = "Locked Premium category", tint = Accent)
            }
            Text(category.name, color = Cream, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

private fun categoryEmoji(name: String): String = when {
    name.contains("Food", true) -> "🍎"
    name.contains("Health", true) -> "🩺"
    name.contains("Family", true) -> "👨‍👩‍👧"
    name.contains("Education", true) -> "📚"
    name.contains("Travel", true) -> "🚆"
    name.contains("Work", true) -> "💼"
    name.contains("Animal", true) -> "🐾"
    name.contains("Technology", true) -> "💻"
    name.contains("Nature", true) -> "🌦️"
    name.contains("Clothing", true) -> "👕"
    name.contains("Home", true) -> "🏠"
    name.contains("Money", true) -> "💰"
    else -> "📖"
}

@Composable
private fun CategoryImage(url: String, modifier: Modifier = Modifier, fallback: String) {
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = url) {
        value = null
        val id = Regex("/d/([^/]+)").find(url)?.groupValues?.getOrNull(1)
        val imageUrl = when {
            url.isBlank() -> null
            id != null && url.contains("drive.google.com") -> "https://drive.google.com/uc?export=download&id=$id"
            else -> url
        }
        value = if (imageUrl == null) null else withContext(Dispatchers.IO) {
            try {
                URL(imageUrl).openConnection().apply { connectTimeout = 5000; readTimeout = 5000 }
                    .getInputStream().use { BitmapFactory.decodeStream(it) }
            } catch (_: Exception) { null }
        }
    }
    if (bitmap != null) {
        Image(bitmap!!.asImageBitmap(), contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    } else if (fallback.isNotBlank()) {
        Box(modifier, contentAlignment = Alignment.Center) { Text(fallback, fontSize = 27.sp) }
    } else {
        Box(modifier.background(Color(0xFF302016)))
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(22.dp)).background(Surface).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Muted, fontSize = 11.sp)
        Text(value, color = Cream, fontSize = 21.sp, fontWeight = FontWeight.Bold)
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
private fun PremiumScreen() {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("PREMIUM", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text("Learn without limits", color = Cream, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
        listOf("▦  Unlock every Premium category", "∞  Unlimited practice", "♡  Unlimited favorites", "▤  Unlimited rule look-ups").forEach {
            Text(it, color = Cream, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface).padding(18.dp))
        }
        Text("1 Week — ৳30", color = Cream, modifier = Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(14.dp)).padding(16.dp))
        Text("1 Month — ৳100", color = Cream, modifier = Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(14.dp)).padding(16.dp))
        Text("1 Year — ৳500", color = Cream, modifier = Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(14.dp)).padding(16.dp))
        Button(onClick = { }, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Background)) {
            Text("Get Premium on WhatsApp", fontWeight = FontWeight.Bold)
        }
        Text("Manual Premium activation is not connected in this version. Google Sign-In and Firebase entitlement must be implemented separately.", color = Muted, fontSize = 12.sp)
    }
}
