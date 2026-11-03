
package com.islampur.cableiptv

import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private val playlistUrl = "https://m3u.ch/orKl0V.m3u"

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var playerFrame: FrameLayout
    private lateinit var channelList: LinearLayout
    private lateinit var status: TextView
    private lateinit var title: TextView
    private lateinit var heading: TextView
    private lateinit var scroll: ScrollView
    private lateinit var fullscreenButton: Button

    private var fullscreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(12, 20, 38))
        }

        title = TextView(this).apply {
            text = "ISLAMPUR CABLE IPTV"
            textSize = 25f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(8, 12, 8, 12)
            setTypeface(null, Typeface.BOLD)
        }

        status = TextView(this).apply {
            text = "Loading channels..."
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

        fullscreenButton = Button(this).apply {
            text = "FULL SCREEN"
            textSize = 12f
            isAllCaps = false
            setOnClickListener {
                toggleFullscreen(root)
            }
        }

        val buttonParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            45,
            Gravity.BOTTOM or Gravity.END
        )
        playerFrame.addView(fullscreenButton, buttonParams)

        heading = TextView(this).apply {
            text = "LIVE TV CHANNELS"
            textSize = 20f
            setTextColor(Color.WHITE)
            setPadding(12, 12, 12, 12)
            setTypeface(null, Typeface.BOLD)
        }

        channelList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 4, 8, 8)
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
                status.text = "Stream error - Try another channel"
            }
        })

        loadPlaylist()
    }

    private fun toggleFullscreen(root: LinearLayout) {

        fullscreen = !fullscreen

        if (fullscreen) {
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

            fullscreenButton.text = "EXIT FULL SCREEN"

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

            fullscreenButton.text = "FULL SCREEN"
        }

        playerFrame.requestLayout()
    }

    private fun loadPlaylist() {

        status.text = "Loading playlist..."

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

                val channels = mutableListOf<Pair<String, String>>()

                connection.inputStream.bufferedReader().use { reader ->

                    var channelName = ""

                    reader.forEachLine { line ->

                        val item = line.trim()

                        if (item.startsWith("#EXTINF")) {

                            val commaName = item.substringAfter(
                                ",",
                                ""
                            ).trim()

                            val tvgName = Regex(
                                """tvg-name="([^"]*)""""
                            ).find(item)?.groupValues?.get(1)

                            channelName = commaName.ifBlank {
                                tvgName ?: ""
                            }

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
                        status.text = "No channels found"
                        return@runOnUiThread
                    }

                    status.text = "Total Channels: ${channels.size}"

                    channels.forEachIndexed { index, channel ->

                        val button = Button(this).apply {

                            text = "${index + 1}. ${channel.first}"
                            textSize = 15f
                            isAllCaps = false
                            setTextColor(Color.WHITE)
                            setBackgroundColor(
                                Color.rgb(25, 85, 160)
                            )

                            isFocusable = true
                            isClickable = true

                            setOnClickListener {
                                playChannel(channel.second)
                                status.text =
                                    "Selected: ${channel.first}"
                            }
                        }

                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            58
                        )

                        params.setMargins(5, 5, 5, 5)

                        channelList.addView(button, params)
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {
                    status.text = "Playlist loading failed"

                    Toast.makeText(
                        this,
                        "Internet or playlist error",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun playChannel(streamUrl: String) {

        try {
            val mediaItem = MediaItem.fromUri(streamUrl)

            player?.stop()
            player?.clearMediaItems()
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.play()

        } catch (e: Exception) {
            status.text = "Unable to play this channel"
        }
    }

    override fun onBackPressed() {
        if (fullscreen) {
            val root = playerFrame.parent as? LinearLayout
            if (root != null) {
                toggleFullscreen(root)
            }
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
