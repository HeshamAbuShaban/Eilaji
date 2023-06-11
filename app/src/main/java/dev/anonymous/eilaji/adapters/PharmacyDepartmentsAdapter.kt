package dev.anonymous.eilaji.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.anonymous.eilaji.databinding.ItemPharmacyDepartmentBinding
import dev.anonymous.eilaji.models.PharmacyDepartmentModel

class PharmacyDepartmentsAdapter(private var listAds: ArrayList<PharmacyDepartmentModel>) :
    RecyclerView.Adapter<PharmacyDepartmentsAdapter.PharmacyDepartmentsViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PharmacyDepartmentsViewHolder {
        val binding =
            ItemPharmacyDepartmentBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return PharmacyDepartmentsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PharmacyDepartmentsViewHolder, position: Int) {
        val listModels = listAds[position]
        holder.bind(listModels, position, listAds.size)
    }

    override fun getItemCount(): Int {
        return listAds.size
    }

    class PharmacyDepartmentsViewHolder(private var binding: ItemPharmacyDepartmentBinding) :
        RecyclerView.ViewHolder(
            binding.root
        ) {
        fun bind(model: PharmacyDepartmentModel?, position: Int, listSize: Int) {
            binding.apply {
                if (position == 0 || position == 1) {
                    // margin top first tow item
                    parentPharDep.setPadding(0, 60, 0, 0)
                } else if (position == listSize - 2 || position == listSize - 1) {
                    // margin bottom last tow item
                    parentPharDep.setPadding(0, 0, 0, 222)
                }

                model?.let {
                    ivPharmacyDepartment.setImageResource(it.image)
                    tvPharmacyDepartment.text = it.name
                }
            }
        }
    }
}