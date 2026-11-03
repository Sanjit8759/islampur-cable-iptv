
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

    private var player: ExoPlayer? = null
    private lateinit var root: LinearLayout
    private lateinit var playerView: PlayerView
    private lateinit var playerFrame: FrameLayout
    private lateinit var channelList: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var status: TextView
    private lateinit var title: TextView
    private lateinit var heading: TextView
    private lateinit var fullButton: Button

    private var isFullscreen = false

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
            textSize = 25f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(10, 12, 10, 12)
            setTypeface(null, Typeface.BOLD)
        }

        status = TextView(this).apply {
            text = "Loading Channels..."
            textSize = 16f
            setTextColor(Color.YELLOW)
            gravity = Gravity.CENTER
            setPadding(8, 10, 8, 10)
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

        fullButton = Button(this).apply {
            text = "FULL SCREEN"
            textSize = 15f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(25, 85, 160))

            setOnClickListener {
                toggleFullscreen()
            }
        }

        heading = TextView(this).apply {
            text = "LIVE TV CHANNELS"
            textSize = 20f
            setTextColor(Color.WHITE)
            setPadding(12, 12, 12, 12)
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

        root.addView(
            fullButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                55
            )
        )

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
                    status.text = "Playing Live TV"
                }
            }
        })

        loadPlaylist()
    }

    private fun toggleFullscreen() {

        isFullscreen = !isFullscreen

        if (isFullscreen) {

            title.visibility = View.GONE
            status.visibility = View.GONE
            heading.visibility = View.GONE
            scroll.visibility = View.GONE

            playerFrame.layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )

            fullButton.text = "EXIT FULL SCREEN"

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        } else {

            title.visibility = View.VISIBLE
            status.visibility = View.VISIBLE
            heading.visibility = View.VISIBLE
            scroll.visibility = View.VISIBLE

            playerFrame.layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    260
                )

            fullButton.text = "FULL SCREEN"

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_VISIBLE
        }

        playerFrame.requestLayout()
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

                val channels =
                    mutableListOf<Pair<String, String>>()

                connection.inputStream.bufferedReader().use { reader ->

                    var channelName = ""

                    reader.forEachLine { line ->

                        val item = line.trim()

                        if (item.startsWith("#EXTINF")) {

                            channelName =
                                item.substringAfter(",", "").trim()

                        } else if (
                            item.startsWith("http://") ||
                            item.startsWith("https://")
                        ) {

                            val name = channelName.ifBlank {
                                "Channel ${channels.size + 1}"
                            }

                            channels.add(Pair(name, item))
                            channelName = ""
                        }
                    }
                }

                connection.disconnect()

                runOnUiThread {

                    channelList.removeAllViews()

                    if (channels.isEmpty()) {
                        status.text = "No Channels Found"
                        return@runOnUiThread
                    }

                    status.text =
                        "Total Channels: ${channels.size}"

                    channels.forEachIndexed { index, channel ->

                        val background = GradientDrawable().apply {
                            setColor(Color.rgb(25, 85, 160))
                            cornerRadius = 12f
                        }

                        val channelButton = TextView(this).apply {

                            text = "${index + 1}. ${channel.first}"
                            textSize = 16f
                            setTextColor(Color.WHITE)
                            gravity = Gravity.CENTER_VERTICAL
                            setPadding(18, 8, 12, 8)
                            setTypeface(null, Typeface.BOLD)
                            setBackground(background)

                            isFocusable = true
                            isClickable = true

                            setOnClickListener {

                                status.text =
                                    "Selected: ${channel.first}"

                                playChannel(channel.second)
                            }
                        }

                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            58
                        )

                        params.setMargins(5, 5, 5, 5)

                        channelList.addView(
                            channelButton,
                            params
                        )
                    }
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

    private fun playChannel(streamUrl: String) {

        try {
            player?.stop()
            player?.clearMediaItems()

            val mediaItem = MediaItem.fromUri(streamUrl)

            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.play()

        } catch (e: Exception) {
            status.text = "Unable to Play Channel"
        }
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
