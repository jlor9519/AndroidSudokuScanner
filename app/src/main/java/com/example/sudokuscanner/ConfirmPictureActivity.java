/*

 * ConfirmPictureActivity
 *
 * Version 1.0
 *
 * Author: Jan Lorenzen
 */

package com.example.sudokuscanner;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Diese Klasse enthält Methoden um ein Bild aus der Gallery einzulesen oder über die Kamera ein Bild
 * aufzunehmen, je nachdem was für eine Auswahl in der MainActivity getroffen wurde. Nach dem
 * einlesen, kann der Benutzer die Auswahl nochmal einsehen, das Bild rotieren und die Auswahl
 * bestätigen oder abbrechen.
 * Das zu dieser Klasse gehörende Layout ist "activity_confirm_picture.xml".
 */
public class ConfirmPictureActivity extends AppCompatActivity {

    private final String TAG = "ConfirmPictureActivity";
    private final String ERROR_TEXT = "Something went wrong, please try again..";

    private ImageView selectedImageIv;
    private Button cancelBtn, confirmBtn, rotateLeftBtn, rotateRightBtn;
    private Uri imageUri = null;
    private int resultCode;
    private Mat[][] cellsMat;
    private int[][] cellArray;
    private Bitmap selectedImage;

    private Boolean debug, saveImg;

