package com.jetug.gallery.pro.adapters

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Icon
import android.icu.text.CaseMap
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.jetug.commons.activities.BaseSimpleActivity
import com.jetug.commons.adapters.MyRecyclerViewAdapter
import com.jetug.commons.dialogs.*
import com.jetug.commons.extensions.*
import com.jetug.commons.helpers.*
import com.jetug.commons.models.FileDirItem
import com.jetug.commons.views.FastScroller
import com.jetug.commons.views.MyRecyclerView
import com.jetug.gallery.pro.R
import com.jetug.gallery.pro.activities.MediaActivity
import com.jetug.gallery.pro.dialogs.*
import com.jetug.gallery.pro.extensions.*
import com.jetug.gallery.pro.helpers.*
import com.jetug.gallery.pro.interfaces.DirectoryOperationsListener
import com.jetug.gallery.pro.models.*
import com.jetug.gallery.pro.jetug.*
import com.jetug.gallery.pro.views.MySquareImageView
import kotlinx.android.synthetic.main.activity_media.*
import kotlinx.android.synthetic.main.directory_item_grid_square.view.*
import kotlinx.android.synthetic.main.directory_item_grid_square.view.dir_check
import kotlinx.android.synthetic.main.directory_item_grid_square.view.dir_drag_handle_wrapper
import kotlinx.android.synthetic.main.directory_item_grid_square.view.dir_location
import kotlinx.android.synthetic.main.directory_item_grid_square.view.dir_lock
import kotlinx.android.synthetic.main.directory_item_grid_square.view.dir_name
import kotlinx.android.synthetic.main.directory_item_grid_square.view.dir_pin
import kotlinx.android.synthetic.main.directory_item_grid_square.view.dir_thumbnail
import kotlinx.android.synthetic.main.directory_item_list.view.*
import kotlinx.android.synthetic.main.directory_item_list.view.dir_drag_handle
import kotlinx.android.synthetic.main.directory_item_list.view.dir_holder
import kotlinx.android.synthetic.main.directory_item_list.view.photo_cnt
import kotlinx.android.synthetic.main.item_dir_group.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

