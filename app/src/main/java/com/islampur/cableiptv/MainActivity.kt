
package com.islampur.cableiptv

import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
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
    private var isFullscreen = false
    private var showingChannels = false
    private var currentFrameMode = "FIT"

    private var player: ExoPlayer? = null

    private lateinit var root: LinearLayout
    private lateinit var leftPanel: LinearLayout
    private lateinit var rightPanel: LinearLayout
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

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        createLayout()
        setupGestures()

        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        player?.addListener(object : Player.Listener {

            override fun onPlayerError(error: PlaybackException) {
                status.text = "Stream Error - Try Another Channel"
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    updateStatus()
                }
            }
        })

        loadPlaylist()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

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

        listTitle = makeLabel("GROUPS", 20f, Color.YELLOW)

        leftPanel.addView(
            listTitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(55)
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

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.BLACK)
        }

        channelTitle = makeLabel(
            "LIVE TV PLAYER",
            18f,
            Color.WHITE
        )

        header.addView(
            channelTitle,
            LinearLayout.LayoutParams(0, dp(55), 1f)
        )

        val headerSettings = makeButton("SETTINGS") {
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

        status = makeLabel(
            "Loading Playlist...",
            14f,
            Color.YELLOW
        )

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
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
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

            // Watermark
            val watermark = TextView(this@MainActivity).apply {
                text = "ISLAMPUR CABLE"
                textSize = 15f
                setTextColor(Color.WHITE)
                setTypeface(null, Typeface.BOLD)
                setPadding(dp(10), dp(6), dp(10), dp(6))

                background = GradientDrawable().apply {
                    setColor(Color.argb(170, 0, 0, 0))
                    cornerRadius = dp(5).toFloat()
                }
            }

            val watermarkParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.END
            )

            // Watermark slightly left from bottom-right
            watermarkParams.setMargins(
                dp(5), dp(5), dp(55), dp(15)
            )

            addView(watermark, watermarkParams)

            // Settings button on video
            val videoSettings = Button(this@MainActivity).apply {
                text = "⚙"
                textSize = 20f
                isAllCaps = false
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.argb(170, 0, 0, 0))
                setOnClickListener {
                    showFrameSettings()
                }
            }

            val settingsParams = FrameLayout.LayoutParams(
                dp(55),
                dp(48),
                Gravity.TOP or Gravity.END
            )

            settingsParams.setMargins(
                dp(5), dp(8), dp(12), dp(5)
            )

            addView(videoSettings, settingsParams)
        }

        rightPanel.addView(
            playerFrame,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        // No bottom blue Previous / Next bar

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

        setContentView(root)
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
            setOnClickListener { action() }
        }
    }

    // Video Frame Settings
    private fun showFrameSettings() {

        val options = arrayOf(
            "FIT - সম্পূর্ণ ভিডিও",
            "FILL - স্ক্রিন ভরে",
            "ZOOM - ভিডিও বড় করে"
        )

        val selected = when (currentFrameMode) {
            "FILL" -> 1
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
                        currentFrameMode = "FIT"
                        playerView.resizeMode =
                            AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }

                    1 -> {
                        currentFrameMode = "FILL"
                        playerView.resizeMode =
                            AspectRatioFrameLayout.RESIZE_MODE_FILL
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

                    // First channel starts in fullscreen
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

    // Show only Groups
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
                    dp(65)
                )
            )
        }
    }

    // Show only Channels of selected Group
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
                dp(55)
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
                isClickable = true

                // IMPORTANT:
                // Click channel name -> play channel -> fullscreen
                setOnClickListener {
                    currentIndex = index
                    playChannel(currentIndex)
                    showChannels()

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
    }

    // Play selected channel
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

    // Fullscreen on/off
    private fun toggleFullscreen() {

        isFullscreen = !isFullscreen

        if (isFullscreen) {

            leftPanel.visibility = View.GONE
            channelTitle.visibility = View.GONE
            status.visibility = View.GONE

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        } else {

            leftPanel.visibility = View.VISIBLE
            channelTitle.visibility = View.VISIBLE
            status.visibility = View.VISIBLE

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_VISIBLE
        }

        root.requestLayout()
        playerFrame.requestLayout()
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
