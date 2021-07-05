package com.project.review.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialogFragment
import com.google.android.material.color.MaterialColors
import com.project.review.R


/**
 * dialog to confirm or deny an action
 */
class ConfirmDialog(private val title: String = "", private val root: View) :
    AppCompatDialogFragment() {

    interface Actions {
        fun onPositiveClick()
        fun onNegativeClick()
    }

    private lateinit var _actions: Actions

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val v = activity?.layoutInflater?.inflate(R.layout.confirm_dialog, null)
        val dialog = AlertDialog.Builder(activity)
        v?.findViewById<TextView>(R.id.title)?.text = title
        dialog.setView(v)
        dialog.setTitle("")

        dialog.setPositiveButton(
            getString(R.string.next_step)
        ) { _, _ -> _actions.onPositiveClick() }

        dialog.setNegativeButton(
            getString(R.string.cancel_step)
        ) { _, _ -> _actions.onNegativeClick() }

        val createdDialog = dialog.create()

        createdDialog.setOnShowListener {
            val buttonTextColor = MaterialColors.getColor(root, R.attr.colorOnPrimary)
            createdDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(buttonTextColor)
            createdDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(buttonTextColor)
        }

        return createdDialog
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this._actions = context as Actions
    }
}