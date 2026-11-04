        playerFrame = FrameLayout(this).apply {

            setBackgroundColor(Color.BLACK)

            addView(
                playerView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )

            // Watermark and Internet Contact
            val watermark = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.END
                setPadding(dp(10), dp(6), dp(10), dp(6))

                background = GradientDrawable().apply {
                    setColor(Color.argb(190, 0, 0, 0))
                    cornerRadius = dp(5).toFloat()
                }

                val firstLine = TextView(this@MainActivity).apply {
                    text = "ISLAMPUR CABLE"
                    textSize = 15f
                    setTextColor(Color.WHITE)
                    setTypeface(null, Typeface.BOLD)
                    gravity = Gravity.END
                }

                val secondLine = TextView(this@MainActivity).apply {
                    text = "ইন্টারনেট সংযোগের জন্য যোগাযোগ করুন: 8759411065"
                    textSize = 11f
                    setTextColor(Color.WHITE)
                    setTypeface(null, Typeface.BOLD)
                    gravity = Gravity.END
                }

                addView(firstLine)
                addView(secondLine)
            }

            val watermarkParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.END
            )

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
