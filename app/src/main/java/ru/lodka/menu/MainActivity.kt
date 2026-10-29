package ru.lodka.menu

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.util.Locale

private const val MENU_URL = "https://lodka.moba.market/"
private const val PREFS = "lodka_menu_cache"
private const val CACHE_KEY = "items"
private val Ink = Color(0xFF08090B)
private val Panel = Color(0xFF15171A)
private val Gold = Color(0xFFE2B66D)
private val Cream = Color(0xFFF4EFE5)
private val Muted = Color(0xFFB8B2A8)

data class Dish(val name: String, val price: String, val weight: String, val image: String, val category: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LodkaApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LodkaApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, 0) }
    var dishes by remember { mutableStateOf(readCache(prefs.getString(CACHE_KEY, null))) }
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Все") }
    var page by remember { mutableStateOf("Меню") }
    var selectedDish by remember { mutableStateOf<Dish?>(null) }
    var loading by remember { mutableStateOf(false) }
    var lastUpdated by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun refreshMenu() {
        loading = true; error = null
        try {
            val loaded = withContext(Dispatchers.IO) { MenuParser.fetch() }
            if (loaded.isNotEmpty()) {
                dishes = loaded
                prefs.edit().putString(CACHE_KEY, toJson(loaded)).apply()
                lastUpdated = "Обновлено сейчас"
            } else error = "Не удалось распознать меню на сайте. Проверьте меню по кнопке «Сайт»."
        } catch (_: Exception) {
            error = "Не удалось обновить меню. Проверьте интернет; сохранённые блюда доступны офлайн."
        } finally { loading = false }
    }
    LaunchedEffect(Unit) { refreshMenu() }
    val categories = remember(dishes) {
        listOf("Все") + dishes.map { it.category }.filter { it.isNotBlank() && it != "Меню" }.distinct()
    }
    val filtered = remember(dishes, query, selectedCategory) {
        dishes.filter { (selectedCategory == "Все" || it.category == selectedCategory) && it.name.contains(query, ignoreCase = true) }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Gold, onPrimary = Ink, background = Ink, surface = Panel, onSurface = Cream, secondary = Gold)) {
        Scaffold(
            containerColor = Ink,
            topBar = {
                Column(Modifier.fillMaxWidth().background(Ink)) {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = R.drawable.lodka_wordmark, contentDescription = "La LODKA — ресторан", modifier = Modifier.weight(1f).height(70.dp), contentScale = ContentScale.Fit)
                        IconButton(onClick = { scope.launch { refreshMenu() } }, enabled = !loading) { Icon(Icons.Default.Refresh, "Обновить меню", tint = Gold) }
                    }
                    if (page == "Меню") {
                        OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp), placeholder = { Text("Найти блюдо…", color = Muted) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = Gold) }, singleLine = true, shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, unfocusedBorderColor = Color(0xFF49433A), focusedTextColor = Cream, unfocusedTextColor = Cream))
                        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            items(categories) { category ->
                                FilterChip(selected = selectedCategory == category, onClick = { selectedCategory = category }, label = { Text(category, maxLines = 1) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Gold, selectedLabelColor = Ink, labelColor = Cream))
                            }
                        }
                        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${filtered.size} блюд", style = MaterialTheme.typography.labelMedium, color = Muted)
                            Spacer(Modifier.weight(1f))
                            if (lastUpdated.isNotBlank()) Text(lastUpdated, style = MaterialTheme.typography.labelSmall, color = Muted)
                        }
                    }
                }
            },
            bottomBar = {
                Column(Modifier.fillMaxWidth().background(Ink)) {
                    Divider(color = Color(0xFF3B342B))
                    Text("Сделано Ошурковым А.П.", modifier = Modifier.fillMaxWidth().padding(top = 7.dp, bottom = 5.dp), color = Muted, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    NavigationBar(containerColor = Color(0xFF101113), contentColor = Gold) {
                        listOf("Меню", "Отзывы", "Контакты").forEach { item ->
                            NavigationBarItem(selected = page == item, onClick = { page = item }, icon = { Text(when (item) { "Меню" -> "≋"; "Отзывы" -> "★"; else -> "⌖" }, fontSize = 20.sp) }, label = { Text(item) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Gold, selectedTextColor = Gold, unselectedIconColor = Muted, unselectedTextColor = Muted, indicatorColor = Color(0xFF302719)))
                        }
                    }
                }
            }
        ) { padding ->
            when (page) {
                "Меню" -> Column(Modifier.padding(padding).fillMaxSize()) {
                    if (error != null) Surface(color = Color(0xFF35271D), modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp), shape = RoundedCornerShape(12.dp)) { Text(error!!, Modifier.padding(12.dp), color = Cream, style = MaterialTheme.typography.bodySmall) }
                    if (loading && dishes.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Gold) }
                    else if (filtered.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(if (dishes.isEmpty()) "Меню пока не загружено" else "Ничего не найдено", color = Muted) }
                    else LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 7.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(filtered, key = { it.name + it.category + it.price }) { dish -> DishCard(dish) { selectedDish = dish } }
                    }
                }
                "Отзывы" -> ReviewsPage(Modifier.padding(padding))
                else -> ContactsPage(Modifier.padding(padding))
            }
        }
        selectedDish?.let { dish -> DishDetailsDialog(dish = dish, onDismiss = { selectedDish = null }, onOpenSite = { openUrl(context, MENU_URL) }) }
    }
}

