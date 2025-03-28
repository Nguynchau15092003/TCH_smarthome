package com.example.smarthomevoice.api

import android.content.Context
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.dialogflow.v2.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class DialogflowService(context: Context) {

    private val sessionsClient: SessionsClient
    private val session: SessionName

    init {
        // ✅ Load JSON file from assets folder
        val stream: InputStream = context.assets.open("iotvoicecontrol-jjnx-d82e7254e31b.json")
        val credentials = GoogleCredentials.fromStream(stream)
            .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))

        val settings = SessionsSettings.newBuilder()
            .setCredentialsProvider { credentials }
            .build()

        sessionsClient = SessionsClient.create(settings)
        session = SessionName.of("iotvoicecontrol-jjnx", java.util.UUID.randomUUID().toString())
    }

    suspend fun detectIntent(text: String): DetectIntentResponse = withContext(Dispatchers.IO) {
        val input = QueryInput.newBuilder()
            .setText(TextInput.newBuilder().setText(text).setLanguageCode("en-US"))
            .build()
        return@withContext sessionsClient.detectIntent(session, input)
    }

    fun shutdown() {
        sessionsClient.shutdown()
    }
}
