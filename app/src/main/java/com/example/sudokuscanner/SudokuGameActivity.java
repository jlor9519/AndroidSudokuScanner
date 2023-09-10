/*

 * SudokuGameActivity
 *
 * Version 1.0
 *
 * Author: Jan Lorenzen
 */

package com.example.sudokuscanner;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Chronometer;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;

/**
 * Diese Klasse managed das Sudoku Spiel.
 * Das zu dieser Klasse gehörende Layout ist "activity_sudoku_game.xml".
 */
public class SudokuGameActivity extends AppCompatActivity {

    private final String TAG = "SudokuGameActivity";
    private final String ERROR_TEXT = "Something went wrong, please try again..";

    private Chronometer timer;
    private BoardView sudokuBoard;
    private GameLogic game;

    private int[][] cellArray;
    private long timeWhenPause;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sudoku_game);

        // extras sollte das Array mit den Zahlen des analysierten Sudoku Feldes beinhalten.
        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            cellArray = (int[][]) extras.getSerializable("cells");
        }

        if (!isSudoku(cellArray)) {
            Toast.makeText(SudokuGameActivity.this, ERROR_TEXT, Toast.LENGTH_LONG).show();
            finish();
        }

        timer = findViewById(R.id.timer);
        timer.start();

        sudokuBoard = findViewById(R.id.sudokuBoard);
        // Liest das aktive Spiel aus, welches in der Klasse BoardView initialisiert wurde.
        game = sudokuBoard.getGame();
        game.setSudokuBoard(cellArray);
        game.setSolvable(game.solveSudoku(game.getSolvedSudokuBoard(), 0, 0));

        if (!game.getSolvable()) {
            String text = "Couldn't read Sudoku..";
            Toast.makeText(SudokuGameActivity.this, text, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * Buttons 1-9 sowie der delete Button fügen die jeweiligen Zahlen an den jeweiligen Indizes hinzu, von der Zelle die
     * vom Benutzer angetippt wurde. Das hinzufügen erfolgt über die setNumberPos() Methode aus der
     * Klasse GameLogic(). Bei den Zahlen 1-9 wird ebenfalls immer gecheckt, ob das Sudoku mit dem
     * hinzufügen der Zahl gelöst wurde.
     */
    public void oneBtnPressed(View view) {
        game.setNumberPos(1);
        checkIfSolved();
        sudokuBoard.invalidate();
    }

    public void twoBtnPressed(View view) {
        game.setNumberPos(2);
        checkIfSolved();
        sudokuBoard.invalidate();
    }

    public void threeBtnPressed(View view) {
        game.setNumberPos(3);
        checkIfSolved();
        sudokuBoard.invalidate();
    }

    public void fourBtnPressed(View view) {
        game.setNumberPos(4);
        checkIfSolved();
        sudokuBoard.invalidate();
    }

    public void fiveBtnPressed(View view) {
        game.setNumberPos(5);
        checkIfSolved();
        sudokuBoard.invalidate();
    }

    public void sixBtnPressed(View view) {
        game.setNumberPos(6);
        checkIfSolved();
        sudokuBoard.invalidate();
    }

    public void sevenBtnPressed(View view) {
        game.setNumberPos(7);
        checkIfSolved();
        sudokuBoard.invalidate();
    }

    public void eightBtnPressed(View view) {
        game.setNumberPos(8);
        checkIfSolved();
        sudokuBoard.invalidate();
    }

    public void nineBtnPressed(View view) {
        game.setNumberPos(9);
        checkIfSolved();
        sudokuBoard.invalidate();
    }

    public void delBtnPressed(View view) {
        game.setNumberPos(0);
        sudokuBoard.invalidate();
    }

    /**
     * Beendet die Aktivität
     */
    public void menuBtnPressed(View view) {
        finish();
    }

    /**
     * Deckt eine ungelöste Zahl auf
     */
    public void hintBtnPressed(View view) {
        game.hint();
        checkIfSolved();
        sudokuBoard.invalidate();
    }

    /**
     * Zeigt das gelöste Sudoku an
     */
    public void solveBtnPressed(View view) {
        // Falls das Sudoku board nicht lösbar sein sollte, gib einen Toast aus
        if (!game.getSolvable() && Arrays.deepEquals(game.getSudokuBoard(), game.getSolvedSudokuBoard())) {
            Toast.makeText(this, ERROR_TEXT, Toast.LENGTH_LONG).show();
        }

        // Falls das Spielfeld noch nicht gelöst ist, Zeig einen Alert an. Wenn mit ja bestätigt wird,
        // löse das Sudoku, bei nein wird das Sudoku nicht gelöst
        else if (!game.checkIfSolved()) {
            int elapsedMillis = (int) (SystemClock.elapsedRealtime() - timer.getBase());
            String elapsedTime = millisecondsToString(elapsedMillis);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Solve sudoku");
            builder.setMessage("Are you sure you want to solve this sudoku?");
            builder.setCancelable(true);

            builder.setPositiveButton(
                    "Yes",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            game.setSudokuBoard(game.getSolvedSudokuBoard());
                            //game.isSolved = true;
                            game.setIsSolved(true);
                            timer.stop();
                        }
                    });

            builder.setNegativeButton(
                    "No",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });

            AlertDialog alert = builder.create();
            alert.show();
        }
        sudokuBoard.invalidate();
    }

    /**
     * Überprüft, ob das Sudoku Feld vom Benutzer gelöst wurde, indem die gleichnamige Methode der Klasse
     * GameLogic ausgelöst wird. Wenn das Sudoku vom Benutzer gelöst wurde, wird der Timer angehalten
     * und ein Alert ausgelöst. Mit dem Alert wird dem Benutzer die Entscheidung gegeben, direkt zum
     * Menü zurückzukehren oder in der Aktivität zu bleiben. Das Spielfeld kann danach nichtmehr
     * verändert werden.
     */
    private void checkIfSolved() {
        if (game.checkIfSolved() && !game.getIsSolved()) {
            game.setIsSolved(true);
            timer.stop();

            int elapsedMillis = (int) (SystemClock.elapsedRealtime() - timer.getBase());
            String elapsedTime = millisecondsToString(elapsedMillis);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Congratulations!");
            builder.setMessage("You solved this Sudoku in " + elapsedTime + "!");
            builder.setCancelable(true);

            builder.setPositiveButton(
                    "Menu",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                        }
                    });

            builder.setNegativeButton(
                    "Cancel",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });

            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    /**
     * Konvertiert Millisekunden in Minuten und Sekunden
     *
     * @param millis Millisekunden als Long Datentyp
     * @return String in Form von MM:SS
     */
    private String millisecondsToString(long millis) {
        String minutes = String.format("%02d", (millis / 1000) / 60);
        String seconds = String.format("%02d", (millis / 1000) % 60);

        String timeString = minutes + ":" + seconds;
        return timeString;
    }

    /**
     * Checkt, wenn ein Sudoku erkannt wurde, ob es auch wirklich ein Sudoku war oder etwas anderes
     * erkannt wurde. Ein Sudoku ist ohne Raten lösbar, wenn mindestens 17 Zahlen gegeben sind. Wir
     * beschrenken die Anzahl der Zahlen hier aber nur auf 10.
     *
     * @param array Sudoku Feld
     * @return true wenn es mindestens 10 Zahlen gibt, false sonst
     */
    private boolean isSudoku(int[][] array) {
        int numberCount = 0;

        for (int col = 0; col < 9; col++) {
            for (int row = 0; row < 9; row++) {
                if (array[row][col] != 0) {
                    numberCount += 1;
                }
            }
        }
        return numberCount >= 10;
    }

    /**
     * Pausiere den Timer wenn die Aktivität / App gestoppt wird. onStart starte den Timer wieder.
     */

    @Override
    protected void onStop() {
        Log.d(TAG, "ON STOP");
        timeWhenPause = timer.getBase() - SystemClock.elapsedRealtime();
        timer.stop();
        super.onStop();
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "ON START");
        timer.setBase(SystemClock.elapsedRealtime() + timeWhenPause);
        timer.start();
        super.onStart();
    }
}