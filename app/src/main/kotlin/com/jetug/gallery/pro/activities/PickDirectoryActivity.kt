package com.jetug.gallery.pro.activities

import android.os.Bundle
import com.jetug.commons.extensions.*
import com.jetug.gallery.pro.adapters.*
import com.jetug.gallery.pro.extensions.*
import com.jetug.gallery.pro.models.DirectoryGroup
import com.jetug.gallery.pro.models.FolderItem
import com.jetug.gallery.pro.helpers.*
import kotlinx.android.synthetic.main.activity_pick_directory.directories_grid
import kotlinx.android.synthetic.main.activity_pick_directory.directories_vertical_fastscroller
import kotlinx.android.synthetic.main.activity_pick_directory.directories_horizontal_fastscroller
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.jetug.commons.helpers.VIEW_TYPE_GRID
import com.jetug.commons.views.MyGridLayoutManager
import com.jetug.gallery.pro.R
import com.jetug.gallery.pro.dialogs.ChangeSortingDialog
import com.jetug.gallery.pro.dialogs.ChangeViewTypeDialog
import kotlinx.android.synthetic.main.activity_main.*


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

//    private fun changeViewType() {
//        ChangeViewTypeDialog(this, true) {
//            invalidateOptionsMenu()
//            setupLayoutManager()
//            directories_grid.adapter = null
//            setupAdapter(mDirs)
//        }
//    }
//
//    private fun setupLayoutManager() {
//        if (config.viewTypeFolders == VIEW_TYPE_GRID) {
//            setupGridLayoutManager()
//        } else {
//            setupListLayoutManager()
//        }
//    }
//
//    private fun setupGridLayoutManager() {
//        val layoutManager = directories_grid.layoutManager as MyGridLayoutManager
//        (directories_grid.layoutParams as RelativeLayout.LayoutParams).apply {
//            topMargin = 0
//            bottomMargin = 0
//        }
//
//        if (config.scrollHorizontally) {
//            layoutManager.orientation = RecyclerView.HORIZONTAL
//        } else {
//            layoutManager.orientation = RecyclerView.VERTICAL
//        }
//
//        layoutManager.spanCount = config.dirColumnCnt
//    }
//
//    private fun setupListLayoutManager() {
//        val layoutManager = directories_grid.layoutManager as MyGridLayoutManager
//        layoutManager.spanCount = 1
//        layoutManager.orientation = RecyclerView.VERTICAL
//
//        val smallMargin = resources.getDimension(R.dimen.small_margin).toInt()
//        (directories_grid.layoutParams as RelativeLayout.LayoutParams).apply {
//            topMargin = smallMargin
//            bottomMargin = smallMargin
//        }
//    }
}
