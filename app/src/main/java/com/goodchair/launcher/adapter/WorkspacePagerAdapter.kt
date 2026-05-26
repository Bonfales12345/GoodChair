package com.goodchair.launcher.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import java.util.Collections
import com.goodchair.launcher.R
import com.goodchair.launcher.model.AppInfo

class WorkspacePagerAdapter(
    private var pages: MutableList<MutableList<AppInfo>>,
    private val onAppClick: (AppInfo) -> Unit,
    private val onAppLongClick: (AppInfo, View) -> Boolean,
    private val onAppsChanged: () -> Unit
) : RecyclerView.Adapter<WorkspacePagerAdapter.PageViewHolder>() {

    class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val recyclerView: RecyclerView = view.findViewById(R.id.workspace_grid)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_workspace_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val pageApps = pages[position]
        val adapter = WorkspaceAdapter(pageApps, onAppClick, onAppLongClick)
        holder.recyclerView.adapter = adapter
        holder.recyclerView.layoutManager = GridLayoutManager(holder.itemView.context, 4)

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, 0) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                adapter.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
                onAppsChanged()
                return true
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        })
        itemTouchHelper.attachToRecyclerView(holder.recyclerView)
    }

    override fun getItemCount(): Int = pages.size

    fun addApp(app: AppInfo) {
        // Find first page with space or create new
        var page = pages.lastOrNull()
        if (page == null || page.size >= 16) {
            page = mutableListOf()
            pages.add(page)
            notifyItemInserted(pages.size - 1)
        }
        if (!page.any { it.packageName == app.packageName }) {
            page.add(app)
            notifyItemChanged(pages.size - 1)
            onAppsChanged()
        }
    }
    
    fun removeApp(app: AppInfo) {
        pages.forEachIndexed { index, list ->
            val found = list.removeIf { it.packageName == app.packageName }
            if (found) {
                notifyItemChanged(index)
                onAppsChanged()
            }
        }
    }

    fun getAllApps(): List<AppInfo> = pages.flatten()
}