package com.simplemobiletools.gallery.pro.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.gallery.pro.BuildConfig
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.ui.adapters.DirectoryAdapterControls
import com.simplemobiletools.gallery.pro.data.models.FolderItem
import android.app.Activity
import android.app.SearchManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat.invalidateOptionsMenu
import androidx.core.view.MenuItemCompat
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.commons.dialogs.*
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.*
import com.simplemobiletools.commons.views.*
import com.simplemobiletools.gallery.pro.ui.activities.*
import com.simplemobiletools.gallery.pro.ui.adapters.DirectoryAdapter
import com.simplemobiletools.gallery.pro.ui.dialogs.*
import com.simplemobiletools.gallery.pro.data.extensions.*
import com.simplemobiletools.gallery.pro.data.helpers.*
import com.simplemobiletools.gallery.pro.data.interfaces.DirectoryOperationsListener
import com.simplemobiletools.gallery.pro.data.helpers.jobs.NewPhotoFetcher
import com.simplemobiletools.gallery.pro.data.models.*
import kotlinx.android.synthetic.main.fragment_directory.*
import kotlinx.android.synthetic.main.fragment_directory.view.*
import java.io.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.system.measureTimeMillis
import com.simplemobiletools.gallery.pro.data.extensions.context.*
import com.simplemobiletools.gallery.pro.ui.activities.MediaActivity
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.withContext

class DirectoryFragment : Fragment(), DirectoryOperationsListener {
    //Const
    private val PICK_MEDIA = 2
    private val PICK_WALLPAPER = 3
    private val LAST_MEDIA_CHECK_PERIOD = 3000L
    private val SIZE_TOO_MANY = 40

    ///Jet{
    var activity: SimpleActivity = SimpleActivity()
    lateinit var config: Config
    lateinit var intent: Intent

    private lateinit var rvPosition: RecyclerViewPosition
    private lateinit var binding: View

    private var mDirsToShow = ArrayList<FolderItem>()
    private var openedDirs = ArrayList<ArrayList<FolderItem>>()

    //Properties
    private val mAdapter get() = directories_grid.adapter as DirectoryAdapter?
    private val recyclerAdapter get() = binding.directories_grid.adapter as? DirectoryAdapter
    private val currentlyDisplayedDirs get() = recyclerAdapter?.dirs ?: ArrayList()
    ///}
    //Private
    private var mIsGettingDirs = false
    private var mLoadedInitialPhotos = false
    private var mIsPasswordProtectionPending = false
    private var mShouldStopFetching = false
    private var mIsSearchOpen = false
    private var mWasDefaultFolderChecked = false
    private var mLatestMediaId = 0L
    private var mLatestMediaDateId = 0L
    private var mCurrentPathPrefix = ""                 // used at "Group direct subfolders" for navigation
    private var mOpenedSubfolders = arrayListOf("")     // used at "Group direct subfolders" for navigating Up with the back button
    private var mOpenedGroups = arrayListOf<DirectoryGroup>()
    private var mDateFormat = ""
    private var mTimeFormat = ""
    private var mLastMediaHandler = Handler()
    private var mTempShowHiddenHandler = Handler()
    private var mZoomListener: MyRecyclerView.MyZoomListener? = null
    private var mSearchMenuItem: MenuItem? = null
    private var mLastMediaFetcher: MediaFetcher? = null
    private var mStoredAnimateGifs = true
    private var mStoredCropThumbnails = true
    private var mStoredScrollHorizontally = true
    private var mStoredTextColor = 0
    private var mStoredAdjustedPrimaryColor = 0
    private var mStoredStyleString = ""

