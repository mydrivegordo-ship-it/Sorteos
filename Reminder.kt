package com.example.sorteos

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

// Días con Calendar.DAY_OF_WEEK (domingo=1 ... sábado=7). Revisa que coincidan con el calendario oficial.
data class Sorteo(val id: String, val nombre: String, val dias: Set<Int>)

val SORTEOS = listOf(
    Sorteo("euromillones", "Euromillones", setOf(Calendar.TUESDAY, Calendar.FRIDAY)),
    Sorteo("primitiva", "La Primitiva", setOf(Calendar.MONDAY, Calendar.THURSDAY, Calendar.SATURDAY)),
    Sorteo("bonoloto", "Bonoloto", setOf(2, 3, 4, 5, 6, 7)),
    Sorteo("gordo", "El Gordo de la Primitiva", setOf(Calendar.SUNDAY)),
    Sorteo("nacional", "Lotería Nacional", setOf(Calendar.THURSDAY, Calendar.SATURDAY)),
    Sorteo("quiniela", "La Quiniela", setOf(Calendar.SUNDAY)),
    Sorteo("lototurf", "Lototurf", setOf(Calendar.SUNDAY)),
)

private val NOMBRES_DIA = mapOf(2 to "L", 3 to "M", 4 to "X", 5 to "J", 6 to "V", 7 to "S", 1 to "D")
fun diasTexto(s: Sorteo) = listOf(2, 3, 4, 5, 6, 7, 1).filter { it in s.dias }.joinToString(" ") { NOMBRES_DIA[it]!! }

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("sorteos", Context.MODE_PRIVATE)
    var selected: Set<String>
        get() = sp.getStringSet("sel", emptySet()) ?: emptySet()
        set(v) = sp.edit().putStringSet("sel", v).apply()
    var hour: Int
        get() = sp.getInt("h", 10)
        set(v) = sp.edit().putInt("h", v).apply()
    var minute: Int
        get() = sp.getInt("m", 0)
        set(v) = sp.edit().putInt("m", v).apply()
}

fun sorteosDelDia(sel: Set<String>, diaSemana: Int) =
    SORTEOS.filter { it.id in sel && diaSemana in it.dias }

const val CHANNEL = "sorteos"

fun createChannel(ctx: Context) {
    if (Build.VERSION.SDK_INT >= 26) {
        ctx.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CHANNEL, "Recordatorio de sorteos", NotificationManager.IMPORTANCE_HIGH))
    }
}

fun notify(ctx: Context, texto: String) {
    createChannel(ctx)
    val n = NotificationCompat.Builder(ctx, CHANNEL)
        .setSmallIcon(android.R.drawable.star_on)
        .setContentTitle("Sorteos de hoy")
        .setContentText(texto)
        .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
        .setAutoCancel(true)
        .build()
    try { ctx.getSystemService(NotificationManager::class.java).notify(1, n) } catch (_: SecurityException) {}
}

/** Programa la próxima alarma: el siguiente día (a la hora elegida) con algún sorteo marcado. */
fun schedule(ctx: Context) {
    val p = Prefs(ctx)
    val am = ctx.getSystemService(AlarmManager::class.java)
    val pi = PendingIntent.getBroadcast(
        ctx, 0, Intent(ctx, AlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    am.cancel(pi)
    if (p.selected.isEmpty()) return
    val now = Calendar.getInstance()
    for (i in 0..7) {
        val c = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, i)
            set(Calendar.HOUR_OF_DAY, p.hour); set(Calendar.MINUTE, p.minute)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (c.after(now) && sorteosDelDia(p.selected, c.get(Calendar.DAY_OF_WEEK)).isNotEmpty()) {
            val exacta = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
            if (exacta) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.timeInMillis, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.timeInMillis, pi)
            return
        }
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val p = Prefs(ctx)
        val hoy = sorteosDelDia(p.selected, Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
        if (hoy.isNotEmpty()) notify(ctx, "Hoy hay sorteo de: " + hoy.joinToString(", ") { it.nombre })
        schedule(ctx) // encadena la siguiente
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) { schedule(ctx) }
}
