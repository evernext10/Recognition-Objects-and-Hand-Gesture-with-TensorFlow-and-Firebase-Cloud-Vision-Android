package com.example.android.tflitecamerademo.Home;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.Toast;

import com.example.android.tflitecamerademo.R;
import com.example.android.tflitecamerademo.RecognitionFirebase.RecognitionFirebaseActivity;
import com.example.android.tflitecamerademo.RecognitionObjects.CameraActivity;

public class HomeActivity extends AppCompatActivity {

    CardView information, hand, object, objectfirebase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        information = (CardView) findViewById(R.id.cardInformation);
        hand = (CardView) findViewById(R.id.cardHand);
        object = (CardView) findViewById(R.id.cardObject);
        objectfirebase = (CardView)findViewById(R.id.cardObjectFirebase);

        information.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, AboutPageDevelopers.class);
                startActivity(intent);
            }
        });

        hand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(HomeActivity.this, "¡Proximamente!", Toast.LENGTH_SHORT).show();

            }
        });

        object.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, CameraActivity.class);
                startActivity(intent);
            }
        });

        objectfirebase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, RecognitionFirebaseActivity.class);
                startActivity(intent);
            }
        });
    }
}
