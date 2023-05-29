package dev.anonymous.eilaji;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import dev.anonymous.eilaji.databinding.ItemOnBoardingBinding;


public class AdapterOnBoarding extends RecyclerView.Adapter<AdapterOnBoarding.MyViewHolder> {
    ArrayList<ModelOnBoarding> listModels;

    public AdapterOnBoarding(ArrayList<ModelOnBoarding> listModels) {
        this.listModels = listModels;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOnBoardingBinding binding = ItemOnBoardingBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ModelOnBoarding listModels = this.listModels.get(position);
        holder.bind(listModels);
    }

    @Override
    public int getItemCount() {
        return listModels.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        ItemOnBoardingBinding binding;

        public MyViewHolder(ItemOnBoardingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ModelOnBoarding model) {
            binding.ivOnBoarding.setImageResource(model.getImage());
            binding.tvOnBoarding.setText(model.getText());
        }
    }
}