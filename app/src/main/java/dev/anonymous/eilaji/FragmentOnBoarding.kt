package dev.anonymous.eilaji;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import dev.anonymous.eilaji.databinding.FragmentOnBoardingBinding;

public class FragmentOnBoarding extends Fragment {
    FragmentOnBoardingBinding binding;
    int currentPage = 0;




    public FragmentOnBoarding() {
        // Required empty public constructor
    }

    public static FragmentOnBoarding newInstance(String param1, String param2) {
        FragmentOnBoarding fragment = new FragmentOnBoarding();
        Bundle args = new Bundle();
//
        //\\
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentOnBoardingBinding.inflate(getLayoutInflater(), container, false);


        int paddingHorizontal = (int) (UtilsScreen.getScreenWidth() * 0.08);
        binding.onBoardingPager.setPadding(paddingHorizontal, 0, paddingHorizontal, 0);

        binding.onBoardingPager.setClipToPadding(false);
        binding.onBoardingPager.setClipChildren(false);
        binding.onBoardingPager.setOffscreenPageLimit(3);
        binding.onBoardingPager.getChildAt(0).setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);

        AdapterOnBoarding adapter = new AdapterOnBoarding(DummyData.getListModelOnBoarding());
        binding.onBoardingPager.setAdapter(adapter);

        CompositePageTransformer transformer = new CompositePageTransformer();
        transformer.addTransformer(new MarginPageTransformer(paddingHorizontal));
        binding.onBoardingPager.setPageTransformer(transformer);

        binding.onBoardingPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                boolean isForward = position > currentPage;
                currentPage = position;

                UtilsAnimation.animationProgress(
                        binding.circularProgressIndicator,
                        DummyData.getListModelOnBoarding().size(),
                        currentPage,
                        isForward
                );
            }
        });

        binding.buNextArrow.setOnClickListener(v -> {
            int currentItem = binding.onBoardingPager.getCurrentItem() + 1;
            if (currentItem != DummyData.getListModelOnBoarding().size()) {
                binding.onBoardingPager.setCurrentItem(currentItem);
            } else {
//                startActivity(new Intent(this, MainActivity2.class));
//                finish();
            }
        });

        return binding.getRoot();
    }
}