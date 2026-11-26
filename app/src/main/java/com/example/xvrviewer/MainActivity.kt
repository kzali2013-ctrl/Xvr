package com.example.xvrviewer

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.rtsp.RtspMediaSource
import com.google.android.exoplayer2.ui.StyledPlayerView

class MainActivity : Activity() {
    private val players = arrayOfNulls<ExoPlayer>(4)
    private lateinit var views: Array<StyledPlayerView>
    private lateinit var rows: Array<LinearLayout>
    private lateinit var label: TextView
    private lateinit var setup: LinearLayout
    private lateinit var playback: FrameLayout
    private lateinit var ip: EditText
    private lateinit var user: EditText
    private lateinit var pass: EditText

    private var playing = false
    private var grid = false
    private var hd = false
    private var current = 1
    private var page = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("xvr", MODE_PRIVATE)
        val root = FrameLayout(this)
        root.setBackgroundColor(Color.BLACK)

        setup = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(120, 60, 120, 60)
        }
        fun field(hint: String, value: String) = EditText(this).apply {
            this.hint = hint; setText(value); textSize = 26f
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY)
        }
        ip = field("IP", prefs.getString("ip", "192.168.0.108")!!)
        user = field("User", prefs.getString("user", "admin")!!)
        pass = field("Password", prefs.getString("pass", "")!!)
        val start = Button(this).apply {
            text = "Start"; textSize = 28f
            setOnClickListener {
                prefs.edit().putString("ip", ip.text.toString())
                    .putString("user", user.text.toString())
                    .putString("pass", pass.text.toString()).apply()
                startPlayback()
            }
        }
        val help = TextView(this).apply {
            text = "LEFT/RIGHT: change camera | UP or OK: grid | DOWN: HD | Back: return"
            textSize = 20f; setTextColor(Color.LTGRAY); gravity = Gravity.CENTER
            setPadding(0, 40, 0, 0)
        }
        setup.addView(ip); setup.addView(user); setup.addView(pass)
        setup.addView(start); setup.addView(help)

        playback = FrameLayout(this)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        views = Array(4) { StyledPlayerView(this).apply { useController = false } }
        rows = Array(2) { r ->
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(views[r * 2], LinearLayout.LayoutParams(0, MATCH_PARENT, 1f))
                addView(views[r * 2 + 1], LinearLayout.LayoutParams(0, MATCH_PARENT, 1f))
            }
        }
        col.addView(rows[0], LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        col.addView(rows[1], LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        label = TextView(this).apply {
            textSize = 28f; setTextColor(Color.YELLOW); setPadding(30, 20, 30, 20)
            setBackgroundColor(0x88000000.toInt())
        }
        playback.addView(col, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        playback.addView(label, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.TOP or Gravity.START))
        playback.visibility = View.GONE

        root.addView(setup, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        root.addView(playback, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        setContentView(root)
    }

    private fun startPlayback() {
        playing = true
        setup.visibility = View.GONE
        playback.visibility = View.VISIBLE
        refresh()
    }

    private fun stopPlayback() {
        releaseAll()
        playing = false
        playback.visibility = View.GONE
        setup.visibility = View.VISIBLE
    }

    private fun releaseAll() {
        for (i in players.indices) { players[i]?.release(); players[i] = null }
    }

    private fun refresh() {
        releaseAll()
        val channels = if (grid) (1..4).map { page * 4 + it } else listOf(current)
        rows[1].visibility = if (grid) View.VISIBLE else View.GONE
        views[1].visibility = if (grid) View.VISIBLE else View.GONE
        views[2].visibility = if (grid) View.VISIBLE else View.GONE
        views[3].visibility = if (grid) View.VISIBLE else View.GONE
        val stream = if (hd && !grid) 0 else 1
        channels.forEachIndexed { i, ch ->
            val url = "rtsp://${ip.text}:554/user=${user.text}&password=${pass.text}" +
                "&channel=$ch&stream=$stream.sdp?real_stream"
            val source = RtspMediaSource.Factory().setForceUseRtpTcp(true)
                .createMediaSource(MediaItem.fromUri(url))
            players[i] = ExoPlayer.Builder(this).build().also {
                views[i].player = it
                it.setMediaSource(source)
                it.prepare()
                it.playWhenReady = true
            }
        }
        label.text = if (grid) "CH ${channels.first()}-${channels.last()}"
        else "CH $current" + if (hd) "  HD" else ""
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (!playing) return super.onKeyDown(keyCode, event)
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (grid) page = (page + 1) % 2 else current = current % 8 + 1
                refresh(); return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (grid) page = (page + 1) % 2 else current = if (current == 1) 8 else current - 1
                refresh(); return true
            }
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (grid) current = page * 4 + 1
                grid = !grid; refresh(); return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (!grid) { hd = !hd; refresh() }
                return true
            }
            KeyEvent.KEYCODE_BACK -> { stopPlayback(); return true }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onStop() {
        super.onStop()
        if (playing) stopPlayback()
    }
}
