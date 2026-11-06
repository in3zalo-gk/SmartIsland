/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */
package com.agupta07505.smartisland.ui.glance

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** A self-contained, embeddable Glance gallery. It owns no app navigation or settings repository. */
@Composable
fun GlanceStudio(
    modifier: Modifier = Modifier,
    allowPermissionRequest: Boolean = true
) {
    val context = LocalContext.current
    val store = remember(context) { GlanceLocalStore(context.applicationContext) }
    var page by rememberSaveable { mutableStateOf(GlancePage.SUMMARY) }
    var style by rememberSaveable(page) { mutableStateOf(store.styleFor(page)) }
    val now = rememberMinuteClock()
    val styles = GlanceStyle.forPage(page)

    Column(modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Glance Studio", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Informações locais, em cartões compactos.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(GlancePage.entries) { item ->
                AssistChip(onClick = { page = item; style = store.styleFor(item) }, label = { Text(item.label) }, leadingIcon = if (page == item) ({ Icon(Icons.Default.Check, null) }) else null)
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(styles) { candidate ->
                AssistChip(onClick = { style = candidate; store.saveStyle(page, candidate) }, label = { Text(candidate.label) }, leadingIcon = if (style == candidate) ({ Icon(Icons.Default.Check, null) }) else null)
            }
        }
        when (page) {
            GlancePage.SUMMARY -> SummaryGlance(style, now, context)
            GlancePage.CALENDAR -> CalendarGlance(style, context, allowPermissionRequest)
            GlancePage.WATER -> WaterGlance(style, store)
            GlancePage.PRAYER -> PrayerGlance(style, store, now, context)
        }
    }
}

enum class GlancePage(val label: String) { SUMMARY("Resumo"), CALENDAR("Agenda"), WATER("Água"), PRAYER("Oração") }

enum class GlanceStyle(val label: String, val page: GlancePage) {
    CLASSIC("Clássico", GlancePage.SUMMARY), DOT_MATRIX("Dot matrix", GlancePage.SUMMARY), CHRONOGRAPH("Cronógrafo", GlancePage.SUMMARY), BENTO_SUMMARY("Bento", GlancePage.SUMMARY),
    CAL_CLASSIC("Clássico", GlancePage.CALENDAR), TWENTY_FOUR("24 horas", GlancePage.CALENDAR), GLASS("Glass", GlancePage.CALENDAR), BIG_DATE("Data grande", GlancePage.CALENDAR),
    WATER_CLASSIC("Clássico", GlancePage.WATER), LIQUID("Liquid", GlancePage.WATER), EXPRESSIVE("Expressive", GlancePage.WATER), BENTO_WATER("Bento", GlancePage.WATER),
    PRAYER_CLASSIC("Clássico", GlancePage.PRAYER), SKY("Sky", GlancePage.PRAYER), COUNTDOWN("Countdown", GlancePage.PRAYER), TIMELINE("Timeline", GlancePage.PRAYER);
    companion object { fun forPage(page: GlancePage) = entries.filter { it.page == page } }
}

/** Isolated preferences namespace: Glance selections never touch the app settings repository. */
class GlanceLocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("glance_studio_private", Context.MODE_PRIVATE)
    fun styleFor(page: GlancePage): GlanceStyle = runCatching { GlanceStyle.valueOf(prefs.getString("style_${page.name}", defaultStyle(page).name)!!) }.getOrElse { defaultStyle(page) }
    fun saveStyle(page: GlancePage, style: GlanceStyle) { prefs.edit().putString("style_${page.name}", style.name).apply() }
    fun water(day: String): Int { if (prefs.getString("water_day", "") != day) { prefs.edit().putString("water_day", day).putInt("water_ml", 0).apply() }; return prefs.getInt("water_ml", 0) }
    fun addWater(day: String, amount: Int): Int { val next = (water(day) + amount).coerceAtLeast(0); prefs.edit().putInt("water_ml", next).apply(); return next }
    fun waterGoal(): Int = prefs.getInt("water_goal", 2000)
    fun saveWaterGoal(goal: Int) { prefs.edit().putInt("water_goal", goal.coerceIn(250, 10000)).apply() }
    fun prayerTimes(): String = prefs.getString("prayer_times", "")!!
    fun savePrayerTimes(value: String) { prefs.edit().putString("prayer_times", value).apply() }
    companion object { fun defaultStyle(page: GlancePage) = when (page) { GlancePage.SUMMARY -> GlanceStyle.CLASSIC; GlancePage.CALENDAR -> GlanceStyle.CAL_CLASSIC; GlancePage.WATER -> GlanceStyle.WATER_CLASSIC; GlancePage.PRAYER -> GlanceStyle.PRAYER_CLASSIC } }
}

