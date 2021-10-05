package com.jetug.gallery.pro.adapters

import android.annotation.SuppressLint
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.jetug.commons.activities.BaseSimpleActivity
import com.jetug.commons.adapters.MyRecyclerViewAdapter
import com.jetug.commons.interfaces.ItemMoveCallback
import com.jetug.commons.interfaces.ItemTouchHelperContract
import com.jetug.commons.interfaces.StartReorderDragListener
import com.jetug.commons.views.FastScroller
import com.jetug.commons.views.MyRecyclerView
import com.jetug.gallery.pro.extensions.config
import java.util.*

@SuppressLint("NotifyDataSetChanged")
abstract class RecyclerViewAdapterBase(activity: BaseSimpleActivity, recyclerView: MyRecyclerView, fastScroller: FastScroller? = null, val swipeRefreshLayout: SwipeRefreshLayout? = null, itemClick: (Any) -> Unit):
    MyRecyclerViewAdapter(activity, recyclerView, fastScroller, itemClick), ItemTouchHelperContract
{
    protected var isDragAndDropping = false
    protected var startReorderDragListener: StartReorderDragListener? = null

    protected open fun onDragAndDroppingEnded(){}
    open val itemList: ArrayList<*> = arrayListOf<Any>()

    override fun onActionModeDestroyed() {
        if (isDragAndDropping) {
            onDragAndDroppingEnded()
        }
        isDragAndDropping = false
        notifyDataSetChanged()
    }

    override fun onRowMoved(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(itemList, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(itemList, i, i - 1)
            }
        }

        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onRowSelected(myViewHolder: ViewHolder?) {
        swipeRefreshLayout?.isEnabled = false
    }

    override fun onRowClear(myViewHolder: ViewHolder?) {
        swipeRefreshLayout?.isEnabled = activity.config.enablePullToRefresh
    }

    fun changeOrder() {
        enterSelectionMode()
        isDragAndDropping = true
        notifyDataSetChanged()
        actMode?.invalidate()

        if (startReorderDragListener == null) {
            val touchHelper = ItemTouchHelper(ItemMoveCallback(this, true))
            touchHelper.attachToRecyclerView(recyclerView)

            startReorderDragListener = object : StartReorderDragListener {
                override fun requestDrag(viewHolder: RecyclerView.ViewHolder) {
                    touchHelper.startDrag(viewHolder)
                }
            }
        }
    }

    protected fun createDialog(dialog: DialogFragment, tag: String){
        val manager = activity.supportFragmentManager
        dialog.show(manager, tag)
    }
}
