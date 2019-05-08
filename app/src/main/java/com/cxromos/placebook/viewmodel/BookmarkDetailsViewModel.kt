package com.cxromos.placebook.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import android.content.Context
import android.graphics.Bitmap
import com.cxromos.placebook.model.Bookmark
import com.cxromos.placebook.repository.BookmarkRepo
import com.cxromos.placebook.util.ImageUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class BookmarkDetailsViewModel(application: Application): AndroidViewModel(application) {
  private var bookmarkRepo: BookmarkRepo = BookmarkRepo(getApplication())

  private var bookmarkDetailsView: LiveData<BookmarkDetailsView>? = null

  fun getBookmark(bookmarkId: Long): LiveData<BookmarkDetailsView>? {
    if (bookmarkDetailsView == null) {
      mapBookmarkToBookmarkView(bookmarkId)
    }
    return bookmarkDetailsView
  }

  fun updateBookmark(bookmarkView: BookmarkDetailsView) {
    GlobalScope.launch {
      val bookmark = bookmarkViewToBookmark(bookmarkView)
      bookmark?.let { bookmarkRepo.updateBookmark(it) }
    }
  }

  private fun bookmarkViewToBookmark(bookmarkView: BookmarkDetailsView):
      Bookmark? {
    val bookmark = bookmarkView.id?.let {
      bookmarkRepo.getBookmark(it)
    }
    if (bookmark != null) {
      bookmark.id = bookmarkView.id
      bookmark.name = bookmarkView.name
      bookmark.phone = bookmarkView.phone
      bookmark.address = bookmarkView.address
      bookmark.notes = bookmarkView.notes
    }
    return bookmark
  }

  private fun bookmarkToBookmarkView(bookmark: Bookmark): BookmarkDetailsView {
    return BookmarkDetailsView(
        bookmark.id,
        bookmark.name,
        bookmark.phone,
        bookmark.address,
        bookmark.notes
    )
  }

  private fun mapBookmarkToBookmarkView(bookmarkId: Long) {
    val bookmark = bookmarkRepo.getLiveBookmark(bookmarkId)
    bookmarkDetailsView = Transformations.map(bookmark) { bookmark ->
      val bookmarkView = bookmarkToBookmarkView(bookmark)
      bookmarkView
    }
  }

  data class BookmarkDetailsView(
      var id: Long? = null,
      var name: String = "",
      var phone: String = "",
      var address: String = "",
      var notes: String = ""
  ) {

    fun getImage(context: Context): Bitmap? {
      id?.let {
        return ImageUtils.loadBitmapFromFile(context, Bookmark.generateImageFilename(it))
      }
      return null
    }
  }
}


















