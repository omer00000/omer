package com.omerfaruk.dongu

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val Pink = Color(0xFFF2A7C2)
private val PinkStrong = Color(0xFFFF7FAF)
private val Lavender = Color(0xFFD9A7FF)
private val Bg = Color(0xFF101014)
private val CardBg = Color(0xFF19191F)
private val Soft = Color(0xFF24242C)
private val Text2 = Color(0xFFBEB5BA)

private val DonguColors = darkColorScheme(
    primary = Pink,
    onPrimary = Color(0xFF321421),
    secondary = Lavender,
    background = Bg,
    surface = CardBg,
    surfaceVariant = Soft,
    onBackground = Color(0xFFF8F2F5),
    onSurface = Color(0xFFF8F2F5),
    onSurfaceVariant = Text2
)

data class DayEntry(
    val epochDay: Long,
    val periodStart: Boolean = false,
    val periodEnd: Boolean = false,
    val mood: String = "",
    val note: String = "",
    val pain: Int = 0,
    val spotting: Boolean = false
) {
    val date: LocalDate get() = LocalDate.ofEpochDay(epochDay)

    fun isEmpty(): Boolean =
        !periodStart && !periodEnd && mood.isBlank() && note.isBlank() && pain == 0 && !spotting
}

data class Stats(
    val averageCycle: Int?,
    val averagePeriod: Int?,
    val cycleLengths: List<Int>,
    val nextPeriod: LocalDate?,
    val ovulation: LocalDate?,
    val fertileStart: LocalDate?,
    val fertileEnd: LocalDate?,
    val regularity: String,
    val deviation: Double?
)

data class UiState(
    val entries: List<DayEntry> = emptyList(),
    val stats: Stats = calculateStats(emptyList()),
    val reminderEnabled: Boolean = true,
    val reminderDays: Int = 2
)

data class PeriodInterval(val start: LocalDate, val end: LocalDate)

private class Store(context: Context) {
    private val prefs = context.getSharedPreferences("dongu_data", Context.MODE_PRIVATE)

    fun loadEntries(): List<DayEntry> {
        val raw = prefs.getString("entries", "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(
                        DayEntry(
                            epochDay = item.getLong("d"),
                            periodStart = item.optBoolean("s"),
                            periodEnd = item.optBoolean("e"),
                            mood = item.optString("m"),
                            note = item.optString("n"),
                            pain = item.optInt("p").coerceIn(0, 5),
                            spotting = item.optBoolean("sp")
                        )
                    )
                }
            }.sortedBy { it.epochDay }
        }.getOrDefault(emptyList())
    }

    fun saveEntries(entries: List<DayEntry>) {
        val array = JSONArray()
        entries.sortedBy { it.epochDay }.forEach { entry ->
            array.put(
                JSONObject()
                    .put("d", entry.epochDay)
                    .put("s", entry.periodStart)
                    .put("e", entry.periodEnd)
                    .put("m", entry.mood)
                    .put("n", entry.note)
                    .put("p", entry.pain)
                    .put("sp", entry.spotting)
            )
        }
        prefs.edit().putString("entries", array.toString()).apply()
    }

    fun reminderEnabled(): Boolean = prefs.getBoolean("reminder", true)
    fun reminderDays(): Int = prefs.getInt("reminder_days", 2).coerceIn(0, 7)

    fun saveReminder(enabled: Boolean, days: Int) {
        prefs.edit()
            .putBoolean("reminder", enabled)
            .putInt("reminder_days", days.coerceIn(0, 7))
            .apply()
    }
}

