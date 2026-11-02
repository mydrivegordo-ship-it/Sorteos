package com.example.sorteos

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Noche = Color(0xFF0B1B3A)
private val Tarjeta = Color(0xFF13264F)
private val Oro = Color(0xFFD4AF37)
private val Marfil = Color(0xFFF2EFE6)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createChannel(this)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Oro, background = Noche, surface = Tarjeta)) {
                Pantalla()
            }
        }
    }
}

@Composable
fun Pantalla() {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    var sel by remember { mutableStateOf(prefs.selected) }
    var h by remember { mutableIntStateOf(prefs.hour) }
    var m by remember { mutableIntStateOf(prefs.minute) }

    Column(Modifier.fillMaxSize().background(Noche).padding(20.dp)) {
        Spacer(Modifier.height(24.dp))
        Text("★ SORTEOS ★", color = Oro, fontSize = 28.sp, fontWeight = FontWeight.Light,
            fontFamily = FontFamily.Serif, letterSpacing = 4.sp)
        Text("Recordatorio diario de apuestas del Estado", color = Marfil.copy(alpha = .7f), fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(SORTEOS) { s ->
                val on = s.id in sel
                val cambiar = {
                    sel = if (on) sel - s.id else sel + s.id
                    prefs.selected = sel; schedule(ctx)
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = Tarjeta),
                    border = BorderStroke(1.dp, if (on) Oro else Color.Transparent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().clickable { cambiar() }
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.nombre, color = Marfil, fontSize = 17.sp, fontFamily = FontFamily.Serif)
                            Text(diasTexto(s), color = Oro.copy(alpha = .8f), fontSize = 12.sp, letterSpacing = 2.sp)
                        }
                        Checkbox(checked = on, onCheckedChange = { cambiar() },
                            colors = CheckboxDefaults.colors(checkedColor = Oro, checkmarkColor = Noche, uncheckedColor = Marfil.copy(alpha = .5f)))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Oro.copy(alpha = .4f))
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Avisarme a las", color = Marfil, modifier = Modifier.weight(1f))
            OutlinedButton(
                onClick = {
                    TimePickerDialog(ctx, { _, hh, mm ->
                        h = hh; m = mm; prefs.hour = hh; prefs.minute = mm; schedule(ctx)
                    }, h, m, true).show()
                },
                border = BorderStroke(1.dp, Oro)
            ) { Text("%02d:%02d".format(h, m), color = Oro, fontSize = 18.sp) }
        }
        TextButton(onClick = { notify(ctx, "Así se verá tu recordatorio ★") }) {
            Text("Probar notificación", color = Marfil.copy(alpha = .6f))
        }
    }
}
