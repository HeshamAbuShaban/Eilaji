package dev.anonymous.eilaji.ui.other.medicine

import android.app.Activity
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.adapters.AboutMedicationAdapter
import dev.anonymous.eilaji.databinding.FragmentMedicineBinding
import dev.anonymous.eilaji.utils.DummyData
import dev.anonymous.eilaji.utils.UtilsScreen
import net.lucode.hackware.magicindicator.ViewPagerHelper
import net.lucode.hackware.magicindicator.buildins.UIUtil
import net.lucode.hackware.magicindicator.buildins.commonnavigator.CommonNavigator
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.CommonNavigatorAdapter
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerIndicator
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerTitleView
import net.lucode.hackware.magicindicator.buildins.commonnavigator.indicators.LinePagerIndicator
import net.lucode.hackware.magicindicator.buildins.commonnavigator.titles.ClipPagerTitleView
import kotlin.math.abs


class MedicineFragment : Fragment() {
    private var _binding: FragmentMedicineBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MedicineViewModel

    private var menuIcon: Drawable? = null
    private var isFavorite = false
    private var numMedicines = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMedicineBinding.inflate(inflater, container, false)

        viewModel = ViewModelProvider(this)[MedicineViewModel::class.java]

        setUpToolbar()

        binding.fabAddToFavorite.setOnClickListener {
            isFavorite = !isFavorite
            binding.fabAddToFavorite.setImageResource(
                if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            )
        }

        binding.buIncrement.setOnClickListener {
            if (numMedicines < 9) {
                numMedicines++
                binding.tvNumMedicines.text = numMedicines.toString()
            }
        }

        binding.buDecrease.setOnClickListener {
            if (numMedicines > 1) {
                numMedicines--
                binding.tvNumMedicines.text = numMedicines.toString()
            }
        }

        with(binding.onBoardingPager) {
            offscreenPageLimit = 3
            adapter = AboutMedicationAdapter(DummyData.listMedicineModels[0], context as Activity)
        }
        setUpMagicIndicator()

        return binding.root
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.medicine_menu, menu)
        menuIcon = menu.findItem(R.id.share_menu_item).icon
    }

    private fun setUpToolbar() {
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbarMedicine)
        setHasOptionsMenu(true);
        val actionBar: ActionBar? = (activity as AppCompatActivity).supportActionBar

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            val drawable: Drawable? = binding.toolbarMedicine.navigationIcon
            if (drawable != null) {
                binding.appBar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
                    if (abs(verticalOffset) == appBarLayout.totalScrollRange) {
                        filterDrawableColor(requireContext(), drawable, R.color.white)
                        if (menuIcon != null) filterDrawableColor(
                            requireContext(),
                            menuIcon!!,
                            R.color.white
                        )
                    } else if (verticalOffset == 0) {
                        filterDrawableColor(requireContext(), drawable, R.color.black)
                        if (menuIcon != null) filterDrawableColor(
                            requireContext(),
                            menuIcon!!,
                            R.color.black
                        )
                    }
                }
            }
        }
    }

    var mDataList: Array<String>? = arrayOf("ادوية بديلة", "التقييم", "الوصف")

    private fun setUpMagicIndicator() {
        binding.magicIndicator3.setBackgroundResource(R.drawable.shape_magic_indicator)
        val commonNavigator = CommonNavigator(context)
        commonNavigator.adapter = object : CommonNavigatorAdapter() {
            override fun getCount(): Int {
                return if (mDataList == null) 0 else mDataList!!.size
            }

            override fun getTitleView(context: Context, index: Int): IPagerTitleView {
                val pagerTitleView = ClipPagerTitleView(context)
                val textSize = UIUtil.dip2px(context, 16.0).toFloat()
                pagerTitleView.text = mDataList!![index]
                pagerTitleView.textSize = textSize
                pagerTitleView.textColor =
                    if (UtilsScreen.isDarkMode(context))
                        ContextCompat.getColor(context, R.color.white)
                    else
                        ContextCompat.getColor(context, R.color.black)
                pagerTitleView.clipColor = ContextCompat.getColor(context, R.color.white)
                pagerTitleView.setOnClickListener {
                    binding.onBoardingPager.currentItem = index
                }
                return pagerTitleView
            }

            override fun getIndicator(context: Context): IPagerIndicator {
                val indicator = LinePagerIndicator(context)
                // should be same xml value
                val navigatorHeight = UIUtil.dip2px(context, 40.0).toFloat()
                // should put paddingHorizontal in xml as a same value
                val borderWidth = UIUtil.dip2px(context, 2.0).toFloat()
                val lineHeight = navigatorHeight - 2 * borderWidth
                val roundRadius = UIUtil.dip2px(context, 7.0).toFloat()
                indicator.lineHeight = lineHeight
                indicator.roundRadius = roundRadius
                indicator.yOffset = borderWidth + 0f
                indicator.setColors(ContextCompat.getColor(context, R.color.primary_color))
                return indicator
            }
        }
        binding.magicIndicator3.navigator = commonNavigator
        ViewPagerHelper.bind(binding.magicIndicator3, binding.onBoardingPager)
        binding.onBoardingPager.currentItem = 2
    }

    private fun filterDrawableColor(context: Context, drawableIcon: Drawable, newColorId: Int) {
        drawableIcon.setColorFilter(
            ContextCompat.getColor(context, newColorId),
            PorterDuff.Mode.SRC_ATOP
        )
    }

    fun changeDrawableColor(context: Context, icon: Int, newColor: Int): Drawable {
        val mDrawable = ContextCompat.getDrawable(context, icon)!!.mutate()
        mDrawable.colorFilter = PorterDuffColorFilter(newColor, PorterDuff.Mode.SRC_IN)
        return mDrawable
    }
}