class CycleViewModel(application: Application) : AndroidViewModel(application) {
    private val store = Store(application)
    private val _state = MutableStateFlow(
        UiState(
            entries = store.loadEntries(),
            reminderEnabled = store.reminderEnabled(),
            reminderDays = store.reminderDays()
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        refresh(_state.value.entries)
    }

    private fun refresh(entries: List<DayEntry>) {
        val stats = calculateStats(entries)
        _state.value = _state.value.copy(
            entries = entries.sortedBy { it.epochDay },
            stats = stats
        )
        if (_state.value.reminderEnabled) {
            ReminderScheduler.schedule(getApplication(), stats.nextPeriod, _state.value.reminderDays)
        } else {
            ReminderScheduler.cancel(getApplication())
        }
    }

    private fun mutate(date: LocalDate, transform: (DayEntry) -> DayEntry) {
        val entries = _state.value.entries.toMutableList()
        val index = entries.indexOfFirst { it.epochDay == date.toEpochDay() }
        val old = if (index >= 0) entries[index] else DayEntry(date.toEpochDay())
        val updated = transform(old)

        if (updated.isEmpty()) {
            if (index >= 0) entries.removeAt(index)
        } else if (index >= 0) {
            entries[index] = updated
        } else {
            entries.add(updated)
        }

        store.saveEntries(entries)
        refresh(entries)
    }

    fun togglePeriodStart(date: LocalDate) = mutate(date) {
        it.copy(periodStart = !it.periodStart)
    }

    fun togglePeriodEnd(date: LocalDate) = mutate(date) {
        it.copy(periodEnd = !it.periodEnd)
    }

    fun saveDay(date: LocalDate, mood: String, note: String, pain: Int, spotting: Boolean) =
        mutate(date) {
            it.copy(
                mood = mood,
                note = note.trim(),
                pain = pain.coerceIn(0, 5),
                spotting = spotting
            )
        }

    fun setReminderEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(reminderEnabled = enabled)
        store.saveReminder(enabled, _state.value.reminderDays)
        refresh(_state.value.entries)
    }

