package com.cxromos.placebook.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.cxromos.placebook.R
import com.cxromos.placebook.viewmodel.BookmarkDetailsViewModel
import kotlinx.android.synthetic.main.activity_bookmark_detail.*

class BookmarkDetailsActivity : AppCompatActivity() {
  private lateinit var bookmarkDetailsViewModel: BookmarkDetailsViewModel
  private var bookmarkDetailsView: BookmarkDetailsViewModel.BookmarkDetailsView? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_bookmark_detail)
    setupToolbar()

    setupViewModel()
    getIntentData()
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    val inflater = menuInflater
    inflater.inflate(R.menu.menu_bookmark_details, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when(item.itemId) {
      R.id.action_save -> {
        saveChanges()
        return true
      }
      else -> return super.onOptionsItemSelected(item)
    }
  }

  private fun setupToolbar() {
    setSupportActionBar(toolbar)
  }

  private fun setupViewModel() {
    bookmarkDetailsViewModel = ViewModelProviders.of(this).get(BookmarkDetailsViewModel::class.java)
  }

  private fun populateFields() {
    bookmarkDetailsView?.let { bookmarkView ->
      editTextName.setText(bookmarkView.name)
      editTextPhone.setText(bookmarkView.phone)
      editTextNotes.setText(bookmarkView.notes)
      editTextAddress.setText(bookmarkView.address)
    }
  }

  private fun populateImageView() {
    bookmarkDetailsView?.let { bookmarkView ->
      val placeImage = bookmarkView.getImage(this)
      placeImage?.let {
        imageViewPlace.setImageBitmap(placeImage)
      }
    }
  }

  private fun saveChanges() {
    val name = editTextName.text.toString()
    if (name.isEmpty()) {
      return
    }
    bookmarkDetailsView?.let { bookmarkView ->
      bookmarkView.name = editTextName.text.toString()
      bookmarkView.notes = editTextNotes.text.toString()
      bookmarkView.address = editTextAddress.text.toString()
      bookmarkView.phone = editTextPhone.text.toString()
      bookmarkDetailsViewModel.updateBookmark(bookmarkView)
    }
    finish()
  }

  private fun getIntentData() {
    val bookmarkId = intent.getLongExtra(MapsActivity.EXTRA_BOOKMARK_ID, 0)
    bookmarkDetailsViewModel.getBookmark(bookmarkId)?.observe(this, Observer<BookmarkDetailsViewModel.BookmarkDetailsView> {
      it?.let {
        bookmarkDetailsView = it
        populateFields()
        populateImageView()
      }
    })
  }
}
























