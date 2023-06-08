package dev.anonymous.eilaji.ui.onBoarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.adapters.AdapterOnBoarding
import dev.anonymous.eilaji.databinding.FragmentOnBoardingBinding
import dev.anonymous.eilaji.storage.AppSharedPreferences
import dev.anonymous.eilaji.storage.database.EilajViewModel
import dev.anonymous.eilaji.storage.database.tables.Eilaj
import dev.anonymous.eilaji.ui.guard.login.FragmentLogin
import dev.anonymous.eilaji.utils.DummyData
import dev.anonymous.eilaji.utils.UtilsAnimation
import dev.anonymous.eilaji.utils.UtilsScreen

class FragmentOnBoarding : Fragment() {
    private var binding: FragmentOnBoardingBinding? = null
    private lateinit var onBoardingViewModel: OnBoardingViewModel
    private lateinit var viewModel: EilajViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOnBoardingBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[EilajViewModel::class.java]
        val invoked: Boolean = AppSharedPreferences.getInstance(requireContext()).isInvoked
        if (!invoked) {
            invokeCategories()
            AppSharedPreferences.getInstance(requireContext()).invokeDummyData(true)
        }

        setupOnBoardingPager()
        setupNextArrowButton()

        return binding!!.root
    }

    private fun invokeCategories() {
        viewModel.insertManyEilaj(
            Eilaj(1, "fever-pills", "Pills"),
            Eilaj(2, "stomach-pills", "Pills"),
            Eilaj(3, "headache-pills", "Pills"),
            Eilaj(4, "drugs-drinkable", "Liquids"),
            Eilaj(5, "eye-drops", "Liquids")
        )
    }

    private fun setupOnBoardingPager() {
        val paddingHorizontal = (UtilsScreen.screenWidth * 0.08).toInt()
        with(binding!!.onBoardingPager) {
            setPadding(paddingHorizontal, 0, paddingHorizontal, 0)
            clipToPadding = false
            clipChildren = false
            offscreenPageLimit = 3
            getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            adapter = AdapterOnBoarding(DummyData.listModelOnBoarding)
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
        binding!!.buNextArrow.setOnClickListener {
            val currentItem = binding!!.onBoardingPager.currentItem + 1
            if (currentItem != DummyData.listModelOnBoarding.size) {
                binding!!.onBoardingPager.currentItem = currentItem
            } else {
                onBoardingViewModel.onNextButtonClicked()
            }
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onBoardingViewModel = ViewModelProvider(this)[OnBoardingViewModel::class.java]

        onBoardingViewModel.currentPage.observe(viewLifecycleOwner) { currentPage ->
            currentPage?.let {
                val isForward = currentPage > onBoardingViewModel.previousPage
                UtilsAnimation.animationProgress(
                    binding!!.circularProgressIndicator,
                    DummyData.listModelOnBoarding.size.toFloat(),
                    currentPage,
                    isForward
                )
                onBoardingViewModel.previousPage = currentPage
            }
        }

        onBoardingViewModel.navigateToLogin.observe(viewLifecycleOwner) { navigateToLogin ->
            if (navigateToLogin) {
                val loginFragment = FragmentLogin()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.mainActivityContainer, loginFragment)
                    .commitAllowingStateLoss()
            }
        }
    }
}
