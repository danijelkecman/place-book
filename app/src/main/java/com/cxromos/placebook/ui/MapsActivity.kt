package com.cxromos.placebook.ui

import android.Manifest
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.cxromos.placebook.R
import com.cxromos.placebook.adapter.BookmarkInfoWindowAdapter
import com.cxromos.placebook.viewmodel.MapsViewModel
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {
  private lateinit var map: GoogleMap
  private lateinit var fusedLocationClient: FusedLocationProviderClient
  private lateinit var placesClient: PlacesClient
  private lateinit var mapsViewModel: MapsViewModel

  companion object {
    const val EXTRA_BOOKMARK_ID = "com.cxromos.placebook.EXTRA_BOOKMARK_ID"
    private const val REQUEST_LOCATION = 1
    private val TAG = MapsActivity::class.java.simpleName
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_maps)
    val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
    mapFragment.getMapAsync(this)
    setupLocationClient()

    Places.initialize(applicationContext, getString(R.string.google_maps_key))
    placesClient = Places.createClient(this)
  }

  private fun setupViewModel() {
    mapsViewModel = ViewModelProviders.of(this).get(MapsViewModel::class.java)

    createBookmarkMarkerObserver()
  }

  override fun onMapReady(googleMap: GoogleMap) {
    map = googleMap

    setupMapListeners()
    setupViewModel()
    getCurrentLocation()
  }

  private fun setupMapListeners() {
    map.setInfoWindowAdapter(BookmarkInfoWindowAdapter(this))
    map.setOnPoiClickListener {
      displayPoi(it)
    }
    map.setOnInfoWindowClickListener {
      handleInfoWindowClick(it)
    }
  }

  override fun onConnectionFailed(result: ConnectionResult) {
    Log.e(TAG, "Google play connection failed: " + result.errorMessage)
  }

  private fun displayPoi(poi: PointOfInterest) {
    displayPoiGetPlaceStep(poi)
  }

  private fun displayPoiGetPlaceStep(poi: PointOfInterest) {
    var placesFields = listOf(
        Place.Field.ID,
        Place.Field.NAME,
        Place.Field.ADDRESS,
        Place.Field.PHOTO_METADATAS,
        Place.Field.PHONE_NUMBER,
        Place.Field.LAT_LNG
    )
    val request = FetchPlaceRequest.builder(poi.placeId, placesFields).build()
    placesClient.fetchPlace(request).addOnSuccessListener { response ->
      val place = response.place
      displayPoiGetPhotoStep(place)
    }.addOnFailureListener { exception ->
      val apiException = exception as ApiException
      val statusCode = apiException.statusCode
      Log.e(TAG, "Place not found with error message: ${exception.message} and error status code: $statusCode")
    }
  }

  private fun displayPoiGetPhotoStep(place: Place) {
    val photoMetadata = place.photoMetadatas?.get(0)
    if (photoMetadata != null) {
      val photoRequest = FetchPhotoRequest.builder(photoMetadata)
          .setMaxWidth(resources.getDimensionPixelSize(R.dimen.default_image_width))
          .setMaxHeight(resources.getDimensionPixelSize(R.dimen.default_image_height))
          .build()
      placesClient.fetchPhoto(photoRequest).addOnSuccessListener { response ->
        val bitmap = response.bitmap
        displayPoiDisplayStep(place, bitmap)
      }.addOnFailureListener { exception ->
        val apiException = exception as ApiException
        val statusCode = apiException.statusCode
        Log.e(TAG, "Photo not found with error message: ${exception.message} and error status code: $statusCode")
      }
    } else {
      displayPoiDisplayStep(place, null)
    }
  }

  private fun displayPoiDisplayStep(place: Place, photo: Bitmap?) {
    val marker = map.addMarker(MarkerOptions()
        .position(place.latLng!!)
        .title(place.name)
        .snippet(place.phoneNumber)
    )
    marker?.tag = PlaceInfo(place, photo)
    marker?.showInfoWindow()
  }

  private fun addPlaceMarker(bookmark: MapsViewModel.BookmarkMarkerView): Marker? {
    val marker = map.addMarker(MarkerOptions()
        .position(bookmark.location)
        .title(bookmark.name)
        .snippet(bookmark.phone)
        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        .alpha(0.8f)
    )
    marker.tag = bookmark

    return marker
  }

  private fun displayAllBookmarks(bookmarks: List<MapsViewModel.BookmarkMarkerView>) {
    for (bookmark in bookmarks) {
      addPlaceMarker(bookmark)
    }
  }

  private fun createBookmarkMarkerObserver() {
    mapsViewModel.getBookmarkMarkerViews()?.observe(
        this, android.arch.lifecycle.Observer<List<MapsViewModel.BookmarkMarkerView>> {
      map.clear()

      it?.let {
        displayAllBookmarks(it)
      }
    }
    )
  }

  private fun handleInfoWindowClick(marker: Marker) {
    when(marker.tag) {
      is PlaceInfo -> {
        val placeInfo = (marker.tag as PlaceInfo)
        if (placeInfo.place != null && placeInfo.image != null) {
          GlobalScope.launch {
            mapsViewModel.addBookmarkFromPlace(placeInfo.place, placeInfo.image)
          }
        }
        marker.remove()
      }
      is MapsViewModel.BookmarkMarkerView -> {
        val bookmarkMarkerView = (marker.tag as MapsViewModel.BookmarkMarkerView)
        marker.hideInfoWindow()
        bookmarkMarkerView.id?.let {
          startBookmarkDetails(it)
        }
      }
    }

  }

  private fun setupLocationClient() {
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
  }

  private fun requestLocationPermissions() {
    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
        REQUEST_LOCATION
    )
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    if (requestCode == REQUEST_LOCATION) {
      if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        getCurrentLocation()
      } else {
        Log.e(TAG, "Location permissions denied")
      }
    }
  }

  private fun getCurrentLocation() {
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      requestLocationPermissions()
    } else {
      map.isMyLocationEnabled = true
      fusedLocationClient.lastLocation.addOnCompleteListener {
        if (it.result != null) {
          val latLng = LatLng(it.result!!.latitude, it.result!!.longitude)
          val update = CameraUpdateFactory.newLatLngZoom(latLng, 16.0f)
          map.moveCamera(update)
        } else {
          Log.e(TAG, "No location found")
        }
      }
    }
  }

  class PlaceInfo(val place: Place? = null, val image: Bitmap? = null)

  private fun startBookmarkDetails(bookmarkId: Long) {
    val intent = Intent(this, BookmarkDetailsActivity::class.java)
    intent.putExtra(EXTRA_BOOKMARK_ID, bookmarkId)
    startActivity(intent)
  }
}




