@SuppressLint("NotifyDataSetChanged")
class DirectoryAdapter(activity: BaseSimpleActivity, var dirs: ArrayList<FolderItem>, val listener: DirectoryOperationsListener?, recyclerView: MyRecyclerView,
                       private val isPickIntent: Boolean, swipeRefreshLayout: SwipeRefreshLayout? = null, fastScroller: FastScroller? = null, itemClick: (Any) -> Unit) :
    RecyclerViewAdapterBase(activity, recyclerView, fastScroller, swipeRefreshLayout, itemClick){

    private val ITEM_PLACEHOLDER = 0
    private val ITEM_DIRECTORY = 1
    private val ITEM_DIRECTORY_GROUP = 2

    private val config = activity.config
    private val isListViewType get() = config.viewTypeFolders == VIEW_TYPE_LIST

    private var pinnedFolders = config.pinnedFolders
    private var scrollHorizontally = config.scrollHorizontally
    private var animateGifs = config.animateGifs
    private var cropThumbnails = config.cropThumbnails
    private var groupDirectSubfolders = config.groupDirectSubfolders
    private var currentDirectoriesHash = dirs.hashCode()
    private var lockedFolderPaths = ArrayList<String>()

    private var showMediaCount = config.showFolderMediaCount
    private var folderStyle = config.folderStyle
    private var limitFolderTitle = config.limitFolderTitle

    private val selectedGroups: ArrayList<DirectoryGroup>
        get() = ArrayList(selectedItems.filterIsInstance<DirectoryGroup>())

    override val itemList = dirs

    init {
//        dirs.forEach(){
//            if (it.name == "@Test$ (1) (1) (1)"){
//                Log.d("Jet", it.name)
//            }
//        }

//        val p = object : FolderItem(0, "","","",0,0,0,0,0,0,"",0) {}
//        p.placeholder = true
//        dirs.add(0, p)

        setupDragListener(true)
        fillLockedFolders()
    }

    override fun getItemViewType(position: Int): Int {
        return when (dirs[position]) {
            is DirectoryGroup -> ITEM_DIRECTORY_GROUP
            else -> ITEM_DIRECTORY
        }
    }

    override fun getActionMenuId() = R.menu.cab_directories

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutType = when {
            isListViewType -> R.layout.directory_item_list
            viewType == ITEM_DIRECTORY_GROUP -> R.layout.item_dir_group
            folderStyle == FOLDER_STYLE_SQUARE -> R.layout.directory_item_grid_square
            else -> R.layout.directory_item_grid_rounded_corners
        }

        return createViewHolder(layoutType, parent)
    }

    override fun onBindViewHolder(holder: MyRecyclerViewAdapter.ViewHolder, position: Int) {
        val dir = dirs.getOrNull(position) ?: return

        holder.bindView(dir, true, !isPickIntent) { itemView, adapterPosition ->
            if(dir is Directory || dir is DirectoryGroup )
                setupView(itemView, dir, holder, position)
        }

        bindViewHolder(holder)
    }

    override fun getItemCount() = dirs.size

    override fun prepareActionMode(menu: Menu) {
        val selectedPaths = selectedPaths
        if (selectedPaths.isEmpty()) {
            return
        }

        menu.apply {
            findItem(R.id.cab_move_to_top).isVisible = isDragAndDropping
            findItem(R.id.cab_move_to_bottom).isVisible = isDragAndDropping

            findItem(R.id.cab_rename).isVisible = !selectedPaths.contains(FAVORITES) && !selectedPaths.contains(RECYCLE_BIN)
            findItem(R.id.cab_change_cover_image).isVisible = isOneItemSelected

            findItem(R.id.cab_lock).isVisible = selectedPaths.any { !config.isFolderProtected(it) }
            findItem(R.id.cab_unlock).isVisible = selectedPaths.any { config.isFolderProtected(it) }

            findItem(R.id.cab_empty_recycle_bin).isVisible = isOneItemSelected && selectedPaths.first() == RECYCLE_BIN
            findItem(R.id.cab_empty_disable_recycle_bin).isVisible = isOneItemSelected && selectedPaths.first() == RECYCLE_BIN

            findItem(R.id.cab_create_shortcut).isVisible = isOreoPlus() && isOneItemSelected

            findItem(R.id.cab_group).isVisible = selectedItems.size > 1 && selectedGroups.size <= 1
            findItem(R.id.cab_ungroup).isVisible = isOneItemSelected && selectedItems[0] is DirectoryGroup


            checkHideBtnVisibility(this, selectedPaths)
            checkPinBtnVisibility(this, selectedPaths)
        }
    }

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_sort -> sortMedias()
            R.id.cab_group -> groupDirs()
            R.id.cab_ungroup -> ungroupDirs()
            R.id.cab_move_to_top -> moveSelectedItemsToTop()
            R.id.cab_move_to_bottom -> moveSelectedItemsToBottom()
            R.id.cab_properties -> showProperties()
            R.id.cab_rename -> renameDir()
            R.id.cab_pin -> pinFolders(true)
            R.id.cab_unpin -> pinFolders(false)
            R.id.cab_empty_recycle_bin -> tryEmptyRecycleBin(true)
            R.id.cab_empty_disable_recycle_bin -> emptyAndDisableRecycleBin()
            R.id.cab_hide -> toggleFoldersVisibility(true)
            R.id.cab_unhide -> toggleFoldersVisibility(false)
            R.id.cab_exclude -> tryExcludeFolder()
            R.id.cab_lock -> tryLockFolder()
            R.id.cab_unlock -> unlockFolder()
            R.id.cab_copy_to -> copyMoveTo(true)
            R.id.cab_move_to -> moveFilesTo()
            R.id.cab_select_all -> selectAll()
            R.id.cab_create_shortcut -> tryCreateShortcut()
            R.id.cab_delete -> askConfirmDelete()
            R.id.cab_select_photo -> tryChangeAlbumCover(false)
            R.id.cab_use_default -> tryChangeAlbumCover(true)
        }
    }

    override fun getSelectableItemCount() = dirs.size
    override fun getIsItemSelectable(position: Int) = true
    override fun getItemSelectionKey(position: Int) = dirs.getOrNull(position)?.path?.hashCode()
    override fun getItemKeyPosition(key: Int) = dirs.indexOfFirst { it.path.hashCode() == key }
    override fun onActionModeCreated() {}

    override fun onDragAndDroppingEnded() {
        val reorderedFoldersList = dirs.map { it.path }
        config.customFoldersOrder = TextUtils.join("|||", reorderedFoldersList)
        config.directorySorting = SORT_BY_CUSTOM
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        if (!activity.isDestroyed && holder.itemViewType != ITEM_PLACEHOLDER ) {
            Glide.with(activity).clear(holder.itemView.dir_thumbnail!!)
        }
    }

    /////////Public
    fun changeDirs(directories: ArrayList<FolderItem>) {
        val newDirs = directories.clone() as ArrayList<FolderItem>
        val size = dirs.size
        dirs.clear()
        notifyItemRangeRemoved(0, size)
        dirs = newDirs
        notifyDataSetChanged()
    }

    fun sort() {
        dirs = activity.getSortedDirectories(dirs) //sortDirs(dirs, config.directorySorting)
        notifyDataSetChanged()
    }

    fun add(items: ArrayList<FolderItem>){
        dirs.addAll(items)
        getDirsToShow()
    }

    fun add(item: FolderItem){
        dirs.add(item)
        getDirsToShow()
    }

    private fun getDirsToShow(){
        dirs = activity.getDirsToShow(dirs.getDirectories())
        dirs = activity.getSortedDirectories(dirs)
        notifyDataSetChanged()
    }

    fun updateDirs(newDirs: ArrayList<FolderItem>){

//        val newDirs = directories.clone() as ArrayList<FolderItem>
//        val size = dirs.size
//        dirs.clear()
//        withContext(Dispatchers.Main) {
//            notifyItemRangeRemoved(0, size - 1)
//        }
//        recyclerView.getRecycledViewPool().clear()
//        //delay(5000)
//        dirs = newDirs
//        withContext(Dispatchers.Main) {
//            notifyDataSetChanged()
//        }

        val directories = newDirs.clone() as ArrayList<FolderItem>
        if (directories.hashCode() != currentDirectoriesHash) {
            currentDirectoriesHash = directories.hashCode()
            dirs = directories
            fillLockedFolders()
            notifyDataSetChanged()
            finishActMode()
        }
    }

    fun updateAnimateGifs(animateGifs: Boolean) {
        this.animateGifs = animateGifs
        notifyDataSetChanged()
    }

    fun updateCropThumbnails(cropThumbnails: Boolean) {
        this.cropThumbnails = cropThumbnails
        notifyDataSetChanged()
    }
