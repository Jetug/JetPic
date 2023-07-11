package com.simplemobiletools.gallery.pro.data.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val IOScope = CoroutineScope(Dispatchers.IO)
val DefaultScope = CoroutineScope(Dispatchers.Default)
val MainScope = CoroutineScope(Dispatchers.Main)

fun launchIO(block: suspend CoroutineScope.() -> Unit) = IOScope.launch { block() }
fun launchDefault(block: suspend CoroutineScope.() -> Unit) = DefaultScope.launch { block() }
fun launchMain(block: suspend CoroutineScope.() -> Unit) = MainScope.launch { block() }


suspend fun <T> withMainContext(block: suspend CoroutineScope.() -> T) : T = withContext(Dispatchers.Main) { block() }
suspend fun <T> withIOContext  (block: suspend CoroutineScope.() -> T) : T = withContext(Dispatchers.IO)   { block() }