    fun setReminderDays(days: Int) {
        val safeDays = days.coerceIn(0, 7)
        _state.value = _state.value.copy(reminderDays = safeDays)
        store.saveReminder(_state.value.reminderEnabled, safeDays)
        refresh(_state.value.entries)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel(this)
        setContent {
            MaterialTheme(colorScheme = DonguColors) {
                val notificationPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { }

                DonguApp(
                    requestNotificationPermission = {
                        if (
                            Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }
        }
    }
}

private fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= 26) {
        val channel = NotificationChannel(
            "dongu_reminders",
            "Döngü hatırlatmaları",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

object ReminderScheduler {
    private const val REQUEST_CODE = 4401

    fun schedule(context: Context, nextPeriod: LocalDate?, daysBefore: Int) {
        cancel(context)
        if (nextPeriod == null) return

        val target = nextPeriod
            .minusDays(daysBefore.toLong())
            .atTime(9, 0)
            .atZone(ZoneId.systemDefault())

        if (!target.isAfter(ZonedDateTime.now())) return

        val intent = Intent(context, PeriodReminderReceiver::class.java)
            .putExtra("days", daysBefore)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            target.toInstant().toEpochMilli(),
            pendingIntent
        )
    }

    fun cancel(context: Context) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, PeriodReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}

class PeriodReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        createNotificationChannel(context)
        val days = intent.getIntExtra("days", 0)
        val text = when (days) {
            0 -> "Tahmine göre regl bugün başlayabilir."
            1 -> "Tahmine göre regl yarın başlayabilir."
            else -> "Tahmine göre regle yaklaşık $days gün kaldı."
        }
        val openApp = PendingIntent.getActivity(
            context,
            4402,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, "dongu_reminders")
            .setSmallIcon(R.drawable.ic_stat_drop)
            .setContentTitle("Döngü")
            .setContentText(text)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()

        if (
            Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(4403, notification)
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val store = Store(context)
        if (store.reminderEnabled()) {
            ReminderScheduler.schedule(
                context,
                calculateStats(store.loadEntries()).nextPeriod,
                store.reminderDays()
            )
        }
    }
}

private enum class AppTab { CALENDAR, STATS, SETTINGS }

@Composable
private fun DonguApp(
    viewModel: CycleViewModel = viewModel(),
    requestNotificationPermission: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableStateOf(AppTab.CALENDAR) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            NavigationBar(containerColor = CardBg) {
                NavigationBarItem(
                    selected = selectedTab == AppTab.CALENDAR,
                    onClick = { selectedTab = AppTab.CALENDAR },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                    label = { Text("Takvim") }
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.STATS,
                    onClick = { selectedTab = AppTab.STATS },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                    label = { Text("Analiz") }
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.SETTINGS,
                    onClick = { selectedTab = AppTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Ayarlar") }
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            AppTab.CALENDAR -> CalendarScreen(
                state = state,
                modifier = Modifier.padding(padding),
                onDateClick = { selectedDate = it },
                onQuickStart = { viewModel.togglePeriodStart(LocalDate.now()) }
            )

            AppTab.STATS -> StatsScreen(
                state = state,
                modifier = Modifier.padding(padding)
            )

            AppTab.SETTINGS -> SettingsScreen(
                state = state,
                modifier = Modifier.padding(padding),
                onReminderChanged = { enabled ->
                    viewModel.setReminderEnabled(enabled)
                    if (enabled) requestNotificationPermission()
                },
                onReminderDaysChanged = viewModel::setReminderDays
            )
        }
    }

    selectedDate?.let { date ->
        val entry = state.entries.firstOrNull { it.epochDay == date.toEpochDay() }
        DaySheet(
            date = date,
            entry = entry,
            onDismiss = { selectedDate = null },
            onToggleStart = { viewModel.togglePeriodStart(date) },
            onToggleEnd = { viewModel.togglePeriodEnd(date) },
            onSave = { mood, note, pain, spotting ->
                viewModel.saveDay(date, mood, note, pain, spotting)
                selectedDate = null
            }
        )
    }
}

@Composable
private fun CalendarScreen(
    state: UiState,
    modifier: Modifier,
    onDateClick: (LocalDate) -> Unit,
    onQuickStart: () -> Unit
) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    val today = LocalDate.now()
    val todayStarted = state.entries.any {
        it.epochDay == today.toEpochDay() && it.periodStart
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Döngü", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(periodHeadline(state.stats), color = Text2, fontSize = 13.sp)
        }

        item {
            Surface(
                color = if (todayStarted) Soft else Pink,
                contentColor = if (todayStarted) Color.White else Color(0xFF321421),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().clickable(onClick = onQuickStart)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.WaterDrop, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            if (todayStarted) "Bugünkü başlangıç kaydedildi" else "Bugün regl başladı",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (todayStarted) "Geri almak için dokun" else "Tek dokunuşla başlangıcı işaretle",
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    MonthHeader(
                        month = month,
                        previous = { month = month.minusMonths(1) },
                        next = { month = month.plusMonths(1) }
                    )
                    WeekHeader()
                    MonthGrid(month, today, state, onDateClick)
                    CalendarLegend()
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard(
                    title = "Ort. döngü",
                    value = state.stats.averageCycle?.let { "$it gün" } ?: "—",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Ort. regl",
                    value = state.stats.averagePeriod?.let { "$it gün" } ?: "—",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Düzen",
                    value = state.stats.regularity,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, previous: () -> Unit, next: () -> Unit) {
    val tr = Locale("tr", "TR")
    val monthName = month.month
        .getDisplayName(TextStyle.FULL, tr)
        .replaceFirstChar { it.titlecase(tr) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = previous) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Önceki ay")
        }
        Text(
            text = "$monthName ${month.year}",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = next) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Sonraki ay")
        }
    }
}

@Composable
private fun WeekHeader() {
    Row(Modifier.fillMaxWidth()) {
        listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz").forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f).padding(vertical = 6.dp),
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                color = Text2
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    state: UiState,
    onDateClick: (LocalDate) -> Unit
) {
    val offset = month.atDay(1).dayOfWeek.value - 1
    val entryMap = state.entries.associateBy { it.epochDay }
    val actualPeriods = periodIntervals(state.entries)

    Column {
        repeat(6) { row ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { column ->
                    val dayNumber = row * 7 + column - offset + 1
                    if (dayNumber !in 1..month.lengthOfMonth()) {
                        Spacer(Modifier.weight(1f).height(62.dp))
                    } else {
                        val date = month.atDay(dayNumber)
                        val entry = entryMap[date.toEpochDay()]
                        val actual = actualPeriods.any {
                            !date.isBefore(it.start) && !date.isAfter(it.end)
                        }
                        val predicted = isPredictedPeriod(date, state.stats)
                        val fertile = state.stats.fertileStart != null &&
                            state.stats.fertileEnd != null &&
                            !date.isBefore(state.stats.fertileStart) &&
                            !date.isAfter(state.stats.fertileEnd)

                        CalendarCell(
                            date = date,
                            entry = entry,
                            isToday = date == today,
                            actualPeriod = actual,
                            predictedPeriod = predicted,
                            fertile = fertile,
                            ovulation = date == state.stats.ovulation,
                            modifier = Modifier.weight(1f),
                            onClick = { onDateClick(date) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarCell(
    date: LocalDate,
    entry: DayEntry?,
    isToday: Boolean,
    actualPeriod: Boolean,
    predictedPeriod: Boolean,
    fertile: Boolean,
    ovulation: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val background = when {
        actualPeriod -> PinkStrong.copy(alpha = 0.85f)
        predictedPeriod -> Pink.copy(alpha = 0.18f)
        fertile -> Lavender.copy(alpha = 0.10f)
        else -> Color.Transparent
    }
    val border = when {
        isToday -> Color.White.copy(alpha = 0.7f)
        predictedPeriod -> Pink.copy(alpha = 0.6f)
        fertile -> Lavender.copy(alpha = 0.4f)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .height(62.dp)
            .padding(2.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(15.dp))
            .clickable(onClick = onClick)
    ) {
        if (!entry?.note.isNullOrBlank()) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp)
                    .size(5.dp)
                    .background(Pink, CircleShape)
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                fontSize = 13.sp,
                fontWeight = if (isToday || entry?.periodStart == true) {
                    FontWeight.Bold
                } else {
                    FontWeight.Medium
                },
                color = if (actualPeriod) Color(0xFF321421) else Color.White
            )
            Spacer(Modifier.height(3.dp))
            when {
                !entry?.mood.isNullOrBlank() -> Text(entry!!.mood, fontSize = 15.sp)
                ovulation -> Box(Modifier.size(6.dp).background(Lavender, CircleShape))
                else -> Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun CalendarLegend() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        LegendDot(PinkStrong, "Regl")
        LegendDot(Pink.copy(alpha = 0.35f), "Tahmin")
        LegendDot(Lavender.copy(alpha = 0.45f), "Doğurgan")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(5.dp).background(Pink, CircleShape))
            Spacer(Modifier.width(4.dp))
            Text("Not", fontSize = 10.sp, color = Text2)
        }
    }
}

@Composable
private fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).background(color, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 10.sp, color = Text2)
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(13.dp)) {
            Text(title, color = Text2, fontSize = 10.sp)
            Text(value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaySheet(
    date: LocalDate,
    entry: DayEntry?,
    onDismiss: () -> Unit,
    onToggleStart: () -> Unit,
    onToggleEnd: () -> Unit,
    onSave: (String, String, Int, Boolean) -> Unit
) {
    var mood by remember(date, entry?.mood) { mutableStateOf(entry?.mood.orEmpty()) }
    var note by remember(date, entry?.note) { mutableStateOf(entry?.note.orEmpty()) }
    var pain by remember(date, entry?.pain) { mutableIntStateOf(entry?.pain ?: 0) }
    var spotting by remember(date, entry?.spotting) { mutableStateOf(entry?.spotting ?: false) }
    var symptomsExpanded by remember {
        mutableStateOf((entry?.pain ?: 0) > 0 || entry?.spotting == true)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(longDate(date), fontWeight = FontWeight.Bold, fontSize = 21.sp)
                    Text("Gün detayları", color = Text2, fontSize = 12.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat")
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onToggleStart,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Pink)
                ) {
                    Text(if (entry?.periodStart == true) "Başlangıç ✓" else "Regl başladı")
                }
                OutlinedButton(
                    onClick = onToggleEnd,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (entry?.periodEnd == true) "Bitiş ✓" else "Regl bitti")
                }
            }

            Spacer(Modifier.height(22.dp))
            Text("Duygu durumu", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            MoodPicker(selected = mood) { selected ->
                mood = if (mood == selected) "" else selected
            }

            Spacer(Modifier.height(18.dp))
            Text("Not", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                placeholder = { Text("Bugün nasıl geçti?") },
                shape = RoundedCornerShape(18.dp)
            )

            Spacer(Modifier.height(14.dp))
            Surface(
                color = Soft,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().clickable {
                    symptomsExpanded = !symptomsExpanded
                }
            ) {
                Row(Modifier.padding(16.dp)) {
                    Text("Semptomlar", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text(
                        if (symptomsExpanded) "Gizle" else "Göster",
                        color = Pink,
                        fontSize = 12.sp
                    )
                }
            }

            AnimatedVisibility(visible = symptomsExpanded) {
                Column(Modifier.padding(top = 12.dp)) {
                    Row {
                        Text("Ağrı düzeyi", Modifier.weight(1f))
                        Text("$pain / 5", color = Pink, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = pain.toFloat(),
                        onValueChange = { pain = it.roundToInt() },
                        valueRange = 0f..5f,
                        steps = 4
                    )
                    FilterChip(
                        selected = spotting,
                        onClick = { spotting = !spotting },
                        label = { Text("Lekelenme") }
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = { onSave(mood, note, pain, spotting) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Kaydet", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun MoodPicker(selected: String, onSelect: (String) -> Unit) {
    val moods = listOf("🙂", "😌", "🥰", "😐", "😔", "😣", "😡", "😴")
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        moods.chunked(4).forEach { rowMoods ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                rowMoods.forEach { mood ->
                    val active = mood == selected
                    Surface(
                        color = if (active) Pink.copy(alpha = 0.22f) else Soft,
                        shape = RoundedCornerShape(17.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clickable { onSelect(mood) }
                            .then(
                                if (active) {
                                    Modifier.border(1.dp, Pink, RoundedCornerShape(17.dp))
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(mood, fontSize = 23.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsScreen(state: UiState, modifier: Modifier) {
    val stats = state.stats
    val history = periodHistory(state.entries)

    LazyColumn(
        modifier = modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item {
            Text("Analiz", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("Kayıtların arttıkça tahminler kişiselleşir.", color = Text2, fontSize = 12.sp)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard(
                    "Ortalama döngü",
                    stats.averageCycle?.let { "$it gün" } ?: "Veri yok",
                    Modifier.weight(1f)
                )
                MetricCard(
                    "Ortalama regl",
                    stats.averagePeriod?.let { "$it gün" } ?: "Veri yok",
                    Modifier.weight(1f)
                )
            }
        }

        stats.nextPeriod?.let { nextPeriod ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Pink.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Tahmin", color = Pink, fontWeight = FontWeight.Bold)
                        Text(
                            "Sonraki regl: ${shortDate(nextPeriod)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                        stats.ovulation?.let {
                            Text(
                                "Ovülasyon: ${shortDate(it)}",
                                color = Text2,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 7.dp)
                            )
                        }
                        if (stats.fertileStart != null && stats.fertileEnd != null) {
                            Text(
                                "Doğurganlık: ${shortDate(stats.fertileStart)} – ${shortDate(stats.fertileEnd)}",
                                color = Text2,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Döngü düzeni", fontWeight = FontWeight.Bold)
                    Text(
                        stats.regularity,
                        color = Pink,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                    stats.deviation?.let {
                        Text(
                            "Standart sapma: ${"%.1f".format(Locale.US, it)} gün",
                            color = Text2,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        if (stats.cycleLengths.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Geçmiş döngüler", fontWeight = FontWeight.Bold)
                        Text("Başlangıçlar arasındaki gün sayısı", color = Text2, fontSize = 12.sp)
                        Spacer(Modifier.height(14.dp))
                        CycleChart(stats.cycleLengths.takeLast(8))
                    }
                }
            }
        }

        if (history.isNotEmpty()) {
            item {
                Text("Regl geçmişi", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            items(history) { item ->
                HistoryRow(item.first, item.second)
            }
        } else {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        "En az iki başlangıç kaydıyla analiz oluşmaya başlar.",
                        modifier = Modifier.padding(20.dp),
                        color = Text2
                    )
                }
            }
        }
    }
}

@Composable
private fun CycleChart(values: List<Int>) {
    val min = values.minOrNull() ?: 0
    val max = values.maxOrNull() ?: 1
    val range = (max - min).coerceAtLeast(1)

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(145.dp)
            .background(Soft, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        val step = if (values.size == 1) 0f else size.width / (values.size - 1)
        val points = values.mapIndexed { index, value ->
            Offset(
                x = if (values.size == 1) size.width / 2 else index * step,
                y = size.height - (
                    ((value - min).toFloat() / range) * size.height * 0.72f + size.height * 0.14f
                )
            )
        }

        for (i in 0 until points.lastIndex) {
            drawLine(
                color = Pink,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )
        }
        points.forEach { point ->
            drawCircle(Pink, radius = 7f, center = point)
            drawCircle(Soft, radius = 3f, center = point)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        values.forEach { Text("$it", fontSize = 10.sp, color = Text2) }
    }
}

@Composable
private fun HistoryRow(start: LocalDate, end: LocalDate?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Pink)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(shortDate(start), fontWeight = FontWeight.SemiBold)
                Text(
                    if (end != null) "Bitiş: ${shortDate(end)}" else "Bitiş kaydı yok",
                    color = Text2,
                    fontSize = 12.sp
                )
            }
            if (end != null) {
                Text(
                    "${ChronoUnit.DAYS.between(start, end) + 1} gün",
                    color = Pink,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    state: UiState,
    modifier: Modifier,
    onReminderChanged: (Boolean) -> Unit,
    onReminderDaysChanged: (Int) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Text("Ayarlar", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("Sade, yerel ve özel.", color = Text2, fontSize = 12.sp)
        Spacer(Modifier.height(18.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = Pink)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Regl hatırlatıcısı", fontWeight = FontWeight.Bold)
                        Text("Tahmini tarihten önce bildirim", color = Text2, fontSize = 12.sp)
                    }
                    Switch(
                        checked = state.reminderEnabled,
                        onCheckedChange = onReminderChanged
                    )
                }

                AnimatedVisibility(visible = state.reminderEnabled) {
                    Column(Modifier.padding(top = 14.dp)) {
                        Row {
                            Text("Kaç gün önce?", Modifier.weight(1f))
                            Text(
                                if (state.reminderDays == 0) "Aynı gün" else "${state.reminderDays} gün",
                                color = Pink,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = state.reminderDays.toFloat(),
                            onValueChange = { onReminderDaysChanged(it.roundToInt()) },
                            valueRange = 0f..7f,
                            steps = 6
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(13.dp))
        InfoCard(
            "Gizlilik",
            "Tüm kayıtlar yalnızca bu cihazda tutulur. Uygulama hesap, internet veya bulut bağlantısı istemez."
        )
        Spacer(Modifier.height(13.dp))
        InfoCard(
            "Tahminler hakkında",
            "Regl, ovülasyon ve doğurganlık tarihleri geçmiş kayıtların ortalamasına dayalı yaklaşık tahminlerdir. Tıbbi değerlendirme veya doğum kontrol yöntemi olarak kullanılmamalıdır."
        )
        Spacer(Modifier.height(18.dp))
        Text(
            "Döngü 1.0.0",
            color = Text2,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(
                body,
                color = Text2,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 7.dp)
            )
        }
    }
}

private fun calculateStats(entries: List<DayEntry>): Stats {
    val starts = entries
        .filter { it.periodStart }
        .map { it.date }
        .distinct()
        .sorted()

    val cycleLengths = starts
        .zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b).toInt() }
        .filter { it > 0 }

    val periodDurations = completedPeriodDurations(entries)
    val averageCycle = cycleLengths.takeIf { it.isNotEmpty() }?.average()?.roundToInt()
    val averagePeriod = periodDurations.takeIf { it.isNotEmpty() }?.average()?.roundToInt()

    val deviation = if (cycleLengths.size >= 2) {
        val average = cycleLengths.average()
        sqrt(cycleLengths.sumOf { (it - average) * (it - average) } / cycleLengths.size)
    } else {
        null
    }

    val regularity = when {
        cycleLengths.size < 2 -> "Veri bekleniyor"
        deviation != null && deviation <= 2.0 -> "Düzenli"
        deviation != null && deviation <= 5.0 -> "Değişken"
        else -> "Düzensiz"
    }

    val cycle = averageCycle ?: 28
    var next = starts.lastOrNull()?.plusDays(cycle.toLong())
    while (next != null && !next.isAfter(LocalDate.now())) {
        next = next.plusDays(cycle.toLong())
    }

    val ovulation = next?.minusDays(14)
    return Stats(
        averageCycle = averageCycle,
        averagePeriod = averagePeriod,
        cycleLengths = cycleLengths,
        nextPeriod = next,
        ovulation = ovulation,
        fertileStart = ovulation?.minusDays(5),
        fertileEnd = ovulation?.plusDays(1),
        regularity = regularity,
        deviation = deviation
    )
}

private fun completedPeriodDurations(entries: List<DayEntry>): List<Int> {
    val starts = entries.filter { it.periodStart }.map { it.date }.distinct().sorted()
    val ends = entries.filter { it.periodEnd }.map { it.date }.distinct().sorted()

    return starts.mapIndexedNotNull { index, start ->
        val nextStart = starts.getOrNull(index + 1)
        ends.firstOrNull { end ->
            !end.isBefore(start) && (nextStart == null || end.isBefore(nextStart))
        }?.let { end ->
            (ChronoUnit.DAYS.between(start, end).toInt() + 1).takeIf { it in 1..15 }
        }
    }
}

private fun periodIntervals(entries: List<DayEntry>): List<PeriodInterval> {
    val starts = entries.filter { it.periodStart }.map { it.date }.distinct().sorted()
    val ends = entries.filter { it.periodEnd }.map { it.date }.distinct().sorted()
    val today = LocalDate.now()

    return starts.mapIndexed { index, start ->
        val nextStart = starts.getOrNull(index + 1)
        val recordedEnd = ends.firstOrNull { end ->
            !end.isBefore(start) && (nextStart == null || end.isBefore(nextStart))
        }
        val displayEnd = recordedEnd ?: when {
            start.isAfter(today) -> start
            ChronoUnit.DAYS.between(start, today) <= 9 -> today
            else -> start.plusDays(4)
        }
        PeriodInterval(start, displayEnd)
    }
}

private fun periodHistory(entries: List<DayEntry>): List<Pair<LocalDate, LocalDate?>> {
    val starts = entries.filter { it.periodStart }.map { it.date }.distinct().sorted()
    val ends = entries.filter { it.periodEnd }.map { it.date }.distinct().sorted()

    return starts.mapIndexed { index, start ->
        val nextStart = starts.getOrNull(index + 1)
        val end = ends.firstOrNull {
            !it.isBefore(start) && (nextStart == null || it.isBefore(nextStart))
        }
        start to end
    }.reversed()
}

private fun isPredictedPeriod(date: LocalDate, stats: Stats): Boolean {
    val start = stats.nextPeriod ?: return false
    val end = start.plusDays(((stats.averagePeriod ?: 5) - 1).toLong())
    return !date.isBefore(start) && !date.isAfter(end)
}

private fun periodHeadline(stats: Stats): String {
    val next = stats.nextPeriod ?: return "İlk başlangıç kaydını ekleyerek tahminleri başlat."
    val days = ChronoUnit.DAYS.between(LocalDate.now(), next).toInt()
    return when (days) {
        0 -> "Tahmine göre regl bugün başlayabilir."
        1 -> "Tahmine göre regle 1 gün kaldı."
        else -> "Tahmine göre regle $days gün kaldı."
    }
}

private fun shortDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("tr", "TR")))

private fun longDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", Locale("tr", "TR")))
        .replaceFirstChar { it.uppercase(Locale("tr", "TR")) }
