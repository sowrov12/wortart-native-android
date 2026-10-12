package com.sowrov.germanarticlebangla

import android.os.Bundle
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
        setContent { WortArtApp() }
    }
}

private data class WordCard(
    val german: String,
    val english: String,
    val bengali: String,
    val article: String,
    val level: String = "A1"
)

@Composable
fun WortArtApp() {
    var tab by remember { mutableStateOf("Practice") }
    var index by remember { mutableIntStateOf(0) }
    var played by remember { mutableIntStateOf(173) }
    var correct by remember { mutableIntStateOf(79) }
    var selectedLevel by remember { mutableStateOf("A1") }
    val words = remember {
        listOf(
            WordCard("Musik", "music", "সংগীত", "die"),
            WordCard("Brot", "bread", "রুটি", "das"),
            WordCard("Apfel", "apple", "আপেল", "der"),
            WordCard("Haus", "house", "বাড়ি", "das"),
            WordCard("Familie", "family", "পরিবার", "die")
        )
    }
    val word = words[index % words.size]

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
                        Text("▱  All words  ▾", color = Cream, fontSize = 16.sp, modifier = Modifier
                            .clip(RoundedCornerShape(30.dp)).background(Surface).clickable { selectedLevel = if (selectedLevel == "A1") "A2" else "A1" }.padding(horizontal = 16.dp, vertical = 12.dp))
                        Spacer(Modifier.width(14.dp))
                        Text(selectedLevel, color = Accent, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text("🔥  2", color = Accent, modifier = Modifier.clip(CircleShape).background(Surface).padding(horizontal = 16.dp, vertical = 12.dp))
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Metric("RUN", "0", Modifier.weight(1f))
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
                            Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorite", tint = Cream)
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
                        Text("Tap the correct article", color = Muted, fontSize = 13.sp)
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
                                        if (article == word.article) correct = ((correct * (played - 1) + 100) / played).coerceIn(0, 100)
                                        index++
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
                "Explore" -> SimpleScreen("Explore", "Browse vocabulary by level and category", listOf("All words", "A1 Beginner", "A2 Elementary", "B1 Intermediate", "Food & Drinks", "Family & People", "Home & Housing"))
                "Stats" -> SimpleScreen("Your Stats", "Practice progress", listOf("Words played: $played", "Success rate: $correct%", "Current streak: 2 days"))
                else -> SimpleScreen("WortArt Premium", "Unlock more vocabulary and categories", listOf("Premium categories", "More practice", "Support future updates"))
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
