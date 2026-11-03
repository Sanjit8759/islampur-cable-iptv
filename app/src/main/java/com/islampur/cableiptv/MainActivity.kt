
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
    private var selectedGroup = "ALL"
    private var currentIndex = 0
    private var isFullscreen = false

    private var player: ExoPlayer? = null

    private lateinit var root: LinearLayout
    private lateinit var playerView: PlayerView
    private lateinit var playerFrame: FrameLayout
    private lateinit var channelList: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var status: TextView
    private lateinit var title: TextView
    private lateinit var heading: TextView
    private lateinit var categoryBar: LinearLayout
    private lateinit var categoryScroll: HorizontalScrollView
    private lateinit var fullButton: Button
    private lateinit var previousButton: Button
    private lateinit var nextButton: Button
    private lateinit var listButton: Button
    private lateinit var controlBar: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(12, 20, 38))
        }

        title = TextView(this).apply {
            text = "ISLAMPUR CABLE IPTV"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(8, 10, 8, 10)
            setTypeface(null, Typeface.BOLD)
        }

        status = TextView(this).apply {
            text = "Loading Channels..."
            textSize = 16f
            setTextColor(Color.YELLOW)
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 8)
        }

        playerView = PlayerView(this).apply {
            useController = true
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

        controlBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(4, 5, 4, 5)
            setBackgroundColor(Color.rgb(12, 20, 38))
        }

        previousButton = makeButton("◀ PREVIOUS") {
            playPrevious()
        }

        nextButton = makeButton("NEXT ▶") {
            playNext()
        }

        listButton = makeButton("CHANNEL LIST") {
            showChannelList()
        }

        controlBar.addView(
            previousButton,
            LinearLayout.LayoutParams(0, 55, 1f)
        )

        controlBar.addView(
            nextButton,
            LinearLayout.LayoutParams(0, 55, 1f)
        )

        controlBar.addView(
            listButton,
            LinearLayout.LayoutParams(0, 55, 1f)
        )

        fullButton = makeButton("FULL SCREEN") {
            enterFullscreen()
        }

        categoryBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(6, 5, 6, 5)
        }

        categoryScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(categoryBar)
        }

        heading = TextView(this).apply {
            text = "LIVE TV CHANNELS"
            textSize = 20f
            setTextColor(Color.WHITE)
            setPadding(12, 10, 12, 10)
            setTypeface(null, Typeface.BOLD)
        }

        channelList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 5, 8, 8)
        }

        scroll = ScrollView(this).apply {
            addView(channelList)
        }

        root.addView(title)
        root.addView(status)

        root.addView(
            playerFrame,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                260
            )
        )

        root.addView(controlBar)
        root.addView(fullButton)
        root.addView(categoryScroll)
        root.addView(heading)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)

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

    private fun makeButton(
        label: String,
        action: () -> Unit
    ): Button {

        return Button(this).apply {
            text = label
            textSize = 12f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setBackgroundColor(Color.rgb(25, 85, 160))
            isFocusable = true
            setOnClickListener { action() }
        }
    }

    private fun loadPlaylist() {

        Thread {

            try {

                val connection =
                    URL(playlistUrl).openConnection()
                        as HttpURLConnection

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

                var channelName = ""
                var channelGroup = "Other"

                connection.inputStream.bufferedReader().use { reader ->

                    reader.forEachLine { line ->

                        val item = line.trim()

                        if (item.startsWith("#EXTINF")) {

                            channelName =
                                item.substringAfter(",", "").trim()

                            val groupMatch = Regex(
                                "group-title=\"([^\"]*)\"",
                                RegexOption.IGNORE_CASE
                            ).find(item)

                            channelGroup =
                                groupMatch?.groupValues?.get(1)
                                    ?.trim()
                                    ?.ifBlank { "Other" }
                                    ?: "Other"

                        } else if (
                            item.startsWith("http://") ||
                            item.startsWith("https://")
                        ) {

                            val name = channelName.ifBlank {
                                "Channel ${channels.size + 1}"
                            }

                            channels.add(
                                Channel(name, item, channelGroup)
                            )

                            channelName = ""
                            channelGroup = "Other"
                        }
                    }
                }

                connection.disconnect()

                runOnUiThread {

                    if (channels.isEmpty()) {
                        status.text = "No Channels Found"
                        return@runOnUiThread
                    }

                    showCategories()
                    showChannels()

                    currentIndex = 0
                    playChannelAt(currentIndex)

                    enterFullscreen()
                }

            } catch (e: Exception) {

                runOnUiThread {
                    status.text = "Playlist Loading Failed"

                    Toast.makeText(
                        this,
                        "Playlist Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        }.start()
    }

    private fun showCategories() {

        categoryBar.removeAllViews()

        val groups = channels
            .map { it.group }
            .distinct()
            .sorted()

        val categories = listOf("ALL") + groups

        categories.forEach { group ->

            val button = makeButton(group.uppercase()) {

                selectedGroup = group
                showCategories()
                showChannels()
            }

            categoryBar.addView(
                button,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    48
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
                it.group.equals(selectedGroup, ignoreCase = true)
            }
        }

        heading.text =
            "${selectedGroup.uppercase()} CHANNELS (${filtered.size})"

        filtered.forEach { channel ->

            val channelIndex = channels.indexOf(channel)

            val background = GradientDrawable().apply {
                setColor(Color.rgb(25, 85, 160))
                cornerRadius = 12f
            }

            val channelButton = TextView(this).apply {

                text = "${channelIndex + 1}. ${channel.name}"
                textSize = 16f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(18, 8, 12, 8)
                setTypeface(null, Typeface.BOLD)
                setBackground(background)

                isFocusable = true
                isClickable = true

                setOnClickListener {
                    currentIndex = channelIndex
                    playChannelAt(currentIndex)
                    enterFullscreen()
                }
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                58
            )

            params.setMargins(5, 5, 5, 5)

            channelList.addView(channelButton, params)
        }
    }

    private fun playChannelAt(index: Int) {

        if (channels.isEmpty()) return

        if (index !in channels.indices) return

        currentIndex = index

        val channel = channels[currentIndex]

        status.text = "Loading: ${channel.name}"

        try {

            player?.stop()
            player?.clearMediaItems()

            val mediaItem = MediaItem.fromUri(channel.url)

            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.play()

            updateStatus()

        } catch (e: Exception) {
            status.text = "Unable to Play Channel"
        }
    }

    private fun updateStatus() {

        if (channels.isEmpty()) return

        status.text =
            "Playing: ${channels[currentIndex].name} (${currentIndex + 1}/${channels.size})"
    }

    private fun playNext() {

        if (channels.isEmpty()) return

        val nextIndex =
            (currentIndex + 1) % channels.size

        playChannelAt(nextIndex)
    }

    private fun playPrevious() {

        if (channels.isEmpty()) return

        val previousIndex =
            if (currentIndex == 0) {
                channels.size - 1
            } else {
                currentIndex - 1
            }

        playChannelAt(previousIndex)
    }

    private fun enterFullscreen() {

        isFullscreen = true

        title.visibility = View.GONE
        status.visibility = View.GONE
        fullButton.visibility = View.GONE
        categoryScroll.visibility = View.GONE
        heading.visibility = View.GONE
        scroll.visibility = View.GONE

        playerFrame.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )

        controlBar.visibility = View.VISIBLE

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        root.requestLayout()
    }

    private fun showChannelList() {

        isFullscreen = false

        title.visibility = View.VISIBLE
        status.visibility = View.VISIBLE
        fullButton.visibility = View.VISIBLE
        categoryScroll.visibility = View.VISIBLE
        heading.visibility = View.VISIBLE
        scroll.visibility = View.VISIBLE
        controlBar.visibility = View.GONE

        playerFrame.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                260
            )

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_VISIBLE

        root.requestLayout()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {

        if (isFullscreen) {
            showChannelList()
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
