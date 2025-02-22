package com.plcoding.doodlekong.ui.drawing

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.plcoding.doodlekong.databinding.ActivityDrawingBinding

class DrawingActivity: AppCompatActivity() {

    private lateinit var binding: ActivityDrawingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrawingBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}