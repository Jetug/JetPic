package com.jetug.gallery.pro.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val IOScope = CoroutineScope(Dispatchers.IO)
val DefaultScope = CoroutineScope(Dispatchers.Default)
val MainScope = CoroutineScope(Dispatchers.Main)

fun launchIO(block: suspend CoroutineScope.() -> Unit) = IOScope.launch { block()}
fun launchDefault(block: suspend CoroutineScope.() -> Unit) = DefaultScope.launch { block()}
fun launchMain(block: suspend CoroutineScope.() -> Unit) = MainScope.launch { block()}

