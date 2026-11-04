private fun showFrameSettings() {

    val options = arrayOf(
        "FIT - সম্পূর্ণ ভিডিও",
        "FILL - স্ক্রিন ভরে",
        "ZOOM - ভিডিও বড় করে"
    )

    val selected = when (currentFrameMode) {
        "FILL" -> 1
        "ZOOM" -> 2
        else -> 0
    }

    AlertDialog.Builder(this)
        .setTitle("VIDEO FRAME SETTINGS")
        .setSingleChoiceItems(options, selected) { dialog, which ->

            when (which) {
                0 -> {
                    currentFrameMode = "FIT"
                    playerView.resizeMode =
                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                }

                1 -> {
                    currentFrameMode = "FILL"
                    playerView.resizeMode =
                        AspectRatioFrameLayout.RESIZE_MODE_FILL
                }

                2 -> {
                    currentFrameMode = "ZOOM"
                    playerView.resizeMode =
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            }

            playerView.requestLayout()
            dialog.dismiss()
        }
        .setNegativeButton("CLOSE", null)
        .show()
}
