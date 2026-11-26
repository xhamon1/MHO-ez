package com.mhoguide.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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

private const val CDN = "https://img.gamewith.jp/"
val WEAPON_ICONS = mapOf(
    "Great Sword" to CDN + "img/5c7a4f0eba34d8ad5f96a2f0c08026fe.png",
    "Long Sword" to CDN + "img/5598294dbb9229c7344b5bb5fdf9b1c2.png",
    "Dual Blades" to CDN + "img/15ab1008996818b3232ef8fb42976094.png",
    "Hammer" to CDN + "article_tools/mhoutlanders/gacha/weapon_icon5.png",
    "Charge Blade" to CDN + "article_tools/mhoutlanders/gacha/weapon_icon10.png",
    "Lance" to CDN + "img/2d24e1a482ada690b7a236cfd412436e.png",
    "Heavy Bowgun" to CDN + "img/d19af964d28ad4dbe8d82de6f455e210.png",
    "Bow" to CDN + "img/704ac3ac33e262c8ca53c64570dfe888.png")
val ELEMENTS = listOf("Fire", "Water", "Thunder", "Ice", "Dragon")
val ELEMENT_COLOR = mapOf("Fire" to Color(0xFFE8623C), "Water" to Color(0xFF3F8EF0),
    "Thunder" to Color(0xFFE8C13C), "Ice" to Color(0xFF55D0E8), "Dragon" to Color(0xFF9C5CE0))
private fun elementIcon(e: String) = CDN + "article_tools/mhoutlanders/gacha/attr" +
    (ELEMENTS.indexOf(e) + 1) + ".png"

private val Gold = Color(0xFFE0B04A)
private val Bg = Color(0xFF111114)
private val Card = Color(0xFF1B1B20)
private val Card2 = Color(0xFF26262D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val builds = loadBuilds(this)
        val weapons = loadWeapons(this)
        val armors = loadArmors(this)
        val prefs = getSharedPreferences("mho", MODE_PRIVATE)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Gold, background = Bg, surface = Card)) {
                Surface(color = Bg, modifier = Modifier.fillMaxSize()) { Root(builds, weapons, armors, prefs) }
            }
        }
    }
}

@Composable
fun BuildsScreen(builds: List<Build>, prefs: SharedPreferences) {
    var weapon by remember { mutableStateOf<String?>(null) }
    var element by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var onlyFav by remember { mutableStateOf(false) }
    val favs = remember { mutableStateListOf<String>().apply { addAll(prefs.getStringSet("favs", emptySet())!!) } }
    val weapons = remember { builds.flatMap { it.weapons }.filter { it != "Any" }.distinct() }
    val shown = builds.filter { b ->
        (weapon == null || "Any" in b.weapons || weapon in b.weapons) &&
        (element == null || b.element == element) &&
        (!onlyFav || b.name in favs) &&
        (query.isBlank() || listOf(b.name, b.armor, b.note, b.skills.joinToString()).any { it.contains(query, true) })
    }
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(Modifier.fillMaxWidth().background(
                Brush.horizontalGradient(listOf(Color(0xFF3A2A0E), Card)), RoundedCornerShape(20.dp)).padding(20.dp)) {
                Text("MHO-ez", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Gold)
                Text("MHO guides for noobs", color = Color(0xFFB8B8C0))
            }
        }
        item {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("Search skills, armor, tips…") }, shape = RoundedCornerShape(14.dp))
        }
        item { Label("WEAPON") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(weapons) { w -> WeaponTile(w, weapon == w) { weapon = if (weapon == w) null else w } }
            }
        }
        item { Label("ELEMENT") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ELEMENTS) { e ->
                    Pill(e, ELEMENT_COLOR[e]!!, element == e, elementIcon(e)) { element = if (element == e) null else e }
                }
                item { Pill("★ Favorites", Gold, onlyFav, null) { onlyFav = !onlyFav } }
            }
        }
        item { Text("${shown.size} build(s)", color = Color(0xFF8E8E98), fontSize = 13.sp) }
        if (shown.isEmpty()) item {
            Text("No builds match. Try clearing a filter.", color = Color(0xFFB8B8C0))
        }
        items(shown, key = { it.name }) { b ->
            BuildCard(b, b.name in favs) {
                if (b.name in favs) favs.remove(b.name) else favs.add(b.name)
                prefs.edit().putStringSet("favs", favs.toSet()).apply()
            }
        }
    }
}

@Composable
fun Label(t: String) = Text(t, color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)

