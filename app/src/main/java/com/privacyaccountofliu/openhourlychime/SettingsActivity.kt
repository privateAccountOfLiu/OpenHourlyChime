package com.privacyaccountofliu.openhourlychime

import android.os.Bundle
import com.privacyaccountofliu.openhourlychime.model.SettingsFragment

class SettingsActivity : BaseActivity() {
    override fun getLayoutResId() = R.layout.activity_settings


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        navView.setCheckedItem(R.id.nav_home)
        setupToolbar()
        setupNavigation()


        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu) // 使用菜单图标
        }

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.popBackStackImmediate()) {
            return true
        }
        return super.onSupportNavigateUp()
    }

    override fun onResume() {
        super.onResume()
        // 每次返回时更新选中状态
        navView.setCheckedItem(R.id.nav_settings)
    }
}