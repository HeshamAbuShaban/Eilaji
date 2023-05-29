package dev.anonymous.eilaji

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import dev.anonymous.eilaji.databinding.FragmentOnBoardingBinding

class FragmentOnBoarding : Fragment() {
    var binding: FragmentOnBoardingBinding? = null
    var currentPage = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOnBoardingBinding.inflate(layoutInflater, container, false)
        val paddingHorizontal = (UtilsScreen.screenWidth * 0.08).toInt()
        binding!!.onBoardingPager.setPadding(paddingHorizontal, 0, paddingHorizontal, 0)
        binding!!.onBoardingPager.clipToPadding = false
        binding!!.onBoardingPager.clipChildren = false
        binding!!.onBoardingPager.offscreenPageLimit = 3
        binding!!.onBoardingPager.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        val adapter = AdapterOnBoarding(DummyData.listModelOnBoarding)
        binding!!.onBoardingPager.adapter = adapter
        val transformer = CompositePageTransformer()
        transformer.addTransformer(MarginPageTransformer(paddingHorizontal))
        binding!!.onBoardingPager.setPageTransformer(transformer)
        binding!!.onBoardingPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val isForward = position > currentPage
                currentPage = position
                UtilsAnimation.animationProgress(
                    binding!!.circularProgressIndicator,
                    DummyData.listModelOnBoarding.size.toFloat(),
                    currentPage,
                    isForward
                )
            }
        })
        binding!!.buNextArrow.setOnClickListener { v: View? ->
            val currentItem = binding!!.onBoardingPager.currentItem + 1
            if (currentItem != DummyData.listModelOnBoarding.size) {
                binding!!.onBoardingPager.currentItem = currentItem
            } else {
                val loginFragment = FragmentLogin()
                val fm = parentFragmentManager
                val tr = fm.beginTransaction()
                tr.replace(R.id.mainActivityContainer, loginFragment)
                tr.commitAllowingStateLoss()
            }
        }
        return binding!!.root
    }

    companion object {
        fun newInstance(param1: String?, param2: String?): FragmentOnBoarding {
            val fragment = FragmentOnBoarding()
            val args = Bundle()

            fragment.arguments = args
            return fragment
        }
    }
}