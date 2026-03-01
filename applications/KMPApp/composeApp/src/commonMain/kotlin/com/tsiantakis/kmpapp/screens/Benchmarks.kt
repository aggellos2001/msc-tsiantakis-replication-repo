package com.tsiantakis.kmpapp.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random


const val REPETITIONS = 30

expect object AppContext

expect fun printToLog(message: String)
var testsRunning by mutableStateOf(false)
expect fun debugBuild(): String
expect fun debuggable(context: AppContext): Boolean
expect fun profileable(context: AppContext): Boolean

inline fun repeatBlock(iterations: Int, block: () -> Unit) {
    testsRunning = true
    require(iterations > 0)
    repeat(iterations) {
        block()
    }
    testsRunning = false
}

@Composable
fun Benchmarks() {
    val context = AppContext
    val scope = rememberCoroutineScope()
    var currentTestRunning by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.padding(10.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text("Build type: ${debugBuild()}")
        Text("Debuggable flag: ${debuggable(AppContext)}")
        Text("Profileable flag: ${profileable(AppContext)}")

        Button(enabled = !testsRunning, onClick = {
            scope.launch {
                currentTestRunning = "accelerometer"
                repeatBlock(REPETITIONS) {
                    accelerometerTest(AppContext)
                }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Run accelerometer test") }

        Button(enabled = !testsRunning, onClick = {
            scope.launch {
                currentTestRunning = "image"
                val filename = "solar_system.jpg"
                loadImageToDevice(context = context, filename)
                repeatBlock(REPETITIONS) {
                    imageTest(context, filename)
                }
                deleteImageFromDevice(AppContext, filename)
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Run image test") }

        Button(enabled = !testsRunning, onClick = {
            scope.launch {
                currentTestRunning = "contact"
                repeatBlock(REPETITIONS) {
                    contactTest(context, "John Doe", "6934567890")
                }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Run contact test") }

        Button(enabled = !testsRunning, onClick = {
            scope.launch {
                currentTestRunning = "light"
                repeatBlock(REPETITIONS) {
                    lightTest(AppContext)
                }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Run light test") }

        Button(enabled = !testsRunning, onClick = {
            scope.launch {
                currentTestRunning = "math"
                repeatBlock(REPETITIONS) {
                    mathTest()
                }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Run math test") }

        AnimatedVisibility(testsRunning) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator()
                Text(
                    style = TextStyle(fontWeight = FontWeight.Bold),
                    text = "The $currentTestRunning test is running. Please wait..."
                )
            }
        }
    }
}

expect suspend fun accelerometerTest(context: AppContext)
expect suspend fun loadImageToDevice(context: AppContext, fileName: String)
expect suspend fun imageTest(context: AppContext, fileName: String)
expect suspend fun deleteImageFromDevice(context: AppContext, fileName: String)
expect suspend fun contactTest(context: AppContext, name: String, phoneNumber: String)
expect suspend fun lightTest(context: AppContext)

expect fun traceBeginAsyncSection(methodName: String, cookie: Int)
expect fun traceEndAsyncSection(methodName: String, cookie: Int)
suspend fun mathTest() = withContext(Dispatchers.Default) {
    var localResult = 0.0
    val cookie = Random.nextInt()
    traceBeginAsyncSection("kmp:math", cookie)
    for (j in 1..5) {
        for (k in 1..1_000_000) {
            localResult += log2(k.toDouble()) + (3 * k / 2 * j) + sqrt(k.toDouble()) + k.toDouble()
                .pow(j - 1)
        }
    }
    traceEndAsyncSection("kmp:math", cookie)
    printToLog("Calculated math result as $localResult")
}