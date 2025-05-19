package com.tchassistant.smarthomevoice

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class VoiceApiManager {
    private val TAG = "VoiceApiManager"
    private val API_URL = "https://tchvoiceserver.onrender.com/predict"

    // List of supported devices with their synonyms
    private val deviceSynonyms = mapOf(
        "fan" to listOf("fan", "ceiling fan", "air fan"),
        "light" to listOf("light", "lights", "bulb", "bulbs"),
        "curtain" to listOf("curtain", "curtains", "blind", "blinds", "window cover"),
        "night light" to listOf("night light", "night lamp", "bedroom light", "lamp"),
        "door" to listOf("door", "doors", "entrance", "exit")
    )

    // Get the canonical device name from a synonym
    private fun getCanonicalDeviceName(deviceName: String): String? {
        return deviceSynonyms.entries.find { (_, synonyms) ->
            synonyms.any { it.equals(deviceName, ignoreCase = true) }
        }?.key
    }

    // List of supported devices
    private val supportedDevices = listOf(
        "light",
        "fan",
        "door",
        "curtain",
        "lamp"
    )

    // Class to hold the prediction result
    data class PredictionResult(
        val modelUsed: String,
        val wordPredictions: List<WordPrediction>
    )

    data class WordPrediction(
        val word: String,
        val intent: Int
    )

    /**
     * Send the spoken text to the API for prediction
     * @param text the spoken text from voice recognition
     * @return PredictionResult containing the model used and word predictions
     */
    suspend fun getPrediction(text: String): PredictionResult? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Sending prediction request for: $text")

            // Create connection
            val url = URL(API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            // Create JSON payload
            val jsonPayload = JSONObject().apply {
                put("text", text)
            }

            // Send request
            val outputWriter = OutputStreamWriter(connection.outputStream)
            outputWriter.write(jsonPayload.toString())
            outputWriter.flush()

            // Check response
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read response
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }

                reader.close()

                // Parse response
                val responseJson = JSONObject(response.toString())
                val modelUsed = responseJson.getString("model_used")
                val resultArray = responseJson.getJSONArray("result")

                // Create word predictions list
                val wordPredictions = mutableListOf<WordPrediction>()
                for (i in 0 until resultArray.length()) {
                    val item = resultArray.getJSONArray(i)
                    val word = item.getString(0)
                    val intent = item.getInt(1)
                    wordPredictions.add(WordPrediction(word, intent))
                }

                Log.d(TAG, "Prediction complete. Model: $modelUsed, Predictions: $wordPredictions")
                return@withContext PredictionResult(modelUsed, wordPredictions)
            } else {
                Log.e(TAG, "Error response from API: $responseCode")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during API call", e)
            return@withContext null
        }
    }

    /**
     * Extract entities from word predictions based on intent values
     */
    fun extractEntities(predictions: List<WordPrediction>, modelType: String): Map<String, String> {
        val entityMap = mutableMapOf<String, String>()
        val words = predictions.map { it.word }

        when (modelType) {
            "DEVICE" -> {
                // Extract device name (intent 1) and status (intent 0)
                val deviceWords = predictions.filter { it.intent == 1 }
                    .map { it.word }
                    .joinToString(" ")
                val statusWords = predictions.filter { it.intent == 0 }
                    .map { it.word }
                    .joinToString(" ")

                // Clean up device name and status
                val deviceName = deviceWords.trim()
                val deviceStatus = statusWords.trim()

                // Get canonical device name from synonyms
                val canonicalDevice = getCanonicalDeviceName(deviceName)
                if (canonicalDevice == null) {
                    Log.d(TAG, "Unsupported device: $deviceName")
                    return emptyMap()
                }

                // Handle special cases for common commands
                val status = when {
                    // Handle "turn on/off" pattern
                    deviceStatus.contains("turn") -> {
                        if (deviceStatus.contains("on")) "on" else "off"
                    }
                    // Handle direct on/off commands
                    deviceStatus.contains("on") || deviceStatus.contains("off") -> {
                        if (deviceStatus.contains("on")) "on" else "off"
                    }
                    // Handle open/close commands
                    deviceStatus.contains("open") || deviceStatus.contains("close") -> {
                        if (deviceStatus.contains("open")) "open" else "close"
                    }
                    // Default case
                    else -> deviceStatus
                }

                // Only return entities if we have both a valid device and status
                if (canonicalDevice.isNotEmpty() && status.isNotEmpty()) {
                    entityMap["devicename"] = canonicalDevice
                    entityMap["devicestatus"] = status
                }
            }
            "EVENT" -> {
                // Extract action (intent 0), time (intent 1), duration (intent 2), and date (intent 3)
                val actionWords = predictions.filter { it.intent == 0 }.map { it.word }
                val timeWords = predictions.filter { it.intent == 1 }.map { it.word }
                val durationWords = predictions.filter { it.intent == 2 }.map { it.word }
                val dateWords = predictions.filter { it.intent == 3 }.map { it.word }

                if (actionWords.isNotEmpty()) {
                    entityMap["location"] = actionWords.joinToString(" ")
                }

                // Process time and duration
                val timeText = timeWords.joinToString(" ")
                if (timeText.isNotEmpty()) {
                    // Clean up time text
                    val cleanedTime = timeText
                        .replace("at", "")
                        .replace(":", "")
                        .replace(".", "")
                        .replace("a m", "am")
                        .replace("p m", "pm")
                        .replace(Regex("\\s+"), " ") // Replace multiple spaces with single space
                        .trim()
                    
                    // Check if the time text contains a range (from X to Y)
                    if (cleanedTime.contains("from") && cleanedTime.contains("to")) {
                        val timeParts = cleanedTime.split("to")
                        if (timeParts.size == 2) {
                            // Extract start time (remove "from" prefix)
                            val startTime = timeParts[0].replace("from", "").trim()
                            entityMap["time"] = startTime
                            // Extract end time
                            entityMap["endtime"] = timeParts[1].trim()
                        }
                    } else {
                        // Single time point with duration
                        entityMap["time"] = cleanedTime
                        
                        // Use duration words for end time
                        if (durationWords.isNotEmpty()) {
                            entityMap["endtime"] = durationWords.joinToString(" ")
                        }
                    }
                }

                // Clean up date text
                if (dateWords.isNotEmpty()) {
                    val cleanedDate = dateWords
                        .filter { it != "." && it != "for" } // Remove punctuation and unwanted words
                        .joinToString(" ")
                        .trim()
                    if (cleanedDate.isNotEmpty()) {
                        entityMap["date"] = cleanedDate
                    }
                }
            }
            "TASK" -> {
                // Extract task name (intent 0) and date (intent 1)
                val taskWords = predictions.filter { it.intent == 0 }.map { it.word }
                val dateWords = predictions.filter { it.intent == 1 }.map { it.word }

                if (taskWords.isNotEmpty()) {
                    entityMap["taskname"] = taskWords.joinToString(" ")
                }

                if (dateWords.isNotEmpty()) {
                    entityMap["date"] = dateWords.joinToString(" ")
                }
            }
        }

        return entityMap
    }
}