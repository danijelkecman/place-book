package com.cxromos.placebook.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.cxromos.placebook.R
import com.cxromos.placebook.ui.MapsActivity
import com.cxromos.placebook.viewmodel.MapsViewModel

class BookmarkListAdapter(
    private var bookmarkData: List<MapsViewModel.BookmarkView>?,
    private val mapsActivity: MapsActivity) : RecyclerView.Adapter<BookmarkListAdapter.ViewHolder>() {

  class ViewHolder(v: View, private val mapsActivity: MapsActivity) : RecyclerView.ViewHolder(v) {
    val nameTextView: TextView = v.findViewById(R.id.bookmarkNameTextView) as TextView
    val categoryImageView: ImageView = v.findViewById(R.id.bookmarkIcon) as ImageView
  }

  fun setBookmarkData(bookmarks: List<MapsViewModel.BookmarkView>) {
    this.bookmarkData = bookmarks
    notifyDataSetChanged()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkListAdapter.ViewHolder {
    val holder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.bookmark_item, parent, false), mapsActivity)
    return holder
  }

  override fun getItemCount(): Int {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun onBindViewHolder(p0: BookmarkListAdapter.ViewHolder, p1: Int) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

}