package com.sahmfood.pos.util

import kotlin.math.round

/** Multiplatform money formatting (replaces JVM String.format). */
fun Double.toMoneyString(): String {
    val scaled = round(this * 100).toLong()
    val whole = scaled / 100
    val frac = kotlin.math.abs(scaled % 100)
    return "$whole.${frac.toString().padStart(2, '0')}"
}
