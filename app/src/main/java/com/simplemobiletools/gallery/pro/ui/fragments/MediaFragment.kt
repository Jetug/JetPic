package com.simplemobiletools.gallery.pro.ui.fragments

import java.io.File
import java.util.HashMap
import java.io.IOException
import kotlin.collections.ArrayList
import kotlin.system.measureTimeMillis
import kotlinx.android.synthetic.main.fragment_media.*
import kotlinx.android.synthetic.main.fragment_media.view.*
import android.os.*
import android.app.*
import android.view.*
import android.net.Uri
import android.util.Log
import android.content.*
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.graphics.Bitmap
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.annotation.SuppressLint
import androidx.core.view.MenuItemCompat
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.core.app.ActivityCompat.invalidateOptionsMenu
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.ui.activities.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.commons.views.MyGridLayoutManager
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.gallery.pro.ui.adapters.MediaAdapter
import com.simplemobiletools.gallery.pro.data.extensions.*
import com.simplemobiletools.gallery.pro.data.helpers.*
import com.simplemobiletools.gallery.pro.data.models.Medium
import com.simplemobiletools.gallery.pro.data.models.ThumbnailItem
import com.simplemobiletools.gallery.pro.data.models.ThumbnailSection
import com.simplemobiletools.commons.dialogs.CreateNewFolderDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.gallery.pro.data.extensions.context.*
import com.simplemobiletools.gallery.pro.data.databases.GalleryDatabase
import com.simplemobiletools.gallery.pro.data.helpers.asynctasks.GetMediaAsynctask
import com.simplemobiletools.gallery.pro.ui.dialogs.ChangeGroupingDialog
import com.simplemobiletools.gallery.pro.ui.dialogs.ChangeSortingDialog
import com.simplemobiletools.gallery.pro.ui.dialogs.ChangeViewTypeDialog
import com.simplemobiletools.gallery.pro.ui.dialogs.FilterMediaDialog
import com.simplemobiletools.gallery.pro.ui.adapters.MediaAdapterControls
import com.simplemobiletools.gallery.pro.data.interfaces.MediaOperationsListener

interface FragmentControls{
    fun clearAdapter()
}

class MediaFragment : Fragment(), MediaOperationsListener, FragmentControls {
    private val LAST_MEDIA_CHECK_PERIOD = 3000L
    private val IS_SWIPEREFRESH_ENABLED = false

    private var mPath = ""
    private var mIsGetAnyIntent = false
    private var mIsGettingMedia = false
    private var mAllowPickingMultiple = false
    private var mShowAll = false
    private var mLoadedInitialPhotos = false
    private var mIsSearchOpen = false
    private var mLastSearchedText = ""
    private var mDateFormat = ""
    private var mTimeFormat = ""
    private var mLatestMediaId = 0L
    private var mLatestMediaDateId = 0L
    private var mLastMediaHandler = Handler()
    private var mTempShowHiddenHandler = Handler()
    private var mCurrAsyncTask: GetMediaAsynctask? = null
    private var mZoomListener: MyRecyclerView.MyZoomListener? = null
    private var mSearchMenuItem: MenuItem? = null
    private var mStoredAnimateGifs = true
    private var mStoredCropThumbnails = true
    private var mStoredScrollHorizontally = true
    private var mStoredShowFileTypes = true
    private var mStoredRoundedCorners = false
    private var mStoredTextColor = 0
    private var mStoredAdjustedPrimaryColor = 0
    private var mStoredThumbnailSpacing = 0
    private var activity: SimpleActivity = SimpleActivity()

    lateinit var config: Config
    lateinit var intent: Intent

    var mIsGetImageIntent = false
    var mIsGetVideoIntent = false

    companion object {
        var mMedia = ArrayList<ThumbnailItem>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity = getActivity() as SimpleActivity
        config = activity.config
        intent = activity.intent

        try {
            mPath = intent.getStringExtra(DIRECTORY) ?: ""
            if(mPath == "")
                mPath = arguments?.getString(DIRECTORY) ?: ""

        } catch (e: Exception) {
            activity.showErrorToast(e)
            activity.finish()
            return
        }

        intent.apply {
            mIsGetImageIntent = getBooleanExtra(GET_IMAGE_INTENT, false)
            mIsGetVideoIntent = getBooleanExtra(GET_VIDEO_INTENT, false)
            mIsGetAnyIntent = getBooleanExtra(GET_ANY_INTENT, false)
            mAllowPickingMultiple = getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        }

        storeStateVariables()

//        if (mShowAll) {
//            activity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
//            activity.registerFileUpdateListener()
//        }

        activity.updateWidgets()
    }