@Composable
private fun DishCard(dish: Dish, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).clickable(onClick = onClick).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(model = dish.image.ifBlank { null }, contentDescription = dish.name, modifier = Modifier.size(112.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF252525)), contentScale = ContentScale.Crop)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(dish.category.uppercase(Locale("ru")), fontSize = 9.sp, letterSpacing = 1.sp, color = Gold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(dish.name, fontSize = 15.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold, color = Cream, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (dish.weight.isNotBlank()) Text(dish.weight, fontSize = 12.sp, color = Muted)
            Text(dish.price, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Gold)
            Text("Состав и описание  ›", fontSize = 11.sp, color = Muted)
        }
    }
}

@Composable
private fun DishDetailsDialog(dish: Dish, onDismiss: () -> Unit, onOpenSite: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = Panel, titleContentColor = Cream, textContentColor = Cream,
        title = { Text(dish.name, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AsyncImage(model = dish.image.ifBlank { null }, contentDescription = dish.name, modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)).background(Ink), contentScale = ContentScale.Crop)
                Text(dish.price, color = Gold, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                if (dish.weight.isNotBlank()) Text("Вес / порция: ${dish.weight}", color = Muted)
                Text("Описание и состав", color = Gold, fontWeight = FontWeight.SemiBold)
                Text("Подробный состав и описание этого блюда не передаются в доступной карточке меню. Чтобы не указывать неподтверждённые ингредиенты, откройте официальное меню ресторана.", color = Cream, fontSize = 13.sp)
            }
        }, confirmButton = { TextButton(onClick = onOpenSite) { Icon(Icons.Default.OpenInNew, null, tint = Gold); Spacer(Modifier.width(5.dp)); Text("Открыть сайт", color = Gold) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Закрыть", color = Muted) } })
}

@Composable
private fun ReviewsPage(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val reviews = listOf(
        "Максим" to "Качество продукта отличное!!!",
        "Татьяна" to "Прекрасное заведение для тихих и уютных вечеров. Кухня выше всяких похвал. Устрицы бесподобны.",
        "Виктор Симонов" to "Спасибо за отличный обед и хорошее обслуживание. Помогли сделать правильный выбор блюда.",
        "Галина" to "Спасибо 🥰, очень вкусно и атмосферно!"
    )
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Отзывы гостей", color = Gold, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text("Отзывы, опубликованные на сайте ресторана", color = Muted, fontSize = 13.sp)
        reviews.forEach { (name, review) ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Panel).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("★★★★★", color = Gold, fontSize = 17.sp)
                Text(name, color = Cream, fontWeight = FontWeight.SemiBold)
                Text(review, color = Muted, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
        Button(onClick = { openUrl(context, MENU_URL) }, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink), modifier = Modifier.fillMaxWidth()) { Text("Оставить отзыв на сайте") }
    }
}

@Composable
private fun ContactsPage(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Контакты", color = Gold, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text("Ресторан морепродуктов la LODKA", color = Cream, fontSize = 17.sp)
        ContactRow(Icons.Default.LocationOn, "Адрес", "Тюмень, ул. Семакова, 30") { openUrl(context, "https://maps.google.com/?q=Тюмень+Семакова+30") }
        ContactRow(Icons.Default.Call, "Телефон", "+7 (982) 787-45-54") { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+79827874554"))) }
        ContactRow(Icons.Default.OpenInNew, "Время работы", "Ежедневно, 12:00–00:00") { openUrl(context, MENU_URL) }
        ContactRow(Icons.Default.OpenInNew, "Официальный сайт", "lodka.moba.market") { openUrl(context, MENU_URL) }
        Spacer(Modifier.height(6.dp))
        Text("Сделано Ошурковым А.П.", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun ContactRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, detail: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Panel).clickable(onClick = onClick).padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Gold, modifier = Modifier.size(26.dp)); Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) { Text(title, color = Cream, fontWeight = FontWeight.SemiBold); Text(detail, color = Muted, fontSize = 13.sp) }
        Icon(Icons.Default.OpenInNew, null, tint = Muted, modifier = Modifier.size(17.dp))
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Exception) { }
}

