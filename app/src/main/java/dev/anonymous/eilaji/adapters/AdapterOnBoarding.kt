package dev.anonymous.eilaji.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.anonymous.eilaji.models.ModelOnBoarding
import dev.anonymous.eilaji.adapters.AdapterOnBoarding.MyViewHolder
import dev.anonymous.eilaji.databinding.ItemOnBoardingBinding

class AdapterOnBoarding(private var listModels: ArrayList<ModelOnBoarding>) :
    RecyclerView.Adapter<MyViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding =
            ItemOnBoardingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val listModels = listModels[position]
        holder.bind(listModels)
    }

    override fun getItemCount(): Int {
        return listModels.size
    }

    class MyViewHolder(private var binding: ItemOnBoardingBinding) : RecyclerView.ViewHolder(
        binding.root
    ) {
        fun bind(model: ModelOnBoarding?) {
            binding.apply {
                ivOnBoarding.setImageResource(model!!.image)
                tvOnBoarding.text = model.text
            }
        }
    }
}