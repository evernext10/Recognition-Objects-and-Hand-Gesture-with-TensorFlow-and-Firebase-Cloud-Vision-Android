package com.example.android.tflitecamerademo.Home

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.android.tflitecamerademo.R
import com.example.android.tflitecamerademo.RecognitionFirebase.RecognitionFirebaseActivity
import com.example.android.tflitecamerademo.RecognitionObjects.CameraActivity
import com.example.android.tflitecamerademo.RecognitionObjectsTensorFlow.Camera2Activity
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        cardHand.setOnClickListener {
            val intent = Intent(this@HomeActivity, Camera2Activity::class.java)
            startActivity(intent)
        }
        cardObject.setOnClickListener {
            val intent = Intent(this@HomeActivity, CameraActivity::class.java)
            startActivity(intent)
        }
        cardObjectFirebase.setOnClickListener {
            val intent = Intent(this@HomeActivity, RecognitionFirebaseActivity::class.java)
            startActivity(intent)
        }
    }
}