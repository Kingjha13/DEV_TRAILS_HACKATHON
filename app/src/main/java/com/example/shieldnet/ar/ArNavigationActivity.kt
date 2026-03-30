package com.example.shieldnet.ar

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.shieldnet.R
import com.google.ar.core.Session
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ArNavigationActivity : AppCompatActivity() {

    private var arSession: Session? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar_navigation)

        initAr()
    }

    private fun initAr() {
        try {
            arSession = Session(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        arSession?.resume()
    }

    override fun onPause() {
        super.onPause()
        arSession?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        arSession?.close()
    }
}