    private lateinit var binding: View
    val adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>? get() {
        try{
            return binding.media_grid.adapter
        }
        catch (e: UninitializedPropertyAccessException){
            return null
        }

    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View? {
        binding = inflater.inflate(R.layout.fragment_media, container, false)

        setHasOptionsMenu(true)
        clearAdapter()

        binding.media_refresh_layout.isEnabled = IS_SWIPEREFRESH_ENABLED
        binding.media_refresh_layout.setOnRefreshListener { getMedia() }
        binding.media_empty_text_placeholder_2.setOnClickListener {
            showFilterMediaDialog()
        }

        if(mPath != "")
            config.showAll = false

        val dirName = when {
            mPath == FAVORITES -> activity.getString(R.string.favorites)
            mPath == RECYCLE_BIN -> activity.getString(R.string.recycle_bin)
            mPath == config.OTGPath -> activity.getString(R.string.usb)
            else -> activity.getHumanizedFilename(mPath)
        }
        activity.updateActionBarTitle(if (mShowAll) resources.getString(R.string.all_folders) else dirName)

        return binding
    }

    override fun onStart() {
        super.onStart()
        mTempShowHiddenHandler.removeCallbacksAndMessages(null)
    }

    override fun onResume() {
        val elapsedTime = measureTimeMillis {
            super.onResume()
            ///Jet
            activity.makeTranslucentBars()
            restoreRVPosition()
            activity.setTopPaddingToActionBarsHeight(binding.media_grid)
            activity.setTopMarginToActionBarsHeight(binding.media_vertical_fastscroller)
            ///

            mDateFormat = config.dateFormat
            mTimeFormat = activity.getTimeFormat()

            if (mStoredAnimateGifs != config.animateGifs) {
                mediaAdapter?.updateAnimateGifs(config.animateGifs)
            }

            if (mStoredCropThumbnails != config.cropThumbnails) {
                mediaAdapter?.updateCropThumbnails(config.cropThumbnails)
            }

            if (mStoredScrollHorizontally != config.scrollHorizontally) {
                mLoadedInitialPhotos = false
                binding.media_grid.adapter = null
                getMedia()
            }

            if (mStoredShowFileTypes != config.showThumbnailFileTypes) {
                mediaAdapter?.updateShowFileTypes(config.showThumbnailFileTypes)
            }

            if (mStoredTextColor != config.textColor) {
                mediaAdapter?.updateTextColor(config.textColor)
            }

            val adjustedPrimaryColor = activity.getAdjustedPrimaryColor()
            if (mStoredAdjustedPrimaryColor != adjustedPrimaryColor) {
                mediaAdapter?.updatePrimaryColor(config.primaryColor)
                binding.media_horizontal_fastscroller.updatePrimaryColor(adjustedPrimaryColor)
                binding.media_vertical_fastscroller.updatePrimaryColor(adjustedPrimaryColor)
            }

            if (mStoredThumbnailSpacing != config.thumbnailSpacing) {
                binding.media_grid.adapter = null
                setupAdapter()
            }

            if (mStoredRoundedCorners != config.fileRoundedCorners) {
                binding.media_grid.adapter = null
                setupAdapter()
            }

            binding.media_horizontal_fastscroller.updateBubbleColors()
            binding.media_vertical_fastscroller.updateBubbleColors()
            binding.media_refresh_layout.isEnabled = config.enablePullToRefresh
            binding.media_empty_text_placeholder.setTextColor(config.textColor)
            binding.media_empty_text_placeholder_2.setTextColor(activity.getAdjustedPrimaryColor())

            if (!mIsSearchOpen) {
                invalidateOptionsMenu(activity)
            }

            if (mMedia.isEmpty() || activity.getFolderSorting(mPath) and SORT_BY_RANDOM == 0) {
                if (shouldSkipAuthentication()) {
                    tryLoadGallery()
                } else {
                    activity.handleLockedFolderOpening(mPath) { success ->
                        if (success) {
                            tryLoadGallery()
                        } else {
                            activity.finish()
                        }
                    }
                }
            }
        }
        Log.e("Jet","Media on Resume $elapsedTime ms")
    }

    override fun onPause() {
        super.onPause()
        mIsGettingMedia = false
        binding.media_refresh_layout.isRefreshing = false
        storeStateVariables()
        mLastMediaHandler.removeCallbacksAndMessages(null)

        if (!mMedia.isEmpty()) {
            mCurrAsyncTask?.stopFetching()
        }

        ///Jet
        saveRVPosition()
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
        if (config.showAll && !activity.isChangingConfigurations) {
            config.temporarilyShowHidden = false
            config.tempSkipDeleteConfirmation = false
            activity.unregisterFileUpdateListener()
            GalleryDatabase.destroyInstance()
        }

        mTempShowHiddenHandler.removeCallbacksAndMessages(null)
        mMedia.clear()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_media, menu)

        val isDefaultFolder = !config.defaultFolder.isEmpty() && File(config.defaultFolder).compareTo(File(mPath)) == 0

        menu.apply {
            findItem(R.id.group).isVisible = !config.scrollHorizontally

            findItem(R.id.empty_recycle_bin).isVisible = mPath == RECYCLE_BIN
            findItem(R.id.empty_disable_recycle_bin).isVisible = mPath == RECYCLE_BIN
            findItem(R.id.restore_all_files).isVisible = mPath == RECYCLE_BIN

            //findItem(R.id.folder_view).isVisible = mShowAll
            findItem(R.id.open_camera).isVisible = mShowAll
            //findItem(R.id.about).isVisible = mShowAll
            findItem(R.id.create_new_folder).isVisible = !mShowAll && mPath != RECYCLE_BIN && mPath != FAVORITES

            findItem(R.id.temporarily_show_hidden).isVisible = !config.shouldShowHidden
            findItem(R.id.stop_showing_hidden).isVisible = config.temporarilyShowHidden

            findItem(R.id.set_as_default_folder).isVisible = !isDefaultFolder
            findItem(R.id.unset_as_default_folder).isVisible = isDefaultFolder

            val viewType = config.getFolderViewType(if (mShowAll) SHOW_ALL else mPath)
            findItem(R.id.increase_column_count).isVisible = viewType == VIEW_TYPE_GRID && config.mediaColumnCnt < MAX_COLUMN_COUNT
            findItem(R.id.reduce_column_count).isVisible = viewType == VIEW_TYPE_GRID && config.mediaColumnCnt > 1
            findItem(R.id.toggle_filename).isVisible = viewType == VIEW_TYPE_GRID

            findItem(R.id.settings).isVisible = false
            findItem(R.id.about).isVisible = false
        }

        setupSearch(menu)
        activity.updateMenuItemColors(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.cab_change_order -> mediaAdapter?.changeOrder()
            R.id.sort -> showSortingDialog()
            R.id.filter -> showFilterMediaDialog()
            R.id.empty_recycle_bin -> emptyRecycleBin()
            R.id.empty_disable_recycle_bin -> emptyAndDisableRecycleBin()
            R.id.restore_all_files -> restoreAllFiles()
            R.id.toggle_filename -> toggleFilenameVisibility()
            R.id.open_camera -> activity.launchCamera()
            // R.id.folder_view -> switchToFolderView()
            R.id.change_view_type -> changeViewType()
            R.id.group -> showGroupByDialog()
            R.id.create_new_folder -> createNewFolder()
            R.id.temporarily_show_hidden -> tryToggleTemporarilyShowHidden()
            R.id.stop_showing_hidden -> tryToggleTemporarilyShowHidden()
            R.id.increase_column_count -> increaseColumnCount()
            R.id.reduce_column_count -> reduceColumnCount()
            R.id.set_as_default_folder -> setAsDefaultFolder()
            R.id.unset_as_default_folder -> unsetAsDefaultFolder()
            R.id.slideshow -> startSlideshow()
            R.id.settings -> activity.launchSettings()
            R.id.about -> activity.launchAbout()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun startSlideshow() {
        if (mMedia.isNotEmpty()) {
            Intent(activity, ViewPagerActivity::class.java).apply {
                val item = mMedia.firstOrNull { it is Medium } as? Medium ?: return
                putExtra(SKIP_AUTHENTICATION, shouldSkipAuthentication())
                putExtra(PATH, item.path)
                putExtra(SHOW_ALL, mShowAll)
                putExtra(SLIDESHOW_START_ON_ENTER, true)
                startActivity(this)
            }
        }
    }

    private fun storeStateVariables() {
        config.apply {
            mStoredAnimateGifs = animateGifs
            mStoredCropThumbnails = cropThumbnails
            mStoredScrollHorizontally = scrollHorizontally
            mStoredShowFileTypes = showThumbnailFileTypes
            mStoredTextColor = textColor
            mStoredThumbnailSpacing = thumbnailSpacing
            mStoredRoundedCorners = fileRoundedCorners
            mShowAll = showAll
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
                        mLastSearchedText = newText
                        searchQueryChanged(newText)
                    }
                    return true
                }
            })
        }

        MenuItemCompat.setOnActionExpandListener(mSearchMenuItem, object : MenuItemCompat.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                mIsSearchOpen = true
                binding.media_refresh_layout.isEnabled = false
                return true
            }

            // this triggers on device rotation too, avoid doing anything
            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                if (mIsSearchOpen) {
                    mIsSearchOpen = false
                    mLastSearchedText = ""

                    binding.media_refresh_layout.isEnabled = config.enablePullToRefresh
                    searchQueryChanged("")
                }
                return true
            }
        })
    }

    private fun searchQueryChanged(text: String) {
        ensureBackgroundThread {
            try {
                val filtered = mMedia.filter { it is Medium && it.name.contains(text, true) } as ArrayList
                filtered.sortBy { it is Medium && !it.name.startsWith(text, true) }
                val grouped = MediaFetcher(activity.applicationContext).groupMedia(filtered as ArrayList<Medium>, mPath)
                activity.runOnUiThread {
                    if (grouped.isEmpty()) {
                        binding.media_empty_text_placeholder.text = activity.getString(R.string.no_items_found)
                        binding.media_empty_text_placeholder.beVisible()
                    } else {
                        binding.media_empty_text_placeholder.beGone()
                    }

                    handleGridSpacing(grouped)
                    mediaAdapter?.updateMedia(grouped)
                    measureRecyclerViewContent(grouped)
                }
            } catch (ignored: Exception) {
            }
        }
    }

    private fun tryLoadGallery() {
        activity.handlePermission(PERMISSION_WRITE_STORAGE) {
            if (it) {
                getMedia()
                setupLayoutManager()
            } else {
                activity.toast(R.string.no_storage_permissions)
                activity.finish()
            }
        }
    }

    private val mediaAdapter get() = binding.media_grid.adapter as? MediaAdapter

    private fun saveRVPosition(){
        val ox = binding.media_grid.computeHorizontalScrollOffset()
        val oy = binding.media_grid.computeVerticalScrollOffset()
        mediaScrollPositions[mPath] = Pair(ox, oy)
    }

    private fun restoreRVPosition(){
        val pos = mediaScrollPositions[mPath]
        if (pos != null) {
            (binding.media_grid.layoutManager as MyGridLayoutManager).scrollToPositionWithOffset(pos.first, -pos.second)
        }
    }

    val mediaControls = object : MediaAdapterControls {
        override fun recreateAdapter() { getMedia() }
    }


    override fun clearAdapter(){
        val size = mMedia.size
        mMedia.clear()
        adapter?.notifyItemRangeChanged(0, size)
        binding.media_grid.adapter = null
    }

    private fun setupAdapter() {
        if (!mShowAll && isDirEmpty()) {
            return
        }

        val currAdapter = binding.media_grid.adapter
        if (currAdapter == null) {
            initZoomListener()
            val fastScroller = if (config.scrollHorizontally) media_horizontal_fastscroller else media_vertical_fastscroller
            MediaAdapter(activity, mMedia.clone() as ArrayList<ThumbnailItem>, this, mIsGetImageIntent || mIsGetVideoIntent || mIsGetAnyIntent,
                mAllowPickingMultiple, mPath, binding.media_grid, fastScroller, media_refresh_layout, mediaControls) {
                if (it is Medium && !activity.isFinishing) {
                    itemClicked(it.path)
                }
            }.apply {
                setupZoomListener(mZoomListener)
                binding.media_grid.adapter = this
            }

            val viewType = config.getFolderViewType(if (mShowAll) SHOW_ALL else mPath)
            if (viewType == VIEW_TYPE_LIST) {
                binding.media_grid.scheduleLayoutAnimation()
            }

            setupLayoutManager()
            handleGridSpacing()
            measureRecyclerViewContent(mMedia)
        } else if (mLastSearchedText.isEmpty()) {
            (currAdapter as MediaAdapter).updateMedia(mMedia)
            handleGridSpacing()
            measureRecyclerViewContent(mMedia)
        } else {
            searchQueryChanged(mLastSearchedText)
        }
        setupScrollDirection()
    }

    private fun setupScrollDirection() {
        val viewType = config.getFolderViewType(if (mShowAll) SHOW_ALL else mPath)
        val allowHorizontalScroll = config.scrollHorizontally && viewType == VIEW_TYPE_GRID
        binding.media_vertical_fastscroller.isHorizontal = false
        binding.media_vertical_fastscroller.beGoneIf(allowHorizontalScroll)

        binding.media_horizontal_fastscroller.isHorizontal = true
        binding.media_horizontal_fastscroller.beVisibleIf(allowHorizontalScroll)

        val sorting = activity.getFolderSorting(if (mShowAll) SHOW_ALL else mPath)
        if (allowHorizontalScroll) {
            binding.media_horizontal_fastscroller.setViews(binding.media_grid, binding.media_refresh_layout) {
                binding.media_horizontal_fastscroller.updateBubbleText(getBubbleTextItem(it, sorting))
            }
        } else {
            binding.media_vertical_fastscroller.setViews(binding.media_grid, binding.media_refresh_layout) {
                binding.media_vertical_fastscroller.updateBubbleText(getBubbleTextItem(it, sorting))
            }
        }
    }

    private fun getBubbleTextItem(index: Int, sorting: Int): String {
        var realIndex = index
        val mediaAdapter = mediaAdapter
        if (mediaAdapter?.isASectionTitle(index) == true) {
            realIndex++
        }
        return mediaAdapter?.getItemBubbleText(realIndex, sorting, mDateFormat, mTimeFormat) ?: ""
    }

    private fun checkLastMediaChanged() {
        if (activity.isDestroyed || activity.getFolderSorting(mPath) and SORT_BY_RANDOM != 0) {
            return
        }

        mLastMediaHandler.removeCallbacksAndMessages(null)
        mLastMediaHandler.postDelayed({
            ensureBackgroundThread {
                val mediaId = activity.getLatestMediaId()
                val mediaDateId = activity.getLatestMediaByDateId()
                if (mLatestMediaId != mediaId || mLatestMediaDateId != mediaDateId) {
                    mLatestMediaId = mediaId
                    mLatestMediaDateId = mediaDateId
                    activity.runOnUiThread {
                        getMedia()
                    }
                } else {
                    checkLastMediaChanged()
                }
            }
        }, LAST_MEDIA_CHECK_PERIOD)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showSortingDialog() {
        ChangeSortingDialog(activity, false, true, mPath) {
            mLoadedInitialPhotos = false
            //binding.media_grid.adapter = null
            mediaAdapter?.sort()
            //getMedia()
        }
    }

    private fun showFilterMediaDialog() {
        FilterMediaDialog(activity) {
            mLoadedInitialPhotos = false
            binding.media_refresh_layout.isRefreshing = true
            binding.media_grid.adapter = null
            getMedia()
        }
    }

    private fun emptyRecycleBin() {
        activity.showRecycleBinEmptyingDialog {
            activity.emptyTheRecycleBin {
                activity.finish()
            }
        }
    }

    private fun emptyAndDisableRecycleBin() {
        activity.showRecycleBinEmptyingDialog {
            activity.emptyAndDisableTheRecycleBin {
                activity.finish()
            }
        }
    }

    private fun restoreAllFiles() {
        val paths = mMedia.filter { it is Medium }.map { (it as Medium).path } as ArrayList<String>
        activity.restoreRecycleBinPaths(paths) {
            ensureBackgroundThread {
                activity.directoryDao.deleteDirPath(RECYCLE_BIN)
            }
            activity.finish()
        }
    }

    private fun toggleFilenameVisibility() {
        config.displayFileNames = !config.displayFileNames
        mediaAdapter?.updateDisplayFilenames(config.displayFileNames)
    }

    private fun switchToFolderView() {
        config.showAll = false
        startActivity(Intent(activity, MainActivity::class.java))
        activity.finish()
    }

    private fun changeViewType() {
        ChangeViewTypeDialog(activity, false, mPath) {
            invalidateOptionsMenu(activity)
            setupLayoutManager()
            binding.media_grid.adapter = null
            setupAdapter()
        }
    }

    private fun showGroupByDialog() {
        ChangeGroupingDialog(activity, mPath) {
            mLoadedInitialPhotos = false
            binding.media_grid.adapter = null
            //mediaAdapter?.sort()
            getMedia()
        }
    }

    private fun deleteDirectoryIfEmpty() {
        if (config.deleteEmptyFolders) {
            val fileDirItem = FileDirItem(mPath, mPath.getFilenameFromPath(), true)
            if (!fileDirItem.isDownloadsFolder() && fileDirItem.isDirectory) {
                ensureBackgroundThread {
                    if (fileDirItem.getProperFileCount(activity, true) == 0) {
                        activity.tryDeleteFileDirItem(fileDirItem, true, true)
                    }
                }
            }
        }
    }

    private fun getMedia() {
        if (mIsGettingMedia) {
            return
        }

        mIsGettingMedia = true
        if (mLoadedInitialPhotos) {
            startAsyncTask()
        } else {
            activity.getCachedMedia(mPath, mIsGetVideoIntent, mIsGetImageIntent) {
                if (it.isEmpty()) {
                    activity.runOnUiThread {
                        media_refresh_layout.isRefreshing = true
                    }
                } else {
                    gotMedia(it, true)
                }
                startAsyncTask()
            }
        }

        mLoadedInitialPhotos = true
    }

    fun isMediasEquals(newMedia: ArrayList<ThumbnailItem>): Boolean {
        val oldMedia = mMedia.clone() as ArrayList<ThumbnailItem>

        //if(newMedia.size == oldMedia.size) return false

        for(i in 0 until newMedia.size){
            val old = oldMedia[i]
            val new = newMedia[i]

            if(old is Medium && new is Medium && new.name != old.name)
                return false
        }

        return true
    }

    private fun startAsyncTask() {
        mCurrAsyncTask?.stopFetching()
        mCurrAsyncTask = GetMediaAsynctask(activity.applicationContext, mPath, mIsGetImageIntent, mIsGetVideoIntent, mShowAll) {
            ensureBackgroundThread {
                val oldMedia = mMedia.clone() as ArrayList<ThumbnailItem>
                val newMedia = it
                try {
                    gotMedia(newMedia, false)

                    // remove cached files that are no longer valid for whatever reason
                    val newPaths = newMedia.mapNotNull { it as? Medium }.map { it.path }
                    oldMedia.mapNotNull { it as? Medium }.filter { !newPaths.contains(it.path) }.forEach {
                        if (mPath == FAVORITES && activity.getDoesFilePathExist(it.path)) {
                            activity.favoritesDB.deleteFavoritePath(it.path)
                            activity.mediaDB.updateFavorite(it.path, false)
                        } else {
                            activity.mediaDB.deleteMediumPath(it.path)
                        }
                    }
                } catch (e: Exception) {
                }
            }
        }

        mCurrAsyncTask!!.execute()
    }

    private fun startAsyncTask2() {
        mCurrAsyncTask?.stopFetching()
        mCurrAsyncTask = GetMediaAsynctask(activity.applicationContext, mPath, mIsGetImageIntent, mIsGetVideoIntent, mShowAll) {
            //restoreRVPosition()
            //ensureBackgroundThread {
            launchDefault {
                val oldMedia = mMedia.clone() as ArrayList<ThumbnailItem>
                val newMedia = it
                //if(isMediasEquals(newMedia)){
                try {
                    gotMedia(newMedia, false)
                    oldMedia.filter { !newMedia.contains(it) }.mapNotNull { it as? Medium }.filter { !activity.getDoesFilePathExist(it.path) }.forEach {
                        activity.mediaDB.deleteMediumPath(it.path)
                    }
                } catch (e: Exception) {
                }
                //}
            }
        }

        mCurrAsyncTask!!.execute()
    }

    private fun isDirEmpty(): Boolean {
        return if (mMedia.size <= 0 && config.filterMedia > 0) {
            if (mPath != FAVORITES && mPath != RECYCLE_BIN) {
                deleteDirectoryIfEmpty()
                deleteDBDirectory()
            }

            if (mPath == FAVORITES) {
                ensureBackgroundThread {
                    activity.directoryDao.deleteDirPath(FAVORITES)
                }
            }

            //activity.finish()
            true
        } else {
            false
        }
    }

    private fun deleteDBDirectory() {
        ensureBackgroundThread {
            try {
                activity.directoryDao.deleteDirPath(mPath)
            } catch (ignored: Exception) {
            }
        }
    }

    private fun createNewFolder() {
        CreateNewFolderDialog(activity, mPath) {
            config.tempFolderPath = it
        }
    }

    private fun tryToggleTemporarilyShowHidden() {
        if (config.temporarilyShowHidden) {
            toggleTemporarilyShowHidden(false)
        } else {
            activity.handleHiddenFolderPasswordProtection {
                toggleTemporarilyShowHidden(true)
            }
        }
    }

    private fun toggleTemporarilyShowHidden(show: Boolean) {
        mLoadedInitialPhotos = false
        config.temporarilyShowHidden = show
        getMedia()
        invalidateOptionsMenu(activity)
    }

    private fun setupLayoutManager() {
        val viewType = config.getFolderViewType(if (mShowAll) SHOW_ALL else mPath)
        if (viewType == VIEW_TYPE_GRID) {
            setupGridLayoutManager()
        } else {
            setupListLayoutManager()
        }
    }

    private fun setupGridLayoutManager() {
        val layoutManager = binding.media_grid.layoutManager as MyGridLayoutManager
        (binding.media_grid.layoutParams as RelativeLayout.LayoutParams).apply {
            topMargin = 0
            bottomMargin = 0
        }

        if (config.scrollHorizontally) {
            layoutManager.orientation = RecyclerView.HORIZONTAL
            binding.media_refresh_layout.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        } else {
            layoutManager.orientation = RecyclerView.VERTICAL
            binding.media_refresh_layout.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        layoutManager.spanCount = config.mediaColumnCnt
        val adapter = mediaAdapter
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (adapter?.isASectionTitle(position) == true) {
                    layoutManager.spanCount
                } else {
                    1
                }
            }
        }
    }

    private fun measureRecyclerViewContent(media: ArrayList<ThumbnailItem>) {
        binding.media_grid.onGlobalLayout {
            if (config.scrollHorizontally) {
                calculateContentWidth(media)
            } else {
                calculateContentHeight(media)
            }
        }
    }

    private fun calculateContentWidth(media: ArrayList<ThumbnailItem>) {
        val layoutManager = binding.media_grid.layoutManager as MyGridLayoutManager
        val thumbnailWidth = layoutManager.getChildAt(0)?.width ?: 0
        val spacing = config.thumbnailSpacing
        val fullWidth = ((media.size - 1) / layoutManager.spanCount + 1) * (thumbnailWidth + spacing) - spacing
        binding.media_horizontal_fastscroller.setContentWidth(fullWidth)
        binding.media_horizontal_fastscroller.setScrollToX(binding.media_grid.computeHorizontalScrollOffset())
    }

    private fun calculateContentHeight(media: ArrayList<ThumbnailItem>) {
        val layoutManager = binding.media_grid.layoutManager as MyGridLayoutManager
        val pathToCheck = if (mPath.isEmpty()) SHOW_ALL else mPath
        val hasSections = config.getFolderGrouping(pathToCheck) and GROUP_BY_NONE == 0 && !config.scrollHorizontally
        val sectionTitleHeight = if (hasSections) layoutManager.getChildAt(0)?.height ?: 0 else 0
        val thumbnailHeight = if (hasSections) layoutManager.getChildAt(1)?.height ?: 0 else layoutManager.getChildAt(0)?.height ?: 0

        var fullHeight = 0
        var curSectionItems = 0
        media.forEach {
            if (it is ThumbnailSection) {
                fullHeight += sectionTitleHeight
                if (curSectionItems != 0) {
                    val rows = ((curSectionItems - 1) / layoutManager.spanCount + 1)
                    fullHeight += rows * thumbnailHeight
                }
                curSectionItems = 0
            } else {
                curSectionItems++
            }
        }

        val spacing = config.thumbnailSpacing
        fullHeight += ((curSectionItems - 1) / layoutManager.spanCount + 1) * (thumbnailHeight + spacing) - spacing
        binding.media_vertical_fastscroller.setContentHeight(fullHeight)
        binding.media_vertical_fastscroller.setScrollToY(binding.media_grid.computeVerticalScrollOffset())
    }

    private fun handleGridSpacing(media: ArrayList<ThumbnailItem> = mMedia) {
        val viewType = config.getFolderViewType(if (mShowAll) SHOW_ALL else mPath)
        if (viewType == VIEW_TYPE_GRID) {
            val spanCount = config.mediaColumnCnt
            val spacing = config.thumbnailSpacing
            val useGridPosition = media.firstOrNull() is ThumbnailSection

            var currentGridDecoration: GridSpacingItemDecoration? = null
            if (binding.media_grid.itemDecorationCount > 0) {
                //currentGridDecoration = binding.media_grid.getItemDecorationAt(0) as GridSpacingItemDecoration
                //currentGridDecoration.items = media
            }

            val newGridDecoration = GridSpacingItemDecoration(spanCount, spacing, config.scrollHorizontally, config.fileRoundedCorners, media, useGridPosition)
            if (currentGridDecoration.toString() != newGridDecoration.toString()) {
                if (currentGridDecoration != null) {
                    binding.media_grid.removeItemDecoration(currentGridDecoration)
                }
                binding.media_grid.addItemDecoration(newGridDecoration)
            }
        }
    }

    private fun initZoomListener() {
        val viewType = config.getFolderViewType(if (mShowAll) SHOW_ALL else mPath)
        if (viewType == VIEW_TYPE_GRID) {
            val layoutManager = binding.media_grid.layoutManager as MyGridLayoutManager
            mZoomListener = object : MyRecyclerView.MyZoomListener {
                override fun zoomIn() {
                    if (layoutManager.spanCount > 1) {
                        reduceColumnCount()
                        mediaAdapter?.finishActMode()
                    }
                }

                override fun zoomOut() {
                    if (layoutManager.spanCount < MAX_COLUMN_COUNT) {
                        increaseColumnCount()
                        mediaAdapter?.finishActMode()
                    }
                }
            }
        } else {
            mZoomListener = null
        }
    }

    private fun setupListLayoutManager() {
        val layoutManager = binding.media_grid.layoutManager as MyGridLayoutManager
        layoutManager.spanCount = 1
        layoutManager.orientation = RecyclerView.VERTICAL
        media_refresh_layout.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val smallMargin = resources.getDimension(R.dimen.small_margin).toInt()
        (binding.media_grid.layoutParams as RelativeLayout.LayoutParams).apply {
            topMargin = smallMargin
            bottomMargin = smallMargin
        }

        mZoomListener = null
    }

    private fun increaseColumnCount() {
        config.mediaColumnCnt = ++(binding.media_grid.layoutManager as MyGridLayoutManager).spanCount
        columnCountChanged()
    }

    private fun reduceColumnCount() {
        config.mediaColumnCnt = --(binding.media_grid.layoutManager as MyGridLayoutManager).spanCount
        columnCountChanged()
    }

    private fun columnCountChanged() {
        handleGridSpacing()
        invalidateOptionsMenu(activity)
        mediaAdapter?.apply {
            notifyItemRangeChanged(0, media.size)
            measureRecyclerViewContent(media)
        }
    }

    private fun isSetWallpaperIntent() = intent.getBooleanExtra(SET_WALLPAPER_INTENT, false)

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == REQUEST_EDIT_IMAGE) {
            if (resultCode == Activity.RESULT_OK && resultData != null) {
                mMedia.clear()
                refreshItems()
            }
        }
        super.onActivityResult(requestCode, resultCode, resultData)
    }

    private fun itemClicked(path: String) {
        val elapsedTime = measureTimeMillis {
            if (isSetWallpaperIntent()) {
                activity.toast(R.string.setting_wallpaper)

                val wantedWidth = activity.wallpaperDesiredMinimumWidth
                val wantedHeight = activity.wallpaperDesiredMinimumHeight
                val ratio = wantedWidth.toFloat() / wantedHeight

                val options = RequestOptions()
                    .override((wantedWidth * ratio).toInt(), wantedHeight)
                    .fitCenter()

                Glide.with(this)
                    .asBitmap()
                    .load(File(path))
                    .apply(options)
                    .into(object : SimpleTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            try {
                                WallpaperManager.getInstance(activity.applicationContext).setBitmap(resource)
                                activity.setResult(Activity.RESULT_OK)
                            } catch (ignored: IOException) {
                            }

                            activity.finish()
                        }
                    })
            }
            else if (mIsGetImageIntent || mIsGetVideoIntent || mIsGetAnyIntent) {
                Intent().apply {
                    data = Uri.parse(path)
                    activity.setResult(Activity.RESULT_OK, this)
                }
                activity.finish()
            } else {
                val isVideo = path.isVideoFast()
                if (isVideo) {
                    val extras = HashMap<String, Boolean>()
                    extras[SHOW_FAVORITES] = mPath == FAVORITES

                    if (shouldSkipAuthentication()) {
                        extras[SKIP_AUTHENTICATION] = true
                    }
                    activity.openPath(path, false, extras)
                } else {
                    Intent(activity, ViewPagerActivity::class.java).apply {
                        putExtra(SKIP_AUTHENTICATION, shouldSkipAuthentication())
                        putExtra(PATH, path)
                        putExtra(SHOW_ALL, mShowAll)
                        putExtra(SHOW_FAVORITES, mPath == FAVORITES)
                        putExtra(SHOW_RECYCLE_BIN, mPath == RECYCLE_BIN)
                        startActivity(this)
                    }
                }
            }
        }
        Log.e("Jet","Media on Click $elapsedTime ms")
    }

    private fun gotMedia(media: ArrayList<ThumbnailItem>, isFromCache: Boolean) {
        try {
            mIsGettingMedia = false
            checkLastMediaChanged()
            mMedia = media

            activity.runOnUiThread {
                binding.media_refresh_layout.isRefreshing = false
                binding.media_empty_text_placeholder.beVisibleIf(media.isEmpty() && !isFromCache)
                binding.media_empty_text_placeholder_2.beVisibleIf(media.isEmpty() && !isFromCache)

                if (binding.media_empty_text_placeholder.isVisible()) {
                    binding.media_empty_text_placeholder.text = activity.getString(R.string.no_media_with_filters)
                }
                binding.media_grid.beVisibleIf(binding.media_empty_text_placeholder.isGone())

                val viewType = config.getFolderViewType(if (mShowAll) SHOW_ALL else mPath)
                val allowHorizontalScroll = config.scrollHorizontally && viewType == VIEW_TYPE_GRID
                binding.media_vertical_fastscroller.beVisibleIf(binding.media_grid.isVisible() && !allowHorizontalScroll)
                binding.media_horizontal_fastscroller.beVisibleIf(binding.media_grid.isVisible() && allowHorizontalScroll)
                setupAdapter()
            }

            mLatestMediaId = activity.getLatestMediaId()
            mLatestMediaDateId = activity.getLatestMediaByDateId()
            if (!isFromCache) {
                val mediaToInsert = (mMedia).filter { it is Medium && it.deletedTS == 0L }.map { it as Medium }
                Thread {
                    try {
                        activity.mediaDB.insertAll(mediaToInsert)
                    } catch (e: Exception) {
                    }
                }.start()
            }
        }
        catch (e:IllegalStateException){
            Log.d("Jet", "error gotMedia() IllegalStateException")
        }
    }

    override fun tryDeleteFiles(fileDirItems: ArrayList<FileDirItem>) {
        val filtered = fileDirItems.filter { !activity.getIsPathDirectory(it.path) && it.path.isMediaFile() } as ArrayList
        if (filtered.isEmpty()) {
            return
        }

        if (config.useRecycleBin && !filtered.first().path.startsWith(activity.recycleBinPath)) {
            val movingItems = resources.getQuantityString(R.plurals.moving_items_into_bin, filtered.size, filtered.size)
            activity.toast(movingItems)

            activity.movePathsInRecycleBin(filtered.map { it.path } as ArrayList<String>) {
                if (it) {
                    deleteFilteredFiles(filtered)
                } else {
                    activity.toast(R.string.unknown_error_occurred)
                }
            }
        } else {
            val deletingItems = resources.getQuantityString(R.plurals.deleting_items, filtered.size, filtered.size)
            activity.toast(deletingItems)
            deleteFilteredFiles(filtered)
        }
    }

    private fun shouldSkipAuthentication() = intent.getBooleanExtra(SKIP_AUTHENTICATION, false)

    private fun deleteFilteredFiles(filtered: ArrayList<FileDirItem>) {
        activity.deleteFiles(filtered) {
            if (!it) {
                activity.toast(R.string.unknown_error_occurred)
                return@deleteFiles
            }

            mMedia.removeAll { filtered.map { it.path }.contains((it as? Medium)?.path) }

            ensureBackgroundThread {
                val useRecycleBin = config.useRecycleBin
                filtered.forEach {
                    if (it.path.startsWith(activity.recycleBinPath) || !useRecycleBin) {
                        activity.deleteDBPath(it.path)
                    }
                }
            }

            if (mMedia.isEmpty()) {
                deleteDirectoryIfEmpty()
                deleteDBDirectory()
                //activity.finish()
            }
        }
    }

    override fun refreshItems() {
        getMedia()
    }

    override fun selectedPaths(paths: ArrayList<String>) {
        Intent().apply {
            putExtra(PICKED_PATHS, paths)
            activity.setResult(Activity.RESULT_OK, this)
        }
        activity.finish()
    }

    override fun updateMediaGridDecoration(media: ArrayList<ThumbnailItem>) {
        var currentGridPosition = 0
        media.forEach {
            if (it is Medium) {
                it.gridPosition = currentGridPosition++
            } else if (it is ThumbnailSection) {
                currentGridPosition = 0
            }
        }

        if (binding.media_grid.itemDecorationCount > 0) {
            val currentGridDecoration = binding.media_grid.getItemDecorationAt(0) as GridSpacingItemDecoration
            currentGridDecoration.items = media
        }
    }

    private fun setAsDefaultFolder() {
        config.defaultFolder = mPath
        invalidateOptionsMenu(activity)
    }

    private fun unsetAsDefaultFolder() {
        config.defaultFolder = ""
        invalidateOptionsMenu(activity)
    }
}
