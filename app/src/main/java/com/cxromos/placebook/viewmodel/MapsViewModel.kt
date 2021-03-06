package com.cxromos.placebook.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.cxromos.placebook.model.Bookmark
import com.cxromos.placebook.repository.BookmarkRepo
import com.cxromos.placebook.util.ImageUtils
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place

class MapsViewModel(application: Application): AndroidViewModel(application) {
    private val TAG = MapsViewModel::class.java.simpleName

    private var bookmarkRepo: BookmarkRepo = BookmarkRepo(getApplication())
    private var bookmarks: LiveData<List<BookmarkView>>? = null

    fun addBookmarkFromPlace(place: Place, image: Bitmap) {
        val bookmark = bookmarkRepo.createBookmark()
        bookmark.placeId = place.id ?: ""
        bookmark.name = place.name.toString()
        bookmark.latitude = place.latLng?.latitude ?: -1.0
        bookmark.longitude = place.latLng?.longitude ?: -1.0
        bookmark.phone = place.phoneNumber ?: ""
        bookmark.address = place.address ?: ""
        bookmark.category = getPlaceCategory(place)

        val newId = bookmarkRepo.addBookmark(bookmark)
        bookmark.setImage(image, getApplication())

        Log.i(TAG, "New bookmark $newId added to the database")
    }

    fun getBookmarkViews(): LiveData<List<BookmarkView>>? {
        if (bookmarks == null) {
            mapBookmarksToBookmarkView()
        }
        return bookmarks
    }

    fun addBookmark(latLng: LatLng): Long? {
        val bookmark = bookmarkRepo.createBookmark()
        bookmark.name = "Untitled"
        bookmark.longitude = latLng.longitude
        bookmark.latitude = latLng.latitude
        bookmark.category = "Other"
        return bookmarkRepo.addBookmark(bookmark)
    }

    private fun bookmarkToBookmarkView(bookmark: Bookmark): BookmarkView {
        return BookmarkView(
            bookmark.id,
            LatLng(bookmark.latitude, bookmark.longitude),
            bookmark.name,
            bookmark.phone,
            bookmarkRepo.getCategoryResourceId(bookmark.category)
        )
    }

    private fun mapBookmarksToBookmarkView() {
        val allBookmarks = bookmarkRepo.allBookmarks

        bookmarks = Transformations.map(allBookmarks) { bookmarks ->
            val bookmarkMarkerViews = bookmarks.map { bookmark ->
                bookmarkToBookmarkView(bookmark)
            }
            bookmarkMarkerViews
        }
    }

    private fun getPlaceCategory(place: Place): String {
        var category = "Other"
        val placeTypes = place.types

        if (placeTypes != null && placeTypes.size > 0) {
            val placeType = placeTypes[0]
            category = bookmarkRepo.placeTypeToCategory(placeType.ordinal)
        }

        return category
    }

    data class BookmarkView(
        var id: Long? = null,
        var location: LatLng = LatLng(0.0, 0.0),
        var name: String = "",
        var phone: String = "",
        var categoryResourceId: Int? = null
    ) {
        fun getImage(context: Context): Bitmap? {
            id?.let {
                return ImageUtils.loadBitmapFromFile(context, Bookmark.generateImageFilename(it))
            }
            return null
        }
    }
}









