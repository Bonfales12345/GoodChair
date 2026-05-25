package com.goodchair.launcher.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.goodchair.launcher.R
import com.goodchair.launcher.model.AppInfo
import java.util.Collections

class WorkspaceAdapter(
    private var apps: MutableList<AppInfo>,
    private val onAppClick: (AppInfo) -> Unit,
    private val onAppLongClick: (AppInfo, View) -> Boolean
) : RecyclerView.Adapter<WorkspaceAdapter.WorkspaceViewHolder>() {

    class WorkspaceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.app_icon)
        val name: TextView = view.findViewById(R.id.app_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkspaceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workspace_app, parent, false)
        return WorkspaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkspaceViewHolder, position: Int) {
        val app = apps[position]
        val prefs = holder.itemView.context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
        val customWidth = prefs.getInt("app_width_${app.packageName}", -2)
        val customHeight = prefs.getInt("app_height_${app.packageName}", -2)
        
        if (customWidth > 100 && customHeight > 100) {
            holder.itemView.layoutParams.width = customWidth
            holder.itemView.layoutParams.height = customHeight
            
            val iconCard = holder.itemView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.icon_card)
            if (iconCard != null) {
                val params = iconCard.layoutParams
                params.width = (customWidth * 0.7f).toInt()
                params.height = (customHeight * 0.7f).toInt()
                iconCard.layoutParams = params
                
                iconCard.radius = (Math.min(params.width, params.height) * 0.3f)
            }
        }

        holder.name.text = app.label
        holder.icon.setImageDrawable(app.icon)
        holder.itemView.setOnClickListener { onAppClick(app) }
        holder.itemView.setOnLongClickListener { onAppLongClick(app, it) }
    }

    override fun getItemCount(): Int = apps.size

    fun addApp(app: AppInfo) {
        if (!apps.any { it.packageName == app.packageName }) {
            apps.add(app)
            notifyItemInserted(apps.size - 1)
        }
    }

    fun removeApp(app: AppInfo) {
        val index = apps.indexOfFirst { it.packageName == app.packageName }
        if (index != -1) {
            apps.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        Collections.swap(apps, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }
    
    fun getApps(): List<AppInfo> = apps
}
