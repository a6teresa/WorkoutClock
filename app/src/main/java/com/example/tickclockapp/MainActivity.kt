package com.example.tickclockapp

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.RingtoneManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

enum class AppScreen { Screen1, Screen2 }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TickClockScreen()
                }
            }
        }
    }
}

@Composable
fun TickClockScreen() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    var currentScreen by remember { mutableStateOf(AppScreen.Screen1) }

    // Screen 1 States
    var isRunning1 by remember { mutableStateOf(false) }
    var totalSeconds1 by remember { mutableIntStateOf(0) }
    var cycleSeconds1 by remember { mutableIntStateOf(0) }

    // Screen 2 States
    var isRunning2 by remember { mutableStateOf(false) }
    var totalSeconds2 by remember { mutableIntStateOf(0) }

    // Keep screen on while the app is visible
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Logic for Screen 1 (with 30s audio cycle)
    LaunchedEffect(isRunning1) {
        if (isRunning1) {
            while (isRunning1) {
                cycleSeconds1++
                if (cycleSeconds1 > 30) cycleSeconds1 = 1
                
                playToneForSecond(cycleSeconds1)
                totalSeconds1++
                
                if (totalSeconds1 > 0 && (totalSeconds1 % 240 == 225)) {
                    playNotificationSound(context)
                }
                delay(1000)
            }
        }
    }

    // Logic for Screen 2 (no beeps, notification every 1 min)
    LaunchedEffect(isRunning2) {
        if (isRunning2) {
            while (isRunning2) {
                totalSeconds2++
                if (totalSeconds2 > 0 && totalSeconds2 % 60 == 0) {
                    playNotificationSound(context)
                }
                delay(1000)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                currentScreen = if (currentScreen == AppScreen.Screen1) AppScreen.Screen2 else AppScreen.Screen1
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val lightGreen = Color(0xFF90EE90)
            val darkGreen = Color(0xFF006400)
            val mainButtonSize = 240.dp

            if (currentScreen == AppScreen.Screen1) {
                // SCREEN 1 CONTENT
                OutlinedButton(
                    onClick = { isRunning1 = !isRunning1 },
                    modifier = Modifier.size(mainButtonSize),
                    shape = CircleShape,
                    border = BorderStroke(3.dp, lightGreen),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isRunning1) darkGreen else Color.Transparent,
                        contentColor = Color.White
                    )
                ) { }

                Spacer(modifier = Modifier.height(60.dp))

                val minutes = totalSeconds1 / 60
                val remainingSeconds = totalSeconds1 % 60
                Text(
                    text = "%02d:%02d".format(minutes, remainingSeconds),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            } else {
                // SCREEN 2 CONTENT
                OutlinedButton(
                    onClick = { isRunning2 = !isRunning2 },
                    modifier = Modifier.size(mainButtonSize),
                    shape = CircleShape,
                    border = BorderStroke(3.dp, lightGreen),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isRunning2) darkGreen else Color.Transparent,
                        contentColor = Color.White
                    )
                ) { }

                Spacer(modifier = Modifier.height(60.dp))

                val hours = totalSeconds2 / 3600
                val minutes = (totalSeconds2 % 3600) / 60
                val seconds = totalSeconds2 % 60
                Text(
                    text = "%02d:%02d:%02d".format(hours, minutes, seconds),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            OutlinedButton(
                onClick = { activity?.finish() },
                modifier = Modifier
                    .width(120.dp)
                    .height(50.dp),
                border = BorderStroke(1.dp, lightGreen.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White.copy(alpha = 0.7f)
                )
            ) {
                Text("Exit", fontSize = 18.sp)
            }
        }
    }
}

private fun playNotificationSound(context: Context) {
    try {
        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val r = RingtoneManager.getRingtone(context, notification)
        r.play()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun playToneForSecond(cycleSecond: Int) {
    val freq392 = 392.0 // G4
    val freq440 = 440.0 // A4
    val freq523 = 523.0 // C5
    val freq659 = 659.0 // E5
    
    when (cycleSecond) {
        in 1..13 -> generateTone(freq392, 100)
        14 -> generateTone(freq440, 1800)
        15 -> { }
        in 16..25 -> { }
        in 26..28 -> generateTone(freq523, 100)
        29 -> generateTone(freq659, 1800)
        30 -> { }
    }
}

private fun generateTone(freqHz: Double, durationMs: Int) {
    val sampleRate = 44100
    val numSamples = (durationMs * sampleRate / 1000)
    val sample = DoubleArray(numSamples)
    val generatedSnd = ByteArray(2 * numSamples)
    val fadeDurationMs = 50
    val fadeSamples = (fadeDurationMs * sampleRate / 1000)

    for (i in 0 until numSamples) {
        var amplitude = 1.0
        if (i < fadeSamples) {
            amplitude = i.toDouble() / fadeSamples
        } else if (i > numSamples - fadeSamples) {
            amplitude = (numSamples - i).toDouble() / fadeSamples
        }
        sample[i] = amplitude * sin(2.0 * PI * i.toDouble() / (sampleRate.toDouble() / freqHz))
    }

    var idx = 0
    for (dVal in sample) {
        val valShort = (dVal * 32767).toInt().toShort()
        generatedSnd[idx++] = (valShort.toInt() and 0x00ff).toByte()
        generatedSnd[idx++] = ((valShort.toInt() and 0xff00) ushr 8).toByte()
    }

    val audioTrack = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
        )
        .setBufferSizeInBytes(generatedSnd.size)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()

    audioTrack.setVolume(AudioTrack.getMaxVolume())
    audioTrack.write(generatedSnd, 0, generatedSnd.size)
    audioTrack.play()
    
    Thread {
        Thread.sleep(durationMs.toLong() + 200)
        audioTrack.stop()
        audioTrack.release()
    }.start()
}
