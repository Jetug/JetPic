package com.simplemobiletools.gallery.pro.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.adapters.*
import com.simplemobiletools.gallery.pro.dialogs.ChangeSortingDialog
import com.simplemobiletools.gallery.pro.extensions.*
import com.simplemobiletools.gallery.pro.helpers.*
import com.simplemobiletools.gallery.pro.models.DirectoryGroup
import com.simplemobiletools.gallery.pro.models.FolderItem
import kotlinx.android.synthetic.main.activity_pick_directory.*


class PickDirectoryActivity : SimpleActivity() {
    private var rvPosition = RecyclerViewPosition(null)
    //private var isGridViewType = config.viewTypeFolders == VIEW_TYPE_GRID
    val adapter get() = directories_grid.adapter as DirectoryAdapter

    private var mOpenedGroups = arrayListOf<DirectoryGroup>()
    private var shownDirectories = ArrayList<FolderItem>()
    private var currentPathPrefix = ""
    private var openedSubfolders = arrayListOf("")
    private var sourcePath: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pick_directory)

        sourcePath = intent.getStringExtra(PICK_DIR_INPUT_PATH)
        if(sourcePath != null) {
            rvPosition = RecyclerViewPosition(directories_grid)
            setupAdapter(mDirs)
        }
    }

    override fun onResume() {
        super.onResume()
        makeTranslucentBars()
        setTitle(resources.getString(R.string.select_destination))
        setTopPaddingToActionBarsHeight(directories_grid)
        setTopMarginToActionBarsHeight(directories_vertical_fastscroller)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        menuInflater.inflate(R.menu.menu_main_intent, menu)
        menu?.apply {
            findItem(R.id.change_view_type).isVisible = false
            findItem(R.id.temporarily_show_hidden).isVisible = !config.shouldShowHidden
            findItem(R.id.stop_showing_hidden).isVisible = config.temporarilyShowHidden
        }
        updateMenuItemColors(menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sort -> showSortingDialog()
            //R.id.change_view_type -> changeViewType()
            R.id.temporarily_show_hidden -> tryToggleTemporarilyShowHidden()
            R.id.stop_showing_hidden -> tryToggleTemporarilyShowHidden()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
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
            val group = mOpenedGroups.takeLast()
            rvPosition.restoreRVPosition()
            setupAdapter(mDirs)
        }
        else{
            super.onBackPressed()
        }
    }

    private fun setupAdapter(newDirs: ArrayList<FolderItem>) {
        val distinctDirs = newDirs.distinctBy { it.path.getDistinctPath() }.toMutableList() as ArrayList<FolderItem>
        val dirsToShow = if (mOpenedGroups.isEmpty())
            getDirsToShow(distinctDirs.getDirectories(), mDirs.getDirectories(), currentPathPrefix).clone() as ArrayList<FolderItem>
        else mOpenedGroups.last().innerDirs as ArrayList<FolderItem>

        val dirs = getSortedDirectories(dirsToShow)

        if (dirs.hashCode() == shownDirectories.hashCode())
            return

        shownDirectories = dirs

        val clonedDirs = dirs.clone() as ArrayList<FolderItem>
        val adapter = DirectoryAdapter(this, clonedDirs, null, directories_grid, true, itemClick = ::onItemClicked)
        val scrollHorizontally = config.scrollHorizontally //&& isGridViewType
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

    private fun onItemClicked(it: Any){
        val clickedDir = it as FolderItem
        val path = clickedDir.path
        if (clickedDir.subfoldersCount == 1 || !config.groupDirectSubfolders) {
            if (path.trimEnd('/') == sourcePath) {
                toast(R.string.source_and_destination_same)
                return
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

    private fun callback(path: String){
        val data = Intent().putExtra(PICK_DIR_OUTPUT_PATH, path)
        setResult(
            RESULT_OK,
            data
        )
    }

    private fun showSortingDialog() {
        ChangeSortingDialog(this, true, false) {
            adapter.sort()
        }
    }

    private fun tryToggleTemporarilyShowHidden() {
        if (config.temporarilyShowHidden) {
            toggleTemporarilyShowHidden(false)
            setupAdapter(publicDirs)
//            directories_grid.adapter = null
            //getDirectories()
        } else {
            handleHiddenFolderPasswordProtection {
                toggleTemporarilyShowHidden(true)
                publicDirs = mDirs.clone() as ArrayList<FolderItem>
                if (allDirs.isNotEmpty())
                    setupAdapter(allDirs)
            }
        }
    }

    private fun toggleTemporarilyShowHidden(show: Boolean) {
        config.temporarilyShowHidden = show
        invalidateOptionsMenu()
    }
}
