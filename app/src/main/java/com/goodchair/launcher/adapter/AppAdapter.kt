package com.goodchair.launcher.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.goodchair.launcher.R
import com.goodchair.launcher.model.AppInfo

class AppAdapter(
    private var apps: List<AppInfo>,
    private val onAppClick: (AppInfo) -> Unit,
    private val onAppLongClick: ((AppInfo, View) -> Boolean)? = null,
    private val isDock: Boolean = false
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    fun addApp(app: AppInfo) {
        val mutableApps = apps.toMutableList()
        if (!mutableApps.any { it.packageName == app.packageName }) {
            mutableApps.add(app)
            apps = mutableApps
            notifyItemInserted(apps.size - 1)
        }
    }

    fun updateApps(newApps: List<AppInfo>) {
        apps = newApps
        notifyDataSetChanged()
    }

    fun getApps(): List<AppInfo> = apps

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.app_icon)
        val name: TextView? = view.findViewById(R.id.app_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val layout = if (isDock) R.layout.item_dock_app else R.layout.item_app
        val view = LayoutInflater.from(parent.context)
            .inflate(layout, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.name?.text = app.label
        holder.icon.setImageDrawable(app.icon)
        holder.itemView.setOnClickListener { onAppClick(app) }
        holder.itemView.setOnLongClickListener { 
            onAppLongClick?.invoke(app, it) ?: false 
        }
    }

    override fun getItemCount(): Int = apps.size
}