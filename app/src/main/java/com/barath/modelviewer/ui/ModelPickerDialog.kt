package com.barath.modelviewer.ui

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import com.barath.modelviewer.databinding.LayoutModelPickerSheetBinding
import com.barath.modelviewer.model.ModelCatalog
import com.barath.modelviewer.model.ModelItem
import com.google.android.material.bottomsheet.BottomSheetDialog

class ModelPickerDialog(
    context: Context,
    private val onModelSelected: (ModelItem) -> Unit
) : BottomSheetDialog(context) {

    private val binding: LayoutModelPickerSheetBinding =
        LayoutModelPickerSheetBinding.inflate(layoutInflater)

    init {
        setContentView(binding.root)

        binding.rvModels.layoutManager = LinearLayoutManager(context)
        binding.rvModels.adapter = ModelPickerAdapter(ModelCatalog.AVAILABLE_MODELS) { selectedModel ->
            dismiss()
            onModelSelected(selectedModel)
        }

        binding.btnSheetClose.setOnClickListener {
            dismiss()
        }
    }
}
