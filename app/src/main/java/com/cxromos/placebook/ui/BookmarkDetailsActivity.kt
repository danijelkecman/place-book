package com.cxromos.placebook.ui

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.cxromos.placebook.R
import com.cxromos.placebook.util.ImageUtils
import com.cxromos.placebook.viewmodel.BookmarkDetailsViewModel
import kotlinx.android.synthetic.main.activity_bookmark_detail.*
import java.io.File
import java.io.IOException

class BookmarkDetailsActivity : AppCompatActivity(), PhotoOptionDialogFragment.PhotoOptionDialogListener {

  private lateinit var bookmarkDetailsViewModel: BookmarkDetailsViewModel
  private var bookmarkDetailsView: BookmarkDetailsViewModel.BookmarkDetailsView? = null
  private var photoFile: File? = null

  companion object {
    private const val REQUEST_CAPTURE_IMAGE = 1
    private const val REQUEST_GALLERY_IMAGE = 2
  }

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

    imageViewPlace.setOnClickListener {
      replaceImage()
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

  override fun onCaptureClick() {
    photoFile = null
    try {
      photoFile = ImageUtils.createUniqueImageFile(this)
      if (photoFile == null) {
        return
      }
    } catch (ex: IOException) {
      return
    }
    val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    val photoUri = FileProvider.getUriForFile(this, "com.cxromos.placebook.fileprovider", photoFile!!)
    captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

    val intentActivities = packageManager.queryIntentActivities(captureIntent, PackageManager.MATCH_DEFAULT_ONLY)
    intentActivities
      .map { it.activityInfo.packageName }
      .forEach { grantUriPermission(it, photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }

    startActivityForResult(captureIntent, REQUEST_CAPTURE_IMAGE)
  }

  override fun onPickClick() {
    val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
    startActivityForResult(pickIntent, REQUEST_GALLERY_IMAGE)
  }

  private fun replaceImage() {
    val newFragment = PhotoOptionDialogFragment.newInstance(this)
    newFragment?.show(supportFragmentManager, "photoOptionDialog")
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (resultCode == Activity.RESULT_OK) {
      when(requestCode) {
        REQUEST_CAPTURE_IMAGE -> {
          val photoFile = photoFile ?: return
          val uri = FileProvider.getUriForFile(this, "com.cxromos.placebook.fileprovider", photoFile)
          revokeUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

          val image = getImageWithPath(photoFile.absolutePath)
          image?.let {
            updateImage(it)
          }
        }
        REQUEST_GALLERY_IMAGE -> {
          val imageUri = data!!.data
          val image = getImageWithAuthority(imageUri)
          image?.let { updateImage(it) }
        }
      }
    }
  }

  private fun updateImage(image: Bitmap) {
    val bookmarkView = bookmarkDetailsView ?: return
    imageViewPlace.setImageBitmap(image)
    bookmarkView.setImage(this, image)
  }

  private fun getImageWithPath(filePath: String): Bitmap? {
    return ImageUtils.decodeFileToSize(filePath,
      resources.getDimensionPixelSize(R.dimen.default_image_width),
      resources.getDimensionPixelSize(R.dimen.default_image_height)
    )
  }

  private fun getImageWithAuthority(uri: Uri): Bitmap? {
    return ImageUtils.decodeUriStreamToSize(uri,
      resources.getDimensionPixelSize(R.dimen.default_image_width),
      resources.getDimensionPixelSize(R.dimen.default_image_height),
      this
    )
  }

}
























