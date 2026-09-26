package com.barath.modelviewer.ui

import android.content.Context
import com.barath.modelviewer.databinding.DialogGestureGuideBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

class GuideDialog(context: Context) : BottomSheetDialog(context) {

    private val binding: DialogGestureGuideBinding =
        DialogGestureGuideBinding.inflate(layoutInflater)

    init {
        setContentView(binding.root)
        binding.btnGuideClose.setOnClickListener {
            dismiss()
        }
    }
}
