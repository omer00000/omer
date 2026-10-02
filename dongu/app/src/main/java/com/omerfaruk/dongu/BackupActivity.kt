package com.omerfaruk.dongu

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.OffsetDateTime

private val BackupPink = Color(0xFFF2A7C2)
private val BackupPinkStrong = Color(0xFFFF7FAF)
private val BackupBg = Color(0xFF101014)
private val BackupCard = Color(0xFF19191F)
private val BackupSoft = Color(0xFF24242C)
private val BackupText2 = Color(0xFFBEB5BA)

private val BackupColors = darkColorScheme(
    primary = BackupPink,
    onPrimary = Color(0xFF321421),
    background = BackupBg,
    surface = BackupCard,
    surfaceVariant = BackupSoft,
    onBackground = Color(0xFFF8F2F5),
    onSurface = Color(0xFFF8F2F5),
    onSurfaceVariant = BackupText2
)

class BackupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = BackupColors) {
                BackupScreen(
                    context = this,
                    onBack = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun BackupScreen(context: Context, onBack: () -> Unit) {
    val backupFileName = remember { "Dongu-yedek-${LocalDate.now()}.json" }

    val createBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { writer ->
                writer.write(createBackupJson(context))
            } ?: error("Dosya açılamadı")
        }.onSuccess {
            Toast.makeText(context, "Yedek başarıyla oluşturuldu", Toast.LENGTH_LONG).show()
        }.onFailure {
            Toast.makeText(context, "Yedek oluşturulamadı: ${it.message ?: "bilinmeyen hata"}", Toast.LENGTH_LONG).show()
        }
    }

    val restoreBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("Dosya okunamadı")
            restoreBackupJson(context, text)
        }.onSuccess { count ->
            Toast.makeText(context, "$count günlük kayıt geri yüklendi", Toast.LENGTH_LONG).show()
            context.startActivity(
                Intent(context, MainActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                )
            )
            if (context is BackupActivity) context.finish()
        }.onFailure {
            Toast.makeText(context, "Geçersiz yedek: ${it.message ?: "dosya okunamadı"}", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(BackupBg)
            .statusBarsPadding()
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
            }
            Column(Modifier.weight(1f)) {
                Text("Yedekleme", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("Döngü verilerini güvenli bir dosyada sakla.", color = BackupText2, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(20.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = BackupCard),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Yerel yedek", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "Regl başlangıç/bitişleri, notlar, duygu durumları, semptomlar ve hatırlatıcı ayarları tek bir JSON dosyasına kaydedilir.",
                    color = BackupText2,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )

                Button(
                    onClick = { createBackup.launch(backupFileName) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BackupPink)
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text("Yedek oluştur", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { restoreBackup.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text("Yedekten geri yükle", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = BackupSoft),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Yedek dosyası seçtiğin konuma kaydedilir. Uygulama dosyayı internete göndermez. Geri yükleme mevcut Döngü verilerinin yerine yedekteki verileri yazar.",
                modifier = Modifier.padding(16.dp),
                color = BackupText2,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

private fun createBackupJson(context: Context): String {
    val prefs = context.getSharedPreferences("dongu_data", Context.MODE_PRIVATE)
    val entriesText = prefs.getString("entries", "[]") ?: "[]"
    val entries = runCatching { JSONArray(entriesText) }.getOrElse { JSONArray() }

    return JSONObject()
        .put("app", "Döngü")
        .put("formatVersion", 1)
        .put("appVersion", "1.1.0")
        .put("createdAt", OffsetDateTime.now().toString())
        .put("entries", entries)
        .put("reminderEnabled", prefs.getBoolean("reminder", true))
        .put("reminderDays", prefs.getInt("reminder_days", 2).coerceIn(0, 7))
        .toString(2)
}

private fun restoreBackupJson(context: Context, text: String): Int {
    val root = JSONObject(text)
    require(root.optString("app") == "Döngü") { "Bu dosya Döngü yedeği değil" }
    require(root.optInt("formatVersion", -1) == 1) { "Desteklenmeyen yedek sürümü" }

    val sourceEntries = root.optJSONArray("entries") ?: error("Kayıtlar bulunamadı")
    val cleanEntries = JSONArray()

    for (i in 0 until sourceEntries.length()) {
        val item = sourceEntries.optJSONObject(i) ?: continue
        require(item.has("d")) { "Kayıt tarihi eksik" }
        val epochDay = item.getLong("d")
        LocalDate.ofEpochDay(epochDay)

        cleanEntries.put(
            JSONObject()
                .put("d", epochDay)
                .put("s", item.optBoolean("s"))
                .put("e", item.optBoolean("e"))
                .put("m", item.optString("m").take(16))
                .put("n", item.optString("n").take(10000))
                .put("p", item.optInt("p").coerceIn(0, 5))
                .put("sp", item.optBoolean("sp"))
        )
    }

    val reminderEnabled = root.optBoolean("reminderEnabled", true)
    val reminderDays = root.optInt("reminderDays", 2).coerceIn(0, 7)

    context.getSharedPreferences("dongu_data", Context.MODE_PRIVATE)
        .edit()
        .putString("entries", cleanEntries.toString())
        .putBoolean("reminder", reminderEnabled)
        .putInt("reminder_days", reminderDays)
        .commit()

    return cleanEntries.length()
}