    val controls = object : DirectoryAdapterControls {
        override fun recreateAdapter(dirs: ArrayList<FolderItem>) {
            setupAdapter(dirs)
        }

        override fun clearAdapter() {
            directories_grid.adapter = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = getActivity() as SimpleActivity
        config = activity.config
        intent = activity.intent
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        ///Jet{
        binding = inflater.inflate(R.layout.fragment_directory, container, false)
        rvPosition = RecyclerViewPosition(binding.directories_grid)

        setHasOptionsMenu(true)

        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressed()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(onBackPressedCallback)

        ///}
        if (savedInstanceState == null) {
            config.temporarilyShowHidden = false
            config.tempSkipDeleteConfirmation = false
            removeTempFolder()
            checkRecycleBinItems()
            startNewPhotoFetcher()
        }

        binding.directories_refresh_layout.setOnRefreshListener { getDirectories() }
        storeStateVariables()
        //checkWhatsNewDialog()

        mIsPasswordProtectionPending = config.isAppPasswordProtectionOn
        setupLatestMediaId()

        if (!config.wereFavoritesPinned) {
            config.addPinnedFolders(hashSetOf(FAVORITES))
            config.wereFavoritesPinned = true
        }

        if (!config.wasRecycleBinPinned) {
            config.addPinnedFolders(hashSetOf(RECYCLE_BIN))
            config.wasRecycleBinPinned = true
            config.saveFolderGrouping(SHOW_ALL, GROUP_BY_DATE_TAKEN_DAILY or GROUP_DESCENDING)
        }

        if (!config.wasSVGShowingHandled) {
            config.wasSVGShowingHandled = true
            if (config.filterMedia and TYPE_SVGS == 0) {
                config.filterMedia += TYPE_SVGS
            }
        }

        if (!config.wasSortingByNumericValueAdded) {
            config.wasSortingByNumericValueAdded = true
            config.sorting = config.sorting or SORT_USE_NUMERIC_VALUE
        }

//        updateWidgets()
//        registerFileUpdateListener()

        binding.directories_switch_searching.setOnClickListener {
            launchSearchActivity()
        }

        return binding
    }

    override fun onStart() {
        super.onStart()
        mTempShowHiddenHandler.removeCallbacksAndMessages(null)
    }

    override fun onResume() {
        super.onResume()
        ///Jet{
        activity.makeTranslucentBars()
        activity.setTopPaddingToActionBarsHeight(binding.directories_grid)
        activity.setTopMarginToActionBarsHeight(binding.directories_vertical_fastscroller)
        activity.setTopMarginToActionBarsHeight(binding.directories_switch_searching)
        //setTopMarginToActionBarsHeight(manager)
        activity.updateActionBarTitle(resources.getString(R.string.gallery_short))

        binding.directories_switch_searching.height = activity.topBarsHeight
        ///}
        config.isThirdPartyIntent = false
        mDateFormat = config.dateFormat
        mTimeFormat = activity.getTimeFormat()

        if (mStoredAnimateGifs != config.animateGifs) {
            recyclerAdapter?.updateAnimateGifs(config.animateGifs)
        }

        if (mStoredCropThumbnails != config.cropThumbnails) {
            recyclerAdapter?.updateCropThumbnails(config.cropThumbnails)
        }

        if (mStoredScrollHorizontally != config.scrollHorizontally) {
            mLoadedInitialPhotos = false
            binding.directories_grid.adapter = null
            getDirectories()
        }

        if (mStoredTextColor != config.textColor) {
            recyclerAdapter?.updateTextColor(config.textColor)
        }

        val adjustedPrimaryColor = activity.getAdjustedPrimaryColor()
        if (mStoredAdjustedPrimaryColor != adjustedPrimaryColor) {
            recyclerAdapter?.updatePrimaryColor(config.primaryColor)
            binding.directories_vertical_fastscroller.updatePrimaryColor(adjustedPrimaryColor)
            binding.directories_horizontal_fastscroller.updatePrimaryColor(adjustedPrimaryColor)
        }

        val styleString = "${config.folderStyle}${config.showFolderMediaCount}${config.limitFolderTitle}"
        if (mStoredStyleString != styleString) {
            setupAdapter(mDirs, forceRecreate = true)
        }

        binding.directories_horizontal_fastscroller.updateBubbleColors()
        binding.directories_vertical_fastscroller.updateBubbleColors()
        binding.directories_refresh_layout.isEnabled = config.enablePullToRefresh
        binding.directories_empty_placeholder.setTextColor(config.textColor)
        binding.directories_empty_placeholder_2.setTextColor(adjustedPrimaryColor)
        binding.directories_switch_searching.setTextColor(adjustedPrimaryColor)
        binding.directories_switch_searching.underlineText()

        if (!mIsSearchOpen) {
            invalidateOptionsMenu(activity)
            if (mIsPasswordProtectionPending && !mWasProtectionHandled) {
                activity.handleAppPasswordProtection {
                    mWasProtectionHandled = it
                    if (it) {
                        mIsPasswordProtectionPending = false
                        tryLoadGallery()
                    } else {
                        activity.finish()
                    }
                }
            } else {
                tryLoadGallery()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        binding.directories_refresh_layout.isRefreshing = false
        mIsGettingDirs = false
        storeStateVariables()
        mLastMediaHandler.removeCallbacksAndMessages(null)
    }

    override fun onStop() {
        super.onStop()

        if (config.temporarilyShowHidden || config.tempSkipDeleteConfirmation) {
            mTempShowHiddenHandler.postDelayed({
                config.temporarilyShowHidden = false
                config.tempSkipDeleteConfirmation = false
            }, SHOW_TEMP_HIDDEN_DURATION)
        } else {
            mTempShowHiddenHandler.removeCallbacksAndMessages(null)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!activity.isChangingConfigurations) {
            mTempShowHiddenHandler.removeCallbacksAndMessages(null)
            removeTempFolder()

            if (!config.showAll) {
                mLastMediaFetcher?.shouldStop = true
            }
        }
    }

    fun onBackPressed() {
        if (config.groupDirectSubfolders) {
            if (mCurrentPathPrefix.isEmpty()) {
                activity.finish()
            } else {
                mOpenedSubfolders.removeAt(mOpenedSubfolders.size - 1)
                mCurrentPathPrefix = mOpenedSubfolders.last()
                rvPosition.restoreRVPosition()
                setupAdapter(mDirs)
            }
        } else if (mOpenedGroups.isNotEmpty()) {
            mOpenedGroups.takeLast()
            if (mDirs.size == 0) {
                getDirectories()
            }
            rvPosition.restoreRVPosition()
            updateDirs(activity.getSortedDirectories(mDirsToShow))
        } else {
            activity.finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        if (mIsThirdPartyIntent) {
            inflater.inflate(R.menu.menu_main_intent, menu)
        } else {
            inflater.inflate(R.menu.menu_main, menu)
            val useBin = config.useRecycleBin
            menu.apply {
                findItem(R.id.increase_column_count).isVisible = config.viewTypeFolders == VIEW_TYPE_GRID && config.dirColumnCnt < MAX_COLUMN_COUNT
                findItem(R.id.reduce_column_count).isVisible = config.viewTypeFolders == VIEW_TYPE_GRID && config.dirColumnCnt > 1
                findItem(R.id.hide_the_recycle_bin).isVisible = useBin && config.showRecycleBinAtFolders
                findItem(R.id.show_the_recycle_bin).isVisible = useBin && !config.showRecycleBinAtFolders
                findItem(R.id.set_as_default_folder).isVisible = !config.defaultFolder.isEmpty()
                setupSearch(this)
            }
        }

        menu.apply {
            findItem(R.id.temporarily_show_hidden).isVisible = !config.shouldShowHidden
            findItem(R.id.stop_showing_hidden).isVisible = config.temporarilyShowHidden

            findItem(R.id.temporarily_show_excluded).isVisible = !findItem(R.id.temporarily_show_hidden).isVisible && !config.temporarilyShowExcluded
            findItem(R.id.stop_showing_excluded).isVisible = !findItem(R.id.temporarily_show_hidden).isVisible && config.temporarilyShowExcluded
        }

        activity.updateMenuItemColors(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.cab_change_order -> recyclerAdapter?.changeOrder()
            R.id.sort -> showSortingDialog()
            R.id.filter -> showFilterMediaDialog()
            R.id.open_camera -> activity.launchCamera()
            R.id.change_view_type -> changeViewType()
            R.id.temporarily_show_hidden -> tryToggleTemporarilyShowHidden()
            R.id.stop_showing_hidden -> tryToggleTemporarilyShowHidden()
            R.id.stop_showing_hidden -> tryToggleTemporarilyShowHidden()
            R.id.temporarily_show_excluded -> tryToggleTemporarilyShowExcluded()
            R.id.stop_showing_excluded -> tryToggleTemporarilyShowExcluded()
            R.id.create_new_folder -> createNewFolder()
            R.id.show_the_recycle_bin -> toggleRecycleBin(true)
            R.id.hide_the_recycle_bin -> toggleRecycleBin(false)
            R.id.increase_column_count -> increaseColumnCount()
            R.id.reduce_column_count -> reduceColumnCount()
            R.id.set_as_default_folder -> setAsDefaultFolder()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
    
    private fun storeStateVariables() {
        config.apply {
            mStoredAnimateGifs = animateGifs
            mStoredCropThumbnails = cropThumbnails
            mStoredScrollHorizontally = scrollHorizontally
            mStoredTextColor = textColor
            mStoredStyleString = "$folderStyle$showFolderMediaCount$limitFolderTitle"
        }
        mStoredAdjustedPrimaryColor = activity.getAdjustedPrimaryColor()
    }

    private fun setupSearch(menu: Menu) {
        val searchManager = activity.getSystemService(Context.SEARCH_SERVICE) as SearchManager
        mSearchMenuItem = menu.findItem(R.id.search)
        (mSearchMenuItem?.actionView as? SearchView)?.apply {
            setSearchableInfo(searchManager.getSearchableInfo(activity.componentName))
            isSubmitButtonEnabled = false
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String) = false

                override fun onQueryTextChange(newText: String): Boolean {
                    if (mIsSearchOpen) {
                        setupAdapter(mDirs, newText)
                    }
                    return true
                }
            })
        }

        MenuItemCompat.setOnActionExpandListener(mSearchMenuItem, object : MenuItemCompat.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                binding.directories_switch_searching.beVisible()
                mIsSearchOpen = true
                binding.directories_refresh_layout.isEnabled = false
                return true
            }

            // this triggers on device rotation too, avoid doing anything
            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                if (mIsSearchOpen) {
                    binding.directories_switch_searching.beGone()
                    mIsSearchOpen = false
                    binding.directories_refresh_layout.isEnabled = config.enablePullToRefresh
                    setupAdapter(mDirs, "")
                }
                return true
            }
        })
    }

