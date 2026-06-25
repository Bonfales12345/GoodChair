package com.goodchair.launcher.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import androidx.recyclerview.widget.ItemTouchHelper
import com.goodchair.launcher.R
import com.goodchair.launcher.model.AppInfo
import java.util.Collections

class WorkspaceAdapter(
    private var apps: MutableList<AppInfo?>,
    private val onAppClick: (AppInfo) -> Unit,
    private val onAppLongClick: (AppInfo, View) -> Boolean
) : RecyclerView.Adapter<WorkspaceAdapter.WorkspaceViewHolder>() {

    private var itemTouchHelper: ItemTouchHelper? = null

    fun setItemTouchHelper(helper: ItemTouchHelper) {
        this.itemTouchHelper = helper
    }

    class WorkspaceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconCard: com.google.android.material.card.MaterialCardView = view.findViewById(R.id.icon_card)
        val icon: ImageView = view.findViewById(R.id.app_icon)
        val name: TextView = view.findViewById(R.id.app_name)
        val handler = Handler(Looper.getMainLooper())
        var dragRunnable: Runnable? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkspaceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workspace_app, parent, false)
        return WorkspaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkspaceViewHolder, position: Int) {
        val app = apps[position]
        
        if (app == null) {
            holder.itemView.visibility = View.INVISIBLE
            holder.itemView.setOnClickListener(null)
            holder.itemView.setOnLongClickListener(null)
            holder.itemView.setOnTouchListener(null)
            return
        }

        holder.itemView.visibility = View.VISIBLE

        val prefs = holder.itemView.context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
        val customWidth = prefs.getInt("app_width_${app.packageName}", -2)
        val customHeight = prefs.getInt("app_height_${app.packageName}", -2)
        
        if (customWidth > 100 && customHeight > 100) {
            holder.itemView.layoutParams.width = customWidth
            holder.itemView.layoutParams.height = customHeight
            
            val params = holder.iconCard.layoutParams
            params.width = (customWidth * 0.7f).toInt()
            params.height = (customHeight * 0.7f).toInt()
            holder.iconCard.layoutParams = params
            holder.iconCard.radius = (Math.min(params.width, params.height) * 0.3f)
        } else {
            holder.itemView.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            holder.itemView.layoutParams.height = (120 * holder.itemView.context.resources.displayMetrics.density).toInt()
            
            val params = holder.iconCard.layoutParams
            params.width = (48 * holder.itemView.context.resources.displayMetrics.density).toInt()
            params.height = (48 * holder.itemView.context.resources.displayMetrics.density).toInt()
            holder.iconCard.layoutParams = params
            holder.iconCard.radius = params.width / 2f
        }

        holder.name.text = app.label
        holder.icon.setImageDrawable(app.icon)
        holder.itemView.setOnClickListener { onAppClick(app) }
        holder.itemView.setOnLongClickListener { onAppLongClick(app, it) }

        holder.dragRunnable?.let { holder.handler.removeCallbacks(it) }
        holder.dragRunnable = Runnable {
            itemTouchHelper?.startDrag(holder)
        }

        holder.itemView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    holder.dragRunnable?.let { holder.handler.postDelayed(it, 2000) }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    holder.dragRunnable?.let { holder.handler.removeCallbacks(it) }
                }
            }
            false
        }
    }

    override fun getItemCount(): Int = 16

    fun updateSlot(position: Int, app: AppInfo?) {
        if (position in 0 until 16) {
            apps[position] = app
            notifyItemChanged(position)
        }
    }

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        Collections.swap(apps, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }
    
    fun getApps(): List<AppInfo?> = apps
}