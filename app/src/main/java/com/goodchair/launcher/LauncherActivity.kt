package com.goodchair.launcher

import android.annotation.SuppressLint
import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Bundle
import android.view.DragEvent
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.color.DynamicColors
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.search.SearchView
import com.goodchair.launcher.adapter.AppAdapter
import com.goodchair.launcher.adapter.WorkspaceAdapter
import com.goodchair.launcher.adapter.WorkspacePagerAdapter
import com.goodchair.launcher.model.AppInfo
import com.goodchair.launcher.view.GestureFrameLayout
import java.util.Collections
import java.util.Locale
import android.text.Editable
import android.text.TextWatcher
import android.graphics.Rect

class LauncherActivity : AppCompatActivity() {

    private lateinit var appAdapter: AppAdapter
    private lateinit var searchAdapter: AppAdapter
    private lateinit var dockAdapter: AppAdapter
    private lateinit var workspacePagerAdapter: WorkspacePagerAdapter
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var dockRecyclerView: RecyclerView
    private lateinit var workspaceViewPager: ViewPager2
    
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var appWidgetHost: AppWidgetHost
    
    private lateinit var homeLayout: View
    private var allApps = listOf<AppInfo>()
    private var gestureDetector: GestureDetector? = null
    
    companion object {
        private const val APPWIDGET_HOST_ID = 1024
        private const val REQUEST_PICK_APPWIDGET = 1
        private const val REQUEST_CREATE_APPWIDGET = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DynamicColors.applyToActivitiesIfAvailable(application)

        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        
        setContentView(R.layout.activity_launcher)

        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.setDimAmount(0f)

        appWidgetManager = AppWidgetManager.getInstance(this)
        appWidgetHost = AppWidgetHost(this, APPWIDGET_HOST_ID)

        homeLayout = findViewById(R.id.home_layout)

        setupBottomSheet()
        setupHome()
        setupRecyclerView()
        setupSearch()
        setupSettings()
        loadApps()
        loadPinnedApps()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (::bottomSheetBehavior.isInitialized && bottomSheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        appWidgetHost.startListening()
    }

    override fun onStop() {
        super.onStop()
        appWidgetHost.stopListening()
    }

    override fun onResume() {
        super.onResume()
        updateGridLayout()
        loadApps()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupHome() {
        workspaceViewPager = homeLayout.findViewById(R.id.workspace_viewpager)
        
        workspacePagerAdapter = WorkspacePagerAdapter(mutableListOf(), { appInfo ->
            launchApp(appInfo)
        }, { appInfo, view ->
            showWorkspaceItemMenu(appInfo, view)
            true
        }, {
            savePinnedApps()
        })
        
        workspaceViewPager.adapter = workspacePagerAdapter

        dockRecyclerView = homeLayout.findViewById(R.id.dock_list)
        dockAdapter = AppAdapter(listOf(), { appInfo ->
            launchApp(appInfo)
        }, { appInfo, view ->
            showDockItemMenu(appInfo, view)
            true
        }, isDock = true)
        dockRecyclerView.adapter = dockAdapter
        dockRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val dockBlurBg = homeLayout.findViewById<View>(R.id.dock_blur_background)
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                dockBlurBg?.setRenderEffect(RenderEffect.createBlurEffect(25f, 25f, Shader.TileMode.CLAMP))
            }
        } catch (e: Exception) {}

        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (::bottomSheetBehavior.isInitialized && e1 != null) {
                    val diffY = e1.y - e2.y
                    if (diffY > 50) {
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                        return true
                    }
                }
                return false
            }
            
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                if (::bottomSheetBehavior.isInitialized && e1 != null) {
                    val diffY = e1.y - e2.y
                    val diffX = Math.abs(e1.x - e2.x)
                    if (diffY > 100 && diffY > diffX) {
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                        return true
                    }
                }
                return false
            }
        })
        gestureDetector = detector

        if (homeLayout is GestureFrameLayout) {
            (homeLayout as GestureFrameLayout).setGestureDetector(detector)
        }

        homeLayout.setOnTouchListener { _, event ->
            gestureDetector?.onTouchEvent(event)
            true
        }

        homeLayout.setOnLongClickListener {
            showHomeMenu(it)
            true
        }
        
        val dragListener = View.OnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                DragEvent.ACTION_DRAG_ENTERED -> true
                DragEvent.ACTION_DROP -> {
                    val packageName = event.clipData.getItemAt(0).text.toString()
                    val dockContainer = homeLayout.findViewById<View>(R.id.dock_container)
                    
                    if (dockContainer != null) {
                        val location = IntArray(2)
                        dockContainer.getLocationOnScreen(location)
                        val parentLocation = IntArray(2)
                        homeLayout.getLocationOnScreen(parentLocation)
                        val relativeX = location[0] - parentLocation[0]
                        val relativeY = location[1] - parentLocation[1]
                        val rect = Rect(relativeX, relativeY, relativeX + dockContainer.width, relativeY + dockContainer.height)
                        
                        if (rect.contains(event.x.toInt(), event.y.toInt())) {
                            pinAppToDock(packageName)
                        } else {
                            pinAppToWorkspace(packageName)
                        }
                    } else {
                        pinAppToWorkspace(packageName)
                    }
                    true
                }
                else -> true
            }
        }
        homeLayout.setOnDragListener(dragListener)
    }

    private fun isPointInsideView(x: Float, y: Float, view: View): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val parentLocation = IntArray(2)
        homeLayout.getLocationOnScreen(parentLocation)
        
        val relativeX = location[0] - parentLocation[0]
        val relativeY = location[1] - parentLocation[1]
        
        val rect = Rect(relativeX, relativeY, relativeX + view.width, relativeY + view.height)
        return rect.contains(x.toInt(), y.toInt())
    }

    private fun showDockItemMenu(appInfo: AppInfo, anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add("Remove")
        popup.menu.add("App Info")
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Remove" -> {
                    val mutableApps = dockAdapter.getApps().toMutableList()
                    mutableApps.removeIf { it.packageName == appInfo.packageName }
                    dockAdapter.updateApps(mutableApps)
                    saveDockApps()
                }
                "App Info" -> {
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = android.net.Uri.fromParts("package", appInfo.packageName.toString(), null)
                    startActivity(intent)
                }
            }
            true
        }
        popup.show()
    }

    private fun showWorkspaceItemMenu(appInfo: AppInfo, anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add("Move")
        popup.menu.add("Resize")
        popup.menu.add("Remove")
        popup.menu.add("App Info")
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Move" -> {
                    Toast.makeText(this, "Drag to move", Toast.LENGTH_SHORT).show()
                }
                "Resize" -> startResizingApp(appInfo, anchor)
                "Remove" -> {
                    workspacePagerAdapter.removeApp(appInfo)
                    savePinnedApps()
                }
                "App Info" -> {
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = android.net.Uri.fromParts("package", appInfo.packageName.toString(), null)
                    startActivity(intent)
                }
            }
            true
        }
        popup.show()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun startResizingApp(appInfo: AppInfo, itemView: View) {
        val overlay = homeLayout.findViewById<FrameLayout>(R.id.resize_overlay)
        if (overlay == null) return
        overlay.visibility = View.VISIBLE
        overlay.removeAllViews()
        
        val handles = LayoutInflater.from(this).inflate(R.layout.layout_widget_resize_handles, overlay, false)
        overlay.addView(handles)
        
        val handleBottomRight = handles.findViewById<View>(R.id.handle_bottom_right)
        val outline = handles.findViewById<View>(R.id.resize_outline)
        outline.visibility = View.VISIBLE
        
        val itemLoc = IntArray(2)
        itemView.getLocationOnScreen(itemLoc)
        val parentLoc = IntArray(2)
        homeLayout.getLocationOnScreen(parentLoc)
        
        val startX = itemLoc[0] - parentLoc[0]
        val startY = itemLoc[1] - parentLoc[1]
        
        outline.layoutParams = FrameLayout.LayoutParams(itemView.width, itemView.height).apply {
            leftMargin = startX
            topMargin = startY
        }
        
        handleBottomRight.translationX = (startX + itemView.width - 20).toFloat()
        handleBottomRight.translationY = (startY + itemView.height - 20).toFloat()

        handleBottomRight?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_MOVE) {
                val newWidth = (event.rawX - itemLoc[0]).toInt()
                val newHeight = (event.rawY - itemLoc[1]).toInt()
                
                if (newWidth > 100 && newHeight > 100) {
                    outline.layoutParams.width = newWidth
                    outline.layoutParams.height = newHeight
                    outline.requestLayout()
                    
                    handleBottomRight.translationX = (event.rawX - parentLoc[0]) - 20
                    handleBottomRight.translationY = (event.rawY - parentLoc[1]) - 20

                    itemView.layoutParams.width = newWidth
                    itemView.layoutParams.height = newHeight

                    val iconCard = itemView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.icon_card)
                    if (iconCard != null) {
                        val cardParams = iconCard.layoutParams
                        cardParams.width = (newWidth * 0.8f).toInt()
                        cardParams.height = (newHeight * 0.8f).toInt()
                        iconCard.layoutParams = cardParams
                        iconCard.radius = (Math.min(cardParams.width, cardParams.height) * 0.25f)
                        itemView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.home_root_card)?.setCardBackgroundColor(0x40FFFFFF)
                    }
                    
                    itemView.requestLayout()
                }
            } else if (event.action == MotionEvent.ACTION_UP) {
                getSharedPreferences("launcher_prefs", MODE_PRIVATE).edit()
                    .putInt("app_width_${appInfo.packageName}", itemView.width)
                    .putInt("app_height_${appInfo.packageName}", itemView.height).apply()
                overlay.visibility = View.GONE
            }
            true
        }
    }

    private fun showHomeMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add("Add Widget")
        popup.menu.add("Open App List")
        popup.menu.add("Launcher Settings")
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Add Widget" -> selectWidget()
                "Open App List" -> bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                "Launcher Settings" -> startActivity(Intent(this, SettingsActivity::class.java))
            }
            true
        }
        popup.show()
    }

    private fun selectWidget() {
        val appWidgetId = appWidgetHost.allocateAppWidgetId()
        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK)
        pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_PICK_APPWIDGET -> {
                    val appWidgetId = data?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
                    if (appWidgetId != -1) {
                        configureWidget(data)
                    }
                }
                REQUEST_CREATE_APPWIDGET -> {
                    val appWidgetId = data?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
                    if (appWidgetId != -1) {
                        createWidget(appWidgetId)
                    }
                }
            }
        } else if (resultCode == RESULT_CANCELED && data != null) {
            val appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            if (appWidgetId != -1) {
                appWidgetHost.deleteAppWidgetId(appWidgetId)
            }
        }
    }

    private fun configureWidget(data: Intent?) {
        val extras = data?.extras
        val appWidgetId = extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        val appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
        if (appWidgetInfo != null) {
            if (appWidgetInfo.configure != null) {
                val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                intent.component = appWidgetInfo.configure
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                startActivityForResult(intent, REQUEST_CREATE_APPWIDGET)
            } else {
                createWidget(appWidgetId)
            }
        }
    }

    private fun createWidget(appWidgetId: Int) {
        val appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
        if (appWidgetInfo == null) return

        try {
            val hostView = appWidgetHost.createView(this, appWidgetId, appWidgetInfo)
            hostView.setAppWidget(appWidgetId, appWidgetInfo)
            
            val prefs = getSharedPreferences("launcher_prefs", MODE_PRIVATE)
            val width = prefs.getInt("widget_width_${appWidgetId}", ViewGroup.LayoutParams.MATCH_PARENT)
            val height = prefs.getInt("widget_height_${appWidgetId}", ViewGroup.LayoutParams.WRAP_CONTENT)
            
            hostView.layoutParams = LinearLayout.LayoutParams(width, height)
            
            hostView.setOnLongClickListener {
                showWidgetMenu(hostView, appWidgetId)
                true
            }

            val container = homeLayout.findViewById<LinearLayout>(R.id.widget_container)
            container.removeAllViews()
            container.addView(hostView)
            
            getSharedPreferences("launcher_prefs", MODE_PRIVATE).edit()
                .putInt("widget_id", appWidgetId).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showWidgetMenu(hostView: AppWidgetHostView, appWidgetId: Int) {
        val popup = PopupMenu(this, hostView)
        popup.menu.add("Resize")
        popup.menu.add("Remove")
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Resize" -> startResizing(hostView, appWidgetId)
                "Remove" -> {
                    appWidgetHost.deleteAppWidgetId(appWidgetId)
                    val container = findViewById<LinearLayout>(R.id.widget_container)
                    container.removeAllViews()
                    getSharedPreferences("launcher_prefs", MODE_PRIVATE).edit().remove("widget_id").apply()
                }
            }
            true
        }
        popup.show()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun startResizing(hostView: AppWidgetHostView, appWidgetId: Int) {
        val overlay = homeLayout.findViewById<FrameLayout>(R.id.resize_overlay)
        if (overlay == null) return
        overlay.visibility = View.VISIBLE
        overlay.removeAllViews()
        
        val handles = LayoutInflater.from(this).inflate(R.layout.layout_widget_resize_handles, overlay, false)
        overlay.addView(handles)
        
        val handleBottomRight = handles.findViewById<View>(R.id.handle_bottom_right)
        val outline = handles.findViewById<View>(R.id.resize_outline)
        outline.visibility = View.VISIBLE
        
        val itemLoc = IntArray(2)
        hostView.getLocationOnScreen(itemLoc)
        val parentLoc = IntArray(2)
        homeLayout.getLocationOnScreen(parentLoc)
        
        val startX = itemLoc[0] - parentLoc[0]
        val startY = itemLoc[1] - parentLoc[1]
        
        outline.layoutParams = FrameLayout.LayoutParams(hostView.width, hostView.height).apply {
            leftMargin = startX
            topMargin = startY
        }
        
        handleBottomRight.translationX = (startX + hostView.width - 20).toFloat()
        handleBottomRight.translationY = (startY + hostView.height - 20).toFloat()

        handleBottomRight?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_MOVE) {
                val newWidth = (event.rawX - itemLoc[0]).toInt()
                val newHeight = (event.rawY - itemLoc[1]).toInt()
                
                if (newWidth > 100 && newHeight > 100) {
                    outline.layoutParams.width = newWidth
                    outline.layoutParams.height = newHeight
                    outline.requestLayout()
                    
                    handleBottomRight.translationX = (event.rawX - parentLoc[0]) - 20
                    handleBottomRight.translationY = (event.rawY - parentLoc[1]) - 20

                    hostView.layoutParams.width = newWidth
                    hostView.layoutParams.height = newHeight
                    hostView.requestLayout()
                }
            } else if (event.action == MotionEvent.ACTION_UP) {
                getSharedPreferences("launcher_prefs", MODE_PRIVATE).edit()
                    .putInt("widget_width_${appWidgetId}", hostView.width)
                    .putInt("widget_height_${appWidgetId}", hostView.height).apply()
                overlay.visibility = View.GONE
            }
            true
        }
    }

    private fun setupBottomSheet() {
        val sheet = findViewById<View>(R.id.app_list_sheet)
        if (sheet == null) return
        bottomSheetBehavior = BottomSheetBehavior.from(sheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            homeLayout.setRenderEffect(null)
                        }
                    } catch (e: Exception) {}
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (slideOffset > 0) {
                    val radius = (slideOffset * 25f).coerceAtLeast(0.1f)
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            homeLayout.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP))
                        }
                    } catch (e: Exception) {}
                } else {
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            homeLayout.setRenderEffect(null)
                        }
                    } catch (e: Exception) {}
                }
            }
        })
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.app_list)
        appAdapter = AppAdapter(listOf(), { appInfo ->
            launchApp(appInfo)
        }, { appInfo, view ->
            val item = ClipData.Item(appInfo.packageName)
            val dragData = ClipData(appInfo.label, arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN), item)
            val shadow = View.DragShadowBuilder(view)
            view.startDragAndDrop(dragData, shadow, null, 0)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            true
        })
        recyclerView.adapter = appAdapter
        updateGridLayout()
    }

    private fun pinAppToWorkspace(packageName: String) {
        val appInfo = allApps.find { it.packageName.toString() == packageName }
        if (appInfo != null) {
            workspacePagerAdapter.addApp(appInfo)
            savePinnedApps()
        }
    }

    private fun pinAppToDock(packageName: String) {
        val appInfo = allApps.find { it.packageName.toString() == packageName }
        if (appInfo != null) {
            dockAdapter.addApp(appInfo)
            saveDockApps()
        }
    }

    private fun savePinnedApps() {
        val packages = workspacePagerAdapter.getAllApps().filterNotNull().map { it.packageName.toString() }.joinToString(",")
        getSharedPreferences("launcher_prefs", MODE_PRIVATE).edit()
            .putString("pinned_apps", packages).apply()
    }

    private fun saveDockApps() {
        val packages = dockAdapter.getApps().map { it.packageName.toString() }.joinToString(",")
        getSharedPreferences("launcher_prefs", MODE_PRIVATE).edit()
            .putString("dock_apps", packages).apply()
    }

    private fun loadPinnedApps() {
        val prefs = getSharedPreferences("launcher_prefs", MODE_PRIVATE)
        
        val workspacePackages = prefs.getString("pinned_apps", "") ?: ""
        if (workspacePackages.isNotEmpty()) {
            workspacePackages.split(",").forEach { packageName ->
                val appInfo = allApps.find { it.packageName.toString() == packageName }
                if (appInfo != null) {
                    workspacePagerAdapter.addApp(appInfo)
                }
            }
        }

        val dockPackages = prefs.getString("dock_apps", "") ?: ""
        if (dockPackages.isNotEmpty()) {
            dockPackages.split(",").forEach { pinAppToDock(it) }
        } else {
            allApps.take(4).forEach { pinAppToDock(it.packageName.toString()) }
        }
        
        val widgetId = prefs.getInt("widget_id", -1)
        if (widgetId != -1) {
            createWidget(widgetId)
        }
    }

    private fun launchApp(appInfo: AppInfo) {
        val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName.toString())
        if (launchIntent != null) {
            startActivity(launchIntent)
        }
    }

    private fun updateGridLayout() {
        val prefs = getSharedPreferences("launcher_prefs", MODE_PRIVATE)
        val columns = prefs.getInt("grid_columns", 4)
        recyclerView.layoutManager = GridLayoutManager(this, columns)
    }

    private fun setupSearch() {
        val searchBar = findViewById<com.google.android.material.search.SearchBar>(R.id.search_bar)
        val searchView = findViewById<SearchView>(R.id.search_view)
        val searchResults = findViewById<RecyclerView>(R.id.search_results)

        searchAdapter = AppAdapter(listOf(), { appInfo ->
            launchApp(appInfo)
            searchView.hide()
        })
        searchResults.adapter = searchAdapter
        searchResults.layoutManager = GridLayoutManager(this, 4)

        searchView.editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                val filtered = if (query.isEmpty()) {
                    listOf()
                } else {
                    allApps.filter {
                        it.label.toString().lowercase(Locale.ROOT).contains(query.lowercase(Locale.ROOT))
                    }
                }
                searchAdapter.updateApps(filtered)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        searchBar.setOnClickListener {
            searchView.show()
        }
    }

    private fun setupSettings() {
    }

    private fun loadApps() {
        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)

        val prefs = getSharedPreferences("launcher_prefs", MODE_PRIVATE)
        val hideSystemApps = prefs.getBoolean("hide_system_apps", false)

        val resolvedInfos = pm.queryIntentActivities(mainIntent, 0)
        allApps = resolvedInfos.map { resolveInfo ->
            AppInfo(
                label = resolveInfo.loadLabel(pm),
                packageName = resolveInfo.activityInfo.packageName,
                icon = resolveInfo.loadIcon(pm),
                isSystemApp = (resolveInfo.activityInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            )
        }.filter { 
            if (hideSystemApps) !it.isSystemApp else true
        }.sortedBy { it.label.toString().lowercase(Locale.ROOT) }

        appAdapter.updateApps(allApps)
    }
}