private object MenuParser {
    private val categoryNames = listOf("🔥Акции", "Закуски", "Горячие закуски", "Мидии", "Салаты", "Супы", "Горячее", "Пасты и ризотто", "Локальная кухня", "Рыба", "Гады", "Устрицы и другие моллюски", "Суши", "Гунканы", "Роллы", "Гарниры", "Десерты", "Соусы", "Хлеб ремесленный", "Напитки")
    fun fetch(): List<Dish> {
        val doc = Jsoup.connect(MENU_URL).userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36").timeout(20000).get()
        val found = LinkedHashMap<String, Dish>(); var currentCategory = "Меню"
        for (el in doc.getAllElements()) {
            val text = el.ownText().trim()
            if (text.isNotEmpty() && isCategoryMarker(el, text)) currentCategory = categoryNames.firstOrNull { it.equals(text, ignoreCase = true) } ?: text
            if (el.tagName() != "img") continue
            val name = (el.attr("alt").ifBlank { el.attr("title") }).trim()
            val image = absoluteImage(el, doc.location())
            if (name.length < 2 || image.isBlank()) continue
            if (!image.contains("pics.pizket.com") && !image.contains("moba.market")) continue
            val card = findCard(el, name) ?: continue
            val cardText = card.text().replace(Regex("\\s+"), " ").trim()
            val price = extractPrice(cardText, name); if (price.isBlank()) continue
            val weight = Regex("(?i)\\b\\d{1,4}(?:[.,]\\d+)?\\s*(?:г|гр|кг|мл|л|шт\\.?|порц\\.?|см)\\b").find(cardText.substringAfter(name, ""))?.value?.trim().orEmpty()
            val category = findCategory(card, currentCategory)
            found.putIfAbsent(name.lowercase(Locale("ru")) + "|" + price, Dish(name, price, weight, image, category))
        }
        return found.values.toList()
    }
    private fun isCategoryMarker(el: Element, text: String): Boolean {
        if (categoryNames.none { it.equals(text, ignoreCase = true) }) return false
        val tag = el.tagName().lowercase(Locale.ROOT); val cls = el.className().lowercase(Locale.ROOT)
        return tag in setOf("h1", "h2", "h3", "h4", "h5", "h6") || cls.contains("category-title") || cls.contains("section-title") || cls.contains("group-title")
    }
    private fun findCard(img: Element, name: String): Element? {
        var node: Element? = img; var best: Element? = null; var depth = 0
        while (node != null && depth < 9) { val t = node.text().replace(Regex("\\s+"), " ").trim(); if (t.contains(name, true) && Regex("\\d{2,5}").containsMatchIn(t)) { best = node; if (t.length > name.length + 120) break }; node = node.parent(); depth++ }
        return best
    }
    private fun extractPrice(text: String, name: String): String {
        val beforeName = text.substringBefore(name)
        val candidate = Regex("(?i)(?:от\\s*)?\\b\\d{2,5}(?:[\\s ]\\d{3})*\\b").findAll(beforeName).toList().lastOrNull()?.value?.trim() ?: Regex("(?i)от\\s*\\d{2,5}").find(text)?.value?.trim() ?: return ""
        val digits = candidate.replace(Regex("\\D"), ""); val value = digits.toIntOrNull() ?: return ""; if (value !in 100..100000) return ""
        val prefix = if (candidate.lowercase(Locale.ROOT).startsWith("от")) "от " else ""
        return prefix + String.format(Locale("ru"), "%,d", value).replace(',', ' ') + " ₽"
    }
    private fun findCategory(card: Element, fallback: String): String {
        var node: Element? = card
        repeat(7) { if (node == null) return@repeat; for (candidate in node!!.select("h1,h2,h3,h4,h5,h6,.category-title,.section-title,.group-title")) { val t = candidate.text().trim(); if (categoryNames.any { it.equals(t, true) }) return t }; node = node!!.parent() }
        return fallback
    }
    private fun absoluteImage(img: Element, base: String): String {
        val raw = img.attr("data-src").ifBlank { img.attr("src") }.ifBlank { img.attr("data-original") }
        if (raw.isBlank() || raw.startsWith("data:")) return ""
        return try { URL(URL(base), raw).toString() } catch (_: Exception) { "" }
    }
}

private fun toJson(dishes: List<Dish>): String {
    val arr = JSONArray(); dishes.forEach { arr.put(JSONObject().put("name", it.name).put("price", it.price).put("weight", it.weight).put("image", it.image).put("category", it.category)) }; return arr.toString()
}
private fun readCache(raw: String?): List<Dish> {
    if (raw.isNullOrBlank()) return emptyList()
    return try { val arr = JSONArray(raw); (0 until arr.length()).map { i -> val o = arr.getJSONObject(i); Dish(o.optString("name"), o.optString("price"), o.optString("weight"), o.optString("image"), o.optString("category", "Меню")) } } catch (_: Exception) { emptyList() }
}
