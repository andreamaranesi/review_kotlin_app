package com.project.review.ui.filters

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.project.review.R
import com.project.review.databinding.FilterModeBinding
import com.project.review.settings.Settings

/**
 * allows user to select how the reviews are sorted
 *
 * @see com.project.review.ui.filters.RecyclerViewFilters
 */
class FilterModeActivity : AppCompatActivity() {


    private lateinit var filterMode: FilterModeBinding
    private lateinit var checkedFilter: RecyclerViewFilters.OrderBy
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        filterMode = DataBindingUtil.setContentView(this, R.layout.filter_mode)

        supportActionBar?.hide()

        val checkedName = intent.extras?.getString(
            Settings.ORDER_BY,
            RecyclerViewFilters.OrderBy.values()[0].name
        )
        if(checkedName!=null)
            checkedFilter =
                RecyclerViewFilters.OrderBy.valueOf(checkedName)
        else
            finish()

        setRadioButtons()

    }


    class RadioOption(val type: RecyclerViewFilters.OrderBy) {
        var id: Int = View.generateViewId()
    }

    /**
     * injects the RadioButtons into a RadioGroup and manages their click by the user
     */
    private fun setRadioButtons() {
        val radioGroup = filterMode.filterMode
        val list = mutableListOf<RadioOption>()
        for (filter: RecyclerViewFilters.OrderBy in RecyclerViewFilters.OrderBy.values()) {
            list.add(RadioOption(filter))
        }

        for (i in list) {
            val view: RadioButton =
                layoutInflater.inflate(
                    R.layout.filter_mode_radio_button,
                    radioGroup,
                    false
                ) as RadioButton
            view.id = i.id
            view.text = RecyclerViewFilters.OrderBy.getName(i.type, this)
            radioGroup.addView(view)
            if (i.type == checkedFilter)
                view.isChecked = true
        }
        radioGroup.setOnCheckedChangeListener(object : RadioGroup.OnCheckedChangeListener {
            override fun onCheckedChanged(p0: RadioGroup?, p1: Int) {
                val _object = list.firstOrNull { it.id == p1 }
                if (_object != null && _object.type != checkedFilter) {
                    val intent = Intent()
                    intent.putExtra(Settings.RESULT, _object.type.name)
                    setResult(RESULT_OK, intent)
                    finish()
                }
            }

        })
    }
}