    private fun startNewPhotoFetcher() {
        if (isNougatPlus()) {
            val photoFetcher = NewPhotoFetcher()
            if (!photoFetcher.isScheduled(activity.applicationContext)) {
                photoFetcher.scheduleJob(activity.applicationContext)
            }
        }
    }

    private fun tryLoadGallery() {
        fun load (it: Boolean){
            launchDefault {
                if (it) {
                    if (!mWasDefaultFolderChecked) {
                        openDefaultFolder()
                        mWasDefaultFolderChecked = true
                    }

                    checkOTGPath()
                    withContext(Main) { setupLayoutManager() }
                    getDirectories()
                } else {
                    activity.toast(R.string.no_storage_permissions)
                    activity.finish()
                }
            }
        }

        activity.handlePermission(PERMISSION_WRITE_STORAGE) {
            load(it)
        }
    }

    private fun getDirectories() {
        val mTime = measureTimeMillis {
            if (mIsGettingDirs)
                return

            mShouldStopFetching = true
            mIsGettingDirs = true
            val getImagesOnly = mIsPickImageIntent || mIsGetImageContentIntent
            val getVideosOnly = mIsPickVideoIntent || mIsGetVideoContentIntent

            launchDefault {
                val time2 = measureTimeMillis {
                    activity.getCachedDirectories(getVideosOnly, getImagesOnly, true) {
                        val time3 = measureTimeMillis {
                            allDirs = activity.addTempFolderIfNeeded(it as ArrayList<FolderItem>)
                        }
                        Log.i(JET,"addTempFolderIfNeeded()+++ $time3 ms")
                    }
                }
                Log.i(JET,"getCachedDirectories() SHOW HIDDEN $time2 ms")
            }

            launchDefault {
                val time = measureTimeMillis {
                    activity.getCachedDirectories(getVideosOnly, getImagesOnly) {
                        gotDirectories(activity.addTempFolderIfNeeded(it as ArrayList<FolderItem>))
                    }
                }
                Log.i(JET, "getCachedDirectories() -- $time ms")
            }
        }
        Log.e(JET,"getDirectories() $mTime ms")
    }

