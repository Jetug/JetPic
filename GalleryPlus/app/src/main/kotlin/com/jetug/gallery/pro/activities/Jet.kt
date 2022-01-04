package com.jetug.gallery.pro.activities

import com.jetug.gallery.pro.models.FolderItem

const val PICK_DIR_INPUT_PATH = "sourcePath"
const val PICK_DIR_OUTPUT_PATH = "resultPath"

val mediaScrollPositions = mutableMapOf<String, Pair<Int,Int>>()
var mDirs = ArrayList<FolderItem>()
var allDirs = ArrayList<FolderItem>()
var publicDirs = ArrayList<FolderItem>()
