package com.cxromos.placebook.adapter

import android.app.Activity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.cxromos.placebook.R
import com.cxromos.placebook.ui.MapsActivity
import com.cxromos.placebook.viewmodel.MapsViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class BookmarkInfoWindowAdapter(private val context: Activity) : GoogleMap.InfoWindowAdapter {
    private val contents: View = context.layoutInflater.inflate(R.layout.content_bookmark_info, null)

    override fun getInfoContents(marker: Marker?): View? {
        val titleView = contents.findViewById<TextView>(R.id.title)
        titleView.text = marker?.title ?: ""
        val phoneView = contents.findViewById<TextView>(R.id.phone)
        phoneView.text = marker?.snippet ?: ""
        val imageView = contents.findViewById<ImageView>(R.id.photo)
        when(marker?.tag) {
            is MapsActivity.PlaceInfo -> {
                imageView.setImageBitmap((marker.tag as MapsActivity.PlaceInfo).image)
            }

            is MapsViewModel.BookmarkMarkerView -> {
                val bookmarkView = marker.tag as MapsViewModel.BookmarkMarkerView
                imageView.setImageBitmap(bookmarkView.getImage(context))
            }
        }

        return contents
    }

    override fun getInfoWindow(marker: Marker?): View? {
        return null
    }

}