    private fun gotDirectories(newDirs: ArrayList<FolderItem>) {
        val mTime = measureTimeMillis {
            mIsGettingDirs = false
            mShouldStopFetching = false

            // if hidden item showing is disabled but all Favorite items are hidden, hide the Favorites folder
            if (!config.shouldShowHidden) {
                val favoritesFolder = newDirs.firstOrNull { it.areFavorites() }
                if (favoritesFolder != null && favoritesFolder.tmb.getFilenameFromPath().startsWith('.')) {
                    newDirs.remove(favoritesFolder)
                }
            }

            val dirs = activity.getSortedDirectories(newDirs).getDirectories()
            //if (config.groupDirectSubfolders) {
            mDirs = dirs.clone() as ArrayList<FolderItem>
            //}

            var isPlaceholderVisible = dirs.isEmpty()

            activity.runOnUiThread{
                checkPlaceholderVisibility(dirs as ArrayList<FolderItem>)

                val allowHorizontalScroll = config.scrollHorizontally && config.viewTypeFolders == VIEW_TYPE_GRID
                binding.directories_vertical_fastscroller.beVisibleIf(binding.directories_grid.isVisible() && !allowHorizontalScroll)
                binding.directories_horizontal_fastscroller.beVisibleIf(binding.directories_grid.isVisible() && allowHorizontalScroll)
                //Jet
                setupAdapter(dirs.clone() as ArrayList<FolderItem>)
            }

            // cached folders have been loaded, recheck folders one by one starting with the first displayed
            mLastMediaFetcher?.shouldStop = true
            mLastMediaFetcher = MediaFetcher(activity.applicationContext)
            val getImagesOnly = mIsPickImageIntent || mIsGetImageContentIntent
            val getVideosOnly = mIsPickVideoIntent || mIsGetVideoContentIntent
            val hiddenString = activity.getString(R.string.hidden)
            val albumCovers = config.parseAlbumCovers()
            val includedFolders = config.includedFolders
            val noMediaFolders = activity.getNoMediaFoldersSync()
            val tempFolderPath = config.tempFolderPath
            val getProperFileSize = config.directorySorting and SORT_BY_SIZE != 0
            val favoritePaths = activity.getFavoritePaths()
            val dirPathsToRemove = ArrayList<String>()
            val lastModifieds = mLastMediaFetcher!!.getLastModifieds()
            val dateTakens = mLastMediaFetcher!!.getDateTakens()

            try {
                for (directory in dirs) {
                    if (mShouldStopFetching || activity.isDestroyed || activity.isFinishing) {
                        return
                    }

                    val sorting = activity.getSorting(directory.path)
                    val grouping = config.getFolderGrouping(directory.path)
                    val getProperDateTaken = config.directorySorting and SORT_BY_DATE_TAKEN != 0 ||
                        sorting and SORT_BY_DATE_TAKEN != 0 ||
                        grouping and GROUP_BY_DATE_TAKEN_DAILY != 0 ||
                        grouping and GROUP_BY_DATE_TAKEN_MONTHLY != 0

                    val getProperLastModified = config.directorySorting and SORT_BY_DATE_MODIFIED != 0 ||
                        sorting and SORT_BY_DATE_MODIFIED != 0 ||
                        grouping and GROUP_BY_LAST_MODIFIED_DAILY != 0 ||
                        grouping and GROUP_BY_LAST_MODIFIED_MONTHLY != 0

                    val curMedia = mLastMediaFetcher!!.getFilesFrom(directory.path, getImagesOnly, getVideosOnly, getProperDateTaken, getProperLastModified,
                        getProperFileSize, favoritePaths, false, lastModifieds, dateTakens)

                    val newDir = if (curMedia.isEmpty()) {
                        if (directory.path != tempFolderPath) {
                            dirPathsToRemove.add(directory.path)
                        }
                        directory
                    } else {
                        activity.createDirectoryFromMedia(directory.path, curMedia, albumCovers, hiddenString, includedFolders, getProperFileSize, noMediaFolders)
                    }

                    // we are looping through the already displayed folders looking for changes, do not do anything if nothing changed

                    if (directory.copy(subfoldersCount = 0, subfoldersMediaCount = 0) == newDir) {
                        continue
                    }

                    directory.apply {
                        tmb = newDir.tmb
                        name = newDir.name
                        mediaCnt = newDir.mediaCnt
                        modified = newDir.modified
                        taken = newDir.taken
                        this@apply.size = newDir.size
                        types = newDir.types
                        //sortValue = ""//getDirectorySortingValue(curMedia, path, name, size)
                    }

                    //setupAdapter(dirs as ArrayList<FolderItem>)

                    // update directories and media files in the local db, delete invalid items. Intentionally creating a new thread
                    activity.updateDirectory(directory)
                    if (!directory.isRecycleBin()) {
                        Thread {
                            try {
                                activity.mediaDB.insertAll(curMedia)
                            } catch (ignored: Exception) {}
                        }.start()
                    }

                    if (!directory.isRecycleBin()) {
                        activity.getCachedMedia(directory.path, getVideosOnly, getImagesOnly) {
                            val mediaToDelete = ArrayList<Medium>()
                            it.forEach {
                                if (!curMedia.contains(it)) {
                                    val medium = it as? Medium
                                    val path = medium?.path
                                    if (path != null) {
                                        mediaToDelete.add(medium)
                                    }
                                }
                            }
                            activity.mediaDB.deleteMedia(*mediaToDelete.toTypedArray())
                        }
                    }
                }

                if (dirPathsToRemove.isNotEmpty()) {
                    val dirsToRemove = dirs.filter { dirPathsToRemove.contains(it.path) }
                    dirsToRemove.forEach {
                        activity.directoryDao.deleteDirPath(it.path)
                    }
                    dirs.removeAll(dirsToRemove)
                    setupAdapter(dirs as ArrayList<FolderItem>)
                }
            } catch (ignored: Exception) { }

            val foldersToScan = mLastMediaFetcher!!.getFoldersToScan()
            foldersToScan.add(FAVORITES)
            if (config.showRecycleBinAtFolders)
                foldersToScan.add(RECYCLE_BIN)
            else
                foldersToScan.remove(RECYCLE_BIN)

            dirs.forEach {
                foldersToScan.remove(it.path)
            }  
            // check the remaining folders which were not cached at all yet
            val newDirs = arrayListOf<Directory>()
            for (folder in foldersToScan) {
                if (mShouldStopFetching || activity.isDestroyed || activity.isFinishing) {
                    return
                }

                val sorting = activity.getSorting(folder)
                val grouping = config.getFolderGrouping(folder)
                val getProperDateTaken = config.directorySorting and SORT_BY_DATE_TAKEN != 0 ||
                    sorting and SORT_BY_DATE_TAKEN != 0 ||
                    grouping and GROUP_BY_DATE_TAKEN_DAILY != 0 ||
                    grouping and GROUP_BY_DATE_TAKEN_MONTHLY != 0

                val getProperLastModified = config.directorySorting and SORT_BY_DATE_MODIFIED != 0 ||
                    sorting and SORT_BY_DATE_MODIFIED != 0 ||
                    grouping and GROUP_BY_LAST_MODIFIED_DAILY != 0 ||
                    grouping and GROUP_BY_LAST_MODIFIED_MONTHLY != 0

                val newMedia = mLastMediaFetcher!!.getFilesFrom(folder, getImagesOnly, getVideosOnly, getProperDateTaken, getProperLastModified,
                    getProperFileSize, favoritePaths, false, lastModifieds, dateTakens)

                if (newMedia.isEmpty()) {
                    continue
                }

                if (isPlaceholderVisible) {
                    isPlaceholderVisible = false
                    //activity.runOnUiThread
                    launchMain{
                        binding.directories_empty_placeholder.beGone()
                        binding.directories_empty_placeholder_2.beGone()
                        binding.directories_grid.beVisible()
                    }
                }

                val newDir = activity.createDirectoryFromMedia(folder, newMedia, albumCovers, hiddenString, includedFolders, getProperFileSize, noMediaFolders)
                newDirs.add(newDir)
                //Jet
                //            dirs.add(newDir)
                //            setupAdapter(dirs as ArrayList<FolderItem>)

                // make sure to create a new thread for these operations, dont just use the common bg thread
                launchIO{
                    try {
                        activity.directoryDao.insert(newDir)
                        if (folder != RECYCLE_BIN) {
                            activity.mediaDB.insertAll(newMedia)
                        }
                    } catch (e: Exception) {
                        Log.e(JET, e.message, e)
                    }
                }
            }

            if(newDirs.isNotEmpty()){
                dirs.addAll(newDirs)
                //            val adapter = recyclerAdapter
                //            adapter?.add(newDirs as ArrayList<FolderItem>)
                setupAdapter(dirs as ArrayList<FolderItem>)
            }

            mLoadedInitialPhotos = true
            checkLastMediaChanged()

            //activity.runOnUiThread
            launchMain{
                binding.directories_refresh_layout.isRefreshing = false
                checkPlaceholderVisibility(dirs as ArrayList<FolderItem>)
            }

            checkInvalidDirectories(dirs as ArrayList<FolderItem>)

            if (mDirs.size > SIZE_TOO_MANY) {
                activity.excludeSpamFolders()
            }

            val excludedFolders = config.excludedFolders
            val everShownFolders = config.everShownFolders.toMutableSet() as HashSet<String>

            // do not add excluded folders and their subfolders at everShownFolders
            dirs.filter { dir ->
                if (excludedFolders.any { dir.path.startsWith(it) }) {
                    return@filter false
                }
                return@filter true
            }.mapTo(everShownFolders) { it.path }

            try {
                // scan the internal storage from time to time for new folders
                if (config.appRunCount == 1 || config.appRunCount % 30 == 0) {
                    everShownFolders.addAll(getFoldersWithMedia(config.internalStoragePath))
                }

                // catch some extreme exceptions like too many everShownFolders for storing, shouldnt really happen
                config.everShownFolders = everShownFolders
            } catch (e: Exception) {
                config.everShownFolders = HashSet()
            }

            mDirs = dirs.clone() as ArrayList<FolderItem>
        }
        Log.e(JET,"gotDirectories() $mTime ms")
    }

