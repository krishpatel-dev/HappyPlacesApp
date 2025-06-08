package com.krishhh.happyplaces

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.krishhh.happyplaces.databinding.ActivityAddHappyPlaceBinding

class AddHappyPlaceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddHappyPlaceBinding

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
    }
}
