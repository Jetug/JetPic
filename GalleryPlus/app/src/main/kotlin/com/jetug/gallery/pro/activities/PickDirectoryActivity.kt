package com.jetug.gallery.pro

import android.app.Activity
import android.os.Bundle
import com.jetug.commons.extensions.*
import com.jetug.gallery.pro.adapters.DirectoryAdapter
import com.jetug.gallery.pro.extensions.*
import com.jetug.gallery.pro.models.DirectoryGroup
import com.jetug.gallery.pro.models.FolderItem
import kotlinx.android.synthetic.main.activity_pick_directory.*
import kotlinx.android.synthetic.main.dialog_directory_picker.view.*
import com.jetug.gallery.pro.activities.SimpleActivity
import com.jetug.gallery.pro.helpers.RecyclerViewPosition
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_pick_directory.directories_grid
import kotlinx.android.synthetic.main.activity_pick_directory.directories_vertical_fastscroller
import kotlinx.android.synthetic.main.activity_pick_directory.directories_horizontal_fastscroller
import com.jetug.gallery.pro.activities.MainActivity

import android.content.Intent
import com.jetug.commons.helpers.VIEW_TYPE_GRID


class PickDirectoryActivity : SimpleActivity() {

    private val rvPosition = RecyclerViewPosition(directories_grid)
    private var isGridViewType = config.viewTypeFolders == VIEW_TYPE_GRID

    private val recyclerAdapter get() = directories_grid.adapter as? DirectoryAdapter

    private var mOpenedGroups = arrayListOf<DirectoryGroup>()
    private var shownDirectories = ArrayList<FolderItem>()
    private var mDirs = ArrayList<FolderItem>()
    private var currentPathPrefix = ""
    private var openedSubfolders = arrayListOf("")
    private val sourcePath: String

    init{
        val arguments = intent.extras
        sourcePath = arguments!!["sourcePath"].toString()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pick_directory)



        setupAdapter(mDirs)
    }

    private fun setupAdapter(dirs: ArrayList<FolderItem>) {
        if (dirs.hashCode() == shownDirectories.hashCode())
            return
        shownDirectories = dirs

        val adapter = DirectoryAdapter(this, dirs.clone() as ArrayList<FolderItem>, null, view.directories_grid, true) {
            val clickedDir = it as FolderItem
            val path = clickedDir.path
            if (clickedDir.subfoldersCount == 1 || !config.groupDirectSubfolders) {
                if (path.trimEnd('/') == sourcePath) {
                    toast(R.string.source_and_destination_same)
                    return@DirectoryAdapter
                }
                else {
                    if (clickedDir is DirectoryGroup && clickedDir.innerDirs.isNotEmpty()) {
                        mOpenedGroups.add(clickedDir)
                        setupAdapter(clickedDir.innerDirs as ArrayList<FolderItem>)
                    } else {
                        handleLockedFolderOpening(path) { success ->
                            if (success) {
                                callback(path)
                            }
                            finish()
                        }
                    }
                }
            }
            else {
                currentPathPrefix = path
                openedSubfolders.add(path)
                setupAdapter(mDirs)
            }
        }

        val scrollHorizontally = config.scrollHorizontally && isGridViewType
        val sorting = config.directorySorting
        val dateFormat = config.dateFormat
        val timeFormat = getTimeFormat()

        directories_grid.adapter = adapter

        directories_vertical_fastscroller.isHorizontal = false
        directories_vertical_fastscroller.beGoneIf(scrollHorizontally)

        directories_horizontal_fastscroller.isHorizontal = true
        directories_horizontal_fastscroller.beVisibleIf(scrollHorizontally)

        if (scrollHorizontally) {
            directories_horizontal_fastscroller.setViews(directories_grid) {
                directories_horizontal_fastscroller.updateBubbleText(dirs[it].getBubbleText(sorting, this, dateFormat, timeFormat))
            }
        } else {
            directories_vertical_fastscroller.setViews(directories_grid) {
                directories_vertical_fastscroller.updateBubbleText(dirs[it].getBubbleText(sorting, this, dateFormat, timeFormat))
            }
        }
    }

    override fun onBackPressed() {
        if (config.groupDirectSubfolders) {
            if (currentPathPrefix.isEmpty()) {
                super.onBackPressed()
            } else {
                rvPosition.restoreRVPosition()
                openedSubfolders.removeAt(openedSubfolders.size - 1)
                currentPathPrefix = openedSubfolders.last()
                setupAdapter(mDirs)
            }
        } else if(mOpenedGroups.isNotEmpty()){
            rvPosition.restoreRVPosition()
            setupAdapter(mDirs)
        }
        else{
            super.onBackPressed()
        }
    }
}
