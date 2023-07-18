package com.simplemobiletools.gallery.pro.ui.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import com.simplemobiletools.commons.dialogs.ConfirmationAdvancedDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.gallery.pro.BuildConfig
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.*
import com.simplemobiletools.gallery.pro.data.databases.GalleryDatabase
import com.simplemobiletools.gallery.pro.data.extensions.*
import com.simplemobiletools.gallery.pro.data.extensions.context.config
import com.simplemobiletools.gallery.pro.data.extensions.context.launchSettings
import com.simplemobiletools.gallery.pro.data.extensions.context.startSettingsScanner
import com.simplemobiletools.gallery.pro.data.extensions.context.updateWidgets
import com.simplemobiletools.gallery.pro.data.helpers.*
import com.simplemobiletools.gallery.pro.data.helpers.khttp.post
import com.simplemobiletools.gallery.pro.data.jetug.workers.getAllTasks
import com.simplemobiletools.gallery.pro.ui.fragments.DirectoryFragment
import com.simplemobiletools.gallery.pro.ui.fragments.MediaFragment
import java.net.InetAddress
import kotlin.system.measureTimeMillis

var mWasProtectionHandled = false
var mIsThirdPartyIntent = false
var mIsPickImageIntent = false
var mIsPickVideoIntent = false
var mIsGetImageContentIntent = false
var mIsGetVideoContentIntent = false
var mIsGetAnyContentIntent = false
var mIsSetWallpaperIntent = false
var mAllowPickingMultiple = false

private val isThirdPartyIntent get() = mIsPickImageIntent || mIsPickVideoIntent || mIsGetImageContentIntent ||
    mIsGetVideoContentIntent || mIsGetAnyContentIntent || mIsSetWallpaperIntent

private var currentMediaFragment: MediaFragment? = null

class MainActivity : SimpleActivity() {
    private var toggle: ActionBarDrawerToggle? = null

    private val isProApp: Boolean get(){
        val info: PackageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        val permissions: Array<String> = info.requestedPermissions //This array contains the requested permissions.
        return permissions.contains("android.permission.MANAGE_EXTERNAL_STORAGE")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mIsPickImageIntent = isPickImageIntent(intent)
        mIsPickVideoIntent = isPickVideoIntent(intent)
        mIsGetImageContentIntent = isGetImageContentIntent(intent)
        mIsGetVideoContentIntent = isGetVideoContentIntent(intent)
        mIsGetAnyContentIntent = isGetAnyContentIntent(intent)
        mIsSetWallpaperIntent = isSetWallpaperIntent(intent)
        mAllowPickingMultiple = intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        mIsThirdPartyIntent = isThirdPartyIntent
        //config.showAll = false

        appLaunched(BuildConfig.APPLICATION_ID)
        updateWidgets()
        registerFileUpdateListener()
        startSettingsScanner()

        setupDrawerLayout()
        handleStoragePermission()

        if (savedInstanceState == null) {
            if (config.showAll) showAllImages() else showDirectories()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            config.temporarilyShowHidden = false
            config.tempSkipDeleteConfirmation = false
            unregisterFileUpdateListener()
            if (!config.showAll) GalleryDatabase.destroyInstance()
        }
    }

    override fun onResume() {
        super.onResume()
        checkDefaultSpamFolders()
    }

    @SuppressLint("MissingSuperCall")
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(WAS_PROTECTION_HANDLED, mWasProtectionHandled)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mWasProtectionHandled = savedInstanceState.getBoolean(WAS_PROTECTION_HANDLED, false)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(toggle != null && toggle!!.onOptionsItemSelected(item)){
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun disableDrawerLayout(){
        toggle = null
        val color = baseConfig.primaryColor.getContrastColor()
        val drawableId = R.drawable.ic_arrow_left_vector
        val icon = resources.getColoredDrawableWithColor(drawableId, color)
        supportActionBar?.setHomeAsUpIndicator(icon)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    private fun setupDrawerLayout(){
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        toggle = ActionBarDrawerToggle(this, drawerLayout, 0,0)
        drawerLayout.addDrawerListener(toggle!!)
        toggle!!.syncState()

        findViewById<NavigationView>(R.id.navView)
            .setNavigationItemSelectedListener (::onNavigationItemSelected)

        val navView = findViewById<NavigationView>(R.id.navView)
        val navMenu = navView.menu
        navMenu.findItem(R.id.download).isVisible = !isFullApp
    }

    private fun onNavigationItemSelected(item: MenuItem):Boolean{
        when(item.itemId){
            R.id.folders -> showDirectories()
            R.id.all_images -> showAllImages()
            R.id.favorites -> showFavorites()
            R.id.recycle_bin -> showRecyclerBin()
            R.id.tasks -> task()
            R.id.download -> downloadFullApp()
            R.id.settings -> launchSettings()
            R.id.about -> launchAbout()
        }
        return true
    }

    private fun task(){
        startActivity(Intent(this, TasksActivity::class.java))
    }

    private fun downloadFullApp(){
        launchViewIntent(FULL_APP_LINK)
    }

    private var directoriesFragment: Fragment? = null

    private fun showDirectories(){
        config.showAll = false

        if(directoriesFragment == null)
            directoriesFragment = DirectoryFragment()
        showFragment(DirectoryFragment())
    }

    private fun showAllImages(){
        config.showAll = true
        showMediaFragment("")
    }

    private fun showFavorites(){
        config.showAll = false
        showMediaFragment(FAVORITES)
    }

    private fun showRecyclerBin(){
        config.showAll = false
        showMediaFragment(RECYCLE_BIN)
    }

    private fun showMediaFragment(dirName: String){
        currentMediaFragment?.clearAdapter()
        val fragment = MediaFragment()
        currentMediaFragment = fragment
        val bundle = Bundle()
        bundle.putString(DIRECTORY, dirName)
        fragment.arguments = bundle
        showFragment(fragment)
    }

    private fun showFragment(fragment: Fragment){
        supportFragmentManager.beginTransaction()
            .replace(R.id.mainContent, fragment)
            .commit()
    }

    private fun handleStoragePermission() {
        actionOnPermission = null
        if (hasStoragePermission)
            return
        if (isRPlus() && isProApp && baseConfig.appRunCount <= 5) {
            requestManageAllFilesPermission()
        } else {
            handlePermission(PERMISSION_WRITE_STORAGE) {hasPermission ->
                if (hasPermission) return@handlePermission
                toast(R.string.no_storage_permissions)
                finish()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun requestManageAllFilesPermission() {
        ConfirmationAdvancedDialog(this, "", R.string.access_storage_prompt, R.string.ok, 0) { success ->
            if (success) {
                isAskingPermissions = true
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.data = Uri.parse("package:$packageName")
                    startActivityForResult(intent, MANAGE_STORAGE_RC)
                } catch (e: Exception) {
                    showErrorToast(e)
                    val intent = Intent()
                    intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    startActivityForResult(intent, MANAGE_STORAGE_RC)
                }
            }
        }
    }

}
