package com.barath.modelviewer.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PointF
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.barath.modelviewer.R
import com.barath.modelviewer.databinding.ViewModelContainerBinding
import com.barath.modelviewer.model.GlbParser
import com.barath.modelviewer.model.ModelItem
import com.barath.modelviewer.util.Math3D
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ModelContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    val binding: ViewModelContainerBinding =
        ViewModelContainerBinding.inflate(LayoutInflater.from(context), this, true)

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    var modelItem: ModelItem? = null
        private set

    var isInteractionMode: Boolean = false
        private set

    var areLabelsVisible: Boolean = false
        private set

    var onCloseRequested: ((ModelContainerView) -> Unit)? = null

    private var modelNode: ModelNode? = null
    private val labelledEntities = mutableListOf<LabelledEntity>()
    private val projectedLabelItems = mutableListOf<ProjectedLabelItem>()

    // Transform tracking for 3D Interaction mode
    private var currentRotationX = 0f
    private var currentRotationY = 0f
    private var currentModelScale = 1.0f

    // 2D Container Gesture tracking
    private var lastTouchRawX = 0f
    private var lastTouchRawY = 0f
    private var initialContainerX = 0f
    private var initialContainerY = 0f
    private var isDraggingContainer = false

    // 3D Touch tracking
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    // Pinch scale detectors
    private val containerScaleDetector: ScaleGestureDetector
    private val model3DScaleDetector: ScaleGestureDetector

    private val minContainerSizeDp = 180f
    private val maxContainerSizeDp = 480f
    private val density = resources.displayMetrics.density

    private val tempPoint = PointF()
    private val projMatrix = DoubleArray(16)
    private val viewMatrix = DoubleArray(16)
    private val worldMatrix = FloatArray(16)

    private data class LabelledEntity(
        val entity: Int,
        val nodeName: String,
        val labelText: String
    )

    init {
        // Setup Container Scale Detector (for Normal mode pinch-to-resize)
        containerScaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (isInteractionMode) return false
                val scaleFactor = detector.scaleFactor
                val curW = layoutParams.width.toFloat()
                val curH = layoutParams.height.toFloat()

                val minPx = minContainerSizeDp * density
                val maxPx = maxContainerSizeDp * density

                val newW = (curW * scaleFactor).coerceIn(minPx, maxPx).toInt()
                val newH = (curH * scaleFactor).coerceIn(minPx, maxPx).toInt()

                if (newW != layoutParams.width || newH != layoutParams.height) {
                    layoutParams.width = newW
                    layoutParams.height = newH
                    requestLayout()
                    post { updateLabelProjections() }
                }
                return true
            }
        })

        // Setup 3D Model Scale Detector (for Interaction mode pinch-to-zoom)
        model3DScaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (!isInteractionMode) return false
                val scaleFactor = detector.scaleFactor
                currentModelScale = (currentModelScale * scaleFactor).coerceIn(0.25f, 4.0f)
                modelNode?.scale = Scale(currentModelScale)
                updateLabelProjections()
                return true
            }
        })

        setupButtons()
    }

    private fun setupButtons() {
        // 1. Interaction Mode Toggle Button
        binding.btnToggleInteraction.setOnClickListener {
            toggleInteractionMode()
        }

        // 2. Part Labels Toggle Button
        binding.btnToggleLabels.setOnClickListener {
            togglePartLabels()
        }

        // 3. Close Button
        binding.btnClose.setOnClickListener {
            onCloseRequested?.invoke(this)
        }
    }

    fun loadModel(item: ModelItem) {
        this.modelItem = item
        binding.tvModelTitle.text = "${item.iconEmoji} ${item.title}"
        binding.progressBarLoading.visibility = View.VISIBLE
        currentModelScale = item.defaultScale

        coroutineScope.launch {
            // Step 1: Parse GLB JSON chunk for extras.prop part labels off the main thread
            val parsedLabels = withContext(Dispatchers.IO) {
                try {
                    val stream = context.assets.open(item.assetPath)
                    GlbParser.parsePartLabels(stream)
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyMap<String, String>()
                }
            }

            // Step 2: Load Model in SceneView
            try {
                val modelInstance = binding.sceneView.modelLoader.createModelInstance(item.assetPath)
                if (modelInstance != null) {
                    val node = ModelNode(
                        modelInstance = modelInstance,
                        scaleToUnits = 1.0f,
                        centerOrigin = Position(0f, 0f, 0f)
                    )
                    modelNode = node
                    binding.sceneView.addChildNode(node)

                    // Step 3: Match labelled nodes with Filament entities
                    labelledEntities.clear()
                    val asset = modelInstance.asset
                    val entities = asset.entities
                    for (entity in entities) {
                        val entityName = asset.getName(entity)
                        if (entityName != null && parsedLabels.containsKey(entityName)) {
                            val labelText = parsedLabels[entityName] ?: entityName
                            labelledEntities.add(
                                LabelledEntity(
                                    entity = entity,
                                    nodeName = entityName,
                                    labelText = labelText
                                )
                            )
                        }
                    }

                    binding.tvLabelStatus.text = "Labels: ${labelledEntities.size} available"
                    binding.progressBarLoading.visibility = View.GONE

                    post { updateLabelProjections() }
                } else {
                    binding.progressBarLoading.visibility = View.GONE
                    binding.tvLabelStatus.text = "Failed to load model"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                binding.progressBarLoading.visibility = View.GONE
                binding.tvLabelStatus.text = "Error loading 3D asset"
            }
        }
    }

    fun toggleInteractionMode() {
        isInteractionMode = !isInteractionMode
        if (isInteractionMode) {
            binding.containerRoot.setBackgroundResource(R.drawable.bg_container_active)
            binding.btnToggleInteraction.setBackgroundResource(R.drawable.bg_action_btn_active)
            binding.btnToggleInteraction.setColorFilter(Color.parseColor("#10B981"))
            binding.tvModeBadge.setBackgroundResource(R.drawable.bg_badge_interaction)
            binding.tvModeBadge.setTextColor(Color.parseColor("#10B981"))
            binding.tvModeBadge.text = "🔄 3D Orbit & Zoom"
        } else {
            binding.containerRoot.setBackgroundResource(R.drawable.bg_container_card)
            binding.btnToggleInteraction.setBackgroundResource(R.drawable.bg_action_btn)
            binding.btnToggleInteraction.setColorFilter(Color.parseColor("#94A3B8"))
            binding.tvModeBadge.setBackgroundResource(R.drawable.bg_badge_normal)
            binding.tvModeBadge.setTextColor(Color.parseColor("#38BDF8"))
            binding.tvModeBadge.text = "📍 Drag & Resize"
        }
    }

    fun togglePartLabels() {
        areLabelsVisible = !areLabelsVisible
        binding.labelOverlayView.areLabelsVisible = areLabelsVisible
        if (areLabelsVisible) {
            binding.btnToggleLabels.setBackgroundResource(R.drawable.bg_action_btn_active)
            binding.btnToggleLabels.setColorFilter(Color.parseColor("#38BDF8"))
            binding.tvLabelStatus.text = "Labels: Visible (${labelledEntities.size})"
            updateLabelProjections()
        } else {
            binding.btnToggleLabels.setBackgroundResource(R.drawable.bg_action_btn)
            binding.btnToggleLabels.setColorFilter(Color.parseColor("#94A3B8"))
            binding.tvLabelStatus.text = "Labels: Hidden"
        }
    }

    /**
     * Projects each 3D labelled entity onto 2D screen coordinates using Filament Camera matrices.
     */
    fun updateLabelProjections() {
        if (!areLabelsVisible || modelNode == null || labelledEntities.isEmpty()) return
        val viewW = binding.sceneView.width
        val viewH = binding.sceneView.height
        if (viewW <= 0 || viewH <= 0) return

        try {
            val camera = binding.sceneView.cameraNode.camera
            camera.getProjectionMatrix(projMatrix)
            camera.getViewMatrix(viewMatrix)

            val tm = binding.sceneView.engine.transformManager
            projectedLabelItems.clear()

            for (item in labelledEntities) {
                if (tm.hasComponent(item.entity)) {
                    val ti = tm.getInstance(item.entity)
                    tm.getWorldTransform(ti, worldMatrix)

                    val worldX = worldMatrix[12]
                    val worldY = worldMatrix[13]
                    val worldZ = worldMatrix[14]

                    val isFront = Math3D.projectWorldToScreen(
                        worldX = worldX,
                        worldY = worldY,
                        worldZ = worldZ,
                        viewMatrix = viewMatrix,
                        projMatrix = projMatrix,
                        viewWidth = viewW,
                        viewHeight = viewH,
                        outPoint = tempPoint
                    )

                    projectedLabelItems.add(
                        ProjectedLabelItem(
                            nodeName = item.nodeName,
                            labelText = item.labelText,
                            screenX = tempPoint.x,
                            screenY = tempPoint.y,
                            isVisible = isFront
                        )
                    )
                }
            }

            binding.labelOverlayView.updateLabels(projectedLabelItems)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // Allow header action buttons to receive clicks directly
        val header = binding.headerLayout
        val x = ev.x
        val y = ev.y
        if (x >= header.left && x <= header.right && y >= header.top && y <= header.bottom) {
            return false
        }
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        bringToFront()
        (parent as? View)?.invalidate()

        if (isInteractionMode) {
            // === INTERACTION MODE: 3D MODEL MANIPULATION ===
            model3DScaleDetector.onTouchEvent(event)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!model3DScaleDetector.isInProgress && event.pointerCount == 1) {
                        val dx = event.x - lastTouchX
                        val dy = event.y - lastTouchY

                        // 1-finger Drag rotates 3D model (Yaw & Pitch)
                        currentRotationY += dx * 0.45f
                        currentRotationX = (currentRotationX + dy * 0.45f).coerceIn(-85f, 85f)

                        modelNode?.rotation = Rotation(x = currentRotationX, y = currentRotationY, z = 0f)
                        updateLabelProjections()

                        lastTouchX = event.x
                        lastTouchY = event.y
                    }
                }
            }
            return true
        } else {
            // === NORMAL MODE: CONTAINER MANIPULATION ===
            containerScaleDetector.onTouchEvent(event)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchRawX = event.rawX
                    lastTouchRawY = event.rawY
                    initialContainerX = this.x
                    initialContainerY = this.y
                    isDraggingContainer = true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!containerScaleDetector.isInProgress && event.pointerCount == 1 && isDraggingContainer) {
                        val deltaX = event.rawX - lastTouchRawX
                        val deltaY = event.rawY - lastTouchRawY

                        val parentView = parent as? ViewGroup
                        val parentW = parentView?.width ?: resources.displayMetrics.widthPixels
                        val parentH = parentView?.height ?: resources.displayMetrics.heightPixels

                        val newX = (initialContainerX + deltaX).coerceIn(-dp(30f), (parentW - width + dp(30f)))
                        val newY = (initialContainerY + deltaY).coerceIn(0f, (parentH - height + dp(30f)))

                        this.x = newX
                        this.y = newY
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDraggingContainer = false
                }
            }
            return true
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        post { updateLabelProjections() }
    }

    private fun dp(value: Float): Float = value * density

    fun destroy() {
        try {
            modelNode?.let { node ->
                binding.sceneView.removeChildNode(node)
                node.destroy()
            }
            modelNode = null
            labelledEntities.clear()
            projectedLabelItems.clear()
            binding.sceneView.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