    fun setupAdapter(dirs: ArrayList<FolderItem>, textToSearch: String = "", forceRecreate: Boolean = false) {
        launchDefault{
            val setupTime = measureTimeMillis {
                val currAdapter = recyclerAdapter
                val distinctDirs = dirs.distinctBy { it.path.getDistinctPath() }.toMutableList() as ArrayList<FolderItem>

                var dirsToShow: ArrayList<FolderItem>
                val getDirsToShowTime = measureTimeMillis {
                    dirsToShow = if (mOpenedGroups.isEmpty())
                        activity.getDirsToShow(distinctDirs.getDirectories(), mDirs.getDirectories(), mCurrentPathPrefix).clone() as ArrayList<FolderItem>
                    else
                        mOpenedGroups.last().innerDirs as ArrayList<FolderItem>
                }
                Log.e(JET,"DirFrag Setup getDirsToShow(): $getDirsToShowTime ms")

                if (mOpenedGroups.isEmpty())
                    mDirsToShow = dirsToShow

                if (currAdapter == null || forceRecreate) {
                    initZoomListener()
                    withContext(Main){
                        initAdapter(dirsToShow)
                    }
                    measureRecyclerViewContent(dirsToShow)
                } else {
                    launchMain {
                        if (textToSearch.isNotEmpty()) {
                            dirsToShow = dirsToShow.filter { it.name.contains(textToSearch, true) }.sortedBy { !it.name.startsWith(textToSearch, true) }
                                .toMutableList() as ArrayList
                        }
                        checkPlaceholderVisibility(dirsToShow)
                        currAdapter.updateDirs(dirsToShow)
                        measureRecyclerViewContent(dirsToShow)
                    }
                }

                // recyclerview sometimes becomes empty at init/update, triggering an invisible refresh like this seems to work fine
                binding.directories_grid.postDelayed({
                    binding.directories_grid.scrollBy(0, 0)
                }, 500)
            }
            Log.e(JET,"DirFragment Setup: $setupTime ms")
        }
    }

    private fun initAdapter(dirsToShow:ArrayList<FolderItem>) {
        val time = measureTimeMillis {
            val fastScroller = if (config.scrollHorizontally) binding.directories_horizontal_fastscroller else binding.directories_vertical_fastscroller
            val adapter = DirectoryAdapter(activity, dirsToShow, this, binding.directories_grid,isPickIntent(intent) || isGetAnyContentIntent(intent),
                binding.directories_refresh_layout, fastScroller, controls){
                onItemClicked(it)
            }.apply {
                setupZoomListener(mZoomListener)
            }
            //activity.runOnUiThread
            launchMain{
                binding.directories_grid.adapter = adapter
                setupScrollDirection()

                if (config.viewTypeFolders == VIEW_TYPE_LIST) {
                    binding.directories_grid.scheduleLayoutAnimation()
                }
            }
        }
        Log.e(JET,"DirFragment initAdapter(): $time ms")
    }

    private fun onItemClicked(it: Any){
        val clickedDir = it as FolderItem
        val path = clickedDir.path
        if (clickedDir.subfoldersCount == 1 || !config.groupDirectSubfolders) {
            if(clickedDir is DirectoryGroup && clickedDir.innerDirs.isNotEmpty()){
                rvPosition.saveRVPosition()
                mOpenedGroups.add(clickedDir)
                setupAdapter(clickedDir.innerDirs as ArrayList<FolderItem>, "")
            }
            else if (path != config.tempFolderPath) {
                openFolder(path)
            }
        } else {
            mCurrentPathPrefix = path
            mOpenedSubfolders.add(path)
            rvPosition.saveRVPosition()
            setupAdapter(mDirs, "")
        }
    }

    private fun updateDirs(dirs: ArrayList<FolderItem>){
        val currAdapter = recyclerAdapter
        if (currAdapter != null) {
            currAdapter.updateDirs(dirs)
        }
    }

    private fun removeTempFolder() {
        if (config.tempFolderPath.isNotEmpty()) {
            val newFolder = File(config.tempFolderPath)
            if (activity.getDoesFilePathExist(newFolder.absolutePath) && newFolder.isDirectory) {
                if (newFolder.list()?.isEmpty() == true && newFolder.getProperSize(true) == 0L && newFolder.getFileCount(true) == 0) {
                    activity.toast(String.format(activity.getString(R.string.deleting_folder), config.tempFolderPath), Toast.LENGTH_LONG)
                    activity.tryDeleteFileDirItem(newFolder.toFileDirItem(activity.applicationContext), true, true)
                }
            }
            config.tempFolderPath = ""
        }
    }

    private fun checkOTGPath() {
        ensureBackgroundThread {
            if (!config.wasOTGHandled && activity.hasPermission(PERMISSION_WRITE_STORAGE) && activity.hasOTGConnected() && config.OTGPath.isEmpty()) {
                activity.getStorageDirectories().firstOrNull { it.trimEnd('/') != activity.internalStoragePath && it.trimEnd('/') != activity.sdCardPath }?.apply {
                    config.wasOTGHandled = true
                    val otgPath = trimEnd('/')
                    config.OTGPath = otgPath
                    config.addIncludedFolder(otgPath)
                }
            }
        }
    }

    //Jet
    private fun showSortingDialog() {
        ChangeSortingDialog(activity, true, false) {
            mAdapter?.sort()
        }
    }

    private fun showFilterMediaDialog() {
        FilterMediaDialog(activity) {
            mShouldStopFetching = true
            binding.directories_refresh_layout.isRefreshing = true
            //binding.directories_grid.adapter = null
            getDirectories()
        }
    }

    private fun showAllMedia() {
        config.showAll = true
        Intent(activity, MediaActivity::class.java).apply {
            putExtra(DIRECTORY, "")

            if (mIsThirdPartyIntent) {
                handleMediaIntent(this)
            } else {
                startActivity(this)
            }
        }
    }

    private fun changeViewType() {
        ChangeViewTypeDialog(activity, true) {
            invalidateOptionsMenu(activity)
            setupLayoutManager()
            binding.directories_grid.adapter = null
            setupAdapter(mDirs)
        }
    }

    private fun tryToggleTemporarilyShowExcluded() {
        if (config.temporarilyShowExcluded) {
            toggleTemporarilyShowExcluded(false)
        } else {
            activity.handleExcludedFolderPasswordProtection {
                toggleTemporarilyShowExcluded(true)
            }
        }
    }

    private fun toggleTemporarilyShowExcluded(show: Boolean) {
        mLoadedInitialPhotos = false
        config.temporarilyShowExcluded = show
        directories_grid.adapter = null
        getDirectories()
        //refreshMenuItems()
    }

    private fun tryToggleTemporarilyShowHidden() {
        if (config.temporarilyShowHidden) {
            setupAdapter(publicDirs)
            toggleTemporarilyShowHidden(false)
        } else {
            activity.handleHiddenFolderPasswordProtection {
                publicDirs = mDirs.clone() as ArrayList<FolderItem>
                if (allDirs.isNotEmpty())
                    setupAdapter(allDirs)
                toggleTemporarilyShowHidden(true)
            }
        }
    }

    private fun toggleTemporarilyShowHidden(show: Boolean) {
        mLoadedInitialPhotos = false
        config.temporarilyShowHidden = show
        //binding.directories_grid.adapter = null

        val shouldShowHidden = config.shouldShowHidden
        val temporarilyShowHidden = config.temporarilyShowHidden

        Log.i("Hidden", "show: $show")
        Log.i("Hidden", "shouldShowHidden $shouldShowHidden")
        Log.i("Hidden", "temporarilyShowHidden $temporarilyShowHidden")

        getDirectories()
        invalidateOptionsMenu(activity)
    }