@Composable private fun SummaryGlance(style: GlanceStyle, now: Date, context: Context) {
    val time = SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(now)
    val date = DateFormat.getDateInstance(DateFormat.FULL, Locale("pt", "BR")).format(now)
    val battery = remember(now) { (context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100) }
    when (style) {
        GlanceStyle.DOT_MATRIX -> DarkCard { Text(time, fontFamily = FontFamily.Monospace, fontSize = 48.sp, letterSpacing = 5.sp, color = Color(0xFF9CFF9C)); Text(date, color = Color(0xFF9CFF9C)); DotLine(); Text("BATERIA  $battery%", fontFamily = FontFamily.Monospace, color = Color(0xFF9CFF9C)) }
        GlanceStyle.CHRONOGRAPH -> Chronograph(time, date, battery)
        GlanceStyle.BENTO_SUMMARY -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetricCard("Hora", time, Modifier.weight(1f)); MetricCard("Bateria", "$battery%", Modifier.weight(1f)); MetricCard("Hoje", SimpleDateFormat("dd MMM", Locale("pt", "BR")).format(now), Modifier.weight(1f)) }
        else -> ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp)) { Text(time, fontSize = 48.sp, fontWeight = FontWeight.Bold); Text(date, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(14.dp)); BatteryBar(battery) } }
    }
}
@Composable private fun Chronograph(time: String, date: String, battery: Int) {
    val color = MaterialTheme.colorScheme.onTertiaryContainer
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) { Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) { Canvas(Modifier.matchParentSize()) { drawCircle(Color.Transparent, style = Stroke(6.dp.toPx())); (0 until 12).forEach { i -> val a = Math.toRadians((i * 30 - 90).toDouble()); val c = center + Offset(cos(a).toFloat(), sin(a).toFloat()) * size.minDimension * .40f; drawCircle(color, 3.dp.toPx(), c) } }; Text(time, fontSize = 34.sp, fontWeight = FontWeight.Bold) }; Text(date); Text("Bateria: $battery%") } }
}
@Composable private fun BatteryBar(value: Int) { Column { Text("Bateria $value%", style = MaterialTheme.typography.labelLarge); Box(Modifier.fillMaxWidth().height(9.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) { Box(Modifier.fillMaxWidth(value / 100f).height(9.dp).background(MaterialTheme.colorScheme.primary)) } } }

@Composable private fun CalendarGlance(
    style: GlanceStyle,
    context: Context,
    allowPermissionRequest: Boolean
) {
    var granted by remember { mutableStateOf(context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) }
    val activityResultRegistryOwner = LocalActivityResultRegistryOwner.current
    val request = if (allowPermissionRequest && activityResultRegistryOwner != null) {
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    } else {
        null
    }
    val events = remember(granted) { if (granted) readNextEvents(context) else emptyList() }
    val today = Calendar.getInstance()
    when (style) {
        GlanceStyle.TWENTY_FOUR -> ScheduleCard("Agenda 24 horas", events, true)
        GlanceStyle.GLASS -> Card(Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp)), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .65f))) { ScheduleContent(events, false) }
        GlanceStyle.BIG_DATE -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { Card(Modifier.width(105.dp)) { Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(SimpleDateFormat("dd", Locale("pt", "BR")).format(today.time), fontSize = 46.sp, fontWeight = FontWeight.Bold); Text(SimpleDateFormat("MMM", Locale("pt", "BR")).format(today.time).uppercase()) } }; ScheduleCard("Próximos", events, false, Modifier.weight(1f)) }
        else -> ScheduleCard("Calendário", events, false)
    }
    if (!granted) {
        if (request != null) {
            Button(onClick = { request.launch(Manifest.permission.READ_CALENDAR) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.CalendarMonth, null); Spacer(Modifier.width(8.dp)); Text("Permitir acesso ao calendário") }
        } else {
            Text("Abra o app para autorizar o acesso ao calendário.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
private data class CalendarEvent(val title: String, val start: Long)
private fun readNextEvents(context: Context): List<CalendarEvent> = runCatching {
    val begin = System.currentTimeMillis(); val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().appendPath(begin.toString()).appendPath((begin + 7 * 86_400_000L).toString()).build()
    context.contentResolver.query(uri, arrayOf(CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN), null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c -> buildList { while (c.moveToNext() && size < 4) add(CalendarEvent(c.getString(0).orEmpty().ifBlank { "Sem título" }, c.getLong(1))) } } ?: emptyList()
}.getOrDefault(emptyList())
@Composable private fun ScheduleCard(title: String, events: List<CalendarEvent>, twentyFour: Boolean, modifier: Modifier = Modifier) = ElevatedCard(modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(title, fontWeight = FontWeight.Bold); ScheduleContent(events, twentyFour) } }
@Composable private fun ScheduleContent(events: List<CalendarEvent>, twentyFour: Boolean) { Spacer(Modifier.height(8.dp)); if (events.isEmpty()) Text("Nenhum próximo evento${if (twentyFour) " nas próximas 24 h" else ""}.", color = MaterialTheme.colorScheme.onSurfaceVariant) else events.filter { !twentyFour || it.start <= System.currentTimeMillis() + 86_400_000L }.forEach { event -> Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) { Text(SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(Date(event.start)), fontFamily = FontFamily.Monospace); Spacer(Modifier.width(10.dp)); Text(event.title, maxLines = 1) } } }

@Composable private fun WaterGlance(style: GlanceStyle, store: GlanceLocalStore) {
    val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(rememberMinuteClock())
    var amount by remember { mutableIntStateOf(store.water(day)) }
    var goal by remember { mutableIntStateOf(store.waterGoal()) }
    var editing by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(day) { amount = store.water(day) }
    val percent = (amount.toFloat() / goal).coerceIn(0f, 1f)
    val animated by animateFloatAsState(percent, tween(500), label = "waterLevel")
    when (style) {
        GlanceStyle.LIQUID -> LiquidCard(amount, goal, animated)
        GlanceStyle.EXPRESSIVE -> Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (percent >= 1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer)) { Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(if (percent >= 1) "Meta atingida!" else "Vamos hidratar", fontSize = 25.sp, fontWeight = FontWeight.Bold); Text("$amount de $goal ml", fontSize = 18.sp); WaterActions { delta -> amount = store.addWater(day, delta) } } }
        GlanceStyle.BENTO_WATER -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetricCard("Consumido", "$amount ml", Modifier.weight(1f)); MetricCard("Falta", "${(goal - amount).coerceAtLeast(0)} ml", Modifier.weight(1f)); Column(Modifier.weight(1f)) { WaterActions { delta -> amount = store.addWater(day, delta) } } }
        else -> ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text("Hidratação", fontWeight = FontWeight.Bold); Text("$amount / $goal ml"); Spacer(Modifier.height(8.dp)); BatteryBar((percent * 100).toInt()); WaterActions { delta -> amount = store.addWater(day, delta) } } }
    }
    TextButton(onClick = { editing = !editing }) { Icon(Icons.Default.Edit, null); Text("Editar meta diária") }
    if (editing) OutlinedTextField(value = goal.toString(), onValueChange = { it.toIntOrNull()?.let { number -> goal = number.coerceIn(250, 10000); store.saveWaterGoal(goal) } }, label = { Text("Meta em ml") }, singleLine = true)
}
@Composable private fun LiquidCard(amount: Int, goal: Int, level: Float) = Card(Modifier.fillMaxWidth()) { Box(Modifier.height(180.dp).fillMaxWidth()) { Canvas(Modifier.matchParentSize()) { drawRoundRect(Color(0xFF1B6EAF), cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx())); drawRect(Color(0xFF66C9FF), topLeft = Offset(0f, size.height * (1f - level)), size = androidx.compose.ui.geometry.Size(size.width, size.height * level)); for (x in 0..8) drawCircle(Color.White.copy(alpha = .25f), 3.dp.toPx(), Offset(x * size.width / 8, size.height * (1f - level))) }; Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) { Text("$amount ml", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold); Text("meta $goal ml", color = Color.White) } } }
@Composable private fun WaterActions(onChange: (Int) -> Unit) = Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) { Button(onClick = { onChange(250) }) { Icon(Icons.Default.Add, null); Text("250 ml") }; TextButton(onClick = { onChange(-250) }) { Text("Desfazer") } }

