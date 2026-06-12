package com.example.tickclockapp

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
enum class AppNav { Main, History, Workouts }

data class WorkoutStep(val screen: AppScreen, val duration: Int)
data class WorkoutRoutine(val name: String, val steps: List<WorkoutStep>)
data class WorkoutLog(val name: String, val timeRange: String)

val routines = listOf(
    WorkoutRoutine(
        "Morning Exercises ☀️",
        listOf(
            WorkoutStep(AppScreen.Screen2, 600), // 10m
            WorkoutStep(AppScreen.Screen1, 300), // 5m
            WorkoutStep(AppScreen.Screen1, 300)  // 5m
        )
    ),
    WorkoutRoutine(
        "Evening Exercises 🌙",
        listOf(
            WorkoutStep(AppScreen.Screen2, 480), // 8m
            WorkoutStep(AppScreen.Screen1, 240), // 4m
            WorkoutStep(AppScreen.Screen1, 240)  // 4m
        )
    ),
    WorkoutRoutine(
        "Workout 1",
        listOf(
            WorkoutStep(AppScreen.Screen2, 60), // 1m
            WorkoutStep(AppScreen.Screen1, 60), // 1m
            WorkoutStep(AppScreen.Screen1, 60)  // 1m
        )
    )
)

private val defaultRoutines = routines

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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TickClockScreen() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    var currentScreen by remember { mutableStateOf(AppScreen.Screen2) }

    // Navigation and Logs
    var navState by remember { mutableStateOf(AppNav.Main) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var workoutLogs by remember { mutableStateOf<List<WorkoutLog>>(loadLogs(context)) }
    var routines by remember { mutableStateOf<List<WorkoutRoutine>>(loadRoutines(context)) }
    var routineStartTime by remember { mutableStateOf("") }

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
                "Today is $dateStr, right now is $timeStr. continue with your next exercise"
            } else {
                "Time now is $timeStr, continue with your next exercise"
            }
            
            tts?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null)
            
            delay(10000) // 10s break
            isBreakActive = false
            
            // Start next step
            if (activeRoutineIndex != -1 && workoutStepIndex < routines[activeRoutineIndex].steps.size) {
                val nextStep = routines[activeRoutineIndex].steps[workoutStepIndex]
                currentScreen = nextStep.screen
                if (currentScreen == AppScreen.Screen1) isRunning1 = true else isRunning2 = true
            }
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

                // Automation transition check
                if (isWorkoutActive && activeRoutineIndex != -1 && routines[activeRoutineIndex].steps[workoutStepIndex].screen == AppScreen.Screen1) {
                    val duration = routines[activeRoutineIndex].steps[workoutStepIndex].duration
                    if (totalSeconds1 >= duration) {
                        isRunning1 = false
                        totalSeconds1 = 0
                        cycleSeconds1 = 0
                        roundCount1 = 0
                        
                        if (workoutStepIndex < routines[activeRoutineIndex].steps.size - 1) {
                            workoutStepIndex++
                            isBreakActive = true
                        } else {
                            val routineName = routines[activeRoutineIndex].name
                            activeRoutineIndex = -1
                            val endTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                            val logEntry = WorkoutLog(routineName, "$routineStartTime - $endTime")
                            workoutLogs = (workoutLogs + logEntry).takeLast(50)
                            saveLogs(context, workoutLogs)
                            tts?.speak("Workout complete", TextToSpeech.QUEUE_FLUSH, null, null)
                            navState = AppNav.History
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
                if (isWorkoutActive && activeRoutineIndex != -1 && routines[activeRoutineIndex].steps[workoutStepIndex].screen == AppScreen.Screen2) {
                    val duration = routines[activeRoutineIndex].steps[workoutStepIndex].duration
                    if (totalSeconds2 >= duration) {
                        isRunning2 = false
                        totalSeconds2 = 0
                        if (workoutStepIndex < routines[activeRoutineIndex].steps.size - 1) {
                            workoutStepIndex++
                            isBreakActive = true
                        } else {
                            val routineName = routines[activeRoutineIndex].name
                            activeRoutineIndex = -1
                            val endTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                            val logEntry = WorkoutLog(routineName, "$routineStartTime - $endTime")
                            workoutLogs = (workoutLogs + logEntry).takeLast(50)
                            saveLogs(context, workoutLogs)
                            tts?.speak("Workout complete", TextToSpeech.QUEUE_FLUSH, null, null)
                            navState = AppNav.History
                        }
                        break
                    } else if (duration - totalSeconds2 == 30) {
                        val minutes = totalSeconds2 / 60
                        val msg = if (minutes > 0) "$minutes minutes and 30 seconds" else "30 seconds"
                        tts?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null)
                    } else if (duration - totalSeconds2 == 15) {
                        // 15s before end sequence: C5 E5 G5
                        launch {
                            generateTone(523.0, 150)
                            delay(250)
                            generateTone(659.0, 150)
                            delay(250)
                            generateTone(784.0, 300)
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF1C1C1C),
                drawerContentColor = Color.White
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                NavigationDrawerItem(
                    label = { Text("Workout Timer") },
                    selected = navState == AppNav.Main,
                    onClick = { navState = AppNav.Main; scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )
                NavigationDrawerItem(
                    label = { Text("History") },
                    selected = navState == AppNav.History,
                    onClick = { navState = AppNav.History; scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )
                NavigationDrawerItem(
                    label = { Text("Workouts") },
                    selected = navState == AppNav.Workouts,
                    onClick = { navState = AppNav.Workouts; scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when (navState) {
                                AppNav.History -> "Workout History"
                                AppNav.Workouts -> "Workouts"
                                else -> ""
                            },
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { activity?.finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Exit", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Black
        ) { padding ->
            Surface(
                modifier = Modifier.fillMaxSize().padding(padding),
                color = MaterialTheme.colorScheme.background
            ) {
                when (navState) {
                    AppNav.Main -> {
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
                                verticalArrangement = Arrangement.Top,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Spacer(modifier = Modifier.height(20.dp))
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
                                    Spacer(modifier = Modifier.height(32.dp))
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
                                    Spacer(modifier = Modifier.height(32.dp))
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

                                Spacer(modifier = Modifier.height(24.dp))

                                val softRed = Color(0xFFD32F2F)
                                val pagerState = rememberPagerState { routines.size }
                                Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
                                        if (index >= routines.size) return@HorizontalPager
                                        val routine = routines[index]
                                        val isThisRoutineActive = activeRoutineIndex == index
                                        val totalElapsedSeconds = if (isThisRoutineActive) {
                                            val completedStepsDuration = routine.steps.take(workoutStepIndex).sumOf { it.duration }
                                            if (!isBreakActive && workoutStepIndex < routine.steps.size) {
                                                val currentStepDuration = if (routine.steps[workoutStepIndex].screen == AppScreen.Screen1) totalSeconds1 else totalSeconds2
                                                completedStepsDuration + currentStepDuration
                                            } else {
                                                completedStepsDuration
                                            }
                                        } else 0

                                        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                                            OutlinedButton(
                                                onClick = {
                                                    if (isThisRoutineActive) {
                                                        if (isBreakActive) {
                                                            activeRoutineIndex = -1
                                                            isBreakActive = false
                                                        } else {
                                                            if (routines[activeRoutineIndex].steps[workoutStepIndex].screen == AppScreen.Screen2) {
                                                                isRunning2 = !isRunning2
                                                            } else isRunning1 = !isRunning1
                                                        }
                                                    } else if (!isWorkoutActive) {
                                                        val announcement = routine.name.replace("☀️", "").replace("🌙", "").replace("-", " ")
                                                        tts?.speak("$announcement begins now", TextToSpeech.QUEUE_FLUSH, null, null)
                                                        activeRoutineIndex = index
                                                        workoutStepIndex = 0
                                                        totalSeconds1 = 0
                                                        cycleSeconds1 = 0
                                                        roundCount1 = 0
                                                        totalSeconds2 = 0
                                                        routineStartTime = SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.getDefault()).format(Date())
                                                        if (index != -1 && index < routines.size && routines[index].steps.isNotEmpty()) {
                                                            currentScreen = routines[index].steps[0].screen
                                                            if (currentScreen == AppScreen.Screen1) isRunning1 = true else isRunning2 = true
                                                        }
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
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 8.dp)) {
                                                    Text(text = routine.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    WorkoutProgressBar(routine, totalElapsedSeconds)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    AppNav.History -> {
                        BackHandler { navState = AppNav.Main }
                        HistoryScreen(workoutLogs)
                    }
                    AppNav.Workouts -> {
                        BackHandler { navState = AppNav.Main }
                        WorkoutsScreen(routines) { updatedRoutines ->
                            routines = updatedRoutines
                            saveRoutines(context, updatedRoutines)
                            navState = AppNav.Main
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutProgressBar(routine: WorkoutRoutine, elapsedSeconds: Int, modifier: Modifier = Modifier) {
    val totalSeconds = routine.steps.sumOf { it.duration }
    val totalMinutes = totalSeconds / 60
    val elapsedMinutes = elapsedSeconds / 60

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val width = this.maxWidth
            var currentStartMinute = 0
            routine.steps.forEach { step ->
                val stepMinutes = step.duration / 60
                val emoji = if (step.screen == AppScreen.Screen1) "🏃" else "🧘"
                val fraction = currentStartMinute.toFloat() / totalMinutes.coerceAtLeast(1)
                
                Text(
                    text = "$emoji$stepMinutes",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.offset(x = (width.value * fraction).dp)
                )
                currentStartMinute += stepMinutes
            }
        }
        
        Spacer(modifier = Modifier.height(2.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().height(6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            repeat(totalMinutes) { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (i < elapsedMinutes) Color(0xFF90EE90) else Color.Gray.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(1.dp)
                        )
                )
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

        // Draw specific ticks (55-00) at the top of the circle
        for (i in listOf(55, 56, 57, 58, 59, 0)) {
            val isHour = i % 5 == 0
            rotate(i * 6f - 90f, pivot = center) {
                val tickLength = (if (isHour) 7.dp else 5.dp).toPx()
                val startInward = 8.dp.toPx() 
                drawLine(
                    color = if (isHour) accentColor else Color.White.copy(alpha = 0.5f),
                    start = Offset(center.x + radius - startInward - tickLength, center.y),
                    end = Offset(center.x + radius - startInward, center.y),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        rotate((smoothHour * 30f) - 90f) {
            drawLine(color = accentColor.copy(alpha = 0.8f), start = center, end = Offset(center.x + (radius * 0.5f), center.y), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }
        rotate((smoothMinute * 6f) - 90f) {
            drawLine(color = accentColor.copy(alpha = 0.8f), start = center, end = Offset(center.x + (radius * 0.75f), center.y), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }
        
        val secondDotColor = if (second == 58L || second == 59L || second == 0L) Color.Red else Color.White
        rotate((second * 6f) - 90f) {
            drawCircle(color = secondDotColor, radius = 4.dp.toPx(), center = Offset(center.x + (radius - 6.dp.toPx()), center.y))
        }
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

@Composable
fun HistoryScreen(logs: List<WorkoutLog>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (logs.isEmpty()) {
            item {
                Text("No history yet.", color = Color.Gray)
            }
        } else {
            items(logs.asReversed()) { log ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text(
                        text = log.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF90EE90)
                    )
                    Text(
                        text = log.timeRange,
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun WorkoutsScreen(
    initialRoutines: List<WorkoutRoutine>,
    onSave: (List<WorkoutRoutine>) -> Unit
) {
    var routinesState by remember { mutableStateOf(initialRoutines) }
    var editingRoutineIndex by remember { mutableIntStateOf(-1) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val isDirty = routinesState != initialRoutines

    BackHandler {
        if (isDirty) {
            showDiscardDialog = true
        } else {
            onSave(initialRoutines) // Just go back
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Unsaved Changes") },
            text = { Text("You have unsaved changes. Do you want to save or discard them?") },
            confirmButton = {
                TextButton(onClick = {
                    onSave(routinesState)
                    showDiscardDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    onSave(initialRoutines)
                    showDiscardDialog = false
                }) { Text("Discard") }
            }
        )
    }

    if (editingRoutineIndex != -1) {
        val routine = routinesState[editingRoutineIndex]
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Edit Workout", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = routine.name,
                onValueChange = { if (it.length <= 25) {
                    routinesState = routinesState.toMutableList().apply {
                        this[editingRoutineIndex] = routine.copy(name = it)
                    }
                } },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF90EE90),
                    unfocusedBorderColor = Color.Gray
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Steps", fontSize = 18.sp, color = Color.Gray)
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(routine.steps.size) { stepIdx ->
                    val step = routine.steps[stepIdx]
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                val nextType = if (step.screen == AppScreen.Screen1) AppScreen.Screen2 else AppScreen.Screen1
                                routinesState = routinesState.toMutableList().apply {
                                    val newSteps = routine.steps.toMutableList().apply {
                                        this[stepIdx] = step.copy(screen = nextType)
                                    }
                                    this[editingRoutineIndex] = routine.copy(steps = newSteps)
                                }
                            },
                            modifier = Modifier.width(100.dp)
                        ) {
                            Text(if (step.screen == AppScreen.Screen1) "Workout" else "Count")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = (step.duration / 60).toString(),
                            onValueChange = {
                                val newMins = it.toIntOrNull() ?: 0
                                routinesState = routinesState.toMutableList().apply {
                                    val newSteps = routine.steps.toMutableList().apply {
                                        this[stepIdx] = step.copy(duration = newMins * 60)
                                    }
                                    this[editingRoutineIndex] = routine.copy(steps = newSteps)
                                }
                            },
                            label = { Text("Min") },
                            modifier = Modifier.width(80.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF90EE90),
                                unfocusedBorderColor = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            routinesState = routinesState.toMutableList().apply {
                                val newSteps = routine.steps.toMutableList().apply { removeAt(stepIdx) }
                                this[editingRoutineIndex] = routine.copy(steps = newSteps)
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Step", tint = Color.Red)
                        }
                    }
                }
                item {
                    TextButton(onClick = {
                        routinesState = routinesState.toMutableList().apply {
                            val newSteps = routine.steps.toMutableList().apply {
                                add(WorkoutStep(AppScreen.Screen2, 60))
                            }
                            this[editingRoutineIndex] = routine.copy(steps = newSteps)
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Add Step")
                    }
                }
            }
            Button(
                onClick = { editingRoutineIndex = -1 },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006400))
            ) {
                Text("Done Editing")
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Your Workouts", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(routinesState.size) { idx ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (idx > 0) {
                                    routinesState = routinesState.toMutableList().apply {
                                        val item = removeAt(idx)
                                        add(idx - 1, item)
                                    }
                                }
                            },
                            enabled = idx > 0
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = if (idx > 0) Color.White else Color.Gray)
                        }
                        IconButton(
                            onClick = {
                                if (idx < routinesState.size - 1) {
                                    routinesState = routinesState.toMutableList().apply {
                                        val item = removeAt(idx)
                                        add(idx + 1, item)
                                    }
                                }
                            },
                            enabled = idx < routinesState.size - 1
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = if (idx < routinesState.size - 1) Color.White else Color.Gray)
                        }
                        Text(routinesState[idx].name, modifier = Modifier.weight(1f), fontSize = 18.sp, color = Color.White)
                        OutlinedButton(onClick = { editingRoutineIndex = idx }) {
                            Text("Edit")
                        }
                        IconButton(onClick = {
                            routinesState = routinesState.toMutableList().apply { removeAt(idx) }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Routine", tint = Color.Red)
                        }
                    }
                }
                item {
                    TextButton(onClick = {
                        routinesState = routinesState + WorkoutRoutine("New Workout", listOf(WorkoutStep(AppScreen.Screen2, 60)))
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Add Workout")
                    }
                }
            }
            Button(
                onClick = { onSave(routinesState) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006400))
            ) {
                Text("Save All Workouts")
            }
        }
    }
}

private fun saveRoutines(context: android.content.Context, routines: List<WorkoutRoutine>) {
    val prefs = context.getSharedPreferences("workout_routines", android.content.Context.MODE_PRIVATE)
    // Format: Name|StepType,Duration;StepType,Duration...||NextRoutine
    val data = routines.joinToString("||") { routine ->
        val stepsData = routine.steps.joinToString(";") { "${if (it.screen == AppScreen.Screen1) "W" else "C"},${it.duration}" }
        "${routine.name}|$stepsData"
    }
    prefs.edit().putString("routines_v1", data).apply()
}

private fun loadRoutines(context: android.content.Context): List<WorkoutRoutine> {
    val prefs = context.getSharedPreferences("workout_routines", android.content.Context.MODE_PRIVATE)
    val data = prefs.getString("routines_v1", "") ?: ""
    if (data.isEmpty()) return defaultRoutines
    
    return try {
        data.split("||").map { routineData ->
            val parts = routineData.split("|")
            val name = parts[0]
            val stepsData = if (parts.size > 1) parts[1] else ""
            val steps = if (stepsData.isNotEmpty()) {
                stepsData.split(";").map { stepStr ->
                    val sParts = stepStr.split(",")
                    val type = if (sParts[0] == "W") AppScreen.Screen1 else AppScreen.Screen2
                    val duration = sParts[1].toIntOrNull() ?: 60
                    WorkoutStep(type, duration)
                }
            } else emptyList()
            WorkoutRoutine(name, steps)
        }
    } catch (e: Exception) {
        defaultRoutines
    }
}

private fun saveLogs(context: android.content.Context, logs: List<WorkoutLog>) {
    val prefs = context.getSharedPreferences("workout_logs", android.content.Context.MODE_PRIVATE)
    val data = logs.joinToString(";") { "${it.name}|${it.timeRange}" }
    prefs.edit().putString("entries", data).apply()
}

private fun loadLogs(context: android.content.Context): List<WorkoutLog> {
    val prefs = context.getSharedPreferences("workout_logs", android.content.Context.MODE_PRIVATE)
    val data = prefs.getString("entries", "") ?: ""
    if (data.isEmpty()) return emptyList()
    return data.split(";").map {
        val parts = it.split("|")
        if (parts.size == 2) WorkoutLog(parts[0], parts[1]) else WorkoutLog(parts[0], "")
    }
}
