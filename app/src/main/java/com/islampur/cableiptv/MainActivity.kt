
package com.islampur.cableiptv

import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.GestureDetector
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private val playlistUrl = "https://m3u.ch/orKl0V.m3u"
    private val backupPrefs = "IPTV_BACKUP"
    private val backupKey = "saved_playlist"

    // YOUR SHOW - Twitch channel website
    private val twitchPage =
        "https://sanjit8759.github.io/islampur-cable-iptv/"

    data class Channel(
        val name: String,
        val url: String,
        val group: String
    )

    private val channels = mutableListOf<Channel>()
    private val groups = mutableListOf<String>()

    private var currentIndex = 0
    private var selectedGroup = "ALL"
    private var currentSection = "HOME"

    private var isFullscreen = false
    private var showingChannels = false
    private var currentFrameMode = "STRETCH"
    private var autoPlayAttempts = 0

    private var player: ExoPlayer? = null
    private var twitchWebView: WebView? = null

    private lateinit var homeRoot: LinearLayout
    private lateinit var root: LinearLayout
    private lateinit var leftPanel: LinearLayout
    private lateinit var rightPanel: LinearLayout
    private lateinit var header: LinearLayout
    private lateinit var headerSettings: Button
    private lateinit var listTitle: TextView
    private lateinit var listScroll: ScrollView
    private lateinit var groupList: LinearLayout
    private lateinit var channelList: LinearLayout
    private lateinit var playerView: PlayerView
    private lateinit var playerFrame: FrameLayout
    private lateinit var status: TextView
    private lateinit var channelTitle: TextView
    private lateinit var watermarkView: View
    private lateinit var videoSettingsView: View
    private lateinit var videoGesture: GestureDetector

    private val navy = Color.rgb(12, 20, 38)
    private val blue = Color.rgb(25, 85, 160)
    private val yellow = Color.rgb(210, 150, 0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        createLayout()
        createHomeScreen()
        setupGestures()
        createPlayer()

        setContentView(homeRoot)
        loadPlaylist()
    }

    // ================= PLAYER =================

    private fun createPlayer() {

        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(
                "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36"
            )
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20000)
            .setReadTimeoutMs(30000)

        val mediaSourceFactory =
            DefaultMediaSourceFactory(httpFactory)

        val renderersFactory =
            DefaultRenderersFactory(this)
                .setEnableDecoderFallback(true)

        player = ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        playerView.player = player

        player?.addListener(object : Player.Listener {

            override fun onPlayerError(error: PlaybackException) {

                val detail =
                    error.cause?.message ?: error.message ?: "Unknown error"

                Log.e(
                    "ISLAMPUR_IPTV",
                    "Playback Error: ${error.errorCodeName} - $detail",
                    error
                )

                val filtered = getSectionChannels()

                if (
                    currentSection == "LIVE" &&
                    filtered.size > 1 &&
                    autoPlayAttempts < filtered.size - 1
                ) {
                    autoPlayAttempts++

                    status.text = "Channel failed. Trying next channel..."

                    playerView.postDelayed({
                        playNext()
                    }, 1000)

                } else {
                    status.text = "Error: ${error.errorCodeName}"

                    Toast.makeText(
                        this@MainActivity,
                        "Channel চালু হচ্ছে না",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onPlaybackStateChanged(state: Int) {

                when (state) {
                    Player.STATE_BUFFERING -> {
                        status.text = "Buffering..."
                    }

                    Player.STATE_READY -> {
                        autoPlayAttempts = 0
                        updateStatus()
                    }

                    Player.STATE_ENDED -> {
                        status.text = "Stream Ended"
                    }

                    Player.STATE_IDLE -> {}
                }
            }
        })
    }

    // ================= BASIC DESIGN =================

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun focusBackground(
        focused: Boolean,
        selected: Boolean = false
    ): GradientDrawable {

        val color = when {
            focused -> yellow
            selected -> blue
            else -> Color.rgb(38, 57, 78)
        }

        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(7).toFloat()
            if (focused) setStroke(dp(3), Color.WHITE)
        }
    }

    private fun makeLabel(
        text: String,
        size: Float,
        color: Int
    ): TextView {

        return TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(5), dp(5), dp(5), dp(5))
        }
    }

    private fun makeButton(
        text: String,
        action: () -> Unit
    ): Button {

        return Button(this).apply {
            this.text = text
            textSize = 13f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(blue)
            isFocusable = true
            setOnClickListener { action() }
        }
    }

    // ================= HOME SCREEN =================

    private fun createHomeScreen() {

        homeRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER

            setBackground(
                GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(
                        Color.rgb(3, 15, 55),
                        Color.rgb(0, 35, 105),
                        Color.rgb(2, 10, 40)
                    )
                )
            )

            setPadding(dp(15), dp(5), dp(15), dp(5))
        }

        val title = makeLabel(
            "📺 ISLAMPUR CABLE IPTV",
            26f,
            Color.WHITE
        )

        homeRoot.addView(
            title,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(65)
            )
        )

        val subtitle = makeLabel(
            "SELECT YOUR ENTERTAINMENT",
            19f,
            Color.YELLOW
        )

        homeRoot.addView(
            subtitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(55)
            )
        )

        val cardRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        homeRoot.addView(
            cardRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(175)
            )
        )

        addHomeButton(
            "📺",
            "LIVE TV",
            Color.rgb(0, 65, 145),
            Color.CYAN,
            cardRow
        ) {
            openSection("LIVE")
        }

        addHomeButton(
            "📻",
            "RADIO",
            Color.rgb(0, 75, 35),
            Color.GREEN,
            cardRow
        ) {
            openSection("RADIO")
        }

        addHomeButton(
            "🎬",
            "YOUR SHOW",
            Color.rgb(65, 0, 105),
            Color.MAGENTA,
            cardRow
        ) {
            openYourShow()
        }

        // HOME PAGE SETTINGS BUTTON
        val homeSettings = makeButton("⚙ SETTINGS") {
            showFrameSettings()
        }

        val settingsParams = LinearLayout.LayoutParams(
            dp(220),
            dp(55)
        )

        settingsParams.gravity = Gravity.CENTER
        settingsParams.setMargins(0, dp(8), 0, dp(8))

        homeRoot.addView(homeSettings, settingsParams)

        val footer = makeLabel(
            "ISLAMPUR CABLE IPTV",
            17f,
            Color.WHITE
        )

        homeRoot.addView(
            footer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(45)
            )
        )
    }

    private fun addHomeButton(
        icon: String,
        title: String,
        color: Int,
        accent: Int,
        parent: LinearLayout,
        action: () -> Unit
    ) {

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isFocusable = true
            isFocusableInTouchMode = true

            background = GradientDrawable().apply {
                setColor(color)
                cornerRadius = dp(8).toFloat()
                setStroke(dp(2), accent)
            }

            setOnFocusChangeListener { view, focused ->

                view.scaleX = if (focused) 1.03f else 1f
                view.scaleY = if (focused) 1.03f else 1f

                background = GradientDrawable().apply {
                    setColor(color)
                    cornerRadius = dp(8).toFloat()
                    setStroke(
                        dp(if (focused) 4 else 2),
                        if (focused) Color.WHITE else accent
                    )
                }
            }

            setOnClickListener {
                action()
            }
        }

        val iconView = makeLabel(icon, 34f, Color.WHITE)

        card.addView(
            iconView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val titleView = makeLabel(title, 13f, Color.WHITE)

        card.addView(
            titleView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(38)
            )
        )

        val params = LinearLayout.LayoutParams(
            0,
            dp(155),
            1f
        )

        params.setMargins(dp(8), dp(5), dp(8), dp(5))
        parent.addView(card, params)
    }

    // ================= YOUR SHOW =================

    private fun openYourShow() {

        currentSection = "SHOW"
        isFullscreen = false
        showingChannels = false

        player?.pause()

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        setContentView(root)

        leftPanel.visibility = View.GONE
        header.visibility = View.GONE
        headerSettings.visibility = View.GONE
        status.visibility = View.GONE
        playerView.visibility = View.GONE
        watermarkView.visibility = View.GONE
        videoSettingsView.visibility = View.GONE

        playerFrame.visibility = View.VISIBLE

        if (twitchWebView == null) {

            twitchWebView = WebView(this).apply {

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = true
                settings.loadsImagesAutomatically = true
                settings.javaScriptCanOpenWindowsAutomatically = true

                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()

                setBackgroundColor(Color.BLACK)
            }
        }

        val web = twitchWebView!!

        if (web.parent == null) {
            playerFrame.addView(
                web,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }

        web.visibility = View.VISIBLE

        if (web.url.isNullOrBlank()) {
            web.loadUrl(twitchPage)
        }
    }

    private fun closeYourShow() {

        twitchWebView?.let { web ->
            web.stopLoading()
            web.visibility = View.GONE
            playerFrame.removeView(web)
        }

        leftPanel.visibility = View.VISIBLE
        header.visibility = View.VISIBLE
        headerSettings.visibility = View.GONE
        status.visibility = View.VISIBLE
        playerView.visibility = View.VISIBLE
        watermarkView.visibility = View.VISIBLE
        videoSettingsView.visibility = View.GONE

        currentSection = "HOME"
        isFullscreen = false

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_VISIBLE

        setContentView(homeRoot)
    }

    // ================= OPEN SECTION =================

    private fun openSection(section: String) {

        currentSection = section
        isFullscreen = false
        autoPlayAttempts = 0

        // Player screen-এ Settings লুকানো
        headerSettings.visibility = View.GONE
        videoSettingsView.visibility = View.GONE

        header.visibility = View.VISIBLE
        channelTitle.visibility = View.VISIBLE
        status.visibility = View.VISIBLE

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_VISIBLE

        setContentView(root)

        if (section == "RADIO") {
            selectedGroup = "RADIO"
            showChannels()
        } else {
            selectedGroup = "ALL"
            showGroups()
            startFirstLiveChannel()
        }
    }

    private fun startFirstLiveChannel() {

        val firstChannel = channels.indexOfFirst {
            !it.group.equals("RADIO", true)
        }

        if (firstChannel >= 0) {

            currentIndex = firstChannel
            showingChannels = false
            autoPlayAttempts = 0

            playChannel(currentIndex)

            playerFrame.post {
                if (!isFullscreen) {
                    toggleFullscreen()
                }
            }

        } else {

            Toast.makeText(
                this,
                "TV Channel পাওয়া যায়নি",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun returnHome() {

        isFullscreen = false
        currentSection = "HOME"
        showingChannels = false

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_VISIBLE

        setContentView(homeRoot)
    }

    // ================= MAIN PLAYER LAYOUT =================

    private fun createLayout() {

        root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(navy)
        }

        leftPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(20, 37, 59))
        }

        leftPanel.addView(
            makeLabel("ISLAMPUR CABLE IPTV", 18f, Color.WHITE),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(60)
            )
        )

        listTitle = makeLabel("GROUPS", 17f, Color.YELLOW)

        leftPanel.addView(
            listTitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(65)
            )
        )

        groupList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(5), dp(3), dp(5), dp(3))
        }

        channelList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(5), dp(3), dp(5), dp(3))
        }

        listScroll = ScrollView(this).apply {
            isFillViewport = true
            isFocusable = false
            addView(groupList)
        }

        leftPanel.addView(
            listScroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        rightPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }

        header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.BLACK)
        }

        channelTitle = makeLabel("LIVE TV PLAYER", 18f, Color.WHITE)

        header.addView(
            channelTitle,
            LinearLayout.LayoutParams(0, dp(55), 1f)
        )

        headerSettings = makeButton("SETTINGS") {
            showFrameSettings()
        }

        header.addView(
            headerSettings,
            LinearLayout.LayoutParams(dp(125), dp(48))
        )

        rightPanel.addView(
            header,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(55)
            )
        )

        status = makeLabel("Loading Playlist...", 14f, Color.YELLOW)

        rightPanel.addView(
            status,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(35)
            )
        )

        playerView = PlayerView(this).apply {
            useController = false
            setBackgroundColor(Color.BLACK)
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
        }

        playerFrame = FrameLayout(this).apply {

            setBackgroundColor(Color.BLACK)

            addView(
                playerView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )

            val watermark = LinearLayout(this@MainActivity).apply {

                orientation = LinearLayout.VERTICAL
                gravity = Gravity.END

                setPadding(dp(5), dp(3), dp(55), dp(10))

                addView(TextView(this@MainActivity).apply {
                    text = "ISLAMPUR CABLE"
                    textSize = 15f
                    setTextColor(Color.WHITE)
                    setTypeface(null, Typeface.BOLD)
                    gravity = Gravity.END
                    includeFontPadding = false
                    setShadowLayer(dp(3).toFloat(), 1f, 1f, Color.BLACK)
                })

                addView(TextView(this@MainActivity).apply {
                    text = "ইন্টারনেট সংযোগের জন্য যোগাযোগ করুন: 8759411065"
                    textSize = 11f
                    setTextColor(Color.WHITE)
                    setTypeface(null, Typeface.BOLD)
                    gravity = Gravity.END
                    includeFontPadding = false
                    setShadowLayer(dp(3).toFloat(), 1f, 1f, Color.BLACK)
                })
            }

            watermarkView = watermark

            addView(
                watermarkView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
                )
            )

            val videoSettings = Button(this@MainActivity).apply {
                text = "⚙"
                textSize = 20f
                isAllCaps = false
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.argb(170, 0, 0, 0))
                isFocusable = true
                setOnClickListener { showFrameSettings() }
            }

            videoSettingsView = videoSettings

            val settingsParams = FrameLayout.LayoutParams(
                dp(55), dp(48), Gravity.TOP or Gravity.END
            )

            settingsParams.setMargins(dp(5), dp(8), dp(12), dp(5))
            addView(videoSettingsView, settingsParams)
        }

        rightPanel.addView(
            playerFrame,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        root.addView(
            leftPanel,
            LinearLayout.LayoutParams(
                dp(300),
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )

        root.addView(
            rightPanel,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            )
        )
    }

    // ================= GESTURES =================

    private fun setupGestures() {

        videoGesture = GestureDetector(
            this,
            object : GestureDetector.SimpleOnGestureListener() {

                override fun onDown(e: MotionEvent): Boolean = true

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    toggleFullscreen()
                    return true
                }

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {

                    if (
                        e1 != null &&
                        kotlin.math.abs(e2.x - e1.x) > dp(80)
                    ) {
                        if (e2.x < e1.x) {
                            playNext()
                        } else {
                            playPrevious()
                        }
                        return true
                    }

                    return false
                }
            }
        )

        playerView.setOnTouchListener { _, event ->
            videoGesture.onTouchEvent(event)
            true
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {

        if (
            event.action == KeyEvent.ACTION_DOWN &&
            currentSection == "SHOW" &&
            event.keyCode == KeyEvent.KEYCODE_BACK
        ) {
            closeYourShow()
            return true
        }

        if (event.action == KeyEvent.ACTION_DOWN && isFullscreen) {

            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    playNext()
                    return true
                }

                KeyEvent.KEYCODE_DPAD_UP -> {
                    playPrevious()
                    return true
                }
            }
        }

        return super.dispatchKeyEvent(event)
    }

    // ================= FRAME SETTINGS =================

    private fun showFrameSettings() {

        val options = arrayOf(
            "FULL SCREEN STRETCH - কালো জায়গা থাকবে না",
            "FIT - সম্পূর্ণ ভিডিও",
            "ZOOM - ভিডিও বড় করে"
        )

        val selected = when (currentFrameMode) {
            "FIT" -> 1
            "ZOOM" -> 2
            else -> 0
        }

        AlertDialog.Builder(this)
            .setTitle("VIDEO FRAME SETTINGS")
            .setSingleChoiceItems(options, selected) { dialog, which ->

                when (which) {
                    0 -> {
                        currentFrameMode = "STRETCH"
                        playerView.resizeMode =
                            AspectRatioFrameLayout.RESIZE_MODE_FILL
                    }

                    1 -> {
                        currentFrameMode = "FIT"
                        playerView.resizeMode =
                            AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }

                    2 -> {
                        currentFrameMode = "ZOOM"
                        playerView.resizeMode =
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                }

                playerView.requestLayout()
                dialog.dismiss()
            }
            .setNegativeButton("CLOSE", null)
            .show()
    }

    // ================= PLAYLIST PARSER =================

    private fun parsePlaylist(raw: String): MutableList<Channel> {

        val result = mutableListOf<Channel>()
        var name = ""
        var group = "Other"

        val cleanRaw = raw.removePrefix("\uFEFF")

        cleanRaw.lineSequence().forEach { line ->

            val item = line.trim()

            if (item.isBlank()) return@forEach

            if (item.startsWith("#EXTINF", true)) {

                name = item.substringAfter(",", "").trim()

                val match = Regex(
                    "group-title=\"([^\"]*)\"",
                    RegexOption.IGNORE_CASE
                ).find(item)

                group = match?.groupValues?.get(1)
                    ?.trim()
                    ?.ifBlank { "Other" }
                    ?: "Other"

            } else if (
                item.startsWith("http://", true) ||
                item.startsWith("https://", true)
            ) {

                result.add(
                    Channel(
                        name.ifBlank {
                            "Channel ${result.size + 1}"
                        },
                        item,
                        group
                    )
                )

                name = ""
                group = "Other"
            }
        }

        Log.d(
            "ISLAMPUR_IPTV",
            "Parsed channels=${result.size}, RADIO=${result.count { it.group.equals("RADIO", true) }}"
        )

        return result
    }

    // ================= LOAD PLAYLIST =================

    private fun loadPlaylist() {

        Thread {

            var loadedChannels = mutableListOf<Channel>()
            var message = ""

            try {

                val connection =
                    URL(playlistUrl).openConnection() as HttpURLConnection

                connection.connectTimeout = 20000
                connection.readTimeout = 30000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")

                try {

                    connection.connect()

                    if (connection.responseCode !in 200..299) {
                        throw Exception("HTTP ${connection.responseCode}")
                    }

                    val rawPlaylist =
                        connection.inputStream.bufferedReader().use {
                            it.readText()
                        }

                    loadedChannels = parsePlaylist(rawPlaylist)

                    if (loadedChannels.isEmpty()) {
                        throw Exception("Playlist is empty")
                    }

                    getSharedPreferences(
                        backupPrefs,
                        MODE_PRIVATE
                    ).edit()
                        .putString(backupKey, rawPlaylist)
                        .apply()

                    message = "Latest Playlist Loaded"

                } finally {
                    connection.disconnect()
                }

            } catch (e: Exception) {

                Log.e("ISLAMPUR_IPTV", "Online playlist failed", e)

                val savedPlaylist = getSharedPreferences(
                    backupPrefs,
                    MODE_PRIVATE
                ).getString(backupKey, null)

                if (!savedPlaylist.isNullOrBlank()) {

                    loadedChannels = parsePlaylist(savedPlaylist)
                    message = "Offline Backup Loaded"

                } else {
                    message = "Playlist Loading Failed"
                }
            }

            runOnUiThread {

                channels.clear()
                channels.addAll(loadedChannels)

                groups.clear()
                groups.add("ALL")

                channels.map { it.group }
                    .distinct()
                    .filter {
                        !it.equals("RADIO", true) &&
                        !it.equals("ALL", true)
                    }
                    .sorted()
                    .forEach { groups.add(it) }

                currentIndex = 0
                selectedGroup = "ALL"
                showingChannels = false
                isFullscreen = false

                status.text = "$message - ${channels.size} Channels"

                if (currentSection == "LIVE") {
                    showGroups()
                    startFirstLiveChannel()
                } else if (currentSection != "SHOW") {
                    setContentView(homeRoot)
                }

                if (loadedChannels.isEmpty()) {
                    Toast.makeText(
                        this,
                        message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        }.start()
    }

    // ================= GROUP LIST =================

    private fun showGroups() {

        currentSection = "LIVE"
        showingChannels = false
        isFullscreen = false

        setContentView(root)

        listTitle.text = "LIVE TV GROUPS"

        listScroll.removeAllViews()
        listScroll.addView(groupList)
        groupList.removeAllViews()

        groups.forEach { group ->

            val button = Button(this).apply {

                text = group.uppercase()
                textSize = 16f
                isAllCaps = false
                setTextColor(Color.WHITE)
                isFocusable = true
                isFocusableInTouchMode = true

                background = focusBackground(false)

                setOnFocusChangeListener { _, focused ->
                    background = focusBackground(focused)

                    if (focused) {
                        listTitle.text =
                            "SELECTED GROUP: ${group.uppercase()}"
                    }
                }

                setOnClickListener {
                    selectedGroup = group
                    showChannels()
                }
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(65)
            )

            params.setMargins(dp(3), dp(3), dp(3), dp(3))
            groupList.addView(button, params)
        }

        groupList.post {
            if (groupList.childCount > 0) {
                groupList.getChildAt(0).requestFocus()
            }
        }
    }

    // ================= CHANNEL LIST =================

    private fun getSectionChannels(): List<Channel> {

        return if (currentSection == "RADIO") {

            channels.filter {
                it.group.equals("RADIO", true)
            }

        } else {

            channels.filter { channel ->
                !channel.group.equals("RADIO", true) &&
                (
                    selectedGroup == "ALL" ||
                    channel.group.equals(selectedGroup, true)
                )
            }
        }
    }

    private fun showChannels() {

        showingChannels = true
        isFullscreen = false

        setContentView(root)

        listTitle.text =
            if (currentSection == "RADIO") {
                "RADIO CHANNELS"
            } else {
                "CHANNELS - $selectedGroup"
            }

        listScroll.removeAllViews()
        listScroll.addView(channelList)
        channelList.removeAllViews()

        val backButton = Button(this).apply {

            text = if (currentSection == "RADIO") {
                "‹ BACK TO HOME"
            } else {
                "‹ BACK TO GROUPS"
            }

            textSize = 14f
            isAllCaps = false
            setTextColor(Color.WHITE)
            isFocusable = true
            background = focusBackground(false)

            setOnFocusChangeListener { _, focused ->
                background = focusBackground(focused)
                if (focused) listTitle.text = "BACK"
            }

            setOnClickListener {
                if (currentSection == "RADIO") {
                    returnHome()
                } else {
                    showGroups()
                }
            }
        }

        channelList.addView(
            backButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(55)
            )
        )

        val filtered = getSectionChannels()

        filtered.forEach { channel ->

            val index = channels.indexOf(channel)

            val button = Button(this).apply {

                text = channel.name
                textSize = 14f
                isAllCaps = false
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER_VERTICAL

                setPadding(dp(12), dp(5), dp(5), dp(5))
                isFocusable = true

                background = focusBackground(
                    false,
                    index == currentIndex
                )

                setOnFocusChangeListener { _, focused ->
                    background = focusBackground(
                        focused,
                        index == currentIndex
                    )

                    if (focused) {
                        listTitle.text = "SELECTED: ${channel.name}"
                    }
                }

                setOnClickListener {

                    currentIndex = index
                    autoPlayAttempts = 0
                    playChannel(currentIndex)

                    if (!isFullscreen) {
                        toggleFullscreen()
                    }
                }
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58)
            )

            params.setMargins(dp(3), dp(3), dp(3), dp(3))
            channelList.addView(button, params)
        }

        channelList.post {
            if (channelList.childCount > 1) {
                channelList.getChildAt(1).requestFocus()
            } else {
                backButton.requestFocus()
            }
        }
    }

    // ================= PLAY CHANNEL =================

    private fun playChannel(index: Int) {

        if (channels.isEmpty() || index !in channels.indices) {
            return
        }

        currentIndex = index
        val channel = channels[index]

        channelTitle.text = channel.name
        status.text = "Loading: ${channel.name}"

        try {

            player?.stop()
            player?.clearMediaItems()

            val mediaItem = MediaItem.Builder()
                .setUri(channel.url)
                .build()

            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.play()

        } catch (e: Exception) {

            Log.e("ISLAMPUR_IPTV", "Channel Start Error", e)
            status.text = "Unable to Play Channel"
        }
    }

    private fun updateStatus() {

        if (channels.isNotEmpty() && currentIndex in channels.indices) {
            status.text =
                "Playing: ${channels[currentIndex].name} " +
                "(${currentIndex + 1}/${channels.size})"
        }
    }

    // ================= NEXT / PREVIOUS =================

    private fun playNext() {

        val filtered = getSectionChannels()
        if (filtered.isEmpty()) return

        val currentPosition = filtered.indexOfFirst {
            channels.indexOf(it) == currentIndex
        }

        val nextPosition =
            if (currentPosition < 0 || currentPosition >= filtered.lastIndex) {
                0
            } else {
                currentPosition + 1
            }

        playChannel(channels.indexOf(filtered[nextPosition]))
    }

    private fun playPrevious() {

        val filtered = getSectionChannels()
        if (filtered.isEmpty()) return

        val currentPosition = filtered.indexOfFirst {
            channels.indexOf(it) == currentIndex
        }

        val previousPosition =
            if (currentPosition <= 0) {
                filtered.lastIndex
            } else {
                currentPosition - 1
            }

        playChannel(channels.indexOf(filtered[previousPosition]))
    }

    // ================= FULLSCREEN =================

    private fun toggleFullscreen() {

        isFullscreen = !isFullscreen

        // Player screen-এ Settings কখনো দেখাবে না
        headerSettings.visibility = View.GONE
        videoSettingsView.visibility = View.GONE

        if (isFullscreen) {

            leftPanel.visibility = View.GONE
            header.visibility = View.GONE
            channelTitle.visibility = View.GONE
            status.visibility = View.GONE

            val videoParams =
                playerFrame.layoutParams as LinearLayout.LayoutParams

            videoParams.bottomMargin = 0
            playerFrame.layoutParams = videoParams

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        } else {

            leftPanel.visibility = View.VISIBLE
            header.visibility = View.VISIBLE
            channelTitle.visibility = View.VISIBLE
            status.visibility = View.VISIBLE

            val videoParams =
                playerFrame.layoutParams as LinearLayout.LayoutParams

            videoParams.bottomMargin = 0
            playerFrame.layoutParams = videoParams

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_VISIBLE

            if (showingChannels) {
                showChannels()
            } else {
                showGroups()
            }
        }

        root.requestLayout()
        playerFrame.requestLayout()
    }

    // ================= BACK BUTTON =================

    @Suppress("DEPRECATION")
    override fun onBackPressed() {

        if (currentSection == "SHOW") {

            closeYourShow()

        } else if (isFullscreen) {

            toggleFullscreen()

        } else if (currentSection == "RADIO") {

            returnHome()

        } else if (currentSection == "LIVE" && showingChannels) {

            showGroups()

        } else if (currentSection == "LIVE") {

            returnHome()

        } else {

            super.onBackPressed()
        }
    }

    // ================= DESTROY =================

    override fun onDestroy() {

        twitchWebView?.let { web ->
            web.stopLoading()
            web.loadUrl("about:blank")
            web.destroy()
        }

        twitchWebView = null

        playerView.player = null

        player?.release()
        player = null

        super.onDestroy()
    }
}
