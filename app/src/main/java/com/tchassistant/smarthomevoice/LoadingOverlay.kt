package com.tchassistant.smarthomevoice

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class LoadingOverlay(private val activity: Activity) {
    private var overlayView: View? = null
    private var messageView: TextView? = null

    fun show(message: String = "Processing...") {
        if (overlayView == null) {
            val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
            overlayView = LayoutInflater.from(activity).inflate(R.layout.overlay_loading, rootView, false)
            messageView = overlayView?.findViewById(R.id.tvLoadingMessage)
            rootView.addView(overlayView)
        }
        messageView?.text = message
        overlayView?.visibility = View.VISIBLE
    }

    fun hide() {
        overlayView?.visibility = View.GONE
    }

    fun isShowing(): Boolean {
        return overlayView?.visibility == View.VISIBLE
    }
} 