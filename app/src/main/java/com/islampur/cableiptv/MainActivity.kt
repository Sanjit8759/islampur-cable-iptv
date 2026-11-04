
package com.islampur.cableiptv

import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
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
    private var player: ExoPlayer? = null

    private lateinit var root: LinearLayout
    private lateinit var leftPanel: LinearLayout
    private lateinit var rightPanel: LinearLayout
    private lateinit var groupList: LinearLayout
    private lateinit var channelList: LinearLayout
    private lateinit var playerView: PlayerView
    private lateinit var playerFrame: FrameLayout
    private lateinit var status: TextView
    private lateinit var channelTitle: TextView
    private lateinit var controlBar: LinearLayout
    private lateinit var fullscreenButton: Button

    private val navy = Color.rgb(12, 20, 38)
    private val blue = Color.rgb(25, 85, 160)
    private val panelColor = Color.rgb(20, 37, 59)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        createLayout()

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
            setBackgroundColor(panelColor)
        }

        val brand = makeLabel(
            "ISLAMPUR CABLE IPTV",
            18f,
            Color.WHITE
        )

        leftPanel.addView(
            brand,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(55)
            )
        )

        leftPanel.addView(
            makeLabel("GROUPS", 17f, Color.YELLOW),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(42)
            )
        )

        groupList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(5), dp(3), dp(5), dp(3))
        }

        val scrollGroups = ScrollView(this).apply {
            isFillViewport = true
            addView(groupList)
        }

        leftPanel.addView(
            scrollGroups,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        leftPanel.addView(
            makeLabel("CHANNELS", 17f, Color.YELLOW),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(42)
            )
        )

        channelList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(5), dp(3), dp(5), dp(3))
        }

        val scrollChannels = ScrollView(this).apply {
            isFillViewport = true
            addView(channelList)
        }

        leftPanel.addView(
            scrollChannels,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                2f
            )
        )

        rightPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }

        channelTitle = makeLabel(
            "LIVE TV PLAYER",
            18f,
            Color.WHITE
        )

        rightPanel.addView(
            channelTitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50)
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
        }

        rightPanel.addView(
            playerFrame,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        controlBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(navy)
            setPadding(dp(3), dp(5), dp(3), dp(5))
        }

        controlBar.addView(
            makeButton("◀ PREVIOUS") { playPrevious() },
            LinearLayout.LayoutParams(0, dp(52), 1f)
        )

        controlBar.addView(
            makeButton("NEXT ▶") { playNext() },
            LinearLayout.LayoutParams(0, dp(52), 1f)
        )

        fullscreenButton = makeButton("EXIT FULL") {
            toggleFullscreen()
        }

        controlBar.addView(
            fullscreenButton,
            LinearLayout.LayoutParams(0, dp(52), 1f)
        )

        rightPanel.addView(controlBar)

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
            textSize = 12f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(blue)
            isFocusable = true
            setOnClickListener { action() }
        }
    }

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

                            group = match
                                ?.groupValues
                                ?.get(1)
                                ?.trim()
                                ?.ifBlank { "Other" }
                                ?: "Other"

                        } else if (
                            item.startsWith("http://") ||
                            item.startsWith("https://")
                        ) {

                            channels.add(
                                Channel(
                                    name = name.ifBlank {
                                        "Channel ${channels.size + 1}"
                                    },
                                    url = item,
                                    group = group
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
                        .forEach { groupName ->
                            if (!groupName.equals("ALL", true)) {
                                groups.add(groupName)
                            }
                        }

                    selectedGroup = "ALL"

                    showGroups()
                    showChannels()

                    currentIndex = 0
                    playChannel(currentIndex)

                    // প্রথম চ্যানেল চালু হওয়ার পর Fullscreen
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

    private fun showGroups() {

        groupList.removeAllViews()

        groups.forEach { group ->

            val button = makeButton(group.uppercase()) {

                selectedGroup = group

                showGroups()
                showChannels()
            }

            button.textSize = 13f

            groupList.addView(
                button,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(45)
                )
            )
        }
    }

    private fun showChannels() {

        channelList.removeAllViews()

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
                textSize = 14f
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

                setOnClickListener {
                    currentIndex = index
                    playChannel(currentIndex)
                    showChannels()
                }
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            )

            params.setMargins(dp(3), dp(3), dp(3), dp(3))

            channelList.addView(button, params)
        }
    }

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

            player?.setMediaItem(
                MediaItem.fromUri(channel.url)
            )

            player?.prepare()
            player?.play()

            showChannels()

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

            val previousIndex =
                if (currentIndex == 0) {
                    channels.size - 1
                } else {
                    currentIndex - 1
                }

            playChannel(previousIndex)
        }
    }

    private fun toggleFullscreen() {

        isFullscreen = !isFullscreen

        if (isFullscreen) {

            leftPanel.visibility = View.GONE
            channelTitle.visibility = View.GONE
            status.visibility = View.GONE

            fullscreenButton.text = "EXIT FULL"

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        } else {

            leftPanel.visibility = View.VISIBLE
            channelTitle.visibility = View.VISIBLE
            status.visibility = View.VISIBLE

            fullscreenButton.text = "FULL SCREEN"

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_VISIBLE
        }

        root.requestLayout()
        playerFrame.requestLayout()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {

        if (isFullscreen) {
            toggleFullscreen()
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
