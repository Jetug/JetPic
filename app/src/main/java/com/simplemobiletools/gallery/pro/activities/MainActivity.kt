package com.simplemobiletools.gallery.pro.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.FAVORITES
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_STORAGE
import com.simplemobiletools.commons.helpers.WAS_PROTECTION_HANDLED
import com.simplemobiletools.gallery.pro.BuildConfig
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.data.databases.GalleryDatabase
import com.simplemobiletools.gallery.pro.data.extensions.*
import com.simplemobiletools.gallery.pro.ui.fragments.DirectoryFragment
import com.simplemobiletools.gallery.pro.ui.fragments.MediaFragment
import com.simplemobiletools.gallery.pro.data.helpers.*
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
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

        val createTime = measureTimeMillis {
            setContentView(R.layout.activity_main)
            //Nav
            setupDrawerLayout()

            if (savedInstanceState == null) {
                setupGalleryFragment()
            }

            mIsPickImageIntent = isPickImageIntent(intent)
            mIsPickVideoIntent = isPickVideoIntent(intent)
            mIsGetImageContentIntent = isGetImageContentIntent(intent)
            mIsGetVideoContentIntent = isGetVideoContentIntent(intent)
            mIsGetAnyContentIntent = isGetAnyContentIntent(intent)
            mIsSetWallpaperIntent = isSetWallpaperIntent(intent)
            mAllowPickingMultiple = intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
            mIsThirdPartyIntent = mIsPickImageIntent || mIsPickVideoIntent || mIsGetImageContentIntent || mIsGetVideoContentIntent ||
                mIsGetAnyContentIntent || mIsSetWallpaperIntent

            appLaunched(BuildConfig.APPLICATION_ID)
            updateWidgets()
            registerFileUpdateListener()

            config.showAll = false

            if (packageName.startsWith(PACKAGE_NAME_PRO)) {
                handleStoragePermission {}
            }

            // just request the permission, tryLoadGallery will then trigger in onResume
            handlePermission(PERMISSION_WRITE_STORAGE) {
                if (!it) {
                    toast(R.string.no_storage_permissions)
                    finish()
                }
            }
        }
        Log.e(JET,"on Create $createTime ms")

//        launchDefault {
//            val time = measureTimeMillis {
//                val a1 = async { call() }.await()
//                val a2 = async { call() }.await()
//
//                Log.e(JET,"a1: ${a1}")
//                Log.e(JET,"a2: ${a2}")
//            }
//            Log.e(JET,"Total time $time ms")
//
//        }

    }

//    suspend fun call(): String{
//        delay(3000)
//        return "sd"
//    }

//    override fun onResume() {
//        super.onResume()
////        if (config.showAll) {
////            showAllMedia()
////        }
//    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            config.temporarilyShowHidden = false
            config.tempSkipDeleteConfirmation = false
            unregisterFileUpdateListener()

            if (!config.showAll) {
                GalleryDatabase.destroyInstance()
            }
        }
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
        //drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    private fun setupDrawerLayout(){
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        toggle = ActionBarDrawerToggle(this, drawerLayout, 0,0)
        drawerLayout.addDrawerListener(toggle!!)
        toggle!!.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val navView = findViewById<NavigationView>(R.id.navView)
        navView.setNavigationItemSelectedListener (::onNavigationItemSelected)
    }

    private fun onNavigationItemSelected(item: MenuItem):Boolean{
        when(item.itemId){
            R.id.settings -> launchSettings()
            R.id.about -> launchAbout()
            R.id.folders -> setupGalleryFragment()
            R.id.all_images -> showAllImagesFragment()
            R.id.favorites -> showFavoritesFragment()
            R.id.recycle_bin -> showRecyclerBinFragment()
        }
        return true
    }

    private fun setupGalleryFragment(){
        config.showAll = false
        val fragment = DirectoryFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.mainContent, fragment)//.addToBackStack(null)
            .commit()
    }

    private fun showAllImagesFragment(){
        config.showAll = true
//        currentMediaFragment?.clearAdapter()
        showMediaFragment("")
    }

    private fun showFavoritesFragment(){
        config.showAll = false
        showMediaFragment(FAVORITES)
    }

    private fun showRecyclerBinFragment(){
        config.showAll = false
        showMediaFragment(RECYCLE_BIN)
    }

    private fun showMediaFragment(folderName: String){
        currentMediaFragment?.clearAdapter()

        val fragment = MediaFragment()
        currentMediaFragment = fragment
        val bundle = Bundle()
        bundle.putString(DIRECTORY, folderName)
        fragment.arguments = bundle

        supportFragmentManager.beginTransaction()
            .replace(R.id.mainContent, fragment) //.addToBackStack(null)
            .commit()
    }
}
