package com.cxromos.placebook.repository

import android.arch.lifecycle.LiveData
import android.content.Context
import com.cxromos.placebook.R
import com.cxromos.placebook.db.BookmarkDao
import com.cxromos.placebook.db.PlaceBookDatabase
import com.cxromos.placebook.model.Bookmark
import com.google.android.libraries.places.api.model.Place

class BookmarkRepo(private val context: Context) {
    private var db: PlaceBookDatabase = PlaceBookDatabase.getInstance(context)
    private var bookmarkDao: BookmarkDao = db.bookmarkDao()
    private var categoryMap: HashMap<Int, String> = buildCategoryMap()
    private var allCategories: HashMap<String, Int> = buildCategories()

    fun getLiveBookmark(bookmarkId: Long): LiveData<Bookmark> {
        return bookmarkDao.loadLiveBookmark(bookmarkId)
    }

    fun addBookmark(bookmark: Bookmark): Long? {
        val newId = bookmarkDao.insertBookmark(bookmark)
        bookmark.id = newId
        return newId
    }

    fun createBookmark(): Bookmark {
        return Bookmark()
    }

    fun updateBookmark(bookmark: Bookmark) {
        bookmarkDao.updateBookmark(bookmark)
    }

    fun getBookmark(bookmarkId: Long): Bookmark {
        return bookmarkDao.loadBookmark(bookmarkId)
    }

    fun deleteBookmark(bookmark: Bookmark) {
        bookmark.deleteImage(context)
        bookmarkDao.deleteBookmark(bookmark)
    }

    val allBookmarks: LiveData<List<Bookmark>>
        get() {
            return bookmarkDao.loadAll()
    }

    fun placeTypeToCategory(placeType: Int): String {
        var category = "Other"
        if (categoryMap.containsKey(placeType)) {
            category = categoryMap[placeType].toString()
        }
        return category
    }

    fun getCategoryResourceId(placeCategory: String): Int? {
        return allCategories[placeCategory]
    }

    private fun buildCategories(): HashMap<String, Int> {
        return hashMapOf(
            "Gas" to R.drawable.ic_gas,
            "Lodging" to R.drawable.ic_lodging,
            "Other" to R.drawable.ic_other,
            "Restaurant" to R.drawable.ic_restaurant,
            "Shopping" to R.drawable.ic_shopping
        )
    }

    private fun buildCategoryMap() : HashMap<Int, String> {
        return hashMapOf(
            Place.Type.BAKERY.ordinal to "Restaurant",
            Place.Type.BAR.ordinal to "Restaurant",
            Place.Type.CAFE.ordinal to "Restaurant",
            Place.Type.FOOD.ordinal to "Restaurant",
            Place.Type.RESTAURANT.ordinal to "Restaurant",
            Place.Type.MEAL_DELIVERY.ordinal to "Restaurant",
            Place.Type.MEAL_TAKEAWAY.ordinal to "Restaurant",
            Place.Type.GAS_STATION.ordinal to "Gas",
            Place.Type.CLOTHING_STORE.ordinal to "Shopping",
            Place.Type.DEPARTMENT_STORE.ordinal to "Shopping",
            Place.Type.FURNITURE_STORE.ordinal to "Shopping",
            Place.Type.GROCERY_OR_SUPERMARKET.ordinal to "Shopping",
            Place.Type.HARDWARE_STORE.ordinal to "Shopping",
            Place.Type.HOME_GOODS_STORE.ordinal to "Shopping",
            Place.Type.JEWELRY_STORE.ordinal to "Shopping",
            Place.Type.SHOE_STORE.ordinal to "Shopping",
            Place.Type.SHOPPING_MALL.ordinal to "Shopping",
            Place.Type.STORE.ordinal to "Shopping",
            Place.Type.LODGING.ordinal to "Lodging",
            Place.Type.ROOM.ordinal to "Lodging"
        )
    }

    val categories: List<String>
        get() = ArrayList(allCategories.keys)
}













