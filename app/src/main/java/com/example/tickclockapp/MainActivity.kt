package com.example.tickclockapp

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.RingtoneManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

enum class AppScreen { Screen1, Screen2 }

data class WorkoutStep(val screen: AppScreen, val duration: Int)
data class WorkoutRoutine(val name: String, val steps: List<WorkoutStep>)

val routines = listOf(
    WorkoutRoutine(
        "Workout 10-5-5",
        listOf(
            WorkoutStep(AppScreen.Screen2, 600), // 10m
            WorkoutStep(AppScreen.Screen1, 300), // 5m
            WorkoutStep(AppScreen.Screen1, 300)  // 5m
        )
    ),
    WorkoutRoutine(
        "Workout 8-4-4",
        listOf(
            WorkoutStep(AppScreen.Screen2, 480), // 8m
            WorkoutStep(AppScreen.Screen1, 240), // 4m
            WorkoutStep(AppScreen.Screen1, 240)  // 4m
        )
    ),
    WorkoutRoutine(
        "Workout 1-1-1",
        listOf(
            WorkoutStep(AppScreen.Screen2, 60), // 1m
            WorkoutStep(AppScreen.Screen1, 60), // 1m
            WorkoutStep(AppScreen.Screen1, 60)  // 1m
        )
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                TickClockScreen()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TickClockScreen() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    var currentScreen by remember { mutableStateOf(AppScreen.Screen2) }

    // TTS Setup
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val ttsInstance = TextToSpeech(context) { _ -> }
        tts = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    // Screen 1 States
    var isRunning1 by remember { mutableStateOf(value = false) }
    var totalSeconds1 by remember { mutableIntStateOf(0) }
    var cycleSeconds1 by remember { mutableIntStateOf(0) }
    var roundCount1 by remember { mutableIntStateOf(0) }

    // Screen 2 States
    var isRunning2 by remember { mutableStateOf(value = false) }
    var totalSeconds2 by remember { mutableIntStateOf(0) }

    // Workout Automation State
    var activeRoutineIndex by remember { mutableIntStateOf(-1) }
    var workoutStepIndex by remember { mutableIntStateOf(0) }
    val isWorkoutActive = activeRoutineIndex != -1
    var isBreakActive by remember { mutableStateOf(false) }

    // Break Management
    LaunchedEffect(isBreakActive) {
        if (isBreakActive) {
            val sdfTime = SimpleDateFormat("h:mm a", Locale.getDefault())
            val sdfDate = SimpleDateFormat("EEEE MMMM d", Locale.getDefault())
            val now = Date()
            val timeStr = sdfTime.format(now)
            val dateStr = sdfDate.format(now)

            // Step 0 is usually Count, Step 1 is first Workout.
            // When Step 1 ends, workoutStepIndex is incremented to 2.
            val msg = if (workoutStepIndex == 2) {
                "Today is $dateStr, right now is $timeStr. start your next exercise"
            } else {
                "Time now is $timeStr, start your next exercise"
            }
            
            tts?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null)
            
            delay(10000) // 10s break
            isBreakActive = false
            
            // Start next step
            val nextStep = routines[activeRoutineIndex].steps[workoutStepIndex]
            currentScreen = nextStep.screen
            if (currentScreen == AppScreen.Screen1) isRunning1 = true else isRunning2 = true
        }
    }

    // Keep screen on while the app is visible
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Logic for Screen 1 (with 30s audio cycle)
    LaunchedEffect(isRunning1, workoutStepIndex) {
        if (isRunning1) {
            while (isRunning1) {
                cycleSeconds1++
                if (cycleSeconds1 > 30) cycleSeconds1 = 1
                if (cycleSeconds1 == 1) roundCount1++
                
                playToneForSecond(cycleSeconds1)
                
                // Voice call at 21st second (Transition phase)
                if (cycleSeconds1 == 21) {
                    tts?.speak(roundCount1.toString(), TextToSpeech.QUEUE_FLUSH, null, null)
                }
                
                // Only increment cumulative workout time once we are in the Active phase (starts at sec 6)
                // or if we already started rounds. 
                if (roundCount1 > 0) totalSeconds1++

                if (isWorkoutActive && (routines[activeRoutineIndex].steps[workoutStepIndex].screen == AppScreen.Screen1)) {
                    if (totalSeconds1 >= routines[activeRoutineIndex].steps[workoutStepIndex].duration) {
                        isRunning1 = false
                        totalSeconds1 = 0
                        cycleSeconds1 = 0
                        roundCount1 = 0
                        
                        if (workoutStepIndex < routines[activeRoutineIndex].steps.size - 1) {
                            workoutStepIndex++
                            isBreakActive = true
                        } else {
                            activeRoutineIndex = -1
                            tts?.speak("Workout complete", TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                        break
                    }
                }
                delay(1000)
            }
        }
    }

    // Logic for Screen 2 (no beeps, TTS every 1 min)
    LaunchedEffect(isRunning2, workoutStepIndex) {
        if (isRunning2) {
            while (isRunning2) {
                totalSeconds2++
                // Automation transition check
                if (isWorkoutActive && routines[activeRoutineIndex].steps[workoutStepIndex].screen == AppScreen.Screen2) {
                    val duration = routines[activeRoutineIndex].steps[workoutStepIndex].duration
                    if (totalSeconds2 >= duration) {
                        isRunning2 = false
                        totalSeconds2 = 0
                        if (workoutStepIndex < routines[activeRoutineIndex].steps.size - 1) {
                            workoutStepIndex++
                            isBreakActive = true
                        } else {
                            activeRoutineIndex = -1
                            tts?.speak("Workout complete", TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                        break
                    } else if (duration - totalSeconds2 == 15) {
                        // 15s before end sequence: C5 E5 G5
                        launch {
                            generateTone(523.0, 150)
                            delay(250)
                            generateTone(659.0, 150)
                            delay(250)
                            generateTone(784.0, 150)
                        }
                    }
                }
                if ((totalSeconds2 > 0) && ((totalSeconds2 % 60) == 0)) {
                    val minutes = totalSeconds2 / 60
                    val hours = minutes / 60
                    val msg = if (hours > 0) {
                        val remMins = minutes % 60
                        if (remMins > 0) "$hours hours $remMins minutes" else "$hours hours"
                    } else "$minutes minutes"
                    tts?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null)
                }
                delay(1000)
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier.fillMaxSize().clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                currentScreen = if (currentScreen == AppScreen.Screen1) AppScreen.Screen2 else AppScreen.Screen1
            }
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val lightGreen = Color(0xFF90EE90)
                val darkGreen = Color(0xFF006400)
                val mainButtonSize = 240.dp

                if (currentScreen == AppScreen.Screen1) {
                    Surface(
                        modifier = Modifier.size(mainButtonSize).combinedClickable(
                            onClick = { isRunning1 = !isRunning1 },
                            onLongClick = {
                                isRunning1 = false
                                totalSeconds1 = 0
                                cycleSeconds1 = 0
                                roundCount1 = 0
                            },
                        ),
                        shape = CircleShape,
                        border = BorderStroke(3.dp, lightGreen),
                        color = if (isRunning1) darkGreen else Color.Transparent,
                        contentColor = Color.White,
                    ) {
                        RealTimeClockOverlay()
                    }
                    Spacer(modifier = Modifier.height(60.dp))
                    Text(
                        text = "%02d:%02d".format(totalSeconds1 / 60, totalSeconds1 % 60),
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(mainButtonSize).combinedClickable(
                            onClick = { isRunning2 = !isRunning2 },
                            onLongClick = {
                                isRunning2 = false
                                totalSeconds2 = 0
                            },
                        ),
                        shape = CircleShape,
                        border = BorderStroke(3.dp, lightGreen),
                        color = if (isRunning2) darkGreen else Color.Transparent,
                        contentColor = Color.White,
                    ) {
                        RealTimeClockOverlay()
                    }
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

                val softRed = Color(0xFFD32F2F)
                val pagerState = rememberPagerState { routines.size }
                Box(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
                        val routine = routines[index]
                        val isThisRoutineActive = activeRoutineIndex == index
                        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                            OutlinedButton(
                                onClick = {
                                    if (isThisRoutineActive) {
                                        if (isBreakActive) {
                                            // For now, pause the routine by ending it or just ignore?
                                            // The user said they can tap to pause/continue.
                                            // To keep it simple, I'll stop the clocks.
                                            // But if isBreakActive is true, no clock is running.
                                            // I'll stop the whole routine if they tap while in break.
                                            activeRoutineIndex = -1
                                            isBreakActive = false
                                        } else {
                                            if (routines[activeRoutineIndex].steps[workoutStepIndex].screen == AppScreen.Screen2) {
                                                isRunning2 = !isRunning2
                                            } else isRunning1 = !isRunning1
                                        }
                                    } else if (!isWorkoutActive) {
                                        val announcement = routine.name.replace("-", " ")
                                        tts?.speak("$announcement begins now", TextToSpeech.QUEUE_FLUSH, null, null)
                                        activeRoutineIndex = index
                                        workoutStepIndex = 0
                                        totalSeconds1 = 0
                                        cycleSeconds1 = 0
                                        roundCount1 = 0
                                        totalSeconds2 = 0
                                        currentScreen = routine.steps[0].screen
                                        if (currentScreen == AppScreen.Screen1) isRunning1 = true else isRunning2 = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(2.dp, if (isThisRoutineActive) softRed else lightGreen.copy(alpha = 0.7f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isThisRoutineActive) softRed.copy(alpha = 0.2f) else Color.Transparent,
                                    contentColor = if (isThisRoutineActive) Color.White else Color.White.copy(alpha = 0.7f),
                                ),
                            ) {
                                Text(text = routine.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(120.dp)) // Move Exit button even lower
                OutlinedButton(
                    onClick = { activity?.finish() },
                    modifier = Modifier.width(120.dp).height(50.dp),
                    border = BorderStroke(1.dp, lightGreen.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = Color.White.copy(alpha = 0.7f))
                ) {
                    Text("Exit", fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun RealTimeClockOverlay(modifier: Modifier = Modifier) {
    val timeState = produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(100)
        }
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        val time = timeState.value
        val zone = java.util.TimeZone.getDefault()
        val localTime = time + zone.getOffset(time)
        val totalSeconds = localTime / 1000
        val second = totalSeconds % 60
        val minute = (totalSeconds / 60) % 60
        val hour = (totalSeconds / 3600) % 12
        val smoothMinute = minute + (second / 60f)
        val smoothHour = hour + (smoothMinute / 60f)
        val center = center
        val radius = size.minDimension / 2
        val accentColor = Color(0xFF90EE90)

        rotate((smoothHour * 30f) - 90f) {
            drawLine(color = accentColor.copy(alpha = 0.8f), start = center, end = Offset(center.x + (radius * 0.5f), center.y), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }
        rotate((smoothMinute * 6f) - 90f) {
            drawLine(color = accentColor.copy(alpha = 0.8f), start = center, end = Offset(center.x + (radius * 0.75f), center.y), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }
        rotate((second * 6f) - 90f) {
            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(center.x + (radius - 6.dp.toPx()), center.y))
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
        // 1. Preparation Phase (1-5s)
        1 -> generateTone(freq523, 100, volume = 0.25f)
        in 2..3 -> generateTone(freq523, 100)
        4 -> generateTone(freq659, 1800)
        5 -> { }
        
        // 2. Active Phase (6-20s)
        in 6..18 -> generateTone(freq392, 100)
        19 -> generateTone(freq440, 1800)
        20 -> { }
        
        // 3. Transition (21s) - Voice call handled in LaunchedEffect
        21 -> { }
        
        // 4. Recovery Phase (22-30s)
        in 22..30 -> { }
    }
}

private fun generateTone(freqHz: Double, durationMs: Int, volume: Float = 1.0f) {
    val sampleRate = 44100
    val numSamples = (durationMs * sampleRate) / 1000
    val sample = DoubleArray(numSamples)
    val generatedSnd = ByteArray(2 * numSamples)
    val fadeDurationMs = 50
    val fadeSamples = (fadeDurationMs * sampleRate) / 1000
    for (i in 0 until numSamples) {
        var amplitude = 1.0
        if (i < fadeSamples) amplitude = i.toDouble() / fadeSamples
        else if (i > (numSamples - fadeSamples)) amplitude = (numSamples - i).toDouble() / fadeSamples
        sample[i] = amplitude * sin((2.0 * PI * i.toDouble()) / (sampleRate.toDouble() / freqHz))
    }
    var idx = 0
    for (dVal in sample) {
        val valShort = (dVal * 32767).toInt().toShort()
        generatedSnd[idx++] = (valShort.toInt() and 0x00ff).toByte()
        generatedSnd[idx++] = ((valShort.toInt() and 0xff00) ushr 8).toByte()
    }
    val audioTrack = try {
        AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(generatedSnd.size)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
    } catch (e: Exception) {
        e.printStackTrace()
        return
    }
    try {
        audioTrack.setVolume(AudioTrack.getMaxVolume() * volume)
        audioTrack.write(generatedSnd, 0, generatedSnd.size)
        audioTrack.play()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    Thread {
        try {
            Thread.sleep(durationMs.toLong() + 200)
            if (audioTrack.state != AudioTrack.STATE_UNINITIALIZED) {
                audioTrack.stop()
                audioTrack.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }.start()
}
