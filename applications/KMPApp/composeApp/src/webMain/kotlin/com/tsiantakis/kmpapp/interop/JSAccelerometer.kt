@file:OptIn(ExperimentalWasmJsInterop::class)

package com.tsiantakis.kmpapp.interop

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.js

external class Accelerometer() : JsAny {
    val x: Double
    val y: Double
    val z: Double
    fun start()
    fun stop()
    fun addEventListener(event: String, callback: () -> Unit)
}

// Helper to create the options object in Wasm
fun sensorOptions(frequency: Int): JsAny =
    js("{ frequency: frequency }")

