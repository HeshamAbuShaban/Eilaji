package dev.anonymous.eilaji.adapters.server

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.anonymous.eilaji.databinding.ItemPharmacyDepartmentBinding
import dev.anonymous.eilaji.models.server.Category
import dev.anonymous.eilaji.utils.GeneralUtils

class CategoryAdapter(private var categoryList: ArrayList<Category>) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding =
            ItemPharmacyDepartmentBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return CategoryViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return categoryList.size
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categoryList[position]
        holder.bind(category, position, categoryList.size)
    }

    class CategoryViewHolder(private var binding: ItemPharmacyDepartmentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(model: Category?, position: Int, listSize: Int) {
            binding.apply {
                if (position == 0 || position == 1) {
                    // margin top first tow item
                    parentPharDep.setPadding(0, 60, 0, 0)
                } else if (position == listSize - 2 || position == listSize - 1) {
                    // margin bottom last tow item
                    parentPharDep.setPadding(0, 0, 0, 222)
                }

                model?.let {
                    GeneralUtils.getInstance().loadImage(it.imageUrl).into(ivPharmacyDepartment)
                    tvPharmacyDepartment.text = it.title
                }
            }
        }
    }

}