    private SudokuAnalyser sudoku;
    private DigitClassifier digitClassifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_picture);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        debug = sharedPref.getBoolean("debug", false);
        saveImg = sharedPref.getBoolean("saveImg", false);

        selectedImageIv = findViewById(R.id.selectedImageIv);
        cancelBtn = findViewById(R.id.cancelBtn);
        confirmBtn = findViewById(R.id.confirmBtn);
        rotateLeftBtn = findViewById(R.id.rorateLeftBtn);
        rotateRightBtn = findViewById(R.id.rotateRightBtn);

        // Klasse um Ziffern aus Bildern zu erkennen
        digitClassifier = new DigitClassifier(this);

        // Initialisiere den TensorflowLite Classifier
        if (!digitClassifier.isInitialized) {
            digitClassifier.initialize()
                    .addOnSuccessListener(d -> Log.d(TAG, "Success setting up digit classifier"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error to setting up digit classifier.", e));
        }

        // Der Intent sollte nur den jeweiligen Result-Code aus der Klasse MainActivity enthalten
        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            resultCode = extras.getInt("code");
            switch (resultCode) {
                case 101: /* Wähle Bild aus Gallery aus */
                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    photoPickerIntent.setType("image/*");
                    startActivityForResult(photoPickerIntent, resultCode);
                    break;
                case 102: /* Erfasse Bild mit Kamera und speichere dieses */
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.TITLE, "New Picture");
                    values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
                    imageUri = getContentResolver().insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    startActivityForResult(intent, resultCode);
                    break;
            }
        }

        /* Button onClickListeners */

        // Cancel Button beendet die Activity
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    confirmImage(selectedImage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        rotateLeftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedImage != null) {
                    selectedImage = rotateBitmap(-90);
                    selectedImageIv.setImageBitmap(selectedImage);
                }
            }
        });

        rotateRightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedImage != null) {
                    selectedImage = rotateBitmap(90);
                    selectedImageIv.setImageBitmap(selectedImage);
                }
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 101: /* Wähle Bild aus Gallery aus */
                    assert data != null;
                    // Lese die Uri aus und übergebe die Uri über das Intent an die neue Activity
                    imageUri = data.getData();
                    final InputStream imageStream;
                    try {
                        imageStream = getContentResolver().openInputStream(imageUri);
                        selectedImage = BitmapFactory.decodeStream(imageStream);
                        if (selectedImage == null) {
                            Toast.makeText(ConfirmPictureActivity.this, "Image not found", Toast.LENGTH_LONG).show();
                            finish();
                        }

                        // Check ob aus den Exifdaten ausgelesen werden kann, ob das Bild rotiert ist.
                        ExifInterface ei = new ExifInterface(getContentResolver().openInputStream(imageUri));
                        int rotation = checkRotation(ei);
                        selectedImage = rotateBitmap(rotation);
                        selectedImageIv.setImageBitmap(selectedImage);
                    } catch (IOException e) {
                        e.printStackTrace();
                        finish();
                    }
                    break;
                case 102: /* Erfasse Bild mit Kamera */
                    // Wenn Bild mit der Kamera erfasst wird, konvertiere es in eine Bitmap
                    try {
                        selectedImage = MediaStore.Images.Media.getBitmap(
                                getContentResolver(), imageUri);
                        // Check ob aus den Exifdaten ausgelesen werden kann, ob das Bild rotiert ist.
                        ExifInterface ei = new ExifInterface(getContentResolver().openInputStream(imageUri));
                        int rotation = checkRotation(ei);
                        selectedImage = rotateBitmap(rotation);
                        if (!saveImg) {
                            deleteImageFromStorage();
                        }
                        confirmImage(selectedImage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
        // Wenn kein Bild geladen wurde, beende die Activity
        if (selectedImageIv.getDrawable() == null) {
            finish();
        }
    }

    /**
     * Löscht ein Bild wenn der komplette Speicherpfad bekannt ist.
     */
    private void deleteImageFromStorage() {
        File file = new File(getPathFromURI(imageUri));

        if (file.exists()) {
            if (file.delete()) {
                Log.d(TAG, "File Deleted");
            } else {
                Log.d(TAG, "File not Deleted");
            }
        }
    }

    /**
     * Liest anhand der Exif Daten aus, ob ein Bild rotiert ist.
     *
     * @param ei ExifInterface, welches bereits die Daten eines Bildes enthält
     * @return int, Gradzahl um die das Bild rotiert werden muss, damit es gerade ausgerichtet ist.
     */
    private int checkRotation(ExifInterface ei) {
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED);
        Log.d(TAG, String.valueOf(orientation));

        switch (orientation) {

            case ExifInterface.ORIENTATION_ROTATE_90:
                Log.d(TAG, "ROTATION 90 DEGREES");
                return 90;

            case ExifInterface.ORIENTATION_ROTATE_180:
                Log.d(TAG, "IN ROTATION 180 DEGREES");
                return 180;

            case ExifInterface.ORIENTATION_ROTATE_270:
                Log.d(TAG, "IN ROTATION 270 DEGREES");
                return 270;

            default:
                Log.d(TAG, "IN ROTATION DEFAULT");
                return 0;
        }
    }

    /**
     * Liest den Pfad eines Bildes aus, wenn die Uri bekannt ist
     *
     * @param contentUri Uri eines Bildes
     * @return Pfad des Bildes als String
     */
    public String getPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }


    /**
     * Startet die Analyse des ausgewählten Bildes mithilfe der Klasse SudokuAnalyser()
     *
     * @param image Bitmap des ausgewählten Bildes
     * @throws IOException Falls das ausgewählte Bild nicht gefunden / geladen werden konnte
     */
    private void confirmImage(Bitmap image) throws IOException {
        sudoku = new SudokuAnalyser();
        // Beinhaltet 81 Bilder der einzelnen Zellen
        cellsMat = sudoku.analyseSudokuPipeline(image, debug);
        if (checkIfEmpty(cellsMat)) {
            emptyArray();
        }
        // Klassifiziert die Bilder aus dem cellsMat Array
        classifyCells();

        if (checkIfEmpty(cellArray)) {
            emptyArray();
        }

        // Intent, welches die Aktivität activity_sudoku_game.xml und die dazu gehörige Klasse startet.
        // Übergeben wird das Array mit den Klassifizierten Zahlen.
        Intent switchToGame = new Intent(getApplicationContext(), SudokuGameActivity.class);
        switchToGame.putExtra("cells", cellArray);
        startActivity(switchToGame);
        finish();
    }

    private void emptyArray() {
        Toast.makeText(ConfirmPictureActivity.this, ERROR_TEXT, Toast.LENGTH_LONG).show();
        finish();
    }

    /**
     * Klassifiziert die Mat Objekte aus dem cellsMat Array und liest die Zahlen aus den Bildern aus.
     */
    private void classifyCells() {
        cellArray = new int[9][9];

        if (digitClassifier.isInitialized) {
            for (int col = 0; col < 9; col++) {
                for (int row = 0; row < 9; row++) {
                    Mat currentCell = cellsMat[col][row];

                    // Wenn das Bild komplett schwarz ist, ist das eine leere Zelle. Weise den Wert
                    // 0 zu.
                    if (Core.countNonZero(currentCell) == 0) {
                        cellArray[col][row] = 0;
                    } else {
                        // Erstelle eine leere Bitmap, mit den selben Dimensionen wie die zu
                        // analysierende Zelle
                        Bitmap cellBitmap = Bitmap.createBitmap(currentCell.cols(), currentCell.rows(),
                                Bitmap.Config.ARGB_8888);
                        // Wandel das Mat Objekt in eine Bitmap um.
                        Utils.matToBitmap(currentCell, cellBitmap);

                        // Klassifiziere die Bitmap und schreibe die Zahl an den passenden Index.
                        cellArray[col][row] = Integer.parseInt(digitClassifier.classify(cellBitmap));
                    }
                }
            }
        }
    }

    /**
     * Dreht eine Bitmap um den angebenen Winkel
     *
     * @param angle Winkel, um den die Bitmap gedreht werden soll
     * @return Neue, rotierte Bitmap
     */
    private Bitmap rotateBitmap(float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(selectedImage, 0, 0, selectedImage.getWidth(), selectedImage.getHeight(), matrix, true);
    }

    /**
     * Überprüft ob ein 2d Array nur null-Werte (wenn ein Mat[][] übergeben wurde) oder nur 0-Werte
     * (wenn ein int[][] übergeben wurde) besitzt
     *
     * @param array Mat[][] oder int[][]
     * @return true, wenn Array leer ist, false sonst
     */
    private boolean checkIfEmpty(Mat[][] array) {
        for (int col = 0; col < 9; col++) {
            for (int row = 0; row < 9; row++) {
                if (array[row][col] != null) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkIfEmpty(int[][] array) {
        for (int col = 0; col < 9; col++) {
            for (int row = 0; row < 9; row++) {
                if (array[row][col] != 0) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "ON STOP");
        digitClassifier.close();
        super.onStop();
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "ON START");
        digitClassifier.initialize();
        super.onStart();
    }

    @Override
    public void onDestroy() {
        // Wenn die MainActivity der App beendet wird, entferne den TensorflowLite classifier
        // ebenfalls aus dem Speicher.
        Log.d(TAG, "ON DESTROY");
        digitClassifier.close();
        super.onDestroy();
    }
}