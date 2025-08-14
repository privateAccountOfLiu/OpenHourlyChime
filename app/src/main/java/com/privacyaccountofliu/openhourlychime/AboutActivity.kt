package com.privacyaccountofliu.openhourlychime

import android.os.Bundle
import android.view.MenuItem

@Suppress("DEPRECATION")
class AboutActivity: BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
    }

    override fun getLayoutResId() = R.layout.activity_about

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun setupToolbar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true) // 汉堡菜单图标
        }
    }
}