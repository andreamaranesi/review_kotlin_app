package com.project.review.binding_adapters

import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.databinding.BindingAdapter
import com.project.review.settings.Tools


@BindingAdapter("imageUrl")
fun imageUrl(imageView: ImageView, url: String) {
    Tools.setImage(imageView, url, imageView.context)
}

@BindingAdapter("reviewBody")
fun imageUrl(textView: TextView, body: String) {
    textView.text = HtmlCompat.fromHtml(body, HtmlCompat.FROM_HTML_MODE_LEGACY)
}

