
package com.islampur.cableiptv

import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private val playlistUrl = "https://m3u.ch/orKl0V.m3u"

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var channelList: LinearLayout
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(12, 20, 38))
        }

        val title = TextView(this).apply {
            text = "ISLAMPUR CABLE IPTV"
            textSize = 23f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(10, 14, 10, 14)
        }

        status = TextView(this).apply {
            text = "Loading channels..."
            textSize = 15f
            setTextColor(Color.YELLOW)
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 8)
        }

        playerView = PlayerView(this).apply {
            useController = true
            setBackgroundColor(Color.BLACK)
        }

        val playerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 230
        )

        val heading = TextView(this).apply {
            text = "LIVE TV CHANNELS"
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(12, 12, 12, 12)
        }

        val scroll = ScrollView(this)

        channelList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 4, 8, 8)
        }

        scroll.addView(channelList)

        root.addView(title)
        root.addView(status)
        root.addView(playerView, playerParams)
        root.addView(heading)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        )

        setContentView(root)

        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        loadPlaylist()
    }

    private fun loadPlaylist() {
        Thread {
            try {
                val connection =
                    URL(playlistUrl).openConnection() as HttpURLConnection

                connection.connectTimeout = 15000
                connection.readTimeout = 20000
                connection.requestMethod = "GET"

                val channels = mutableListOf<Pair<String, String>>()

                connection.inputStream.bufferedReader().use { reader ->
                    var channelName = "Channel"

                    reader.forEachLine { line ->
                        val item = line.trim()

                        if (item.startsWith("#EXTINF")) {
                            channelName =
                                item.substringAfter(",", "Channel").trim()
                        } else if (
                            item.isNotEmpty() &&
                            !item.startsWith("#") &&
                            (item.startsWith("http://") ||
                             item.startsWith("https://"))
                        ) {
                            channels.add(Pair(channelName, item))
                            channelName = "Channel"
                        }
                    }
                }

                connection.disconnect()

                runOnUiThread {
                    channelList.removeAllViews()

                    if (channels.isEmpty()) {
                        status.text = "No channels found in playlist"
                    } else {
                        status.text =
                            "Total Channels: ${channels.size}"

                        channels.forEach { channel ->
                            val button = Button(this).apply {
                                text = channel.first
                                textSize = 16f
                                isFocusable = true
                                isFocusableInTouchMode = true

                                setOnClickListener {
                                    playChannel(channel.second)
                                    status.text =
                                        "Playing: ${channel.first}"
                                }
                            }

                            channelList.addView(
                                button,
                                LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    55
                                )
                            )
                        }

                        channelList.getChildAt(0)?.requestFocus()
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Playlist loading failed"
                    Toast.makeText(
                        this,
                        "Playlist error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun playChannel(streamUrl: String) {
        val mediaItem = MediaItem.fromUri(streamUrl)
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.play()
    }

    override fun onDestroy() {
        playerView.player = null
        player?.release()
        player = null
        super.onDestroy()
    }
}

