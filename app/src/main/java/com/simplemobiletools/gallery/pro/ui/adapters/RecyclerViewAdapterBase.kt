package com.simplemobiletools.gallery.pro.ui.adapters

import android.annotation.SuppressLint
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.interfaces.ItemMoveCallback
import com.simplemobiletools.commons.interfaces.ItemTouchHelperContract
import com.simplemobiletools.commons.interfaces.StartReorderDragListener
import com.simplemobiletools.commons.views.FastScroller
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.gallery.pro.data.extensions.context.config
import java.util.*

@SuppressLint("NotifyDataSetChanged")
abstract class RecyclerViewAdapterBase(activity: BaseSimpleActivity,
                                       recyclerView: MyRecyclerView,
                                       fastScroller: FastScroller? = null,
                                       private val swipeRefreshLayout: SwipeRefreshLayout? = null,
                                       itemClick: (Any) -> Unit):
    MyRecyclerViewAdapter(activity, recyclerView, fastScroller, itemClick), ItemTouchHelperContract
{
    protected var startReorderDragListener: StartReorderDragListener? = null
    protected open fun onItemMoved(fromPosition: Int, toPosition: Int){}
    protected open fun onDragAndDroppingEnded(){}
    open val itemList: ArrayList<*> = arrayListOf<Any>()

    override fun onActionModeDestroyed() {
        if (isDragAndDropping) {
            onDragAndDroppingEnded()
        }
        finishDragMode()
        //isDragAndDropping = false
        notifyDataSetChanged()
    }

    override fun onRowMoved(fromPosition: Int, toPosition: Int) {
        if(selectedKeys.size < 2)
            onItemMoved(fromPosition, toPosition)
        else{
            var toPos = toPosition
            selectedKeys.forEach{ key ->
                val pos = getItemKeyPosition(key)
                onItemMoved(pos, toPos)
                toPos+=1
            }
        }
    }

    override fun onRowSelected(myViewHolder: ViewHolder?) {
        swipeRefreshLayout?.isEnabled = false
    }

    override fun onRowClear(myViewHolder: ViewHolder?) {
        swipeRefreshLayout?.isEnabled = activity.config.enablePullToRefresh
    }

    open fun changeOrder() {
        enterSelectionMode(false)
        enterDragMode()
        //isDragAndDropping = true
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
}