    override fun deleteFolders(folders: ArrayList<File>) {
        val fileDirItems = folders.asSequence().filter { it.isDirectory }.map { FileDirItem(it.absolutePath, it.name, true) }.toMutableList() as ArrayList<FileDirItem>
        when {
            fileDirItems.isEmpty() -> return
            fileDirItems.size == 1 -> {
                try {
                    activity.toast(String.format(activity.getString(R.string.deleting_folder), fileDirItems.first().name))
                } catch (e: Exception) {
                    activity.showErrorToast(e)
                }
            }
            else -> {
                val baseString = if (config.useRecycleBin) R.plurals.moving_items_into_bin else R.plurals.delete_items
                val deletingItems = resources.getQuantityString(baseString, fileDirItems.size, fileDirItems.size)
                activity.toast(deletingItems)
            }
        }

        val itemsToDelete = ArrayList<FileDirItem>()
        val filter = config.filterMedia
        val showHidden = config.shouldShowHidden
        fileDirItems.filter { it.isDirectory }.forEach {
            val files = File(it.path).listFiles()
            files?.filter {
                it.absolutePath.isMediaFile() && (showHidden || !it.name.startsWith('.')) &&
                    ((it.isImageFast() && filter and TYPE_IMAGES != 0) ||
                        (it.isVideoFast() && filter and TYPE_VIDEOS != 0) ||
                        (it.isGif() && filter and TYPE_GIFS != 0) ||
                        (it.isRawFast() && filter and TYPE_RAWS != 0) ||
                        (it.isSvg() && filter and TYPE_SVGS != 0))
            }?.mapTo(itemsToDelete) { it.toFileDirItem(activity.applicationContext) }
        }

        if (config.useRecycleBin) {
            val pathsToDelete = ArrayList<String>()
            itemsToDelete.mapTo(pathsToDelete) { it.path }

            activity.movePathsInRecycleBin(pathsToDelete) {
                if (it) {
                    deleteFilteredFileDirItems(itemsToDelete, folders)
                } else {
                    activity.toast(R.string.unknown_error_occurred)
                }
            }
        } else {
            deleteFilteredFileDirItems(itemsToDelete, folders)
        }
    }

    private fun deleteFilteredFileDirItems(fileDirItems: ArrayList<FileDirItem>, folders: ArrayList<File>) {
        val OTGPath = config.OTGPath
        activity.deleteFiles(fileDirItems) {
            //activity.runOnUiThread
            launchMain{
                refreshItems()
            }

            ensureBackgroundThread {
                folders.filter { !activity.getDoesFilePathExist(it.absolutePath, OTGPath) }.forEach {
                    activity.directoryDao.deleteDirPath(it.absolutePath)
                }

                if (config.deleteEmptyFolders) {
                    folders.filter { !it.absolutePath.isDownloadsFolder() && it.isDirectory && it.toFileDirItem(activity).getProperFileCount(activity, true) == 0 }.forEach {
                        activity.tryDeleteFileDirItem(it.toFileDirItem(activity), true, true)
                    }
                }
            }
        }
    }

    //Jet
    fun launchSearchActivity() {
        Intent(activity, SearchActivity::class.java).apply {
            startActivity(this)
        }
    }

    private fun setupLayoutManager() {
        if (config.viewTypeFolders == VIEW_TYPE_GRID) {
            setupGridLayoutManager()
        } else {
            setupListLayoutManager()
        }
    }

