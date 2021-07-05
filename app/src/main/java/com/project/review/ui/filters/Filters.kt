package com.project.review.ui.filters

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.project.review.R
import com.project.review.databinding.FiltersBinding
import com.project.review.models.Product
import com.project.review.view_models.ReviewViewModel
import com.project.review.settings.Marketplace
import com.project.review.ui.recyclerview_adapters.FilterWordAdapter
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.android.material.color.MaterialColors
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.RangeSlider
import com.project.review.models.Review
import com.project.review.settings.Settings
import java.util.concurrent.locks.ReentrantLock

/**
 * properties of recurring words
 */
data class FilterWord(val name: String, var checked: Boolean = false)


/**
 * shows the possible ways of sorting reviews
 */
class RecyclerViewFilters {

    enum class OrderBy {
        HIGH_TO_LOW, LOW_TO_HIGH, DATE, INV_DATE;

        companion object {
            fun getName(type: OrderBy, context: Context): String {
                return when (type) {
                    HIGH_TO_LOW -> context.getString(R.string.high_to_low)
                    LOW_TO_HIGH -> context.getString(R.string.low_to_high)
                    DATE -> context.getString(R.string.most_recent)
                    INV_DATE -> context.getString(R.string.less_recent)
                }
            }
        }
    }
}

/**
 * shows filters for reviews
 */