@Composable
fun WeaponTile(name: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.width(86.dp).background(if (selected) Card2 else Card, RoundedCornerShape(16.dp))
            .let { if (selected) it.then(Modifier) else it }
            .clickable(onClick = onClick).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(52.dp).background(Color(0xFF0D0D10), RoundedCornerShape(12.dp)), Alignment.Center) {
            AsyncImage(WEAPON_ICONS[name], name, Modifier.size(44.dp), contentScale = ContentScale.Fit)
        }
        Spacer(Modifier.height(6.dp))
        Text(name, fontSize = 11.sp, color = if (selected) Gold else Color.White,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, maxLines = 2,
            lineHeight = 13.sp)
    }
}

@Composable
fun Pill(text: String, color: Color, selected: Boolean, icon: String?, onClick: () -> Unit) {
    Surface(
        onClick = onClick, shape = RoundedCornerShape(50),
        color = if (selected) color.copy(alpha = 0.25f) else Card,
        border = BorderStroke(1.dp, if (selected) color else Color(0xFF33333A))
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { AsyncImage(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
            Text(text, color = if (selected) color else Color.White, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun BuildCard(b: Build, fav: Boolean, onFav: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    val accent = ELEMENT_COLOR[b.element] ?: Color(0xFF8E8E98)
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).background(Card, RoundedCornerShape(18.dp))) {
        Box(Modifier.width(5.dp).fillMaxHeight().background(accent, RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)))
        Column(Modifier.padding(16.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(b.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (b.element in ELEMENTS) { AsyncImage(elementIcon(b.element), null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)) }
                        Text("${b.element} · " + b.weapons.joinToString().replace("Any", "All weapons"),
                            color = accent, fontSize = 13.sp)
                    }
                }
                Text(if (fav) "★" else "☆", fontSize = 26.sp, color = Gold, modifier = Modifier.clickable(onClick = onFav).padding(6.dp))
            }
            Label("ARMOR")
            b.armor.split(", ").forEach { Text("▸ $it", color = Color(0xFFD8D8E0), fontSize = 14.sp) }
            Label("SKILLS")
            b.skills.forEach {
                Text(it, color = Color.White, fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().background(Card2, RoundedCornerShape(10.dp)).padding(10.dp, 8.dp))
            }
            Text(if (open) "Hide tips ▲" else "Show tips & weapon ▼", color = Gold, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { open = !open })
            if (open) Text(b.note, color = Color(0xFFB8B8C0), fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
}

data class Weapon(val name: String, val type: String, val rarity: String, val attack: Int,
                  val affinity: String, val element: String, val elementValue: Int, val sharpness: String, val image: String)

fun loadWeapons(ctx: Context): List<Weapon> {
    val arr = JSONArray(ctx.assets.open("weapons.json").bufferedReader().readText())
    return (0 until arr.length()).map {
        val o = arr.getJSONObject(it)
        Weapon(o.getString("name"), o.getString("type"), o.getString("rarity"), o.getInt("attack"),
            o.getString("affinity"), o.getString("element"), o.getInt("elementValue"), o.getString("sharpness"), o.optString("image"))
    }
}

private val EXTRA_COLOR = mapOf("Poison" to Color(0xFF9BCB3C), "Paralysis" to Color(0xFFE8C13C),
    "Sleep" to Color(0xFF8EA0E8), "Blast" to Color(0xFFE88A3C), "None" to Color(0xFF8E8E98))
private val ALL_ELEM = ELEMENTS + listOf("Poison", "Paralysis", "Sleep", "Blast")
private fun statIcon(e: String): String? {
    val i = ALL_ELEM.indexOf(e); return if (i < 0) null else CDN + "article_tools/mhoutlanders/gacha/attr" + (i + 1) + ".png"
}
private fun sharpColor(s: String) = when (s) {
    "Blue" -> Color(0xFF4A90E8); "Green" -> Color(0xFF5CC85C); else -> Color(0xFF8E8E98)
}

@Composable
fun Root(builds: List<Build>, weapons: List<Weapon>, armors: List<Armor>, prefs: SharedPreferences) {
    var tab by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) { when (tab) { 0 -> BuildsScreen(builds, prefs); 1 -> WeaponsScreen(weapons); else -> ArmorScreen(armors) } }
        NavigationBar(containerColor = Card) {
            NavigationBarItem(tab == 0, { tab = 0 }, { Text("★") }, label = { Text("Builds") })
            NavigationBarItem(tab == 1, { tab = 1 }, { Text("⚔") }, label = { Text("Weapons") })
            NavigationBarItem(tab == 2, { tab = 2 }, { Text("🛡") }, label = { Text("Armor") })
        }
    }
}

@Composable
fun WeaponsScreen(all: List<Weapon>) {
    var type by remember { mutableStateOf<String?>(null) }
    var elem by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    val types = remember { all.map { it.type }.distinct() }
    val elems = remember { all.map { it.element }.filter { it != "None" }.distinct() }
    val shown = all.filter { (type == null || it.type == type) && (elem == null || it.element == elem) &&
        (query.isBlank() || it.name.contains(query, true)) }.sortedByDescending { it.attack }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Weapons", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Gold) }
        item { Text("All 291 weapons · max-upgrade stats", color = Color(0xFF8E8E98), fontSize = 12.sp) }
        item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text("Search weapon name…") }, shape = RoundedCornerShape(14.dp)) }
        item { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(types) { t -> Pill(t, Gold, type == t, WEAPON_ICONS[t]) { type = if (type == t) null else t } } } }
        item { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(elems) { e -> val c = ELEMENT_COLOR[e] ?: EXTRA_COLOR[e] ?: Gold
                Pill(e, c, elem == e, statIcon(e)) { elem = if (elem == e) null else e } } } }
        item { Text("${shown.size} weapon(s)", color = Color(0xFF8E8E98), fontSize = 13.sp) }
        items(shown, key = { it.name + it.type }) { w -> WeaponCard(w) }
    }
}

