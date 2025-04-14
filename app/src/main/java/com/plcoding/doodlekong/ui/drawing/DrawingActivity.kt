package com.plcoding.doodlekong.ui.drawing


import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.plcoding.doodlekong.R
import com.plcoding.doodlekong.databinding.ActivityDrawingBinding
import com.plcoding.doodlekong.utils.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt

@AndroidEntryPoint
class DrawingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDrawingBinding
     private val viewModel: DrawingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrawingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        subscribeToUiStateUpdates()

        binding.colorGroup.setOnCheckedChangeListener { _, checkedId ->
            viewModel.checkRadioButton(
                DrawingViewModel.ColorResourceId(checkedId)
            )
        }
    }

    private fun setColor(color: Int) {
        binding.drawingView.setColor(color)
        binding.drawingView.setThickness(Constants.DEFAULT_PAINT_THICKNESS)

    }

    private fun subscribeToUiStateUpdates() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedColorButtonId.collect {
                    binding.colorGroup.check(it.id)

                    when(it.id) {
                       R.id.rbRed -> setColor(Color.RED)
                       R.id.rbGreen -> setColor(Color.GREEN)
                       R.id.rbBlue -> setColor(Color.BLUE)
                       R.id.rbYellow -> setColor(Color.YELLOW)
                       R.id.rbOrange -> {
                           setColor(ContextCompat.getColor(this@DrawingActivity, R.color.orange))
                       }
                       R.id.rbBlack -> setColor(Color.BLACK)
                       R.id.rbEraser -> {
                           binding.drawingView.setColor(Color.WHITE)
                           binding.drawingView.setThickness(40f)
                       }
                    }
                }
            }
        }
    }
}