class Filters : Fragment(), FilterWordAdapter.Actions {
    private lateinit var binding: FiltersBinding
    val reviewModel: ReviewViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.filters, container, false)
        binding = FiltersBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // SET THE SLIDER TO FILTER BY EVALUATION
        setRangeSlider()

        // SET THE GRAPH THAT APPEARS ABOVE THE RANGESLIDER
        setChart()

        // SET THE METHODS TO CALL BY CLICKING BUTTONS PRESENT IN THE CURRENT VIEW
        setClickListener()

        /* SET FILTER NAME TO SORT REVIEWS ACCORDING TO DEFINED CRITERIA
        FROM ENUM CLASS OrderBy */
        setOrderByText()

        // SET THE METHODS TO UPDATE THE LIST OF RECURRING WORDS
        setPopularWords()

        // SET THE METHODS TO UPDATE THE LIST OF MARKETPLACES WHERE THE PRODUCT WAS FOUND
        listenToAvailableMarketplaces()


    }


    private val createdMarketplaceCheckbox = mutableSetOf<Marketplace>()

    /**
     * verifies that there is always at least one selected marketplace to show reviews for
     *
     * also manages the list of excluded marketplaces
     */
    private fun setOnMarkteplaceSelected(view: CheckBox, marketplace: Marketplace) {
        view.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(p0: CompoundButton?, p1: Boolean) {
                val remainingMarketplaces = availableMarketplaces.size - excludedMarketplaces.size
                if (!p1 && remainingMarketplaces == 1) {
                    view.isChecked = true
                } else {
                    if (p1 && excludedMarketplaces.contains(marketplace)) {
                        excludedMarketplaces.remove(marketplace)
                    } else {
                        excludedMarketplaces.add(marketplace)
                    }
                    reviewModel.setExcludedMarketplace()
                }

            }
        })
    }

    var availableMarketplaces = mutableListOf<Marketplace>()
    lateinit var excludedMarketplaces: MutableSet<Marketplace>

    /**
     * injects RadioButtons, as many as there are marketplaces that have found the product
     */
    private fun setAvailableMarketplaces(product: Product) {
        val group = binding.marketplaceList
        group.visibility = View.VISIBLE
        availableMarketplaces = product.marketplace

        for (marketplace in availableMarketplaces) {
            if (!createdMarketplaceCheckbox.contains(marketplace)) {
                val view: CheckBox =
                    layoutInflater.inflate(
                        R.layout.filter_for_marketplace_checkbox,
                        group,
                        false
                    ) as CheckBox
                view.text = marketplace.name
                view.isChecked = !excludedMarketplaces.contains(marketplace)
                group.addView(view)
                this.setOnMarkteplaceSelected(view, marketplace)
                createdMarketplaceCheckbox.add(marketplace)
            }
        }
    }

    private fun listenToAvailableMarketplaces() {

        if (reviewModel.getCurrentProduct() != null)
            this.setAvailableMarketplaces(reviewModel.getCurrentProduct()!!)

        reviewModel.currentProduct.observe(viewLifecycleOwner, {
            if (it != null)
                this.setAvailableMarketplaces(product = it)
        })
    }

    private fun setPopularWords() {
        val recyclerView = binding.wordsWallet
        val gridLinearManager = GridLayoutManager(context, 2)
        recyclerView.layoutManager = gridLinearManager
        val recyclerViewAdapter = FilterWordAdapter(this)
        recyclerView.adapter = recyclerViewAdapter


        reviewModel._popularWords.observe(viewLifecycleOwner, {
            lock.lock()
            recyclerViewAdapter.submitList(it)
            lock.unlock()
            this.onClick(it)
        })


    }


    private fun setOrderByText() {
        binding.orderByText.text =
            reviewModel.getOrderByText()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == RESULT_OK) {
            reviewModel.setOrderBy(
                data?.extras?.getString(Settings.RESULT)
                    ?: RecyclerViewFilters.OrderBy.values()[0].name
            )
            this.setOrderByText()
        }
    }

    private fun setClickListener() {
        binding.orderByText.setOnClickListener {
            val name = reviewModel.getOrderBy().name
            val intent = Intent(context, FilterModeActivity::class.java)
            intent.putExtra(Settings.ORDER_BY, name)
            startActivityForResult(intent, 0)
        }
    }


    private fun setRangeSlider() {
        val slider = binding.priceSlider

        slider.setValues(reviewModel.getMinReview(), reviewModel.getMaxReview())
        this.setPriceText(reviewModel.getMinReview(), reviewModel.getMaxReview())

        slider.addOnChangeListener { rangeSlider, value, fromUser ->
            val minValue = rangeSlider.values[0]
            val maxValue = rangeSlider.values[1]
            setChartLimit(minValue, maxValue)
            setPriceText(minValue, maxValue)

            reviewModel.setFilterbyReviews(
                minValue.toInt(),
                maxValue.toInt()
            )
        }

        val labelFormatter =
            LabelFormatter { value -> "$value " + getString(R.string.on_total) + " 5.0" }
        slider.setLabelFormatter(labelFormatter)

        reviewModel._reviewListener.observe(viewLifecycleOwner, {
            refresh(it)
        })

    }

    private fun setPriceText(minValue: Float, maxValue: Float) {
        binding.reviewTitle.text =
            getString(R.string.filter_review_range_title) + ": $minValue - $maxValue"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.excludedMarketplaces = reviewModel.excludedMarketplaces
    }


    /**
     * sets default parameters for the chart
     */
    private fun setChart() {
        val chart = binding.reviewLineChart
        chart.description = Description().apply { this.text = "" }
        chart.axisLeft.setDrawGridLines(false)
        chart.axisRight.setDrawGridLines(false)
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.mLabelWidth = 0
        chart.xAxis.axisLineColor = Color.TRANSPARENT
        chart.xAxis.textColor = Color.TRANSPARENT
        chart.xAxis.mLabelHeight = 0
        chart.xAxis.setDrawLabels(false)
        chart.axisLeft.setDrawLabels(false)
        chart.axisRight.setDrawLabels(false)
        chart.legend.isEnabled = false
        chart.isDragEnabled = false
        chart.axisLeft.isEnabled = false
        chart.axisRight.isEnabled = false
        chart.isAutoScaleMinMaxEnabled = true
    }

    /**
     * updates the chart
     */
    private fun refresh(list: MutableList<Review>) {
        drawChart(list)
    }

    private val yValues = mutableListOf<Entry>()

    /**
     * initializes the graph drawing
     */
    private fun drawChart(list: MutableList<Review>) {
        yValues.clear()

        for (i in 1..5 step 1) {
            yValues.add(
                Entry(
                    i.toFloat(),
                    list.filter { it.stars >= i && it.stars < i + 1 }.size.toFloat()
                )
            )
        }

        setChartLimit(binding.priceSlider.values[0], binding.priceSlider.values[1])
    }

    private var firstDraw: Boolean = true

    /**
     * draws the graph
     */
    private fun setChartLimit(beginValue: Float, endValue: Float) {
        val chart = binding.reviewLineChart


        val leftYValues = mutableListOf<Entry>()
        val rightYValues = mutableListOf<Entry>()
        val leftLimitLine = mutableListOf<Entry>()
        val rightLimitLine = mutableListOf<Entry>()

        val lLimit = beginValue.toInt()
        val rLimit = endValue.toInt()

        val middleYValues = yValues.toMutableList()

        if (lLimit >= 2) {
            for (i in 1..lLimit) {
                leftYValues.add(middleYValues[i - 1])
            }

            leftLimitLine.add(Entry(lLimit.toFloat(), middleYValues[lLimit - 1].y))

        }
        if (rLimit < 5) {
            for (i in rLimit..5) {
                rightYValues.add(middleYValues[i - 1])
            }
            if (rLimit != lLimit)
                rightLimitLine.add(Entry(rLimit.toFloat(), middleYValues[rLimit - 1].y))

        }


        for (i in 1 until lLimit)
            middleYValues.removeAt(0)

        for (i in rLimit until 5)
            middleYValues.removeAt(middleYValues.size - 1)

        val yLeftSet = LineDataSet(leftYValues, "")
        val yMiddleSet = LineDataSet(middleYValues, "")
        val yRightSet = LineDataSet(rightYValues, "")
        val yleftLimitLine = LineDataSet(leftLimitLine, "")
        val yrightLimitLine = LineDataSet(rightLimitLine, "")

        yleftLimitLine.apply {
            setDrawCircles(false)
            valueTextSize = 0f
            lineWidth = 3f
            color = MaterialColors.getColor(requireView(), R.attr.colorAccent)
            mode = LineDataSet.Mode.HORIZONTAL_BEZIER
            setDrawFilled(false)
        }

        yrightLimitLine.apply {
            setDrawCircles(false)
            valueTextSize = 0f
            lineWidth = 3f
            color = MaterialColors.getColor(requireView(), R.attr.colorAccent)
            mode = LineDataSet.Mode.HORIZONTAL_BEZIER
            setDrawFilled(false)
        }

        yRightSet.apply {
            setDrawCircles(false)
            valueTextSize = 0f
            lineWidth = 2f
            mode = LineDataSet.Mode.HORIZONTAL_BEZIER
            setDrawFilled(true)
            fillDrawable = Color.parseColor("#1A000000").toDrawable()
        }

        yLeftSet.apply {
            setDrawCircles(false)
            valueTextSize = 0f
            lineWidth = 2f
            mode = LineDataSet.Mode.HORIZONTAL_BEZIER
            setDrawFilled(true)
            fillDrawable = Color.parseColor("#1A000000").toDrawable()
        }
        yMiddleSet.apply {
            setDrawCircles(true)
            valueTextSize = 10f
            valueTextColor = MaterialColors.getColor(requireView(), R.attr.colorOnPrimary)
            lineWidth = 2f
            mode = LineDataSet.Mode.HORIZONTAL_BEZIER
            setDrawFilled(true)
            fillDrawable =
                MaterialColors.getColor(requireView(), R.attr.colorSecondary).toDrawable()
        }

        val dataSets = mutableListOf<ILineDataSet>()
        dataSets.add(yLeftSet)
        dataSets.add(yMiddleSet)
        dataSets.add(yRightSet)
        dataSets.add(yleftLimitLine)
        dataSets.add(yrightLimitLine)


        val data = LineData(dataSets)
        chart.data = data
        if (firstDraw) {
            chart.animateXY(0, 300)
            firstDraw = false
        } else
            chart.invalidate()
    }

    private val lock = ReentrantLock(true)

    /**
     * implements the method to manage the click on one of the recurring words
     */
    override fun onClick(list: MutableList<FilterWord>) {
        lock.lock()
        reviewModel.setListOfWords(mutableListOf<FilterWord>().apply { addAll(list) })
        lock.unlock()
    }


}