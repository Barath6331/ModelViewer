package com.barath.modelviewer

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.barath.modelviewer.databinding.ActivityMainBinding
import com.barath.modelviewer.model.ModelCatalog
import com.barath.modelviewer.model.ModelItem
import com.barath.modelviewer.ui.GuideDialog
import com.barath.modelviewer.ui.ModelContainerView
import com.barath.modelviewer.ui.ModelPickerDialog
import com.barath.modelviewer.util.PerformanceTracker

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val activeContainers = mutableListOf<ModelContainerView>()
    private var performanceTracker: PerformanceTracker? = null

    private val density by lazy { resources.displayMetrics.density }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupListeners()
        setupPerformanceTracker()
        updateUiState()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.topToolbar.setPadding(
                dp(14f).toInt(),
                systemBars.top + dp(6f).toInt(),
                dp(14f).toInt(),
                dp(8f).toInt()
            )
            binding.fabAddModel.translationY = -systemBars.bottom.toFloat()
            insets
        }
    }

    private fun setupListeners() {
        binding.fabAddModel.setOnClickListener {
            showModelPicker()
        }

        binding.btnEmptyAdd.setOnClickListener {
            showModelPicker()
        }

        binding.btnQuickAddAll.setOnClickListener {
            addAllModels()
        }

        binding.btnClearAll.setOnClickListener {
            clearAllModels()
        }

        binding.btnHelp.setOnClickListener {
            GuideDialog(this).show()
        }
    }

    private fun setupPerformanceTracker() {
        performanceTracker = PerformanceTracker { fps, ramMb ->
            binding.tvPerformanceStats.text = "⚡ $fps FPS • ${ramMb}MB"
            // Color code FPS: Green for 45+, Yellow for 25-45, Red for < 25
            val color = when {
                fps >= 45 -> "#10B981"
                fps >= 25 -> "#FBBF24"
                else -> "#EF4444"
            }
            binding.tvPerformanceStats.setTextColor(android.graphics.Color.parseColor(color))
        }
    }

    private fun showModelPicker() {
        ModelPickerDialog(this) { selectedModel ->
            addModelToCanvas(selectedModel)
        }.show()
    }

    private fun addModelToCanvas(modelItem: ModelItem) {
        val container = ModelContainerView(this)
        val containerWidth = dp(270f).toInt()
        val containerHeight = dp(310f).toInt()

        val params = FrameLayout.LayoutParams(containerWidth, containerHeight)
        val index = activeContainers.size
        val screenW = resources.displayMetrics.widthPixels
        val screenH = resources.displayMetrics.heightPixels

        // Cascading staggered placement across the canvas
        val offsetX = (dp(24f) + (index % 4) * dp(36f)).coerceAtMost(screenW - containerWidth - dp(20f))
        val offsetY = (dp(80f) + (index % 4) * dp(48f)).coerceAtMost(screenH - containerHeight - dp(90f))

        container.x = offsetX
        container.y = offsetY
        container.layoutParams = params

        container.onCloseRequested = { closingContainer ->
            removeModelFromCanvas(closingContainer)
        }

        binding.canvasContainer.addView(container)
        activeContainers.add(container)

        container.loadModel(modelItem)
        updateUiState()

        Toast.makeText(this, "Loaded ${modelItem.title}", Toast.LENGTH_SHORT).show()
    }

    private fun addAllModels() {
        val available = ModelCatalog.AVAILABLE_MODELS
        for (model in available) {
            addModelToCanvas(model)
        }
    }

    private fun removeModelFromCanvas(container: ModelContainerView) {
        container.destroy()
        binding.canvasContainer.removeView(container)
        activeContainers.remove(container)
        updateUiState()
    }

    private fun clearAllModels() {
        if (activeContainers.isEmpty()) return
        for (container in activeContainers) {
            container.destroy()
            binding.canvasContainer.removeView(container)
        }
        activeContainers.clear()
        updateUiState()
        Toast.makeText(this, "All models closed", Toast.LENGTH_SHORT).show()
    }

    private fun updateUiState() {
        val count = activeContainers.size
        binding.tvModelCountBadge.text = "$count Active"
        binding.layoutEmptyState.visibility = if (count == 0) View.VISIBLE else View.GONE
    }

    private fun dp(value: Float): Float = value * density

    override fun onResume() {
        super.onResume()
        performanceTracker?.start()
    }

    override fun onPause() {
        super.onPause()
        performanceTracker?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        performanceTracker?.stop()
        for (container in activeContainers) {
            container.destroy()
        }
        activeContainers.clear()
    }
}