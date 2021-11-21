package com.jetug.gallery.pro.dialogs

import android.content.DialogInterface
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.jetug.commons.activities.BaseSimpleActivity
import com.jetug.commons.extensions.beVisibleIf
import com.jetug.commons.extensions.isVisible
import com.jetug.commons.extensions.setupDialogStuff
import com.jetug.commons.helpers.*
import com.jetug.gallery.pro.R
import com.jetug.gallery.pro.extensions.config
import com.jetug.gallery.pro.helpers.SHOW_ALL
import kotlinx.android.synthetic.main.dialog_change_sorting.view.*

class MultiChangeSortingDialog(val activity: BaseSimpleActivity, val showFolderCheckbox: Boolean, val pathList: ArrayList<String>, val callback: () -> Unit) :
    DialogInterface.OnClickListener {
    private var config = activity.config
    private var view: View

    init {
        view = activity.layoutInflater.inflate(R.layout.dialog_change_sorting, null).apply {
            use_for_this_folder_divider.beVisibleIf(true)

            sorting_dialog_numeric_sorting.beVisibleIf(showFolderCheckbox)
            sorting_dialog_numeric_sorting.isChecked = true

            sorting_dialog_use_for_this_folder.beVisibleIf(showFolderCheckbox)
            sorting_dialog_use_for_this_folder.isChecked = true
            sorting_dialog_bottom_note.beVisibleIf(true)
            sorting_dialog_radio_path.beVisibleIf(false)
            sorting_dialog_radio_custom.beVisibleIf(true)

        }

        AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok, this)
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this, R.string.sort_by)
            }

        setupSortRadio()
    }

    private fun setupSortRadio() {
        val sortingRadio = view.sorting_dialog_radio_sorting
        sortingRadio.setOnCheckedChangeListener { group, checkedId ->
            val isSortingByNameOrPath = checkedId == sortingRadio.sorting_dialog_radio_name.id || checkedId == sortingRadio.sorting_dialog_radio_path.id
            view.sorting_dialog_numeric_sorting.beVisibleIf(isSortingByNameOrPath)
            view.use_for_this_folder_divider.beVisibleIf(view.sorting_dialog_numeric_sorting.isVisible() || view.sorting_dialog_use_for_this_folder.isVisible())

            val isCustomSorting = checkedId == sortingRadio.sorting_dialog_radio_custom.id
            view.sorting_dialog_radio_order.beVisibleIf(!isCustomSorting)
            view.sorting_dialog_order_divider.beVisibleIf(!isCustomSorting)
        }

    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        val sortingRadio = view.sorting_dialog_radio_sorting
        var sorting = when (sortingRadio.checkedRadioButtonId) {
            R.id.sorting_dialog_radio_name -> SORT_BY_NAME
            R.id.sorting_dialog_radio_path -> SORT_BY_PATH
            R.id.sorting_dialog_radio_size -> SORT_BY_SIZE
            R.id.sorting_dialog_radio_last_modified -> SORT_BY_DATE_MODIFIED
            R.id.sorting_dialog_radio_random -> SORT_BY_RANDOM
            R.id.sorting_dialog_radio_custom -> SORT_BY_CUSTOM
            else -> SORT_BY_DATE_TAKEN
        }

        if (view.sorting_dialog_radio_order.checkedRadioButtonId == R.id.sorting_dialog_radio_descending) {
            sorting = sorting or SORT_DESCENDING
        }

        if (view.sorting_dialog_numeric_sorting.isChecked) {
            sorting = sorting or SORT_USE_NUMERIC_VALUE
        }

        pathList.forEach {path ->
            if (view.sorting_dialog_use_for_this_folder.isChecked) {
                config.saveCustomSorting(path, sorting)
            } else {
                config.removeCustomSorting(path)
                config.sorting = sorting
            }
        }

        callback()
    }
}