/////////////////////////////////

    private fun sortMedias(){

        if(isOneItemSelected && firstSelectedItem is Directory){
            val path = firstSelectedItemPath
            if (path != null) {
                ChangeSortingDialog(activity, false, true, path) {}
                //selectedItems
            }
        }
        else{
            MultiChangeSortingDialog(activity, true, selectedPaths) {}
        }
    }

    private fun groupDirs(){
        val items = selectedItems
        fun group(name: String){
            for (i in 0 until items.size){
                val item = items[i]
                if(item is Directory) {
                    item.groupName = name
                    //activity.updateDBDirectory(item)

                    activity.saveDirectoryGroup(item.path, name)
                }
            }

            dirs = activity.getDirsToShow(dirs.getDirectories(), arrayListOf())
            notifyDataSetChanged()
            finishActMode()
        }
        val groups = selectedGroups

        if(selectedGroups.size == 1){
            group(groups[0].name)
        }
        else {
            GroupDirectoryDialog(activity) { name ->
                group(name)
            }
        }
    }

    private fun ungroupDirs(){
        val item = selectedItems[0]
        if(item is DirectoryGroup) {
            item.innerDirs.forEach{
                //activity.updateDBDirectory(it)
                activity.saveDirectoryGroup(item.path, "")
            }

            dirs.remove(item)
            dirs.addAll(item.innerDirs)

            dirs = activity.getDirsToShow(dirs.getDirectories(), arrayListOf())
            notifyDataSetChanged()
            finishActMode()
        }
    }

    private fun checkHideBtnVisibility(menu: Menu, selectedPaths: ArrayList<String>) {
        menu.findItem(R.id.cab_hide).isVisible = selectedPaths.any { !it.doesThisOrParentHaveNoMedia(HashMap(), null) }
        menu.findItem(R.id.cab_unhide).isVisible = selectedPaths.any { it.doesThisOrParentHaveNoMedia(HashMap(), null) }
    }

    private fun checkPinBtnVisibility(menu: Menu, selectedPaths: ArrayList<String>) {
        val pinnedFolders = config.pinnedFolders
        menu.findItem(R.id.cab_pin).isVisible = selectedPaths.any { !pinnedFolders.contains(it) }
        menu.findItem(R.id.cab_unpin).isVisible = selectedPaths.any { pinnedFolders.contains(it) }
    }

    private fun moveSelectedItemsToTop() {
        selectedKeys.reversed().forEach { key ->
            val position = dirs.indexOfFirst { it.path.hashCode() == key }
            val tempItem = dirs[position]
            dirs.removeAt(position)
            dirs.add(0, tempItem)
        }

        notifyDataSetChanged()
    }

    private fun moveSelectedItemsToBottom() {
        selectedKeys.forEach { key ->
            val position = dirs.indexOfFirst { it.path.hashCode() == key }
            val tempItem = dirs[position]
            dirs.removeAt(position)
            dirs.add(dirs.size, tempItem)
        }

        notifyDataSetChanged()
    }

    private fun showProperties() {

//        val oy = recyclerView.computeVerticalScrollOffset()
//        Toast.makeText(activity, "$oy", Toast.LENGTH_SHORT).show()
//        val y = recyclerView.offsetChildrenVertical(-150)

        recyclerView.scrollY = 50
        if (selectedKeys.size <= 1) {
            val path = firstSelectedItemPath ?: return
            if (path != FAVORITES && path != RECYCLE_BIN) {
                activity.handleLockedFolderOpening(path) { success ->
                    if (success) {
                        PropertiesDialog(activity, path, config.shouldShowHidden)
                    }
                }
            }
        } else {
            PropertiesDialog(activity, selectedPaths.filter {
                it != FAVORITES && it != RECYCLE_BIN && !config.isFolderProtected(it)
            }.toMutableList(), config.shouldShowHidden)
        }
    }

    private fun renameDir() {
        if (selectedKeys.size == 1 && firstSelectedItem is Directory) {
            val firstDir = firstSelectedItem ?: return
            val sourcePath = firstDir.path
            val dir = File(sourcePath)
            if (activity.isAStorageRootFolder(dir.absolutePath)) {
                activity.toast(R.string.rename_folder_root)
                return
            }

            activity.handleLockedFolderOpening(sourcePath) { success ->
                if (success) {
                    RenameItemDialog(activity, dir.absolutePath) {
                        activity.runOnUiThread {
                            firstDir.apply {
                                path = it
                                name = it.getFilenameFromPath()
                                tmb = File(it, tmb.getFilenameFromPath()).absolutePath
                            }
                            updateDirs(dirs)
                            ensureBackgroundThread {
                                try {
                                    activity.directoryDao.updateDirectoryAfterRename(firstDir.tmb, firstDir.name, firstDir.path, sourcePath)
                                    listener?.refreshItems()
                                } catch (e: Exception) {
                                    activity.showErrorToast(e)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val paths = selectedPaths.filter { !activity.isAStorageRootFolder(it) && !config.isFolderProtected(it) } as ArrayList<String>
            RenameItemsDialog(activity, paths) {
                listener?.refreshItems()
            }
        }
    }

    private fun toggleFoldersVisibility(hide: Boolean) {
        val selectedPaths = selectedPaths
        if (hide && selectedPaths.contains(RECYCLE_BIN)) {
            config.showRecycleBinAtFolders = false
            if (selectedPaths.size == 1) {
                listener?.refreshItems()
                finishActMode()
            }
        }

        if (hide) {
            if (config.wasHideFolderTooltipShown) {
                hideFolders(selectedPaths)
            } else {
                config.wasHideFolderTooltipShown = true
                ConfirmationDialog(activity, activity.getString(R.string.hide_folder_description)) {
                    hideFolders(selectedPaths)
                }
            }
        } else {
            selectedPaths.filter { it != FAVORITES && it != RECYCLE_BIN && (selectedPaths.size == 1 || !config.isFolderProtected(it)) }.forEach {
                val path = it
                activity.handleLockedFolderOpening(path) { success ->
                    if (success) {
                        if (path.containsNoMedia()) {
                            activity.removeNoMedia(path) {
                                if (config.shouldShowHidden) {
                                    updateFolderNames()
                                } else {
                                    activity.runOnUiThread {
                                        listener?.refreshItems()
                                        finishActMode()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun hideFolders(paths: ArrayList<String>) {
        for (path in paths) {
            activity.handleLockedFolderOpening(path) { success ->
                if (success) {
                    hideFolder(path)
                }
            }
        }
    }

    private fun tryEmptyRecycleBin(askConfirmation: Boolean) {
        if (askConfirmation) {
            activity.showRecycleBinEmptyingDialog {
                emptyRecycleBin()
            }
        } else {
            emptyRecycleBin()
        }
    }

    private fun emptyRecycleBin() {
        activity.handleLockedFolderOpening(RECYCLE_BIN) { success ->
            if (success) {
                activity.emptyTheRecycleBin {
                    listener?.refreshItems()
                }
            }
        }
    }

    private fun emptyAndDisableRecycleBin() {
        activity.handleLockedFolderOpening(RECYCLE_BIN) { success ->
            if (success) {
                activity.showRecycleBinEmptyingDialog {
                    activity.emptyAndDisableTheRecycleBin {
                        listener?.refreshItems()
                    }
                }
            }
        }
    }

    private fun updateFolderNames() {
        val includedFolders = config.includedFolders
        val hidden = activity.getString(R.string.hidden)
        dirs.forEach {
            it.name = activity.checkAppendingHidden(it.path, hidden, includedFolders, ArrayList())
        }
        listener?.updateDirectories(dirs.toMutableList() as ArrayList)
        activity.runOnUiThread {
            updateDirs(dirs)
        }
    }

    private fun hideFolder(path: String) {
        activity.addNoMedia(path) {
            if (config.shouldShowHidden) {
                updateFolderNames()
            } else {
                val affectedPositions = ArrayList<Int>()
                val includedFolders = config.includedFolders
                val newDirs = dirs.filterIndexed { index, directory ->
                    val removeDir = directory.path.doesThisOrParentHaveNoMedia(HashMap(), null) && !includedFolders.contains(directory.path)
                    if (removeDir) {
                        affectedPositions.add(index)
                    }
                    !removeDir
                } as ArrayList<FolderItem>

                activity.runOnUiThread {
                    affectedPositions.sortedDescending().forEach {
                        notifyItemRemoved(it)
                    }

                    currentDirectoriesHash = newDirs.hashCode()
                    dirs = newDirs

                    finishActMode()
                    listener?.updateDirectories(newDirs)
                }
            }
        }
    }

    private fun tryExcludeFolder() {
        val selectedPaths = selectedPaths
        val paths = selectedPaths.filter { it != PATH && it != RECYCLE_BIN && it != FAVORITES }.toSet()
        if (selectedPaths.contains(RECYCLE_BIN)) {
            config.showRecycleBinAtFolders = false
            if (selectedPaths.size == 1) {
                listener?.refreshItems()
                finishActMode()
            }
        }

        if (paths.size == 1) {
            ExcludeFolderDialog(activity, paths.toMutableList()) {
                listener?.refreshItems()
                finishActMode()
            }
        } else if (paths.size > 1) {
            config.addExcludedFolders(paths)
            listener?.refreshItems()
            finishActMode()
        }
    }

    private fun tryLockFolder() {
        if (config.wasFolderLockingNoticeShown) {
            lockFolder()
        } else {
            FolderLockingNoticeDialog(activity) {
                lockFolder()
            }
        }
    }

    private fun lockFolder() {
        SecurityDialog(activity, "", SHOW_ALL_TABS) { hash, type, success ->
            if (success) {
                selectedPaths.filter { !config.isFolderProtected(it) }.forEach {
                    config.addFolderProtection(it, hash, type)
                    lockedFolderPaths.add(it)
                }

                listener?.refreshItems()
                finishActMode()
            }
        }
    }

    private fun unlockFolder() {
        val paths = selectedPaths
        val firstPath = paths.first()
        val tabToShow = config.getFolderProtectionType(firstPath)
        val hashToCheck = config.getFolderProtectionHash(firstPath)
        SecurityDialog(activity, hashToCheck, tabToShow) { hash, type, success ->
            if (success) {
                paths.filter { config.isFolderProtected(it) && config.getFolderProtectionType(it) == tabToShow && config.getFolderProtectionHash(it) == hashToCheck }.forEach {
                    config.removeFolderProtection(it)
                    lockedFolderPaths.remove(it)
                }

                listener?.refreshItems()
                finishActMode()
            }
        }
    }

    private fun pinFolders(pin: Boolean) {
        if (pin) {
            config.addPinnedFolders(selectedPaths.toHashSet())
        } else {
            config.removePinnedFolders(selectedPaths.toHashSet())
        }

        currentDirectoriesHash = 0
        pinnedFolders = config.pinnedFolders
        listener?.recheckPinnedFolders()
    }

    private fun moveFilesTo() {
        activity.handleDeletePasswordProtection {
            copyMoveTo(false)
        }
    }

    private fun copyMoveTo(isCopyOperation: Boolean) {
        val paths = ArrayList<String>()
        val showHidden = config.shouldShowHidden
        selectedPaths.forEach {
            val filter = config.filterMedia
            File(it).listFiles()?.filter {
                !File(it.absolutePath).isDirectory &&
                    it.absolutePath.isMediaFile() && (showHidden || !it.name.startsWith('.')) &&
                    ((it.isImageFast() && filter and TYPE_IMAGES != 0) ||
                        (it.isVideoFast() && filter and TYPE_VIDEOS != 0) ||
                        (it.isGif() && filter and TYPE_GIFS != 0) ||
                        (it.isRawFast() && filter and TYPE_RAWS != 0) ||
                        (it.isSvg() && filter and TYPE_SVGS != 0))
            }?.mapTo(paths) { it.absolutePath }
        }

        val fileDirItems = paths.map { FileDirItem(it, it.getFilenameFromPath()) } as ArrayList<FileDirItem>
        activity.tryCopyMoveFilesTo(fileDirItems, isCopyOperation) {
            val destinationPath = it
            val newPaths = fileDirItems.map { "$destinationPath/${it.name}" }.toMutableList() as java.util.ArrayList<String>
            activity.fixDateTaken(newPaths, false)

            config.tempFolderPath = ""
            listener?.refreshItems()
            finishActMode()
        }
    }

    private fun tryCreateShortcut() {
        activity.handleLockedFolderOpening(firstSelectedItemPath ?: "") { success ->
            if (success) {
                createShortcut()
            }
        }
    }

    @SuppressLint("NewApi")
    private fun createShortcut() {
        val manager = activity.getSystemService(ShortcutManager::class.java)
        if (manager.isRequestPinShortcutSupported) {
            val dir = firstSelectedItem ?: return
            val path = dir.path
            val drawable = resources.getDrawable(R.drawable.shortcut_image).mutate()
            val coverThumbnail = config.parseAlbumCovers().firstOrNull { it.tmb == dir.path }?.tmb ?: dir.tmb
            activity.getShortcutImage(coverThumbnail, drawable) {
                val intent = Intent(activity, MediaActivity::class.java)
                intent.action = Intent.ACTION_VIEW
                intent.flags = intent.flags or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                intent.putExtra(DIRECTORY, path)

                val shortcut = ShortcutInfo.Builder(activity, path)
                    .setShortLabel(dir.name)
                    .setIcon(Icon.createWithBitmap(drawable.convertToBitmap()))
                    .setIntent(intent)
                    .build()

                manager.requestPinShortcut(shortcut, null)
            }
        }
    }

    private fun askConfirmDelete() {
        when {
            config.isDeletePasswordProtectionOn -> activity.handleDeletePasswordProtection {
                deleteFolders()
            }
            config.skipDeleteConfirmation -> deleteFolders()
            else -> {
                val itemsCnt = selectedKeys.size
                if (itemsCnt == 1 && selectedItems.first().isRecycleBin()) {
                    ConfirmationDialog(activity, "", R.string.empty_recycle_bin_confirmation, R.string.yes, R.string.no) {
                        deleteFolders()
                    }
                    return
                }

                val items = if (itemsCnt == 1) {
                    val folder = selectedPaths.first().getFilenameFromPath()
                    "\"$folder\""
                } else {
                    resources.getQuantityString(R.plurals.delete_items, itemsCnt, itemsCnt)
                }

                val fileDirItem = firstSelectedItem ?: return
                val baseString = if (!config.useRecycleBin || (isOneItemSelected && fileDirItem.areFavorites())) {
                    R.string.deletion_confirmation
                } else {
                    R.string.move_to_recycle_bin_confirmation
                }

                val question = String.format(resources.getString(baseString), items)
                val warning = resources.getQuantityString(R.plurals.delete_warning, itemsCnt, itemsCnt)
                ConfirmDeleteFolderDialog(activity, question, warning) {
                    deleteFolders()
                }
            }
        }
    }

    private fun deleteFolders() {
        if (selectedKeys.isEmpty()) {
            return
        }

        var SAFPath = ""
        val selectedDirs = selectedItems
        selectedDirs.forEach {
            val path = it.path
            if (activity.needsStupidWritePermissions(path) && config.treeUri.isEmpty()) {
                SAFPath = path
            }
        }

        activity.handleSAFDialog(SAFPath) {
            if (!it) {
                return@handleSAFDialog
            }

            var foldersToDelete = ArrayList<File>(selectedKeys.size)
            selectedDirs.forEach {
                if (it.areFavorites() || it.isRecycleBin()) {
                    if (it.isRecycleBin()) {
                        tryEmptyRecycleBin(false)
                    } else {
                        ensureBackgroundThread {
                            activity.mediaDB.clearFavorites()
                            activity.favoritesDB.clearFavorites()
                            listener?.refreshItems()
                        }
                    }

                    if (selectedKeys.size == 1) {
                        finishActMode()
                    }
                } else {
                    foldersToDelete.add(File(it.path))
                }
            }

            if (foldersToDelete.size == 1) {
                activity.handleLockedFolderOpening(foldersToDelete.first().absolutePath) { success ->
                    if (success) {
                        listener?.deleteFolders(foldersToDelete)
                    }
                }
            } else {
                foldersToDelete = foldersToDelete.filter { !config.isFolderProtected(it.absolutePath) }.toMutableList() as ArrayList<File>
                listener?.deleteFolders(foldersToDelete)
            }
        }
    }

    private fun tryChangeAlbumCover(useDefault: Boolean) {
        activity.handleLockedFolderOpening(firstSelectedItemPath ?: "") { success ->
            if (success) {
                changeAlbumCover(useDefault)
            }
        }
    }

    private fun changeAlbumCover(useDefault: Boolean) {
        if (selectedKeys.size != 1)
            return

        val path = firstSelectedItemPath ?: return

        if (useDefault) {
            val albumCovers = getAlbumCoversWithout(path)
            storeCovers(albumCovers)
        } else {
            pickMediumFrom(path, path)
        }
    }

    private fun pickMediumFrom(targetFolder: String, path: String) {
        PickMediumDialog(activity, path) {
            if (File(it).isDirectory) {
                pickMediumFrom(targetFolder, it)
            } else {
                val albumCovers = getAlbumCoversWithout(path)
                val cover = AlbumCover(targetFolder, it)
                albumCovers.add(cover)
                storeCovers(albumCovers)
            }
        }
    }

    private fun getAlbumCoversWithout(path: String) = config.parseAlbumCovers().filterNot { it.path == path } as ArrayList

    private fun storeCovers(albumCovers: ArrayList<AlbumCover>) {
        config.albumCovers = Gson().toJson(albumCovers)
        finishActMode()
        listener?.refreshItems()
    }

    private val selectedItems get() = selectedKeys.mapNotNull { getItemWithKey(it) } as ArrayList<FolderItem>

    private val selectedPaths get() = selectedItems.getDirectories().map { it.path } as ArrayList<String>

    private val firstSelectedItem get() = getItemWithKey(selectedKeys.first())

    private val firstSelectedItemPath get() = firstSelectedItem?.path

    private fun getItemWithKey(key: Int): FolderItem? = dirs.firstOrNull { it.path.hashCode() == key }

    private fun fillLockedFolders() {
        lockedFolderPaths.clear()
        dirs.map { it.path }.filter { config.isFolderProtected(it) }.forEach {
            lockedFolderPaths.add(it)
        }
    }

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    private fun setupView(view: View, directory: FolderItem, holder: ViewHolder, position: Int) {
        val isSelected = selectedKeys.contains(directory.path.hashCode())
        view.apply {
            dir_path?.text = "${directory.path.substringBeforeLast("/")}/"
            val thumbnailType = when {
                directory.tmb.isVideoFast() -> TYPE_VIDEOS
                directory.tmb.isGif() -> TYPE_GIFS
                directory.tmb.isRawFast() -> TYPE_RAWS
                directory.tmb.isSvg() -> TYPE_SVGS
                else -> TYPE_IMAGES
            }

            dir_check?.beVisibleIf(isSelected)
            if (isSelected) {
                dir_check.background?.applyColorFilter(adjustedPrimaryColor)
                dir_check.applyColorFilter(contrastColor)
            }

            if (isListViewType) {
                dir_holder.isSelected = isSelected
            }

            if (scrollHorizontally && !isListViewType && folderStyle == FOLDER_STYLE_ROUNDED_CORNERS) {
                (dir_thumbnail.layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.ABOVE, dir_name.id)

                val photoCntParams = (photo_cnt.layoutParams as RelativeLayout.LayoutParams)
                val nameParams = (dir_name.layoutParams as RelativeLayout.LayoutParams)
                nameParams.removeRule(RelativeLayout.BELOW)

                if (config.showFolderMediaCount == FOLDER_MEDIA_CNT_LINE) {
                    nameParams.addRule(RelativeLayout.ABOVE, photo_cnt.id)
                    nameParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)

                    photoCntParams.removeRule(RelativeLayout.BELOW)
                    photoCntParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                } else {
                    nameParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                }
            }

            if (lockedFolderPaths.contains(directory.path)) {
                dir_lock.beVisible()
                dir_lock.background = ColorDrawable(config.backgroundColor)
                dir_lock.applyColorFilter(config.backgroundColor.getContrastColor())
            } else {
                dir_lock.beGone()
                val roundedCorners = when {
                    isListViewType -> ROUNDED_CORNERS_SMALL
                    folderStyle == FOLDER_STYLE_SQUARE -> ROUNDED_CORNERS_NONE
                    else -> ROUNDED_CORNERS_BIG
                }

                ///Jet
                fun setupTmb(dir: FolderItem, view: MySquareImageView){
                    val tmbType = when {
                        dir.tmb.isVideoFast() -> TYPE_VIDEOS
                        dir.tmb.isGif() -> TYPE_GIFS
                        dir.tmb.isRawFast() -> TYPE_RAWS
                        dir.tmb.isSvg() -> TYPE_SVGS
                        else -> TYPE_IMAGES
                    }

                    activity.loadImage(tmbType, dir.tmb, view, scrollHorizontally, animateGifs, cropThumbnails, roundedCorners, dir.getKey())
                }

                if (directory is Directory || isListViewType) {
                    setupTmb(directory, dir_thumbnail)
                }
                else if(directory is DirectoryGroup && !isListViewType){
                    val innerDirs = directory.innerDirs
                    val size = innerDirs.size

                    setupTmb(innerDirs[0], dir_thumbnail2)

                    if(size >= 1){
                        setupTmb(innerDirs[0], dir_thumbnail)
                    }
                    if(size == 2){
                        setupTmb(innerDirs[1], dir_thumbnail3)
                    }
                    else if(size >= 3){
                        setupTmb(innerDirs[1], dir_thumbnail2)
                        setupTmb(innerDirs[2], dir_thumbnail3)
                    }
                }
                ///
            }

            dir_pin.beVisibleIf(pinnedFolders.contains(directory.path))
            dir_location.beVisibleIf(directory.location != LOCATION_INTERNAL)
            if (dir_location.isVisible()) {
                dir_location.setImageResource(if (directory.location == LOCATION_SD) R.drawable.ic_sd_card_vector else R.drawable.ic_usb_vector)
            }

            if(config.groupDirectSubfolders)
                photo_cnt.text = directory.subfoldersMediaCount.toString()
            else
                photo_cnt.text = directory.mediaCnt.toString()
            photo_cnt.beVisibleIf(showMediaCount == FOLDER_MEDIA_CNT_LINE)

            if (limitFolderTitle) {
                dir_name.setSingleLine()
                dir_name.ellipsize = TextUtils.TruncateAt.MIDDLE
            }

            var nameCount = directory.name
            if (showMediaCount == FOLDER_MEDIA_CNT_BRACKETS) {
                nameCount += " (${directory.subfoldersMediaCount})"
            }

            if (groupDirectSubfolders) {
                if (directory.subfoldersCount > 1) {
                    nameCount += " [${directory.subfoldersCount}]"
                }
            }
            //Jet
            if(directory is DirectoryGroup && directory.innerDirs.isNotEmpty()){
                nameCount += " [${directory.innerDirs.size}]"
            }

            dir_name.text = nameCount

            if (isListViewType || folderStyle == FOLDER_STYLE_ROUNDED_CORNERS) {
                photo_cnt.setTextColor(textColor)
                dir_name.setTextColor(textColor)
                dir_location.applyColorFilter(textColor)
            }

            if (isListViewType) {
                dir_path?.setTextColor(textColor)
                dir_pin.applyColorFilter(textColor)
                dir_location?.applyColorFilter(textColor)
                dir_drag_handle.beVisibleIf(isDragAndDropping)
            } else {
                dir_drag_handle_wrapper?.beVisibleIf(isDragAndDropping)
            }

            if (isDragAndDropping) {
                dir_drag_handle.applyColorFilter(textColor)

                //drag
                dir_drag_handle.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        startReorderDragListener?.requestDrag(holder)
                    }
                    false
                }
            }
        }
    }
    private fun setupViewColors(view: View){
        view.apply {

        }
    }

    //////////////Jet

    override fun onItemMoved(fromPosition: Int, toPosition: Int){
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(dirs, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(dirs, i, i - 1)
            }
        }

        notifyItemMoved(fromPosition, toPosition)
    }

    ///////////////////
}
