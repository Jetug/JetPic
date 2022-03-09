package com.simplemobiletools.gallery.pro.helpers

import android.content.Intent
import android.provider.MediaStore

fun isPickImageIntent(intent: Intent) = isPickIntent(intent) && (hasImageContentData(intent) || isImageType(intent))

fun isPickVideoIntent(intent: Intent) = isPickIntent(intent) && (hasVideoContentData(intent) || isVideoType(intent))

fun isPickIntent(intent: Intent) = intent.action == Intent.ACTION_PICK

fun isGetContentIntent(intent: Intent) = intent.action == Intent.ACTION_GET_CONTENT && intent.type != null

fun isGetImageContentIntent(intent: Intent) = isGetContentIntent(intent) &&
    (intent.type!!.startsWith("image/") || intent.type == MediaStore.Images.Media.CONTENT_TYPE)

fun isGetVideoContentIntent(intent: Intent) = isGetContentIntent(intent) &&
    (intent.type!!.startsWith("video/") || intent.type == MediaStore.Video.Media.CONTENT_TYPE)

fun isGetAnyContentIntent(intent: Intent) = isGetContentIntent(intent) && intent.type == "*/*"

fun isSetWallpaperIntent(intent: Intent?) = intent?.action == Intent.ACTION_SET_WALLPAPER

fun hasImageContentData(intent: Intent) = (intent.data == MediaStore.Images.Media.EXTERNAL_CONTENT_URI ||
    intent.data == MediaStore.Images.Media.INTERNAL_CONTENT_URI)

fun hasVideoContentData(intent: Intent) = (intent.data == MediaStore.Video.Media.EXTERNAL_CONTENT_URI ||
    intent.data == MediaStore.Video.Media.INTERNAL_CONTENT_URI)

fun isImageType(intent: Intent) = (intent.type?.startsWith("image/") == true || intent.type == MediaStore.Images.Media.CONTENT_TYPE)

fun isVideoType(intent: Intent) = (intent.type?.startsWith("video/") == true || intent.type == MediaStore.Video.Media.CONTENT_TYPE)
