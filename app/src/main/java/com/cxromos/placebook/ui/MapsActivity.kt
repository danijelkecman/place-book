package com.cxromos.placebook.ui

import android.Manifest
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import com.cxromos.placebook.R
import com.cxromos.placebook.adapter.BookmarkInfoWindowAdapter
import com.cxromos.placebook.adapter.BookmarkListAdapter
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
import kotlinx.android.synthetic.main.activity_bookmark_detail.*
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.android.synthetic.main.drawer_view_maps.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {
  private val TAG = MapsActivity::class.java.simpleName

  private lateinit var map: GoogleMap
  private lateinit var fusedLocationClient: FusedLocationProviderClient
  private lateinit var placesClient: PlacesClient
  private lateinit var mapsViewModel: MapsViewModel
  private lateinit var bookmarkListAdapter: BookmarkListAdapter
  private var markers = HashMap<Long, Marker>()

  companion object {
    const val EXTRA_BOOKMARK_ID = "com.cxromos.placebook.EXTRA_BOOKMARK_ID"
    private const val REQUEST_LOCATION = 1
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_maps)

    val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
    mapFragment.getMapAsync(this)
    setupToolbar()
    setupLocationClient()

    Places.initialize(applicationContext, getString(R.string.google_maps_key))
    placesClient = Places.createClient(this)

    setupNavigationDrawer()
  }

  private fun setupViewModel() {
    mapsViewModel = ViewModelProviders.of(this).get(MapsViewModel::class.java)

    createBookmarkObserver()
  }

  private fun setupToolbar() {
    setSupportActionBar(toolbar)
    val toggle = ActionBarDrawerToggle(this,  drawerLayout, toolbar, R.string.open_drawer, R.string.close_drawer)
    toggle.syncState()
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

  private fun updateMapToLocation(location: Location) {
    val latLng = LatLng(location.latitude, location.longitude)
    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16.0f))
  }

  fun moveToBookmark(bookmark: MapsViewModel.BookmarkView) {
    drawerLayout.closeDrawer(drawerView)
    val marker = markers[bookmark.id]
    marker?.showInfoWindow()
    val location = Location("")
    location.latitude = bookmark.location.latitude
    location.longitude = bookmark.location.longitude
    updateMapToLocation(location)
  }

  private fun setupNavigationDrawer() {
    val layoutManager = LinearLayoutManager(this)
    bookmarkRecyclerView.layoutManager = layoutManager
    bookmarkListAdapter = BookmarkListAdapter(null, this)
    bookmarkRecyclerView.adapter = bookmarkListAdapter
  }

  override fun onConnectionFailed(result: ConnectionResult) {
    Log.e(TAG, "Google play connection failed: " + result.errorMessage)
  }

  private fun displayPoi(poi: PointOfInterest) {
    displayPoiGetPlaceStep(poi)
  }

  private fun displayPoiGetPlaceStep(poi: PointOfInterest) {
    val placesFields = listOf(
      Place.Field.ID,
      Place.Field.NAME,
      Place.Field.ADDRESS,
      Place.Field.PHOTO_METADATAS,
      Place.Field.PHONE_NUMBER,
      Place.Field.LAT_LNG,
      Place.Field.TYPES
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

  private fun addPlaceMarker(bookmark: MapsViewModel.BookmarkView): Marker? {
    val marker = map.addMarker(MarkerOptions()
      .position(bookmark.location)
      .title(bookmark.name)
      .snippet(bookmark.phone)
      .icon(bookmark.categoryResourceId?.let {
        BitmapDescriptorFactory.fromResource(it)
      })
      .alpha(0.8f)
    )
    marker.tag = bookmark

    bookmark.id?.let { markers.put(it, marker) }

    return marker
  }

  private fun displayAllBookmarks(bookmarks: List<MapsViewModel.BookmarkView>) {
    for (bookmark in bookmarks) {
      addPlaceMarker(bookmark)
    }
  }

  private fun createBookmarkObserver() {
    mapsViewModel.getBookmarkViews()?.observe(
      this, android.arch.lifecycle.Observer<List<MapsViewModel.BookmarkView>> {
        map.clear()
        markers.clear()

        it?.let {
          displayAllBookmarks(it)
          bookmarkListAdapter.setBookmarkData(it)
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
      is MapsViewModel.BookmarkView -> {
        val bookmarkMarkerView = (marker.tag as MapsViewModel.BookmarkView)
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




























