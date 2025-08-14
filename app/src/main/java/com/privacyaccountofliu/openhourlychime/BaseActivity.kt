package com.privacyaccountofliu.openhourlychime

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

@Suppress("DEPRECATION")
abstract class BaseActivity : AppCompatActivity() {

    protected lateinit var drawerLayout: DrawerLayout
    protected lateinit var navView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutResId())

        // 初始化导航抽屉组件
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        // 设置工具栏和导航监听
        setupNavigation()
        setupToolbar()

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    abstract fun getLayoutResId(): Int

    protected open fun setupToolbar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu) // 汉堡菜单图标
        }
    }

    protected fun setupNavigation() {
        navView.setNavigationItemSelectedListener { menuItem ->
            Log.d("Navigation", "$menuItem")
            handleNavigationItemSelected(menuItem.itemId)
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
        if (this is MainActivity) {
            navView.setCheckedItem(R.id.nav_home)
        } else if (this is SettingsActivity) {
            navView.setCheckedItem(R.id.nav_settings)
        }
    }

    private fun handleNavigationItemSelected(itemId: Int) {
        when (itemId) {
            R.id.nav_home -> navigateToHome()
            R.id.nav_settings -> navigateToSettings()
        }
    }

    private fun navigateToHome() {
        if (this !is MainActivity) {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            finish()
        }
    }

    private fun navigateToSettings() {
        if (this !is SettingsActivity) {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}