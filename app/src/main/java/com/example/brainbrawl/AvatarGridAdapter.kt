package com.example.brainbrawl

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView

class AvatarGridAdapter(
    private val context: Context,
    private val avatarResources: Array<Int>
) : BaseAdapter() {
    override fun getCount() = avatarResources.size
    override fun getItem(position: Int) = avatarResources[position]
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val imageView = convertView as? ImageView ?: ImageView(context).apply {
            layoutParams = ViewGroup.LayoutParams(72.dp, 72.dp)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setPadding(8, 8, 8, 8)
        }
        imageView.setImageResource(avatarResources[position])
        return imageView
    }

    private val Int.dp: Int get() = (this * context.resources.displayMetrics.density).toInt()
}
