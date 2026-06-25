package com.goodchair.launcher

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.Slider

import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("launcher_prefs", MODE_PRIVATE)
        
        val hideSystemSwitch = findViewById<SwitchMaterial>(R.id.hide_system_apps_switch)
        hideSystemSwitch.isChecked = prefs.getBoolean("hide_system_apps", false)
        hideSystemSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("hide_system_apps", isChecked).apply()
        }

        val slider = findViewById<Slider>(R.id.column_slider)
        
        slider.value = prefs.getInt("grid_columns", 4).toFloat()
        slider.addOnChangeListener { _, value, _ ->
            prefs.edit().putInt("grid_columns", value.toInt()).apply()
        }
    }
}