@Composable private fun PrayerGlance(style: GlanceStyle, store: GlanceLocalStore, now: Date, context: Context) {
    var raw by remember { mutableStateOf(store.prayerTimes()) }; var edit by rememberSaveable { mutableStateOf(false) }; val prayers = remember(raw) { parsePrayers(raw) }; val next = nextPrayer(prayers, now)
    when (style) {
        GlanceStyle.SKY -> Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF18284B))) { Column(Modifier.padding(20.dp)) { Text("Próxima oração", color = Color(0xFFE1E9FF)); Text(next?.first ?: "Sem horários", fontSize = 30.sp, color = Color.White, fontWeight = FontWeight.Bold); Text(next?.second ?: "Configure os horários", color = Color(0xFFE1E9FF)) } }
        GlanceStyle.COUNTDOWN -> CountdownCard(next, now)
        GlanceStyle.TIMELINE -> PrayerTimeline(prayers, next)
        else -> SchedulePrayerCard(next, prayers)
    }
    TextButton(onClick = { edit = !edit }) { Icon(Icons.Default.Edit, null); Text("Editar horários manuais") }
    if (edit) { OutlinedTextField(value = raw, onValueChange = { raw = it; store.savePrayerTimes(it) }, modifier = Modifier.fillMaxWidth(), label = { Text("Nome=HH:mm, separado por |") }, supportingText = { Text("Ex.: Fajr=05:30|Dhuhr=12:15. Não usa localização nem cálculo astronômico.") }) }
    FocusTimer(context)
}
@Composable private fun PrayerTimeline(prayers: List<Pair<String, String>>, next: Pair<String, String>?) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("Linha do dia", fontWeight = FontWeight.Bold); prayers.forEach { (name, hour) -> Row(Modifier.padding(vertical = 6.dp)) { Canvas(Modifier.size(18.dp)) { drawCircle(if (name == next?.first) primary else outline, 6.dp.toPx(), center) }; Text("$name  $hour") } } } }
}
private fun parsePrayers(raw: String): List<Pair<String, String>> = raw.split("|").mapNotNull { token -> token.split("=", limit = 2).takeIf { it.size == 2 && Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(it[1].trim()) }?.let { it[0].trim() to it[1].trim() } }
private fun nextPrayer(prayers: List<Pair<String, String>>, now: Date): Pair<String, String>? { val current = SimpleDateFormat("HH:mm", Locale.US).format(now); return prayers.firstOrNull { it.second > current } ?: prayers.firstOrNull() }
@Composable private fun SchedulePrayerCard(next: Pair<String, String>?, prayers: List<Pair<String, String>>) = ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("Próxima oração", fontWeight = FontWeight.Bold); Text(next?.let { "${it.first} • ${it.second}" } ?: "Defina horários manuais"); HorizontalDivider(Modifier.padding(vertical = 10.dp)); Text(prayers.joinToString("   ") { "${it.first} ${it.second}" }, style = MaterialTheme.typography.bodySmall) } }
@Composable private fun CountdownCard(next: Pair<String, String>?, now: Date) { val minutes = next?.second?.let { value -> val fields = value.split(":").map(String::toInt); val cal = Calendar.getInstance().apply { time = now }; (fields[0] * 60 + fields[1] - (cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)) + 1440) % 1440 }; Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("Contagem local", fontWeight = FontWeight.Bold); Text(next?.first ?: "Sem próxima oração", fontSize = 24.sp); Text(minutes?.let { "${it / 60}h ${it % 60}min" } ?: "—", fontSize = 38.sp, fontWeight = FontWeight.Bold) } } }
@Composable private fun FocusTimer(context: Context) { var remaining by rememberSaveable { mutableLongStateOf(0L) }; var running by rememberSaveable { mutableStateOf(false) }; LaunchedEffect(running) { while (running && remaining > 0) { delay(1000); remaining -= 1000 }; if (remaining <= 0) running = false }; Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Foco local", fontWeight = FontWeight.Bold); Text(if (remaining == 0L) "25 min disponíveis" else "%02d:%02d".format(remaining / 60000, (remaining / 1000) % 60)); Text("Funciona só enquanto esta tela está aberta.", style = MaterialTheme.typography.labelSmall) }; IconButton(onClick = { if (remaining == 0L) remaining = 25 * 60_000L; running = !running }, modifier = Modifier.semantics { contentDescription = if (running) "Pausar foco" else "Iniciar foco" }) { Icon(if (running) Icons.Default.Stop else Icons.Default.PlayArrow, null) }; IconButton(onClick = { remaining = 0; running = false }) { Icon(Icons.Default.Stop, "Cancelar") }; IconButton(onClick = { Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).also { if (it.resolveActivity(context.packageManager) != null) context.startActivity(it) } }) { Icon(Icons.Default.Alarm, "Abrir alarmes") } } } }

@Composable private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) = Card(modifier) { Column(Modifier.padding(12.dp)) { Text(label, style = MaterialTheme.typography.labelSmall); Text(value, fontWeight = FontWeight.Bold, maxLines = 1) } }
@Composable private fun DarkCard(content: @Composable () -> Unit) = Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF102014))) { Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { content() } }
@Composable private fun DotLine() = Canvas(Modifier.fillMaxWidth().height(18.dp)) { for (x in 0..20) drawCircle(Color(0xFF9CFF9C), 2.dp.toPx(), Offset(x * size.width / 20, size.height / 2)) }
@Composable private fun rememberMinuteClock(): Date { var now by remember { mutableStateOf(Date()) }; LaunchedEffect(Unit) { while (true) { now = Date(); delay(1000) } }; return now }
