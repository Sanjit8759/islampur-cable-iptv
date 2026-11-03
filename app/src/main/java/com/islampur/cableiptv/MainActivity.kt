
val button = TextView(this).apply {
    text = "${index + 1}. ${channel.first}"
    textSize = 17f
    setTextColor(Color.WHITE)
    gravity = Gravity.CENTER_VERTICAL
    setPadding(20, 8, 12, 8)
    setTypeface(null, Typeface.BOLD)
    setBackgroundColor(Color.rgb(25, 85, 160))
    isFocusable = true
    isClickable = true

    setOnClickListener {
        status.text = "Loading: ${channel.first}"
        playChannel(channel.second)
    }
}

val params = LinearLayout.LayoutParams(
    LinearLayout.LayoutParams.MATCH_PARENT,
    58
)

params.setMargins(5, 5, 5, 5)

channelList.addView(button, params)
