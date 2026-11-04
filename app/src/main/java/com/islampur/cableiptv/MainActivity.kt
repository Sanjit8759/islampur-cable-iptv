
package com.islampur.cableiptv

import android.content.res.Configuration
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.GestureDetector
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private val playlistUrl = "https://m3u.ch/orKl0V.m3u"

    data class Channel(
        val name: String,
        val url: String,
        val group: String
    )

    private val channels = mutableListOf<Channel>()
    private val groups = mutableListOf<String>()

    private var currentIndex = 0
    private var selectedGroup = "ALL"
    private var showingChannels = false
    private var isFullscreen = false
    private var currentFrameMode = "STRETCH"

    private var player: ExoPlayer? = null

    private lateinit var root: LinearLayout
    private lateinit var leftPanel: LinearLayout
    private lateinit var rightPanel: LinearLayout
    private lateinit var header: LinearLayout
    private lateinit var listTitle: TextView
    private lateinit var listScroll: ScrollView
    private lateinit var groupList: LinearLayout
    private lateinit var channelList: LinearLayout
    private lateinit var playerView: PlayerView
    private lateinit var playerFrame: FrameLayout
    private lateinit var status: TextView
    private lateinit var channelTitle: TextView

    private lateinit var videoGesture: GestureDetector

    private val navy = Color.rgb(12, 20, 38)
    private val blue = Color.rgb(25, 85, 160)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        player = ExoPlayer.Builder(this).build()

        player?.addListener(object : Player.Listener {

            override fun onPlayerError(error: PlaybackException) {
                if (::status.isInitialized) {
                    status.text = "Stream Error - Try Another Channel"
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    updateStatus()
                }
            }
        })

        createLayout()
        setupGestures()
        loadPlaylist()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun isPortrait(): Boolean {
        return resources.configuration.orientation ==
            Configuration.ORIENTATION_PORTRAIT
    }

    private fun createLayout() {

        root = LinearLayout(this).apply {
            orientation = if (isPortrait()) {
                LinearLayout.VERTICAL
            } else {
                LinearLayout.HORIZONTAL
            }
            setBackgroundColor(navy)
            isFocusable = true
        }

        leftPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(20, 37, 59))
        }

        leftPanel.addView(
            makeLabel("ISLAMPUR CABLE IPTV", 18f, Color.WHITE),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(55)
            )
        )

        listTitle = makeLabel("GROUPS", 18f, Color.YELLOW)

        leftPanel.addView(
            listTitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(45)
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

        channelTitle = makeLabel(
            "LIVE TV PLAYER",
            17f,
            Color.WHITE
        )

        header.addView(
            channelTitle,
            LinearLayout.LayoutParams(0, dp(48), 1f)
        )

        val settingsButton = makeButton("⚙ SETTINGS") {
            showFrameSettings()
        }

        header.addView(
            settingsButton,
            LinearLayout.LayoutParams(dp(125), dp(45))
        )

        rightPanel.addView(
            header,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            )
        )

        status = makeLabel(
            "Loading Playlist...",
            13f,
            Color.YELLOW
        )

        rightPanel.addView(
            status,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(30)
            )
        )

        playerView = PlayerView(this).apply {
            useController = false
            setBackgroundColor(Color.BLACK)
            resizeMode = when (currentFrameMode) {
                "FIT" -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                "ZOOM" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                else -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            }
            player = this@MainActivity.player
            isFocusable = true
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

            // Transparent watermark
            val watermark = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.END
                setPadding(dp(5), dp(3), dp(55), dp(10))
                setBackgroundColor(Color.TRANSPARENT)

                addView(TextView(this@MainActivity).apply {
                    text = "ISLAMPUR CABLE"
                    textSize = 15f
                    setTextColor(Color.WHITE)
                    setTypeface(null, Typeface.BOLD)
                    gravity = Gravity.END
                    setShadowLayer(
                        dp(3).toFloat(), 1f, 1f, Color.BLACK
                    )
                })

                addView(TextView(this@MainActivity).apply {
                    text = "ইন্টারনেট সংযোগের জন্য যোগাযোগ করুন: 8759411065"
                    textSize = 11f
                    setTextColor(Color.WHITE)
                    setTypeface(null, Typeface.BOLD)
                    gravity = Gravity.END
                    setShadowLayer(
                        dp(3).toFloat(), 1f, 1f, Color.BLACK
                    )
                })
            }

            addView(
                watermark,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
                )
            )

            // Video settings gear
            val videoSettings = Button(this@MainActivity).apply {
                text = "⚙"
                textSize = 22f
                isAllCaps = false
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.argb(170, 0, 0, 0))
                isFocusable = true
                setOnClickListener {
                    showFrameSettings()
                }
            }

            val gearParams = FrameLayout.LayoutParams(
                dp(55),
                dp(48),
                Gravity.TOP or Gravity.END
            )

            gearParams.setMargins(
                dp(5), dp(8), dp(12), dp(5)
            )

            addView(videoSettings, gearParams)
        }

        rightPanel.addView(
            playerFrame,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        if (isPortrait()) {

            root.addView(
                rightPanel,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    2f
                )
            )

            root.addView(
                leftPanel,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            )

        } else {

            root.addView(
                leftPanel,
                LinearLayout.LayoutParams(
                    dp(280),
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

        setContentView(root)

        if (isFullscreen) {
            leftPanel.visibility = View.GONE
            header.visibility = View.GONE
            status.visibility = View.GONE
        } else {
            leftPanel.visibility = View.VISIBLE
            header.visibility = View.VISIBLE
            status.visibility = View.VISIBLE
        }

        if (groups.isNotEmpty()) {
            if (showingChannels) {
                showChannels()
            } else {
                showGroups()
            }
        }
    }

    // Rebuild layout after mobile screen rotation
    override fun onConfigurationChanged(
        newConfig: Configuration
    ) {
        super.onConfigurationChanged(newConfig)
        createLayout()
        setupGestures()
    }

    private fun setupGestures() {

        videoGesture = GestureDetector(
            this,
            object : GestureDetector.SimpleOnGestureListener() {

                override fun onDown(e: MotionEvent): Boolean {
                    return true
                }

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

                    if (e1 != null &&
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
            isFocusableInTouchMode = true
            setOnClickListener { action() }
        }
    }

    // Frame settings
    private fun showFrameSettings() {

        val options = arrayOf(
            "FULL SCREEN STRETCH",
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
            .setSingleChoiceItems(
                options,
                selected
            ) { dialog, which ->

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

    // Load playlist
    private fun loadPlaylist() {

        status.text = "Loading Playlist..."

        Thread {
            try {
                val connection =
                    URL(playlistUrl).openConnection() as HttpURLConnection

                connection.connectTimeout = 20000
                connection.readTimeout = 30000
                connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0"
                )

                connection.connect()

                if (connection.responseCode !in 200..299) {
                    throw Exception("HTTP ${connection.responseCode}")
                }

                channels.clear()

                var name = ""
                var group = "Other"

                connection.inputStream.bufferedReader().use { reader ->

                    reader.forEachLine { line ->

                        val item = line.trim()

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
                            item.startsWith("http://") ||
                            item.startsWith("https://")
                        ) {

                            channels.add(
                                Channel(
                                    name.ifBlank {
                                        "Channel ${channels.size + 1}"
                                    },
                                    item,
                                    group
                                )
                            )

                            name = ""
                            group = "Other"
                        }
                    }
                }

                connection.disconnect()

                runOnUiThread {

                    if (channels.isEmpty()) {
                        status.text = "No Channels Found"
                        return@runOnUiThread
                    }

                    groups.clear()
                    groups.add("ALL")

                    channels.map { it.group }
                        .distinct()
                        .sorted()
                        .forEach {
                            if (!it.equals("ALL", true)) {
                                groups.add(it)
                            }
                        }

                    showingChannels = false
                    showGroups()

                    currentIndex = 0
                    playChannel(currentIndex)

                    root.postDelayed({
                        if (!isFullscreen) {
                            toggleFullscreen()
                        }
                    }, 1500)
                }

            } catch (e: Exception) {

                runOnUiThread {
                    status.text = "Playlist Loading Failed"
                }
            }
        }.start()
    }

    // Show groups
    private fun showGroups() {

        showingChannels = false
        listTitle.text = "GROUPS"

        listScroll.removeAllViews()
        listScroll.addView(groupList)
        groupList.removeAllViews()

        groups.forEach { group ->

            val button = makeButton(group.uppercase()) {
                selectedGroup = group
                showChannels()
            }

            groupList.addView(
                button,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(60)
                )
            )
        }
    }

    // Show channels
    private fun showChannels() {

        showingChannels = true
        listTitle.text = "CHANNELS - $selectedGroup"

        listScroll.removeAllViews()
        listScroll.addView(channelList)
        channelList.removeAllViews()

        val backButton = makeButton("‹ BACK TO GROUPS") {
            showGroups()
        }

        channelList.addView(
            backButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50)
            )
        )

        val filtered = if (selectedGroup == "ALL") {
            channels
        } else {
            channels.filter {
                it.group.equals(selectedGroup, true)
            }
        }

        filtered.forEach { channel ->

            val index = channels.indexOf(channel)

            val button = TextView(this).apply {
                text = channel.name
                textSize = 15f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(5), dp(5), dp(5))
                setTypeface(null, Typeface.BOLD)

                background = GradientDrawable().apply {
                    setColor(
                        if (index == currentIndex) {
                            blue
                        } else {
                            Color.rgb(38, 57, 78)
                        }
                    )
                    cornerRadius = dp(7).toFloat()
                }

                isFocusable = true
                isFocusableInTouchMode = true
                isClickable = true

                setOnClickListener {
                    currentIndex = index
                    playChannel(currentIndex)

                    if (!isFullscreen) {
                        toggleFullscreen()
                    }
                }
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(55)
            )

            params.setMargins(dp(3), dp(3), dp(3), dp(3))
            channelList.addView(button, params)
        }
    }

    // Play channel
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
            player?.setMediaItem(MediaItem.fromUri(channel.url))
            player?.prepare()
            player?.play()

            if (showingChannels) {
                showChannels()
            }

        } catch (e: Exception) {
            status.text = "Unable to Play Channel"
        }
    }

    private fun updateStatus() {
        if (channels.isNotEmpty()) {
            status.text =
                "Playing: ${channels[currentIndex].name} " +
                "(${currentIndex + 1}/${channels.size})"
        }
    }

    private fun playNext() {
        if (channels.isNotEmpty()) {
            playChannel((currentIndex + 1) % channels.size)
        }
    }

    private fun playPrevious() {
        if (channels.isNotEmpty()) {
            val previous = if (currentIndex == 0) {
                channels.size - 1
            } else {
                currentIndex - 1
            }
            playChannel(previous)
        }
    }

    // Fullscreen
    private fun toggleFullscreen() {

        isFullscreen = !isFullscreen

        if (isFullscreen) {
            leftPanel.visibility = View.GONE
            header.visibility = View.GONE
            status.visibility = View.GONE

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        } else {
            leftPanel.visibility = View.VISIBLE
            header.visibility = View.VISIBLE
            status.visibility = View.VISIBLE

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_VISIBLE
        }

        root.requestLayout()
        playerFrame.requestLayout()
    }

    // TV remote key support
    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent?
    ): Boolean {

        when (keyCode) {

            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_CHANNEL_UP -> {
                playNext()
                return true
            }

            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                playPrevious()
                return true
            }

            KeyEvent.KEYCODE_MENU -> {
                showFrameSettings()
                return true
            }

            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                return super.onKeyDown(keyCode, event)
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    // Back: fullscreen -> channels -> groups
    @Suppress("DEPRECATION")
    override fun onBackPressed() {

        if (isFullscreen) {
            toggleFullscreen()
        } else if (showingChannels) {
            showGroups()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        playerView.player = null
        player?.release()
        player = null
        super.onDestroy()
    }
}
