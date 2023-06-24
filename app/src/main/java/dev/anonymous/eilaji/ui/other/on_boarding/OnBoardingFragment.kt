package dev.anonymous.eilaji.ui.other.on_boarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.adapters.OnBoardingAdapter
import dev.anonymous.eilaji.databinding.FragmentOnBoardingBinding
import dev.anonymous.eilaji.storage.AppSharedPreferences
import dev.anonymous.eilaji.utils.DummyData
import dev.anonymous.eilaji.utils.UtilsAnimation
import dev.anonymous.eilaji.utils.UtilsScreen

class OnBoardingFragment : Fragment() {
    private lateinit var _binding: FragmentOnBoardingBinding
    private lateinit var onBoardingViewModel: OnBoardingViewModel
    private val binding get() = _binding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnBoardingBinding.inflate(inflater, container, false)
        onBoardingViewModel = ViewModelProvider(this)[OnBoardingViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupOnBoardingPager()
        setupNextArrowButton()
        animationProgressByCurrentPageValue()
        navToLoginController()
    }

    private fun navToLoginController() {
        onBoardingViewModel.navigateToLogin.observe(viewLifecycleOwner) { navigateToLogin ->
            if (navigateToLogin) {
                //Get the NavController  inside a fragment that is hosted within an activity with a NavHostFragment
                val navController = findNavController()
                // removes the onBoardingScreen of the back stack
                navController.popBackStack()
                // navigate to the Login with making sure there is no return cause of the line above
                navController.navigate(R.id.navigation_Login)
                //Set the sheared Value to True
                AppSharedPreferences.getInstance(requireContext()).doneWithOnBoarding()
                /*@Deprecated
                val loginFragment = FragmentLogin()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.mainActivityContainer, loginFragment)
                    .commitAllowingStateLoss()*/
            }
        }
    }

    private fun animationProgressByCurrentPageValue() {
        onBoardingViewModel.currentPage.observe(viewLifecycleOwner) { currentPage ->
            currentPage?.let {
                val isForward = currentPage > onBoardingViewModel.previousPage
                UtilsAnimation.animationProgress(
                    binding.circularProgressIndicator,
                    DummyData.listModelOnBoarding.size.toFloat(),
                    currentPage,
                    isForward
                )
                onBoardingViewModel.previousPage = currentPage
            }
        }
    }

    private fun setupOnBoardingPager() {
        val paddingHorizontal = (UtilsScreen.screenWidth * 0.08).toInt()
        with(binding.onBoardingPager) {
            setPadding(paddingHorizontal, 0, paddingHorizontal, 0)
            clipToPadding = false
            clipChildren = false
            offscreenPageLimit = 3
            getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            adapter = OnBoardingAdapter(DummyData.listModelOnBoarding)
            setPageTransformer(CompositePageTransformer().apply {
                addTransformer(MarginPageTransformer(paddingHorizontal))
            })
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    onBoardingViewModel.setCurrentPage(position)
                }
            })
        }
    }

    private fun setupNextArrowButton() {
        binding.buNextArrow.setOnClickListener {
            val currentItem = binding.onBoardingPager.currentItem + 1
            if (currentItem != DummyData.listModelOnBoarding.size) {
                binding.onBoardingPager.currentItem = currentItem
            } else {
                onBoardingViewModel.onNextButtonClicked()
            }
        }
    }
}
