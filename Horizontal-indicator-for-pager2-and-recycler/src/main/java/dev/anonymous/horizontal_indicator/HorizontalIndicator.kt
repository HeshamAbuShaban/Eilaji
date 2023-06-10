package dev.anonymous.horizontal_indicator

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.viewpager2.widget.ViewPager2
import dev.anonymous.horizontal_indicator.databinding.LayoutHorizontalIndicatorBinding

class HorizontalIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    private lateinit var binding: LayoutHorizontalIndicatorBinding

    private var indicatorSize = 20
    private var indicatorMargin = 15
    private var indicatorRadius = indicatorSize * 0.5f

    // Determines how large indicators will be.(scale by indicatorSize)
    private var indicatorWidthScale = 1
    private var selectedIndicatorWidthScale = 2

    private var indicatorColor = ContextCompat.getColor(context, R.color.light_gray)
    private var selectedIndicatorColor = ContextCompat.getColor(context, R.color.light_blue)

    private var indicators = mutableListOf<ImageView>()

    init {
        initializeView(context)
        getAttrs(attrs)
    }

    private fun initializeView(context: Context) {
        binding = LayoutHorizontalIndicatorBinding.inflate(LayoutInflater.from(context), this, true)
    }

    private fun getAttrs(attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.HorizontalIndicator)
        setTypedArray(typedArray)
    }

    private fun setTypedArray(typedArray: TypedArray) {
        indicatorSize = typedArray.getDimensionPixelSize(
            R.styleable.HorizontalIndicator_horizontalIndicatorSize,
            indicatorSize
        )
        indicatorRadius = typedArray.getDimensionPixelSize(
            R.styleable.HorizontalIndicator_indicatorRadius,
            (indicatorSize * 0.5f).toInt()
        ).toFloat()
        selectedIndicatorWidthScale = typedArray.getInt(
            R.styleable.HorizontalIndicator_selectedIndicatorWidthScale,
            selectedIndicatorWidthScale
        )
        indicatorWidthScale =
            typedArray.getInt(
                R.styleable.HorizontalIndicator_indicatorWidthScale,
                indicatorWidthScale
            )
        indicatorMargin = typedArray.getDimensionPixelSize(
            R.styleable.HorizontalIndicator_indicatorMargin,
            indicatorMargin
        )

        indicatorColor =
            typedArray.getColor(R.styleable.HorizontalIndicator_indicatorColor, indicatorColor)
        selectedIndicatorColor = typedArray.getColor(
            R.styleable.HorizontalIndicator_selectedIndicatorColor,
            selectedIndicatorColor
        )

        typedArray.recycle()
    }


    /**
     * @param viewPager2 A [ViewPager2] which [HorizontalIndicator] refers to.
     * @param startPosition A start position of [ViewPager2].
     */
    fun setupViewPager2(viewPager2: ViewPager2, startPosition: Int) {
        val itemCount = viewPager2.adapter?.itemCount ?: 0
        for (i in 0 until itemCount) addIndicator(i, startPosition)

        viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)

                val absoluteOffset = position + positionOffset
                val firstIndex = kotlin.math.floor(absoluteOffset).toInt()
                val secondIndex = kotlin.math.ceil(absoluteOffset).toInt()

                // To fix an ui problem which occurs when firstIndex is same as secondIndex
                if (firstIndex != secondIndex) {
                    refreshTwoIndicatorColor(firstIndex, secondIndex, positionOffset)
                    refreshTwoIndicatorWidth(firstIndex, secondIndex, positionOffset)
                }
            }
        })
    }

    private fun addIndicator(index: Int, selectedPosition: Int) {
        val indicator = LayoutInflater.from(context).inflate(R.layout.layout_indicator, this, false)

        // Set color
        val imageView = indicator.findViewById<ImageView>(R.id.image_indicator)
        imageView.background = (imageView.background as GradientDrawable).apply {
            setColor(if (index == selectedPosition) selectedIndicatorColor else indicatorColor)
        }

        // Set size, margin
        val params = imageView.layoutParams as LinearLayout.LayoutParams
        params.height = indicatorSize
        params.width =
            if (index == selectedPosition) selectedIndicatorWidthScale * indicatorSize else indicatorWidthScale * indicatorSize
        params.setMargins(indicatorMargin, 0, 0, 0)
        params.setMargins(0, 0, indicatorMargin, 0)

        // Set cornerRadius to the rectangle background then it will be shown as a circle
        (imageView.background as GradientDrawable).cornerRadius = indicatorRadius

        indicators.add(imageView)
        binding.layout.addView(indicator)
    }

    private fun refreshTwoIndicatorColor(
        firstIndex: Int,
        secondIndex: Int,
        positionOffset: Float
    ) {
        // Blend selectedIndicatorColor and indicatorColor by positionOffset
        val firstColor =
            ColorUtils.blendARGB(selectedIndicatorColor, indicatorColor, positionOffset)
        val secondColor =
            ColorUtils.blendARGB(indicatorColor, selectedIndicatorColor, positionOffset)

        indicators[firstIndex].background =
            (indicators[firstIndex].background as GradientDrawable).apply { setColor(firstColor) }
        indicators[secondIndex].background =
            (indicators[secondIndex].background as GradientDrawable).apply { setColor(secondColor) }
    }

    private fun refreshTwoIndicatorWidth(
        firstIndex: Int,
        secondIndex: Int,
        positionOffset: Float
    ) {
        val firstWidth = calculateWidthByOffset(positionOffset)
        val secondWidth = calculateWidthByOffset(1 - positionOffset)

        val firstParams = indicators[firstIndex].layoutParams
        val secondParams = indicators[secondIndex].layoutParams
        firstParams.width = firstWidth.toInt()
        secondParams.width = secondWidth.toInt()

        indicators[firstIndex].layoutParams = firstParams
        indicators[secondIndex].layoutParams = secondParams
    }

    private fun calculateWidthByOffset(offset: Float) =
        indicatorSize * indicatorWidthScale * offset + indicatorSize * selectedIndicatorWidthScale * (1 - offset)
}