    private fun setupGridLayoutManager() {
        val layoutManager = binding.directories_grid.layoutManager as MyGridLayoutManager
        (binding.directories_grid.layoutParams as RelativeLayout.LayoutParams).apply {
            topMargin = 0
            bottomMargin = 0
        }

        if (config.scrollHorizontally) {
            layoutManager.orientation = RecyclerView.HORIZONTAL
            binding.directories_refresh_layout.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        } else {
            layoutManager.orientation = RecyclerView.VERTICAL
            binding.directories_refresh_layout.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        layoutManager.spanCount = config.dirColumnCnt
    }

    private fun setupListLayoutManager() {
        val layoutManager = binding.directories_grid.layoutManager as MyGridLayoutManager
        layoutManager.spanCount = 1
        layoutManager.orientation = RecyclerView.VERTICAL
        binding.directories_refresh_layout.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val smallMargin = resources.getDimension(R.dimen.small_margin).toInt()
        (binding.directories_grid.layoutParams as RelativeLayout.LayoutParams).apply {
            topMargin = smallMargin
            bottomMargin = smallMargin
        }

        mZoomListener = null
    }

    private fun measureRecyclerViewContent(directories: ArrayList<FolderItem>) {
        binding.directories_grid.onGlobalLayout {
            if (config.scrollHorizontally) {
                calculateContentWidth(directories)
            } else {
                calculateContentHeight(directories)
            }
        }
    }

    private fun calculateContentWidth(directories: ArrayList<FolderItem>) {
        val layoutManager = binding.directories_grid.layoutManager as MyGridLayoutManager

        val fullWidth = if (config.folderStyle == FOLDER_STYLE_SQUARE) {
            val thumbnailWidth = layoutManager.getChildAt(0)?.width ?: 0
            ((directories.size - 1) / layoutManager.spanCount + 1) * thumbnailWidth
        } else {
            val thumbnailWidth = (layoutManager.getChildAt(0)?.width ?: 0) + resources.getDimension(R.dimen.medium_margin).toInt() * 2
            val columnCount = (directories.size - 1) / layoutManager.spanCount + 1
            columnCount * thumbnailWidth
        }

       binding.directories_horizontal_fastscroller.setContentWidth(fullWidth)
       binding.directories_horizontal_fastscroller.setScrollToX(directories_grid.computeHorizontalScrollOffset())
    }

    private fun calculateContentHeight(directories: ArrayList<FolderItem>) {
        val layoutManager = binding.directories_grid.layoutManager as MyGridLayoutManager

        val fullHeight = if (config.folderStyle == FOLDER_STYLE_SQUARE) {
            val thumbnailHeight = layoutManager.getChildAt(0)?.height ?: 0
            ((directories.size - 1) / layoutManager.spanCount + 1) * thumbnailHeight
        } else {
            var thumbnailHeight = (layoutManager.getChildAt(0)?.height ?: 0)
            if (config.viewTypeFolders == VIEW_TYPE_GRID) {
                thumbnailHeight += resources.getDimension(R.dimen.medium_margin).toInt() * 2
            }

            val rowCount = (directories.size - 1) / layoutManager.spanCount + 1
            rowCount * thumbnailHeight
        }

        binding.directories_vertical_fastscroller.setContentHeight(fullHeight)
        binding.directories_vertical_fastscroller.setScrollToY(directories_grid.computeVerticalScrollOffset())
    }
    //Jet zoom
    private fun initZoomListener() {
        if (config.viewTypeFolders == VIEW_TYPE_GRID) {
            val layoutManager = binding.directories_grid.layoutManager as MyGridLayoutManager
            mZoomListener = object : MyRecyclerView.MyZoomListener {
                override fun zoomIn() {
                    if (layoutManager.spanCount > 1) {
                        reduceColumnCount()
                        recyclerAdapter?.finishActMode()
                    }
                }

                override fun zoomOut() {
                    if (layoutManager.spanCount < MAX_COLUMN_COUNT) {
                        increaseColumnCount()
                        recyclerAdapter?.finishActMode()
                    }
                }
            }
        } else {
            mZoomListener = null
        }
    }

    private fun toggleRecycleBin(show: Boolean) {
        config.showRecycleBinAtFolders = show
        invalidateOptionsMenu(activity)
        ensureBackgroundThread {
            var dirs = currentlyDisplayedDirs
            if (!show) {
                dirs = dirs.filter { it.path != RECYCLE_BIN } as ArrayList<FolderItem>
            }
            gotDirectories(dirs)
        }
    }

    private fun createNewFolder() {
        FilePickerDialog(activity, activity.internalStoragePath, false, config.shouldShowHidden, false, true) {
            CreateNewFolderDialog(activity, it) {
                config.tempFolderPath = it
                ensureBackgroundThread {
                    gotDirectories(activity.addTempFolderIfNeeded(currentlyDisplayedDirs))
                }
            }
        }
    }

    private fun increaseColumnCount() {
        config.dirColumnCnt = ++(binding.directories_grid.layoutManager as MyGridLayoutManager).spanCount
        columnCountChanged()
    }

    private fun reduceColumnCount() {
        config.dirColumnCnt = --(binding.directories_grid.layoutManager as MyGridLayoutManager).spanCount
        columnCountChanged()
    }

    private fun columnCountChanged() {
        invalidateOptionsMenu(activity)
        recyclerAdapter?.apply {
            notifyItemRangeChanged(0, dirs.size)
            measureRecyclerViewContent(dirs)
        }
    }

    fun handleMediaIntent(intent: Intent) {
        intent.apply {
            if (mIsSetWallpaperIntent) {
                putExtra(SET_WALLPAPER_INTENT, true)
                startActivityForResult(this, PICK_WALLPAPER)
            } else {
                putExtra(GET_IMAGE_INTENT, mIsPickImageIntent || mIsGetImageContentIntent)
                putExtra(GET_VIDEO_INTENT, mIsPickVideoIntent || mIsGetVideoContentIntent)
                putExtra(GET_ANY_INTENT, mIsGetAnyContentIntent)
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, mAllowPickingMultiple)
                startActivityForResult(this, PICK_MEDIA)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_MEDIA && resultData != null) {
                val resultIntent = Intent()
                var resultUri: Uri? = null
                if (mIsThirdPartyIntent) {
                    when {
                        intent.extras?.containsKey(MediaStore.EXTRA_OUTPUT) == true && intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0 -> {
                            resultUri = fillExtraOutput(resultData)
                        }
                        resultData.extras?.containsKey(PICKED_PATHS) == true -> fillPickedPaths(resultData, resultIntent)
                        else -> fillIntentPath(resultData, resultIntent)
                    }
                }

                if (resultUri != null) {
                    resultIntent.data = resultUri
                    resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                activity.setResult(Activity.RESULT_OK, resultIntent)
                activity.finish()
            } else if (requestCode == PICK_WALLPAPER) {
                activity.setResult(Activity.RESULT_OK)
                activity.finish()
            }
        }
        super.onActivityResult(requestCode, resultCode, resultData)
    }

    private fun fillExtraOutput(resultData: Intent): Uri? {
        val file = File(resultData.data!!.path!!)
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        try {
            val output = intent.extras!!.get(MediaStore.EXTRA_OUTPUT) as Uri
            inputStream = FileInputStream(file)
            outputStream = activity.contentResolver.openOutputStream(output)
            inputStream.copyTo(outputStream!!)
        } catch (e: SecurityException) {
            activity.showErrorToast(e)
        } catch (ignored: FileNotFoundException) {
            return activity.getFilePublicUri(file, BuildConfig.APPLICATION_ID)
        } finally {
            inputStream?.close()
            outputStream?.close()
        }

        return null
    }

    private fun fillPickedPaths(resultData: Intent, resultIntent: Intent) {
        val paths = resultData.extras!!.getStringArrayList(PICKED_PATHS)
        val uris = paths!!.map { activity.getFilePublicUri(File(it), BuildConfig.APPLICATION_ID) } as ArrayList
        val clipData = ClipData("Attachment", arrayOf("image/*", "video/*"), ClipData.Item(uris.removeAt(0)))

        uris.forEach {
            clipData.addItem(ClipData.Item(it))
        }

        resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        resultIntent.clipData = clipData
    }

    private fun fillIntentPath(resultData: Intent, resultIntent: Intent) {
        val data = resultData.data
        val path = if (data.toString().startsWith("/")) data.toString() else data!!.path
        val uri = activity.getFilePublicUri(File(path!!), BuildConfig.APPLICATION_ID)
        val type = path.getMimeType()
        resultIntent.setDataAndTypeAndNormalize(uri, type)
        resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun setAsDefaultFolder() {
        config.defaultFolder = ""
        invalidateOptionsMenu(activity)
    }

    private fun openDefaultFolder() {
        if (config.defaultFolder.isEmpty()) {
            return
        }

        val defaultDir = File(config.defaultFolder)

        if ((!defaultDir.exists() || !defaultDir.isDirectory) && (config.defaultFolder != RECYCLE_BIN && config.defaultFolder != FAVORITES)) {
            config.defaultFolder = ""
            return
        }

        Intent(activity, MediaActivity::class.java).apply {
            putExtra(DIRECTORY, config.defaultFolder)
            handleMediaIntent(this)
        }
    }

    private fun checkPlaceholderVisibility(dirs: ArrayList<FolderItem>) {
        try {
            if(binding.directories_empty_placeholder == null || binding.directories_empty_placeholder_2 == null)
                return

            binding.directories_empty_placeholder.beVisibleIf(dirs.isEmpty() && mLoadedInitialPhotos)
            binding.directories_empty_placeholder_2.beVisibleIf(dirs.isEmpty() && mLoadedInitialPhotos)

            if (mIsSearchOpen) {
                binding.directories_empty_placeholder.text = activity.getString(R.string.no_items_found)
                binding.directories_empty_placeholder_2.beGone()
            } else if (dirs.isEmpty() && config.filterMedia == getDefaultFileFilter()) {
                directories_empty_placeholder.text = activity.getString(R.string.no_media_add_included)
                binding.directories_empty_placeholder_2.text = activity.getString(R.string.add_folder)

                binding.directories_empty_placeholder_2.setOnClickListener {
                    activity.showAddIncludedFolderDialog {
                        refreshItems()
                    }
                }
            } else {
                binding.directories_empty_placeholder.text = activity.getString(R.string.no_media_with_filters)
                binding.directories_empty_placeholder_2.text = activity.getString(R.string.change_filters_underlined)

                binding.directories_empty_placeholder_2.setOnClickListener {
                    showFilterMediaDialog()
                }
            }

            binding.directories_empty_placeholder_2.underlineText()
            binding.directories_grid.beVisibleIf(binding.directories_empty_placeholder.isGone())
        }
        catch (e:IllegalStateException){
            Log.d("Jet", "error checkPlaceholderVisibility() IllegalStateException")
        }
    }

    private fun openFolder(path: String) {
        activity.handleLockedFolderOpening(path) { success ->
            if (success) {
                Intent(activity, MediaActivity::class.java).apply {
                    putExtra(SKIP_AUTHENTICATION, true)
                    putExtra(DIRECTORY, path)
                    handleMediaIntent(this)
                }
            }
        }
    }

    private fun setupScrollDirection() {
        val allowHorizontalScroll = config.scrollHorizontally && config.viewTypeFolders == VIEW_TYPE_GRID
        binding.directories_vertical_fastscroller.isHorizontal = false
        binding.directories_vertical_fastscroller.beGoneIf(allowHorizontalScroll)

        binding.directories_horizontal_fastscroller.isHorizontal = true
        binding.directories_horizontal_fastscroller.beVisibleIf(allowHorizontalScroll)

        if (allowHorizontalScroll) {
            binding.directories_horizontal_fastscroller.setViews(binding.directories_grid, binding.directories_refresh_layout) {
                binding.directories_horizontal_fastscroller.updateBubbleText(getBubbleTextItem(it))
            }
        } else {
            binding.directories_vertical_fastscroller.setViews(binding.directories_grid, binding.directories_refresh_layout) {
                binding.directories_vertical_fastscroller.updateBubbleText(getBubbleTextItem(it))
            }
        }
    }

    private fun checkInvalidDirectories(dirs: ArrayList<FolderItem>) {
        val invalidDirs = ArrayList<FolderItem>()
        val OTGPath = config.OTGPath
        dirs.filter { !it.areFavorites() && !it.isRecycleBin() }.forEach {
            if (!activity.getDoesFilePathExist(it.path, OTGPath)) {
                invalidDirs.add(it)
            } else if (it.path != config.tempFolderPath) {
                val children = if (activity.isPathOnOTG(it.path)) activity.getOTGFolderChildrenNames(it.path) else File(it.path).list()?.asList()
                val hasMediaFile = children?.any {
                    it != null && (it.isMediaFile() || (it.startsWith("img_", true) && File(it).isDirectory))
                } ?: false

                if (!hasMediaFile) {
                    invalidDirs.add(it)
                }
            }
        }

        if (activity.getFavoritePaths().isEmpty()) {
            val favoritesFolder = dirs.firstOrNull { it.areFavorites() }
            if (favoritesFolder != null) {
                invalidDirs.add(favoritesFolder)
            }
        }

        if (config.useRecycleBin) {
            try {
                val binFolder = dirs.firstOrNull { it.path == RECYCLE_BIN }
                if (binFolder != null && activity.mediaDB.getDeletedMedia().isEmpty()) {
                    invalidDirs.add(binFolder)
                }
            } catch (ignored: Exception) {}
        }

        if (invalidDirs.isNotEmpty()) {
            dirs.removeAll(invalidDirs)
            setupAdapter(dirs)
            invalidDirs.forEach {
                try {
                    activity.directoryDao.deleteDirPath(it.path)
                } catch (ignored: Exception) {
                }
            }
        }
    }

    private fun getBubbleTextItem(index: Int) = recyclerAdapter?.dirs?.getOrNull(index)?.getBubbleText(config.directorySorting, activity, mDateFormat, mTimeFormat)
        ?: ""

    private fun setupLatestMediaId() {
        ensureBackgroundThread {
            if (activity.hasPermission(PERMISSION_READ_STORAGE)) {
                mLatestMediaId = activity.getLatestMediaId()
                mLatestMediaDateId = activity.getLatestMediaByDateId()
            }
        }
    }

    private fun checkLastMediaChanged() {
        if (activity.isDestroyed) {
            return
        }

        mLastMediaHandler.postDelayed({
            ensureBackgroundThread {
                val mediaId = activity.getLatestMediaId()
                val mediaDateId = activity.getLatestMediaByDateId()
                if (mLatestMediaId != mediaId || mLatestMediaDateId != mediaDateId) {
                    mLatestMediaId = mediaId
                    mLatestMediaDateId = mediaDateId
                    //activity.runOnUiThread
                    launchMain{
                        getDirectories()
                    }
                } else {
                    mLastMediaHandler.removeCallbacksAndMessages(null)
                    checkLastMediaChanged()
                }
            }
        }, LAST_MEDIA_CHECK_PERIOD)
    }

    private fun checkRecycleBinItems() {
        if (config.useRecycleBin && config.lastBinCheck < System.currentTimeMillis() - DAY_SECONDS * 1000) {
            config.lastBinCheck = System.currentTimeMillis()
            Handler().postDelayed({
                ensureBackgroundThread {
                    try {
                        activity.mediaDB.deleteOldRecycleBinItems(System.currentTimeMillis() - MONTH_MILLISECONDS)
                    } catch (e: Exception) {
                    }
                }
            }, 3000L)
        }
    }

    private fun getFoldersWithMedia(path: String): HashSet<String> {
        val folders = HashSet<String>()
        try {
            val files = File(path).listFiles()
            if (files != null) {
                files.sortBy { !it.isDirectory }
                for (file in files) {
                    if (file.isDirectory && !file.startsWith("${config.internalStoragePath}/Android")) {
                        folders.addAll(getFoldersWithMedia(file.absolutePath))
                    } else if (file.isFile && file.isMediaFile()) {
                        folders.add(file.parent ?: "")
                        break
                    }
                }
            }
        } catch (ignored: Exception) {
        }

        return folders
    }

    override fun refreshItems() {
        getDirectories()
    }

    override fun recheckPinnedFolders() {
        ensureBackgroundThread {
            gotDirectories(activity.movePinnedDirectoriesToFront(currentlyDisplayedDirs))
        }
    }

    override fun updateDirectories(directories: ArrayList<FolderItem>) {
        ensureBackgroundThread {
            activity.storeDirectoryItems(directories)
            activity.removeInvalidDBDirectories()
        }
    }

    private fun checkWhatsNewDialog() {
        arrayListOf<Release>().apply {
            add(Release(213, R.string.release_213))
            add(Release(217, R.string.release_217))
            add(Release(220, R.string.release_220))
            add(Release(221, R.string.release_221))
            add(Release(225, R.string.release_225))
            add(Release(258, R.string.release_258))
            add(Release(277, R.string.release_277))
            add(Release(295, R.string.release_295))
            add(Release(327, R.string.release_327))
            activity.checkWhatsNew(this, BuildConfig.VERSION_CODE)
        }
    }
}
