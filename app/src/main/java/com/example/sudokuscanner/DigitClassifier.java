/*

 * DigitClassifier
 *
 * Version 1.0
 *
 * Author: Jan Lorenzen
 */

package com.example.sudokuscanner;


import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Diese Klasse klassifiziert einzelne Ziffern in einem Bild. Es sollte nur eine Zahl im Bild sein
 * und keine weitere Zahlen oder andere störende Artefakte.
 */
public class DigitClassifier {

    private static final String TAG = "DigitClassifier";

    private static final int FLOAT_TYPE_SIZE = 4;
    private static final int PIXEL_SIZE = 1;
    private static final int OUTPUT_CLASSES_COUNT = 10;

    private final Context context;
    private Interpreter interpreter;
    public Boolean isInitialized;
    private final ExecutorService executorService;
    private int inputImageWidth, inputImageHeight, modelInputSize;

    public DigitClassifier(Context context) {
        this.context = context;

        this.interpreter = null;
        this.isInitialized = false;
        this.executorService = Executors.newCachedThreadPool();

        this.inputImageWidth = 28;
        this.inputImageHeight = 28;
        this.modelInputSize = 0;
    }

    /**
     * Handled die Initialisierung des TensorflowLite interpreters.
     *
     * @return Task
     */
    public Task initialize() {
        TaskCompletionSource task = new TaskCompletionSource();
        this.executorService.execute(() -> {
            try {
                initializeInterpreter();
                task.setResult(null);
                Log.d(TAG, "Success initializing interpreter");
            } catch (IOException e) {
                task.setException(e);
                Log.e(TAG, "ERROR INITIALIZING INTERPRETER");
            }

        });
        return task.getTask();
    }

    /**
     * Initialisiert den TensorflowLite interpreter.
     */
    private void initializeInterpreter() throws IOException {
        // Liest die Gewichte aus der tmnist.tflite Datei aus und initialisiert damit das Model
        AssetManager assetManager = context.getAssets();
        ByteBuffer model = loadModelFile(assetManager, "tmnist.tflite");
        Interpreter interpreter = new Interpreter(model);

        // Liest die Dimension aus der Datei aus, mit der die Bilder klassifiziert werden sollen
        int[] inputShape = interpreter.getInputTensor(0).shape();
        inputImageWidth = inputShape[1];
        inputImageHeight = inputShape[2];
        modelInputSize = FLOAT_TYPE_SIZE * inputImageWidth * inputImageHeight * PIXEL_SIZE;

        this.interpreter = interpreter;

        isInitialized = true;
        Log.d(TAG, "Initialized TFLite interpreter.");
    }

    /**
     * @param assetManager Manager um die .tflite Datei aus dem assets Ordner auszulesen.
     * @param filename     Vollständiger Dateiname der .tflite Datei
     * @return ByteBuffer mit dem Model Daten
     */
    private ByteBuffer loadModelFile(AssetManager assetManager, String filename) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(filename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * Klassifiziert eine Zahl aus einer gegebenen Bitmap.
     *
     * @param bitmap Bitmap die eine Zahl beinhaltet
     * @return Klassifizierte Zahl als String
     */
    public String classify(Bitmap bitmap) {
        int maxIndex = 0;

        if (isInitialized) {
            // Passe die Größe der Bitmap an, damit das .tflite Model das einlesen kann.
            Bitmap resizedImage = Bitmap.createScaledBitmap(
                    bitmap,
                    inputImageWidth,
                    inputImageHeight,
                    true
            );

            ByteBuffer byteBuffer = convertBitmapToByteBuffer(resizedImage);

            // Array wo der komplette Output der Klassifikation gespeichert wird
            float[][] output = new float[1][OUTPUT_CLASSES_COUNT];

            // Klassifizieren
            this.interpreter.run(byteBuffer, output);

            for (int i = 1; i < output[0].length; i++) {
                // result ist die Sicherheit, mit der Zahl i die gesuchte Zahl ist.
                float result = output[0][i];

                //Log.d(TAG, "Output for " + i + ": " + result);

                // Wenn die Klassifikation eine Sicherheit von größer 70% hatte, return frühzeitig.
                if (result >= 0.98f) {
                    //Log.d(TAG, "Max Result of i = " + i + " is: " + output[0][i]);
                    Log.d(TAG, "----------------------------");
                    return Integer.toString(i);
                }

                // Aktualisiere die Zahl, wenn die Sicherheit (result) größer ist als vorher.
                maxIndex = result > output[0][maxIndex] ? i : maxIndex;
            }
        } else {
            Log.d(TAG, "TF Lite Interpreter is not initialized yet.");
        }

        return Integer.toString(maxIndex);
    }

    /**
     * Beende den TensorflowLite Interpreter.
     */
    public void close() {
        this.executorService.execute(() -> {
            Interpreter interpreter = this.interpreter;
            if (interpreter != null) {
                interpreter.close();
            }

            Log.d(TAG, "Closed TFLite interpreter.");
        });
    }

    /**
     * Normalisiert und Konvertiert die Pixelwerte einer Bitmap in einen ByteBuffer
     *
     * @param bitmap Zu klassifizierende Bitmap
     * @return ByteBuffer mit den Normalisierten Pixelwerten
     */
    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(modelInputSize);
        byteBuffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[inputImageWidth * inputImageHeight];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int pixelValue : pixels) {
            int r = (pixelValue >> 16 & 0xFF);
            int g = (pixelValue >> 8 & 0xFF);
            int b = (pixelValue & 0xFF);

            // Konvertiert RGB zu Grauwerten und normalisiert die Pixel Werte in den Bereich [0..1].
            float normalizedPixelValue = (r + g + b) / 3.0f / 255.0f;
            byteBuffer.putFloat(normalizedPixelValue);
        }
        return byteBuffer;
    }
}
