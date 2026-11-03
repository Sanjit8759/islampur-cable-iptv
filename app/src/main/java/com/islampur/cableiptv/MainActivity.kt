package com.islampur.cableiptv

import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.rgb(12, 20, 38))
        }

        val title = TextView(this).apply {
            text = "ISLAMPUR CABLE IPTV"
            textSize = 26f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Welcome to Live Television"
            textSize = 15f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        val username = EditText(this).apply {
            hint = "Username"
            setSingleLine()
        }

        val password = EditText(this).apply {
            hint = "Password"
            setSingleLine()
            inputType = 129
        }

        val login = Button(this).apply {
            text = "LOGIN"
            setOnClickListener {
                if (username.text.isNullOrBlank() ||
                    password.text.isNullOrBlank()) {
                    Toast.makeText(
                        this@MainActivity,
                        "Enter Username and Password",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Server authentication pending",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(username)
        layout.addView(password)
        layout.addView(login)

        setContentView(layout)
    }
}
