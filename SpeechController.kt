package fr.orionai.app

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import java.util.Locale

class SpeechController(context: Context) {
    private var tts: TextToSpeech? = null
    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale.FRENCH
        }
    }
    fun speak(text: String) { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "orion_reply") }
    fun recognitionIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Parle à Orion")
    }
    fun close() { tts?.stop(); tts?.shutdown(); tts = null }
}
