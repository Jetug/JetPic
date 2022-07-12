package com.simplemobiletools.gallery.pro.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.FAVORITES
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_STORAGE
import com.simplemobiletools.commons.helpers.WAS_PROTECTION_HANDLED
import com.simplemobiletools.gallery.pro.BuildConfig
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.data.databases.GalleryDatabase
import com.simplemobiletools.gallery.pro.data.extensions.*
import com.simplemobiletools.gallery.pro.data.extensions.context.startSettingsScanner
import com.simplemobiletools.gallery.pro.ui.fragments.DirectoryFragment
import com.simplemobiletools.gallery.pro.ui.fragments.MediaFragment
import com.simplemobiletools.gallery.pro.data.helpers.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
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

private var currentMediaFragment: MediaFragment? = null

class MainActivity : SimpleActivity() {
    private var toggle: ActionBarDrawerToggle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val createTime = measureTimeMillis {
            //launchDefault {
                val time = measureTimeMillis {
                    mIsPickImageIntent = isPickImageIntent(intent)
                    mIsPickVideoIntent = isPickVideoIntent(intent)
                    mIsGetImageContentIntent = isGetImageContentIntent(intent)
                    mIsGetVideoContentIntent = isGetVideoContentIntent(intent)
                    mIsGetAnyContentIntent = isGetAnyContentIntent(intent)
                    mIsSetWallpaperIntent = isSetWallpaperIntent(intent)
                    mAllowPickingMultiple = intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
                    mIsThirdPartyIntent = mIsPickImageIntent || mIsPickVideoIntent || mIsGetImageContentIntent ||
                        mIsGetVideoContentIntent || mIsGetAnyContentIntent || mIsSetWallpaperIntent
                    //config.showAll = false

                    val time2 = measureTimeMillis {
                         //async{
                             appLaunched(BuildConfig.APPLICATION_ID)
                         //}
                    }
                    Log.e(JET, "!!!!!!!!!!!!!!!!!!! $time2 ms")
                    updateWidgets()
                    registerFileUpdateListener()
                    startSettingsScanner()

                    //withContext(Main) {
                        setupDrawerLayout()
                    //}
                }
                Log.e(JET, "on Create background $time ms")
            //}
            handlePermissions()

            if (savedInstanceState == null) {
                if (config.showAll)
                    showAllImages()
                else
                    showDirectories()
            }
        }
        Log.e(JET, "on Create $createTime ms")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            config.temporarilyShowHidden = false
            config.tempSkipDeleteConfirmation = false
            unregisterFileUpdateListener()

            if (!config.showAll)
                GalleryDatabase.destroyInstance()
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
    }

    private fun onNavigationItemSelected(item: MenuItem):Boolean{
        when(item.itemId){
            R.id.settings -> launchSettings()
            R.id.about -> launchAbout()
            R.id.folders -> showDirectories()
            R.id.all_images -> showAllImages()
            R.id.favorites -> showFavorites()
            R.id.recycle_bin -> showRecyclerBin()
        }
        return true
    }

    private var directoriesFragment: Fragment? = null

    private fun showDirectories(){
        val time = measureTimeMillis {
            config.showAll = false

            if(directoriesFragment == null)
                directoriesFragment = DirectoryFragment()

            //showFragment(directoriesFragment!!)
            showFragment(DirectoryFragment())
        }
        Log.i(JET, "showDirectories() $time ms")
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

    private fun handlePermissions(){
        handlePermission(PERMISSION_WRITE_STORAGE) {
            if (!it) {
                toast(R.string.no_storage_permissions)
                finish()
            }
        }

        if (packageName.startsWith(PACKAGE_NAME_PRO))
            handleStoragePermission {}
    }
}
