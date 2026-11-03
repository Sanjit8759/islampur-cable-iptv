
package com.islampur.cableiptv

import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
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
    private lateinit var channelList: LinearLayout
    private lateinit var status: TextView

    private val navy = Color.rgb(12, 20, 38)
    private val blue = Color.rgb(25, 85, 160)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(navy)
        }

        val title = TextView(this).apply {
            text = "ISLAMPUR CABLE IPTV"
            textSize = 25f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(10, 12, 10, 12)
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
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            setShutterBackgroundColor(Color.BLACK)
        }

        val heading = TextView(this).apply {
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

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(channelList)
        }

        root.addView(title)
        root.addView(status)

        root.addView(
            playerView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                280
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
                status.text = "Channel playback failed"
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    status.text = "Playing Live TV"
                }
            }
        })

        loadPlaylist()
    }

    private fun loadPlaylist() {
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
                        status.text = "No channels found"
                        return@runOnUiThread
                    }

                    status.text = "Total Channels: ${channels.size}"

                    channels.forEachIndexed { index, channel ->

                        val buttonBackground = GradientDrawable().apply {
                            setColor(blue)
                            cornerRadius = 12f
                        }

                        val button = Button(this).apply {
                            text = channel.first
                            textSize = 16f
                            isAllCaps = false
                            setTextColor(Color.WHITE)
                            background = buttonBackground
                            isFocusable = true
                            isFocusableInTouchMode = true

                            setOnClickListener {
                                playChannel(channel.second)
                                status.text = "Loading: ${channel.first}"
                            }
                        }

                        val params = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            56
                        )

                        params.setMargins(5, 5, 5, 5)

                        channelList.addView(button, params)
                    }

                    channelList.getChildAt(0)?.requestFocus()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Playlist loading failed"

                    Toast.makeText(
                        this,
                        "Internet or Playlist error",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun playChannel(streamUrl: String) {
        try {
            val mediaItem = MediaItem.Builder()
                .setUri(streamUrl)
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .build()

            player?.stop()
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.play()

        } catch (e: Exception) {
            status.text = "Unable to play channel"
        }
    }

    override fun onDestroy() {
        playerView.player = null
        player?.release()
        player = null
        super.onDestroy()
    }
}
