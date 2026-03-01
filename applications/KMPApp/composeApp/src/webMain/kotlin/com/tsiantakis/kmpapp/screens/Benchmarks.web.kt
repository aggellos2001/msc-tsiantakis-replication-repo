package com.tsiantakis.kmpapp.screens

import com.tsiantakis.kmpapp.interop.Accelerometer
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.time.TimeSource
import kotlin.time.TimeSource.Monotonic.ValueTimeMark

actual object AppContext

actual fun debugBuild(): String = "Web build"
actual fun debuggable(context: AppContext): Boolean = true
actual fun profileable(context: AppContext): Boolean = true

@OptIn(ExperimentalWasmJsInterop::class)
actual suspend fun accelerometerTest(context: AppContext): Unit =
    suspendCancellableCoroutine { continuation ->
        val clock = TimeSource.Monotonic
        var start: ValueTimeMark? = null
        val accelerometer = Accelerometer()
        accelerometer.addEventListener("reading") {
            print("${accelerometer.x} ${accelerometer.y} ${accelerometer.z}")
            val end = start?.elapsedNow()
            if (continuation.isActive) {
                accelerometer.stop()
                continuation.resume(Unit) { _, _, _ -> }
            }
        }
        accelerometer.start()
        start = clock.markNow()

        continuation.invokeOnCancellation {
            accelerometer.stop()
        }
    }


actual suspend fun loadImageToDevice(context: AppContext, fileName: String) {
}

actual suspend fun imageTest(
    context: AppContext, fileName: String
) {
    TODO("Not yet implemented")
}

actual suspend fun deleteImageFromDevice(context: AppContext, fileName: String) {
    TODO("Not yet implemented")
}

actual suspend fun contactTest(context: AppContext, name: String, phoneNumber: String) {
    TODO("Not yet implemented")
}


actual suspend fun lightTest(context: AppContext) {
    TODO("Not yet implemented")
}

actual fun traceBeginAsyncSection(methodName: String, cookie: Int) {
}

actual fun traceEndAsyncSection(methodName: String, cookie: Int) {
}

actual fun printToLog(message: String) {
    println(message)
}