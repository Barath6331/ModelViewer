package com.barath.modelviewer.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.barath.modelviewer.databinding.ItemModelPickerBinding
import com.barath.modelviewer.model.ModelItem

class ModelPickerAdapter(
    private val models: List<ModelItem>,
    private val onModelSelected: (ModelItem) -> Unit
) : RecyclerView.Adapter<ModelPickerAdapter.ModelViewHolder>() {

    inner class ModelViewHolder(val binding: ItemModelPickerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ModelItem) {
            binding.tvModelEmoji.text = item.iconEmoji
            binding.tvModelName.text = item.title
            binding.tvCategoryTag.text = item.category
            binding.tvModelDescription.text = item.description

            binding.root.setOnClickListener {
                onModelSelected(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModelViewHolder {
        val binding = ItemModelPickerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ModelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ModelViewHolder, position: Int) {
        holder.bind(models[position])
    }

    override fun getItemCount(): Int = models.size
}