@Composable
fun WeaponCard(w: Weapon) {
    val c = ELEMENT_COLOR[w.element] ?: EXTRA_COLOR[w.element] ?: Color(0xFF8E8E98)
    var failed by remember(w.name) { mutableStateOf(w.image.isBlank()) }
    Row(Modifier.fillMaxWidth().background(Card, RoundedCornerShape(16.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(56.dp).background(Color(0xFF0D0D10), RoundedCornerShape(12.dp)), Alignment.Center) {
            if (failed) AsyncImage(WEAPON_ICONS[w.type], w.type, Modifier.size(42.dp))
            else AsyncImage("https://monsterhunteroutlanders.cc/images/weapons/db/" + w.image + ".webp", w.name,
                Modifier.size(52.dp), onError = { failed = true })
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(w.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
            Text("${w.type} · Rarity ${w.rarity}", color = Color(0xFF8E8E98), fontSize = 12.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ATK ${w.attack}", color = Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("  Aff ${w.affinity}", color = Color(0xFFD8D8E0), fontSize = 13.sp)
                if (w.sharpness.isNotBlank()) Text("  ● ${w.sharpness}", color = sharpColor(w.sharpness), fontSize = 13.sp)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            statIcon(w.element)?.let { AsyncImage(it, w.element, Modifier.size(20.dp)) }
            Text(if (w.element == "None") "—" else "${w.element} ${w.elementValue}", color = c, fontSize = 12.sp)
        }
    }
}

data class Armor(val name: String, val part: String, val set: String, val setSkill: String,
                 val rarity: String, val defense: Int, val skills: List<String>, val image: String)

fun loadArmors(ctx: Context): List<Armor> {
    val arr = JSONArray(ctx.assets.open("armors.json").bufferedReader().readText())
    return (0 until arr.length()).map {
        val o = arr.getJSONObject(it)
        Armor(o.getString("name"), o.getString("part"), o.getString("set"), o.getString("setSkill"),
            o.getString("rarity"), o.getInt("defense"), o.getJSONArray("skills").strs(), o.getString("image"))
    }
}

@Composable
fun ArmorScreen(all: List<Armor>) {
    var part by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    val parts = listOf("Head", "Chest", "Arms", "Waist", "Legs")
    val shown = all.filter { a -> (part == null || a.part == part) &&
        (query.isBlank() || listOf(a.name, a.set, a.setSkill, a.skills.joinToString()).any { it.contains(query, true) }) }
        .sortedByDescending { it.defense }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Armor", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Gold) }
        item { Text("Top-defense sets · max-upgrade stats", color = Color(0xFF8E8E98), fontSize = 12.sp) }
        item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text("Search a skill, set or piece…") }, shape = RoundedCornerShape(14.dp)) }
        item { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(parts) { p -> Pill(p, Gold, part == p, null) { part = if (part == p) null else p } } } }
        item { Text("${shown.size} piece(s)", color = Color(0xFF8E8E98), fontSize = 13.sp) }
        items(shown, key = { it.name }) { a -> ArmorCard(a) }
    }
}

@Composable
fun ArmorCard(a: Armor) {
    Row(Modifier.fillMaxWidth().background(Card, RoundedCornerShape(16.dp)).padding(12.dp)) {
        Box(Modifier.size(56.dp).background(Color(0xFF0D0D10), RoundedCornerShape(12.dp)), Alignment.Center) {
            AsyncImage(CDN + "article_tools/mhoutlanders/gacha/" + a.image + ".png", a.name, Modifier.size(52.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(a.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
            Text("${a.part} · ${a.set} · Rarity ${a.rarity} · DEF ${a.defense}", color = Color(0xFF8E8E98), fontSize = 12.sp)
            a.skills.forEach { Text("• $it", color = Color(0xFFD8D8E0), fontSize = 13.sp) }
            Text("Set: ${a.setSkill}", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
