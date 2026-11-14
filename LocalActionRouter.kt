package fr.orionai.app

import android.content.Context
import android.content.Intent
import android.provider.Settings
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LocalActionRouter(private val context: Context) {
    fun run(command: String): String {
        return when (command.trim().lowercase()) {
            "heure" -> "Il est " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            "date" -> "Nous sommes le " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            "parametres" -> {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                "Ouverture des paramètres Android."
            }
            else -> "Commande locale inconnue. Commandes disponibles : heure, date, parametres."
        }
    }
}
