package fr.orionai.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Deep = Color(0xFF07111F)
private val Panel = Color(0xFF101F31)
private val Cyan = Color(0xFF52E5F2)
private val Blue = Color(0xFF2D8CFF)
private val Muted = Color(0xFF9BB0C6)

data class ChatLine(val role: String, val text: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(7, 17, 31)
        window.navigationBarColor = android.graphics.Color.rgb(7, 17, 31)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Cyan, secondary = Blue, background = Deep, surface = Panel,
                    onPrimary = Deep, onBackground = Color.White, onSurface = Color.White
                )
            ) { OrionScreen() }
        }
    }
}

@Composable
private fun OrionScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { SettingsStore(context) }
    val repo = remember { ChatRepository() }
    val speech = remember { SpeechController(context) }
    val local = remember { LocalActionRouter(context) }
    var settings by remember { mutableStateOf(AppSettings()) }
    var loaded by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var voiceEnabled by remember { mutableStateOf(true) }
    var pendingAction by remember { mutableStateOf<String?>(null) }
    var lines by remember {
        mutableStateOf(listOf(ChatLine("assistant", "Systèmes en ligne. Je suis ${settings.assistantName}. Comment puis-je t'aider ?")))
    }

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) input = spoken
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) voiceLauncher.launch(speech.recognitionIntent())
        else lines = lines + ChatLine("assistant", "Autorisation micro refusée. Tu peux continuer par écrit.")
    }

    LaunchedEffect(Unit) {
        settings = store.load()
        lines = listOf(ChatLine("assistant", "Systèmes en ligne. Je suis ${settings.assistantName}. Connexion hybride prête."))
        loaded = true
    }

    fun startVoice() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            voiceLauncher.launch(speech.recognitionIntent())
        } else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    fun sendMessage() {
        val text = input.trim()
        if (text.isEmpty() || busy) return
        input = ""
        if (text.startsWith("/local ")) {
            val command = text.removePrefix("/local ").trim()
            lines = lines + ChatLine("user", text)
            if (command == "parametres" && settings.confirmImportantActions) {
                pendingAction = command
                lines = lines + ChatLine("assistant", "Cette action ouvre les paramètres Android. Confirme dans la fenêtre avant de continuer.")
            } else {
                lines = lines + ChatLine("assistant", local.run(command))
            }
            return
        }
        val updated = lines + ChatLine("user", text)
        lines = updated
        busy = true
        scope.launch {
            try {
                val history = updated.takeLast(24).map {
                    (if (it.role == "assistant") "assistant" else "user") to it.text
                }
                val answer = withContext(Dispatchers.IO) { repo.send(settings, history) }
                lines = lines + ChatLine("assistant", answer)
                if (voiceEnabled) speech.speak(answer)
            } catch (e: Exception) {
                lines = lines + ChatLine("assistant", "Connexion indisponible : ${e.message ?: "erreur inconnue"}. Vérifie les réglages réseau et le fournisseur IA.")
            } finally { busy = false }
        }
    }

    DisposableEffect(Unit) { onDispose { speech.close() } }

    if (!loaded) {
        Box(Modifier.fillMaxSize().background(Deep), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Cyan)
        }
        return
    }

    if (pendingAction != null) {
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text("Confirmer l'action") },
            text = { Text("Autoriser ORION à ouvrir les paramètres Android sur cette tablette ?") },
            confirmButton = {
                TextButton(onClick = {
                    val action = pendingAction
                    pendingAction = null
                    if (action != null) lines = lines + ChatLine("assistant", local.run(action))
                }) { Text("CONFIRMER") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingAction = null
                    lines = lines + ChatLine("assistant", "Action annulée. Rien n'a été modifié.")
                }) { Text("ANNULER") }
            },
            containerColor = Panel,
            titleContentColor = Color.White,
            textContentColor = Muted
        )
    }

    if (showSettings) {
        SettingsScreen(settings,
            onSave = { settings = it; scope.launch { store.save(it) }; showSettings = false },
            onCancel = { showSettings = false })
    } else {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Deep, Color(0xFF0B1B30))))) {
            Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ORION", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                        Text(settings.assistantName, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Light)
                        Text("ASSISTANT PERSONNEL • MODE HYBRIDE", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Surface(color = Color(0xFF123747), shape = RoundedCornerShape(50)) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(7.dp).background(Cyan, CircleShape))
                                Spacer(Modifier.width(7.dp))
                                Text("SYSTÈMES ACTIFS", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        TextButton(onClick = { showSettings = true }) { Text("⚙ PARAMÈTRES", color = Muted) }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatusTile("IA DISTANTE", if (settings.apiKey.isBlank()) "À CONFIGURER" else "CONFIGURÉE", Modifier.weight(1f))
                    StatusTile("VOIX", if (voiceEnabled) "ACTIVE" else "SILENCIEUSE", Modifier.weight(1f))
                    StatusTile("ACTIONS LOCALES", "3 COMMANDES", Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(Modifier.weight(0.28f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier.size(112.dp).background(
                                        Brush.radialGradient(listOf(Color(0xFF1C8295), Color(0xFF102B46), Deep)),
                                        CircleShape
                                    ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("◉", color = Cyan, fontSize = 54.sp)
                                }
                                Spacer(Modifier.height(10.dp))
                                Text("ORION CORE", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Prêt à recevoir", color = Muted, fontSize = 11.sp)
                            }
                        }
                        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("COMMANDES LOCALES", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("/local heure", color = Color.White, fontSize = 12.sp)
                                Text("/local date", color = Color.White, fontSize = 12.sp)
                                Text("/local parametres", color = Color.White, fontSize = 12.sp)
                                Text("Actions limitées et explicites", color = Muted, fontSize = 10.sp)
                            }
                        }
                        OutlinedButton(onClick = { voiceEnabled = !voiceEnabled }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (voiceEnabled) "Désactiver la voix" else "Activer la voix", fontSize = 11.sp)
                        }
                    }
                    Card(
                        Modifier.weight(0.72f).fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xCC0D1B2B)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(Modifier.fillMaxSize().padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("CANAL DE COMMUNICATION", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text("${lines.size} messages", color = Muted, fontSize = 10.sp)
                            }
                            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Color(0xFF234057))
                            LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(lines) { line ->
                                    val user = line.role == "user"
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) {
                                        Surface(
                                            color = if (user) Color(0xFF123D5B) else Color(0xFF14283D),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.widthIn(max = 620.dp)
                                        ) {
                                            Column(Modifier.padding(12.dp)) {
                                                Text(if (user) "VOUS" else "ORION", color = if (user) Cyan else Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                Spacer(Modifier.height(4.dp))
                                                Text(line.text, color = Color.White, fontSize = 14.sp)
                                            }
                                        }
                                    }
                                }
                                if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Cyan) }
                            }
                            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Color(0xFF234057))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { startVoice() }) { Text("🎙", fontSize = 18.sp) }
                                OutlinedTextField(
                                    value = input, onValueChange = { input = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Transmettre une instruction…", color = Muted) },
                                    maxLines = 3,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Cyan, unfocusedBorderColor = Color(0xFF31516C),
                                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                                    )
                                )
                                Button(onClick = { sendMessage() }, enabled = !busy && input.isNotBlank()) {
                                    Text("ENVOYER")
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("ORION AI • Prototype personnel • Les actions sensibles nécessiteront une confirmation explicite", color = Muted, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun StatusTile(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF10243A)), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            Text(title, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingsScreen(settings: AppSettings, onSave: (AppSettings) -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf(settings.assistantName) }
    var endpoint by remember { mutableStateOf(settings.endpoint) }
    var key by remember { mutableStateOf(settings.apiKey) }
    var model by remember { mutableStateOf(settings.model) }
    var prompt by remember { mutableStateOf(settings.systemPrompt) }
    var wakeWord by remember { mutableStateOf(settings.wakeWord) }
    var confirmActions by remember { mutableStateOf(settings.confirmImportantActions) }
    Column(
        Modifier.fillMaxSize().background(Deep).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("CONFIGURATION DU SYSTÈME", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("Personnaliser ORION", color = Color.White, fontSize = 28.sp)
        Text("Les paramètres sont enregistrés sur cette tablette.", color = Muted, fontSize = 12.sp)
        OutlinedTextField(name, { name = it }, label = { Text("Nom de l'assistant") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(endpoint, { endpoint = it }, label = { Text("URL API HTTPS") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(key, { key = it }, label = { Text("Clé API du fournisseur") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
        OutlinedTextField(model, { model = it }, label = { Text("Identifiant du modèle") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(prompt, { prompt = it }, label = { Text("Personnalité / instructions système") }, modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp), minLines = 5)
        OutlinedTextField(wakeWord, { wakeWord = it }, label = { Text("Mot / phrase d'activation souhaité") }, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Confirmer les actions importantes", color = Color.White)
                Text("Demander ton accord avant les actions locales sensibles", color = Muted, fontSize = 11.sp)
            }
            Switch(checked = confirmActions, onCheckedChange = { confirmActions = it })
        }
        Text("Note : la phrase est enregistrée comme préférence. L'écoute permanente en arrière-plan n'est pas encore activée dans cette version.", color = Color(0xFFFFC36B), fontSize = 11.sp)
        Text("Conseil : ne partage jamais ton APK avec une clé API personnelle déjà enregistrée.", color = Color(0xFFFFC36B), fontSize = 11.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("ANNULER") }
            Button(onClick = { onSave(AppSettings(name, endpoint, key, model, prompt, wakeWord, confirmActions)) }, modifier = Modifier.weight(1f)) { Text("ENREGISTRER") }
        }
    }
}
