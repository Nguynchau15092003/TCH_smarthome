package com.example.smarthomevoice

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.smarthomevoice.api.DialogflowService
import com.example.smarthomevoice.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import android.util.Log

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var deviceAdapter: DeviceAdapter
    private lateinit var dialogflowService: DialogflowService

    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            if (!spokenText.isNullOrEmpty()) {
                processVoiceCommand(spokenText)
            }
        } else {
            Toast.makeText(this, "Speech recognition failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "Initializing app")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize device list
        val devices = listOf(
            Device("Fan", R.drawable.ic_fan, false),
            Device("Light", R.drawable.ic_light, false),
            Device("Curtain", R.drawable.ic_curtain, false),
            Device("Night Light", R.drawable.ic_night_light, false),
            Device("Door", R.drawable.ic_door, false)
        )

        // Set up RecyclerView
        deviceAdapter = DeviceAdapter(devices)
        binding.recyclerViewDevices.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerViewDevices.adapter = deviceAdapter

        // Tab Layout listener (if needed later)
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {}
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Initialize Dialogflow service
        dialogflowService = DialogflowService(this)

        // Request permissions
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)

        // Voice button click listener
        binding.fabVoice.setOnClickListener {
            startSpeechToText()
        }
    }

    private fun startSpeechToText() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a command for your smart home")
            }
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e("SpeechRecognition", "Error starting speech recognition", e)
            Toast.makeText(this, "Speech recognition error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processVoiceCommand(command: String) {
        lifecycleScope.launch {
            try {
                val response = dialogflowService.detectIntent(command)
                val intentName = response.queryResult.intent.displayName
                val parameters = response.queryResult.parameters.fieldsMap

                Log.d("Dialogflow", "Intent: $intentName, Parameters: $parameters")

                if (intentName == "devicecontrol") {
                    val device = parameters["devicename"]?.stringValue?.lowercase()
                    val status = parameters["devicestatus"]?.stringValue?.lowercase()

                    if (!device.isNullOrEmpty() && !status.isNullOrEmpty()) {
                        when {
                            // Handle "open" and "close" for door and curtain
                            (device == "door" || device == "curtain") && status == "open" -> openDevice(device)
                            (device == "door" || device == "curtain") && status == "close" -> closeDevice(device)

                            // Handle "on" and "off" for other devices
                            status == "on" -> turnOnDevice(device)
                            status == "off" -> turnOffDevice(device)

                            else -> showToast("Unknown status: $status for $device")
                        }
                    } else {
                        showToast("Device or status not recognized")
                    }
                } else {
                    showToast("Unknown intent: $intentName")
                }
            } catch (e: Exception) {
                Log.e("VoiceCommand", "Error processing voice command", e)
                showToast("Error: ${e.message}")
            }
        }
    }


    private fun turnOnDevice(deviceName: String) {
        Log.d("DeviceControl", "Turning ON: $deviceName")
        showToast("Turning ON $deviceName")
        updateDeviceState(deviceName, true)
    }

    private fun turnOffDevice(deviceName: String) {
        Log.d("DeviceControl", "Turning OFF: $deviceName")
        showToast("Turning OFF $deviceName")
        updateDeviceState(deviceName, false)
    }

    private fun openDevice(deviceName: String) {
        Log.d("DeviceControl", "Opening: $deviceName")
        showToast("Opening $deviceName")
        updateDeviceState(deviceName, true)
    }

    private fun closeDevice(deviceName: String) {
        Log.d("DeviceControl", "Closing: $deviceName")
        showToast("Closing $deviceName")
        updateDeviceState(deviceName, false)
    }

    private fun updateDeviceState(deviceName: String, isOn: Boolean) {
        val device = deviceAdapter.getDevices().find { it.name.equals(deviceName, ignoreCase = true) }
        device?.let {
            it.isOn = isOn
            deviceAdapter.notifyDataSetChanged()
        } ?: Log.w("DeviceControl", "Device not found: $deviceName")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        dialogflowService.shutdown()
    }
}
