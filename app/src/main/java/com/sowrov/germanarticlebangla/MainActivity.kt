package com.sowrov.germanarticlebangla

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import java.io.File
import org.json.JSONArray
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
    var showPracticeMenu by remember { mutableStateOf(false) }
    var exploreCategoriesRequested by remember { mutableStateOf(false) }
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
    var shuffledWords by remember {
    mutableStateOf(filteredWords.shuffled())
}

LaunchedEffect(selectedLevel, selectedCategory, allWords) {
    shuffledWords = filteredWords.shuffled()
    index = 0
    answerMessage = null
}
    val word = shuffledWords.getOrNull(index)
    val activeCategory = categoryInfo.firstOrNull { category ->
        category.name.equals(word?.category, ignoreCase = true)
    } ?: categoryInfo.firstOrNull { it.name.equals(selectedCategory, ignoreCase = true) }

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
            exploreCategoriesRequested = false
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
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "▦ ${categoryLabel(selectedCategory)}",
                            color = Cream,
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(Surface)
                                .clickable {
                                    tab = "Explore"
                                    exploreCategoriesRequested = true
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        )

                        Box {
                            Text(
                                "⋮",
                                color = Cream,
                                fontSize = 28.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { showPracticeMenu = true }
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                            DropdownMenu(
                                expanded = showPracticeMenu,
                                onDismissRequest = { showPracticeMenu = false }
                            ) {
                                listOf("All", "A1", "A2", "B1").forEach { level ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (level == "All") "All levels" else
                                                    if (selectedLevel == level) "✓ $level" else level
                                            )
                                        },
                                        onClick = {
                                            selectedLevel = level
                                            showPracticeMenu = false
                                        }
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Explore categories") },
                                    onClick = {
                                        showPracticeMenu = false
                                        tab = "Explore"
                                        exploreCategoriesRequested = true
                                    }
                                )
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
                        Box(
                            Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(28.dp))
                                .border(1.dp, Color(0xFF39271B), RoundedCornerShape(28.dp))
                        ) {
                            CategoryImage(
                                activeCategory?.backgroundImage.orEmpty(),
                                Modifier.fillMaxSize(),
                                ""
                            )
                            Box(Modifier.fillMaxSize().background(Color(0xAA120D09)))
                            Column(
                                Modifier.fillMaxSize().padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Audio", tint = Cream)
                                    Box(
                                        Modifier.clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xAA211812))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(word.level, color = Cream, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Icon(
                                        if (word.german in favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (word.german in favorites) Accent else Cream,
                                        modifier = Modifier.clickable { toggleFavorite(word.german) }
                                    )
                                }
                                Spacer(Modifier.height(24.dp))
                                Box(
                                    Modifier.size(100.dp).clip(CircleShape)
                                        .background(Color(0xCC17100B))
                                        .border(3.dp, Accent, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CategoryImage(activeCategory?.iconImage.orEmpty(), Modifier.size(78.dp), categoryEmoji(word.category))
                                }
                                Spacer(Modifier.height(22.dp))
                                Text(word.german, color = Cream, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                                Spacer(Modifier.height(10.dp))
                                Text(word.english, color = Color(0xFFE0D2C3), fontSize = 18.sp)
                                Spacer(Modifier.height(6.dp))
                                Text(word.bengali, color = Cream, fontSize = 21.sp)
                                Spacer(Modifier.height(24.dp))
                                Text(
                                    answerMessage ?: "Tap the correct article",
                                    color = if (answerMessage == null) Color(0xFFE0D2C3) else Accent
                                )
                            }
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
                "Explore" -> ExploreScreen(allWords, favorites, categoryInfo, ::toggleFavorite, ::selectCategory, exploreCategoriesRequested)
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
    onCategoryClick: (String) -> Unit,
    openCategoriesRequest: Boolean
) {
    var query by remember { mutableStateOf("") }
    var exploreTab by remember { mutableStateOf("Categories") }
    LaunchedEffect(openCategoriesRequest) {
        if (openCategoriesRequest) exploreTab = "Categories"
    }

    val categories = remember(allWords, categoryInfo) {
        categoryInfo.ifEmpty {
            allWords.map { CategoryInfo(it.category, "", "", "Free") }.distinctBy { it.name }
        }
    }
    val searchResults = remember(allWords, query) {
        if (query.isBlank()) emptyList()
        else allWords.filter { w ->
            w.german.contains(query, ignoreCase = true) ||
                w.english.contains(query, ignoreCase = true) ||
                w.bengali.contains(query, ignoreCase = true)
        }.distinctBy { it.german }.take(30)
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Explore", color = Cream, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("Pick a deck, learn the patterns, look up any word.", color = Muted, fontSize = 14.sp)

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Surface).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Categories", "Rules", "Search").forEach { item ->
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(24.dp))
                        .background(if (exploreTab == item) Accent else Color.Transparent)
                        .clickable { exploreTab = item }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item, color = if (exploreTab == item) Background else Muted, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        when (exploreTab) {
            "Categories" -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { CategoryTile(CategoryInfo("All", "", "", "Free")) { onCategoryClick("All") } }
                    item { CategoryTile(CategoryInfo("Favorites", "", "", "Free")) { onCategoryClick("Favorites") } }
                    items(categories) { category ->
                        CategoryTile(category) { onCategoryClick(category.name) }
                    }
                }
            }

            "Rules" -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    item {
                        RuleGroupCard(
                            article = "der",
                            heading = "der — masculine",
                            summary = "Time words, weather, drinks, directions, car brands and key endings.",
                            rules = listOf(
                                RuleInfo("Months", "ALWAYS", "Months are always masculine.", listOf("Januar", "Februar", "März", "April", "Mai", "Juni", "Juli")),
                                RuleInfo("Seasons", "ALWAYS", "Seasons are always masculine.", listOf("Frühling", "Sommer", "Herbst", "Winter")),
                                RuleInfo("Drinks", "USUALLY", "Except for beer, alcoholic drinks are masculine.", listOf("Wein", "Rum", "Champagner", "Schnaps", "Liquor", "Cognac"), "Exception: das Bier"),
                                RuleInfo("Precipitations", "ALWAYS", "Precipitations are always masculine.", listOf("Regen", "Schnee", "Hagel", "Sturm")),
                                RuleInfo("Weekdays", "ALWAYS", "Weekdays are always masculine.", listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag")),
                                RuleInfo("Compass", "ALWAYS", "Points of the compass are masculine.", listOf("Norden", "Nordosten", "Osten", "Südosten", "Süden", "Südwesten")),
                                RuleInfo("Car brands", "ALWAYS", "Car brands are masculine.", listOf("Mercedes", "Volkswagen", "Ferrari", "Opel", "BMW", "Audi")),
                                RuleInfo("Endings", "ALWAYS", "Common masculine endings.", listOf("-ismus: Journalismus", "-ner: Rechner", "-or: Mentor")),
                                RuleInfo("Endings", "USUALLY", "These endings are usually masculine.", listOf("-ig: Honig", "-ich: Pfirsich", "-ling: Schmetterling", "-ist: Polizist", "-ant: Elefant", "-ent: Agent", "-är: Bär", "-eur: Ingenieur"))
                            )
                        )
                    }
                    item {
                        RuleGroupCard(
                            article = "die",
                            heading = "die — feminine",
                            summary = "Common feminine endings.",
                            rules = listOf(
                                RuleInfo("Endings", "ALWAYS", "These endings are feminine.", listOf("-heit: Gesundheit", "-keit: Süßigkeit", "-schaft: Wissenschaft", "-ung: Bildung", "-tät: Qualität", "-ie: Galaxie", "-ik: Mathematik")),
                                RuleInfo("Endings", "USUALLY", "These endings are usually feminine.", listOf("-in: Lehrerin", "-e: Stunde", "-ei: Polizei", "-ur: Tastatur"))
                            )
                        )
                    }
                    item {
                        RuleGroupCard(
                            article = "das",
                            heading = "das — neuter",
                            summary = "Nouns made from verbs, colors and common endings.",
                            rules = listOf(
                                RuleInfo("Categories", "ALWAYS", "Verbs used as nouns and colors are neuter.", listOf("Essen", "Lesen", "Rot", "Blau")),
                                RuleInfo("Endings", "ALWAYS", "These endings are neuter.", listOf("-chen: Mädchen", "-lein: Büchlein")),
                                RuleInfo("Endings", "USUALLY", "These endings are usually neuter.", listOf("-ial: Material", "-nis: Erlebnis", "-um: Album", "-ment: Dokument", "-o: Auto", "Ge-: Geschenk"))
                            )
                        )
                    }
                }
            }

            else -> {
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
                if (query.isBlank()) {
                    Text("Search suggestions", color = Muted, fontWeight = FontWeight.SemiBold)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("der Apfel", "das Haus", "die Gesundheit", "der Beruf").forEach { suggestion ->
                            val term = suggestion.substringAfter(" ")
                            val found = allWords.firstOrNull { it.german.equals(term, true) }
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                    .background(Surface).clickable {
                                        query = found?.german ?: term
                                    }.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⌕", color = Accent, fontSize = 20.sp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(found?.let { "${it.article} ${it.german}" } ?: suggestion, color = Cream, fontWeight = FontWeight.SemiBold)
                                    Text(found?.let { "${it.bengali} • ${it.english}" } ?: "Tap to search this word", color = Muted, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(searchResults) { word ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                    .background(Surface).padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("${word.article} ${word.german}", color = Cream, fontWeight = FontWeight.Bold)
                                    Text("${word.bengali} • ${word.english}", color = Muted, fontSize = 13.sp)
                                    Text("${word.level} · ${word.category}", color = Accent, fontSize = 12.sp)
                                }
                                Icon(
                                    if (word.german in favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Toggle favorite",
                                    tint = if (word.german in favorites) Accent else Muted,
                                    modifier = Modifier.clickable { onToggleFavorite(word.german) }.padding(6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class RuleInfo(
    val title: String,
    val status: String,
    val explanation: String,
    val examples: List<String>,
    val exception: String? = null
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RuleGroupCard(
    article: String,
    heading: String,
    summary: String,
    rules: List<RuleInfo>
) {
    var expanded by remember { mutableStateOf(true) }
    val articleColor = when (article) {
        "der" -> Color(0xFF63B9F2)
        "die" -> Color(0xFFF27B9B)
        else -> Color(0xFFFFD84D)
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Surface).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF30251D)), contentAlignment = Alignment.Center) {
                Text(article, color = articleColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(heading, color = Cream, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(summary, color = Muted, fontSize = 12.sp)
            }
            Text(if (expanded) "⌃" else "⌄", color = Muted, fontSize = 20.sp)
        }
        if (expanded) {
            rules.forEach { rule ->
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF30261F)).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(rule.title, color = Cream, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(
                            rule.status,
                            color = if (rule.status == "ALWAYS") Color(0xFF70D08A) else Accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFF211812)).padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                    Text(rule.explanation, color = Muted, fontSize = 13.sp)
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rule.examples.forEach { example ->
                            Text(
                                if (example.startsWith("-")) example else "$article $example",
                                color = articleColor,
                                fontSize = 12.sp,
                                modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF211812)).padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                    }
                    rule.exception?.let { Text(it, color = Cream, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
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

private object CategoryBitmapCache {
    private val memory = object : android.util.LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    suspend fun load(context: Context, imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        memory.get(imageUrl)?.let { return@withContext it }

        val cacheFile = File(context.cacheDir, "wortart_image_${imageUrl.hashCode()}.img")
        if (cacheFile.exists()) {
            try {
                val cached = BitmapFactory.decodeFile(cacheFile.absolutePath)
                if (cached != null) {
                    memory.put(imageUrl, cached)
                    return@withContext cached
                }
            } catch (_: Exception) {
                cacheFile.delete()
            }
        }

        try {
            val connection = URL(imageUrl).openConnection().apply {
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            val bitmap = connection.getInputStream().use { BitmapFactory.decodeStream(it) }
            if (bitmap != null) {
                try {
                    cacheFile.outputStream().use { output ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                    }
                } catch (_: Exception) {
                    // The in-memory copy still works if disk caching fails.
                }
                memory.put(imageUrl, bitmap)
            }
            bitmap
        } catch (e: Exception) {
            android.util.Log.e("WortArtImage", "Image loading failed: $imageUrl", e)
            null
        }
    }
}

@Composable
private fun CategoryImage(
    url: String,
    modifier: Modifier = Modifier,
    fallback: String
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = url) {
        if (url.isBlank()) {
            value = null
        } else {
            val id = Regex("/d/([^/]+)").find(url)?.groupValues?.getOrNull(1)
            val imageUrl = if (id != null && url.contains("drive.google.com")) {
                "https://drive.google.com/uc?export=download&id=$id"
            } else {
                url
            }
            value = CategoryBitmapCache.load(context.applicationContext, imageUrl)
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else if (fallback.isNotBlank()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(fallback, fontSize = 27.sp)
        }
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
