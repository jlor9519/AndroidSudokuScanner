/*

 * MainActivity
 *
 * Version 1.0
 *
 * Author: Jan Lorenzen
 */

package com.example.sudokuscanner;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import org.opencv.android.OpenCVLoader;

/**
 * Diese Klasse beinhaltet die Main Methode und wird beim Start der App zuerst aufgerufen.
 * Von hier hat der Benutzer die Wahl über die Kamera ein Bild aufzunehmen, aus der Gallery App ein
 * Bild auszuwählen oder die Einstellungen aufzurufen.
 * Das zu dieser Klasse gehörende Layout ist "activity_main.xml".
 */
public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";

    // Result-Code wenn Bild aus Gallery geladen wird
    private final int RESULT_LOAD_IMG = 101;
    // Result-Code wenn Bild mit Kamera erfasst wird
    private final int RESULT_LOAD_CAM = 102;

    private ImageView logoIv;
    private Button useCamBtn, usePicBtn, settingsBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Verhindere dark-mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Lade die OpenCV Libraries
        OpenCVLoader.initDebug();

        logoIv = findViewById(R.id.logoImageView);
        useCamBtn = findViewById(R.id.useCamBtn);
        usePicBtn = findViewById(R.id.usePicBtn);
        settingsBtn = findViewById(R.id.settingsBtn);


        /* Button onClickListeners */
        usePicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent confirmPictureIntent = new Intent(getApplicationContext(), ConfirmPictureActivity.class);
                confirmPictureIntent.putExtra("code", RESULT_LOAD_IMG);
                startActivity(confirmPictureIntent);
            }
        });

        useCamBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Aktiviere die Kamera App zum erfassen eines Bildes
                Intent cameraIntent = new Intent(getApplicationContext(), ConfirmPictureActivity.class);
                cameraIntent.putExtra("code", RESULT_LOAD_CAM);
                startActivity(cameraIntent);
            }
        });

        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Aktiviere die Kamera App zum erfassen eines Bildes
                Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(settingsIntent);
            }
        });
    }
}