package com.goodchair.launcher.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.goodchair.launcher.R
import com.goodchair.launcher.model.AppInfo

class WorkspacePagerAdapter(
    private var pages: MutableList<MutableList<AppInfo?>>,
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
        for (page in pages) {
            val emptySlotIndex = page.indexOfFirst { it == null }
            if (emptySlotIndex != -1) {
                page[emptySlotIndex] = app
                notifyDataSetChanged()
                onAppsChanged()
                return
            }
        }

        val newPage = MutableList<AppInfo?>(16) { null }
        newPage[0] = app
        pages.add(newPage)
        notifyItemInserted(pages.size - 1)
        onAppsChanged()
    }
    
    fun removeApp(app: AppInfo) {
        pages.forEach { list ->
            val index = list.indexOfFirst { it?.packageName == app.packageName }
            if (index != -1) {
                list[index] = null
                notifyDataSetChanged()
                onAppsChanged()
            }
        }
    }

    fun getAllApps(): List<AppInfo?> = pages.flatten()
    
    fun getPages() = pages
}