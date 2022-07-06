package com.simplemobiletools.gallery.pro.data.helpers

import android.app.Activity
import android.content.Context
import com.simplemobiletools.commons.extensions.getDoesFilePathExist
import com.simplemobiletools.commons.extensions.internalStoragePath
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.gallery.pro.activities.mDirs
import com.simplemobiletools.gallery.pro.data.extensions.config
import com.simplemobiletools.gallery.pro.data.extensions.launchDefault
import java.io.File

fun Activity.checkDefaultSpamFolders() = launchDefault {
    if (!config.spamFoldersChecked) {
        val spamFolders = arrayListOf(
            "/storage/emulated/0/Android/data/com.facebook.orca/files/stickers"
        )

        val OTGPath = config.OTGPath
        spamFolders.forEach {
            if (getDoesFilePathExist(it, OTGPath)) {
                config.addExcludedFolder(it)
            }
        }
        config.spamFoldersChecked = true
    }
}

// exclude probably unwanted folders, for example facebook stickers are split between hundreds of separate folders like
// /storage/emulated/0/Android/data/com.facebook.orca/files/stickers/175139712676531/209575122566323
// /storage/emulated/0/Android/data/com.facebook.orca/files/stickers/497837993632037/499671223448714
fun Context.excludeSpamFolders() {
    ensureBackgroundThread {
        try {
            val internalPath = internalStoragePath
            val checkedPaths = ArrayList<String>()
            val oftenRepeatedPaths = ArrayList<String>()
            val paths = mDirs.map { it.path.removePrefix(internalPath) }.toMutableList() as ArrayList<String>
            paths.forEach {
                val parts = it.split("/")
                var currentString = ""
                for (i in 0 until parts.size) {
                    currentString += "${parts[i]}/"

                    if (!checkedPaths.contains(currentString)) {
                        val cnt = paths.count { it.startsWith(currentString) }
                        if (cnt > 50 && currentString.startsWith("/Android/data", true)) {
                            oftenRepeatedPaths.add(currentString)
                        }
                    }

                    checkedPaths.add(currentString)
                }
            }

            val substringToRemove = oftenRepeatedPaths.filter {
                val path = it
                it == "/" || oftenRepeatedPaths.any { it != path && it.startsWith(path) }
            }

            oftenRepeatedPaths.removeAll(substringToRemove)
            val OTGPath = config.OTGPath
            oftenRepeatedPaths.forEach {
                val file = File("$internalPath/$it")
                if (getDoesFilePathExist(file.absolutePath, OTGPath)) {
                    config.addExcludedFolder(file.absolutePath)
                }
            }
        } catch (e: Exception) {
        }
    }
}
