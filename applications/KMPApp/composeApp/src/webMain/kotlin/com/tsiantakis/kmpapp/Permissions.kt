package com.tsiantakis.kmpapp

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlin.js.js

@ExperimentalWasmJsInterop
external interface PermissionStatus : JsAny {
    val state: String // "granted", "denied", or "prompt"
}

@ExperimentalWasmJsInterop
external interface Permissions {
    fun query(descriptor: JsAny): Promise<PermissionStatus>
}

@ExperimentalWasmJsInterop
fun createPermissionDescriptor(name: String): JsAny =
    js("{ name: name }")

// Extension to make navigator.permissions accessible
@ExperimentalWasmJsInterop
val navigatorPermissions: Permissions = js("navigator.permissions")