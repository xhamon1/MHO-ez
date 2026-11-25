package com.mhoguide.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONArray

data class Build(val name: String, val weapons: List<String>, val element: String,
                 val armor: String, val skills: List<String>, val note: String)

private fun JSONArray.strs() = (0 until length()).map { getString(it) }

fun loadBuilds(ctx: Context): List<Build> {
    val arr = JSONArray(ctx.assets.open("builds.json").bufferedReader().readText())
    return (0 until arr.length()).map {
        val o = arr.getJSONObject(it)
        Build(o.getString("name"), o.getJSONArray("weapons").strs(), o.getString("element"),
            o.getString("armor"), o.getJSONArray("skills").strs(), o.getString("note"))
    }
}

val WEAPONS = listOf("Great Sword", "Long Sword", "Sword & Shield", "Dual Blades", "Hammer",
    "Hunting Horn", "Lance", "Gunlance", "Switch Axe", "Charge Blade", "Insect Glaive",
    "Bow", "Light Bowgun", "Heavy Bowgun")
val ELEMENTS = listOf("Fire", "Water", "Thunder", "Ice", "Dragon", "None")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val builds = loadBuilds(this)
        setContent { MaterialTheme { Surface { GuideScreen(builds) } } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(builds: List<Build>) {
    var weapon by remember { mutableStateOf<String?>(null) }
    var element by remember { mutableStateOf<String?>(null) }
    val result = builds.filter {
        (weapon == null || "Any" in it.weapons || weapon in it.weapons) &&
        (element == null || it.element == element)
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
        Text("MHO-ez", style = MaterialTheme.typography.headlineMedium)
        Text("MHO guides for noobs", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        Text("Weapon"); Chips(WEAPONS, weapon) { weapon = it }
        Spacer(Modifier.height(8.dp))
        Text("Element"); Chips(ELEMENTS, element) { element = it }
        Spacer(Modifier.height(12.dp))
        if (result.isEmpty()) Text("No builds for this selection yet. Add them in assets/builds.json.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(result) { b ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(b.name, style = MaterialTheme.typography.titleMedium)
                        Text("Armor: ${b.armor}")
                        b.skills.forEach { Text("• $it") }
                        Text(b.note, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Chips(options: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { o ->
            FilterChip(selected == o, { onSelect(if (selected == o) null else o) }, { Text(o) })
        }
    }
}
