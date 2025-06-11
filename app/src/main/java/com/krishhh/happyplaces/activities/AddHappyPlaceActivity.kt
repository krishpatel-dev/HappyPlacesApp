package com.krishhh.happyplaces.activities

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import com.google.android.gms.location.LocationRequest
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import android.provider.Settings
import android.util.Log
import com.google.android.gms.location.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.krishhh.happyplaces.BuildConfig
import com.krishhh.happyplaces.R
import com.krishhh.happyplaces.database.DatabaseHandler
import com.krishhh.happyplaces.databinding.ActivityAddHappyPlaceBinding
import com.krishhh.happyplaces.models.HappyPlaceModel
import com.krishhh.happyplaces.utils.GetAddressFromLatLng
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class AddHappyPlaceActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityAddHappyPlaceBinding
    private var cal = Calendar.getInstance()
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    private var cameFromSettingsDialog = false
    private var callingFromCamera = false
    private var saveImageToInternalStorage: Uri? = null
    private var mLatitude: Double = 0.0
    private var mLongitude: Double = 0.0

    private var mHappyPlaceDetails: HappyPlaceModel? = null

    private lateinit var mFusedLocationClient: FusedLocationProviderClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout using view binding
        binding = ActivityAddHappyPlaceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up toolbar as ActionBar
        setSupportActionBar(binding.toolbarAddPlace)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Handle back button click
        binding.toolbarAddPlace.setNavigationOnClickListener {
            onBackPressed()
        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize the places sdk if it is not initialized earlier using the api key.
        if (!Places.isInitialized()) {
            Places.initialize(
                this@AddHappyPlaceActivity,
                BuildConfig.MAPS_API_KEY
            )
        }


        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)) {
            mHappyPlaceDetails =
                intent.getParcelableExtra<HappyPlaceModel>(MainActivity.EXTRA_PLACE_DETAILS)
        }

        // create an OnDateSetListener
        dateSetListener =
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateInView()
            }
        updateDateInView()

        // Filling the existing details to the UI components to edit.
        if (mHappyPlaceDetails != null) {
            supportActionBar?.title = "Edit Happy Place"

            binding.etTitle.setText(mHappyPlaceDetails!!.title)
            binding.etDescription.setText(mHappyPlaceDetails!!.description)
            binding.etDate.setText(mHappyPlaceDetails!!.date)
            binding.etLocation.setText(mHappyPlaceDetails!!.location)
            mLatitude = mHappyPlaceDetails!!.latitude
            mLongitude = mHappyPlaceDetails!!.longitude

            saveImageToInternalStorage = Uri.parse(mHappyPlaceDetails!!.image)

            binding.ivPlaceImage.setImageURI(saveImageToInternalStorage)

            binding.btnSave.text = "UPDATE"
        }

        // We have extended the onClickListener above and the override method as onClick added and here we are setting a listener to date edittext.)
        binding.etDate.setOnClickListener(this)
        binding.tvAddImage.setOnClickListener(this)
        binding.btnSave.setOnClickListener(this)
        binding.etLocation.setOnClickListener(this)
        binding.tvSelectCurrentLocation.setOnClickListener(this)
    }

    // This is a override method after extending the onclick listener interface.)
    override fun onClick(v: View?) {
        when (v!!.id) {
            // Launching the datepicker dialog on click of date edittext.)
            R.id.et_date -> {
                DatePickerDialog(
                    this@AddHappyPlaceActivity,
                    dateSetListener, // This is the variable which have created globally and initialized in setupUI method.
                    // set DatePickerDialog to point to today's date when it loads up
                    cal.get(Calendar.YEAR), // Here the cal instance is created globally and used everywhere in the class where it is required.
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }

            R.id.tv_add_image -> {
                val pictureDialog = AlertDialog.Builder(this)
                pictureDialog.setTitle("Select Action")
                val pictureDialogItems =
                    arrayOf("Select photo from Gallery", "Capture photo from camera")
                pictureDialog.setItems(pictureDialogItems) { _, which ->
                    when (which) {
                        0 -> choosePhotoFromGallery()
                        1 -> takePhotoFromCamera()
                    }
                }
                pictureDialog.show()
            }

            R.id.btn_save -> {

                when {
                    binding.etTitle.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please enter title", Toast.LENGTH_SHORT).show()
                    }

                    binding.etDescription.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please enter description", Toast.LENGTH_SHORT).show()
                    }

                    binding.etLocation.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please select location", Toast.LENGTH_SHORT).show()
                    }

                    saveImageToInternalStorage == null -> {
                        Toast.makeText(this, "Please add image", Toast.LENGTH_SHORT).show()
                    }

                    else -> {

                        // Assigning all the values to data model class.
                        val happyPlaceModel = HappyPlaceModel(
                            if (mHappyPlaceDetails == null) 0 else mHappyPlaceDetails!!.id,
                            binding.etTitle.text.toString(),
                            saveImageToInternalStorage.toString(),
                            binding.etDescription.text.toString(),
                            binding.etDate.text.toString(),
                            binding.etLocation.text.toString(),
                            mLatitude,
                            mLongitude
                        )

                        // Here we initialize the database handler class.
                        val dbHandler = DatabaseHandler(this)

                        if (mHappyPlaceDetails == null) {
                            val addHappyPlace = dbHandler.addHappyPlace(happyPlaceModel)
                            if (addHappyPlace > 0) {
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        } else {
                            val updateHappyPlace = dbHandler.updateHappyPlace(happyPlaceModel)
                            if (updateHappyPlace > 0) {
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        }
                    }
                }
            }

            R.id.et_location -> {
                try {
                    // This is the list of fields which has to be passed.
                    val fields = listOf(
                        Place.Field.ID,
                        Place.Field.NAME,
                        Place.Field.LAT_LNG,
                        Place.Field.ADDRESS
                    )
                    // Start the autocomplete intent with a unique request code.
                    val intent =
                        Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                            .build(this@AddHappyPlaceActivity)
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Added a click event for selecting the current location
            R.id.tv_select_current_location -> {

                if (!isLocationEnabled()) {
                    Toast.makeText(
                        this,
                        "Your location provider is turned off. Please turn it on.",
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                } else {
                    Dexter.withActivity(this)
                        .withPermissions(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                        .withListener(object : MultiplePermissionsListener {
                            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                                cameFromSettingsDialog = false

                                if (report!!.areAllPermissionsGranted()) {
                                    requestNewLocationData()
                                } else if (!cameFromSettingsDialog) {
                                    Toast.makeText(
                                        this@AddHappyPlaceActivity,
                                        "Permission denied for location.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                cameFromSettingsDialog = false
                            }

                            override fun onPermissionRationaleShouldBeShown(
                                permissions: MutableList<PermissionRequest>?,
                                token: PermissionToken?
                            ) {
                                showRationalDialogForPermissions(token!!)
                            }
                        }).onSameThread()
                        .check()
                }
            }

        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GALLERY) {
                if (data != null) {
                    val contentURI = data.data
                    try {
                        val selectedImageBitmap =
                            MediaStore.Images.Media.getBitmap(this.contentResolver, contentURI)

                        saveImageToInternalStorage = saveImageToInternalStorage(selectedImageBitmap)

                        Log.e("Saved image: ", "Path :: $saveImageToInternalStorage")

                        binding.ivPlaceImage.setImageBitmap(selectedImageBitmap)
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(
                            this@AddHappyPlaceActivity,
                            "Failed to load the Image from Gallery!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else if (requestCode == CAMERA) {
                val thumbnail: Bitmap = data!!.extras!!.get("data") as Bitmap

                saveImageToInternalStorage = saveImageToInternalStorage(thumbnail)

                Log.e("Saved image: ", "Path :: $saveImageToInternalStorage")

                binding.ivPlaceImage.setImageBitmap(thumbnail)
            } else if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
                val place: Place = Autocomplete.getPlaceFromIntent(data!!)
                binding.etLocation.setText(place.address)
                mLatitude = place.latLng!!.latitude
                mLongitude = place.latLng!!.longitude
            }
        }
    }

    private fun takePhotoFromCamera() {
        val permissions =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                listOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                listOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
            }

        callingFromCamera = true

        Dexter.withActivity(this)
            .withPermissions(permissions)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    cameFromSettingsDialog = false

                    if (report?.areAllPermissionsGranted() == true) {
                        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        startActivityForResult(cameraIntent, CAMERA)
                    } else {
                        Toast.makeText(
                            this@AddHappyPlaceActivity,
                            "Permission denied for camera.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.let { showRationalDialogForPermissions(it) }
                }
            })
            .onSameThread()
            .check()
    }

    private fun choosePhotoFromGallery() {
        val permissions =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                listOf(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

        callingFromCamera = false

        Dexter.withActivity(this)
            .withPermissions(permissions)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    cameFromSettingsDialog = false

                    if (report?.areAllPermissionsGranted() == true) {
                        val intent =
                            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        startActivityForResult(intent, GALLERY)
                    } else if (!cameFromSettingsDialog) {
                        Toast.makeText(
                            this@AddHappyPlaceActivity,
                            "Permission denied for gallery.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.let { showRationalDialogForPermissions(it) }
                }
            })
            .onSameThread()
            .check()
    }


    private fun showRationalDialogForPermissions(token: PermissionToken) {
        AlertDialog.Builder(this)
            .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setCancelable(false)
            .setPositiveButton("GO TO SETTINGS") { _, _ ->
                cameFromSettingsDialog = true
                token.cancelPermissionRequest()
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                    token.cancelPermissionRequest()
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                token.cancelPermissionRequest()
            }.show()
    }


    private fun updateDateInView() {
        val myFormat = "dd.MM.yyyy" // mention the format you need
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault()) // A date format
        binding.etDate.setText(
            sdf.format(cal.time).toString()
        ) // A selected date using format which we have used is set to the UI.
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap): Uri {

        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir(IMAGE_DIRECTORY, Context.MODE_PRIVATE)

        // Create a file to save the image
        file = File(file, "${UUID.randomUUID()}.jpg")

        try {
            val stream: OutputStream = FileOutputStream(file)
            // Compress bitmap
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            // Flush the stream
            stream.flush()
            // Close stream
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        // Return the saved image uri
        return Uri.parse(file.absolutePath)
    }

    // A function which is used to verify that the location or let's GPS is enable or not of the user's device.
    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    // A function to request the current location. Using the fused location provider client.
    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {

        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1

        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback, Looper.myLooper()
        )
    }

    // A location callback object of fused location provider client where we will get the current location details.
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult!!.lastLocation
            mLatitude = mLastLocation.latitude
            Log.e("Current Latitude", "$mLatitude")
            mLongitude = mLastLocation.longitude
            Log.e("Current Longitude", "$mLongitude")

            // Called the AsyncTask class fot getting an address from the latitude and longitude.
            val addressTask =
                GetAddressFromLatLng(this@AddHappyPlaceActivity, mLatitude, mLongitude)

            addressTask.setAddressListener(object :
                GetAddressFromLatLng.AddressListener {
                override fun onAddressFound(address: String?) {
                    Log.e("Address ::", "" + address)
                    binding.etLocation.setText(address) // Address is set to the edittext
                }

                override fun onError() {
                    Log.e("Get Address ::", "Something went wrong.")
                }
            })

            addressTask.getAddress()
        }
    }


    companion object {
        private const val GALLERY = 1
        private const val CAMERA = 2
        private const val IMAGE_DIRECTORY = "Happy Places Images"
        private const val PLACE_AUTOCOMPLETE_REQUEST_CODE = 3
    }
}
