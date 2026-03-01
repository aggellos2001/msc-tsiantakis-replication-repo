package com.tsiantakis.kmpapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform