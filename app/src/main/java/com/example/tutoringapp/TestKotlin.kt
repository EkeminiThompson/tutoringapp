package com.example.tutoringapp

import java.sql.DriverManager.println
import kotlin.collections.emptyList
import kotlin.collections.mutableListOf
import kotlin.collections.mapOf
import kotlin.text.trim
import kotlin.text.isNotEmpty

fun testKotlin() {
    val empty = emptyList<String>()
    val mutableList = mutableListOf("item")
    val map = mapOf("key" to "value")
    val str = " test ".trim()
    val isNotEmpty = str.isNotEmpty()
    println("Empty: $empty, Mutable: $mutableList, Map: $map, Trimmed: $str, NotEmpty: $isNotEmpty")
}
