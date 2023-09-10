/*

 * GameLogic
 *
 * Version 1.0
 *
 * Author: Jan Lorenzen
 */

package com.example.sudokuscanner;

import java.util.Arrays;
import java.util.Random;

/**
 * Diese Klasse beinhaltet die Spiellogik und managed die Zellen des Sudoku Feldes, sowie
 * den Algorithmus um ein eingelesenes Sudoku zu lösen.
 */
public class GameLogic {

    private final String TAG = "GameLogic";

    private final int N = 9;
    private int selectedRow, selectedCol;

    private int[][] sudokuBoard, sudokuBoardSolved;

    private boolean isSolved, solvable;

    /**
     * Konstruktor. Es werden alle benötigten Variablen initialisiert, die ausgewählte Reihe
     * und Spalte, werden standartmäßig auf -1 gesetzt.
     */
    GameLogic() {
        selectedRow = -1;
        selectedCol = -1;

        sudokuBoard = new int[N][N];
        sudokuBoardSolved = new int[N][N];

        isSolved = false;
    }

    /**
     * Plaziert eine übergebene Zahl an den Index von der Zelle, die vom Benutzer angetippt wurde.
     *
     * @param num Integer von 1-9 die eingefügt werden soll.
     */
    public void setNumberPos(int num) {
        if (!isSolved) {
            // Führe aus, wenn eine gültige Zelle ausgewählt ist.
            if (this.selectedRow != -1 && this.selectedCol != -1) {
                this.sudokuBoard[this.selectedRow - 1][this.selectedCol - 1] = num;
            }
        }
    }

    /**
     * Getter und Setter für das (ungelöste) Sudoku Feld
     */
    public int[][] getSudokuBoard() {
        return this.sudokuBoard;
    }

    public void setSudokuBoard(int[][] cells) {
        this.sudokuBoard = deepCopy(cells);
        setSolvedSudokuBoard(deepCopy(cells));
    }

    /**
     * Getter und Setter für alle Variablen
     */
    public int[][] getSolvedSudokuBoard() {
        return this.sudokuBoardSolved;
    }

    public void setSolvedSudokuBoard(int[][] solvedSudoku) {
        this.sudokuBoardSolved = solvedSudoku;
    }


    public int getSelectedRow() {
        return selectedRow;
    }

    public void setSelectedRow(int row) {
        selectedRow = row;
    }


    public int getSelectedCol() {
        return this.selectedCol;
    }

    public void setSelectedCol(int col) {
        this.selectedCol = col;
    }


    public boolean getSolvable() {
        return this.solvable;
    }

    public void setSolvable(boolean solvable) {
        this.solvable = solvable;
    }


    public boolean getIsSolved() {
        return this.isSolved;
    }

    public void setIsSolved(boolean isSolved) {
        this.isSolved = isSolved;
    }

    /**
     * Kopiert den Inhalt eines 2D Arrays. Nützlich um sicher zu gehen, wenn man die Elemente
     * in einem 2D Array verändern will, ohne zu riskieren die Elemente des Originalen Arrays
     * zu verändern.
     *
     * @param original Originale 2D Array
     * @return Kopiertes 2D Array
     */
    public int[][] deepCopy(int[][] original) {
        if (original == null) {
            return null;
        }

        final int[][] result = new int[original.length][];
        for (int i = 0; i < original.length; i++) {
            result[i] = Arrays.copyOf(original[i], original[i].length);
        }
        return result;
    }

    /**
     * Fügt dem Sudoku Feld eine zufällig ausgewählte Zahl, die noch nicht gelöst wurde, aus dem
     * gelösten Array zu.
     *
     * @return true, wenn eine noch nicht aufgedeckte Zahl gefunden wurde, false wenn das Feld
     * bereits gelöst ist.
     */
    public boolean hint() {
        if (!checkIfSolved()) {
            Random rand = new Random();
            int row = rand.nextInt(9);
            int col = rand.nextInt(9);
            if (sudokuBoard[row][col] == 0 ||
                    sudokuBoard[row][col] != sudokuBoardSolved[row][col]) {
                sudokuBoard[row][col] = sudokuBoardSolved[row][col];
                return true;
            }
            return hint();
        }
        return false;
    }

    /**
     * Überprüft, ob das Sudoku Feld vom Benutzer gelöst wurde, indem das Sudoku Feld mit dem gelösten
     * Sudoku Array Zahl für Zahl verglichen wird.
     *
     * @return true, wenn alle Zahlen richtig gelöst wurden, false sobald eine falsche Zahl gefunden wurde.
     */
    public boolean checkIfSolved() {
        for (int row = 0; row < N; row++) {
            for (int col = 0; col < N; col++) {
                if (sudokuBoard[row][col] != sudokuBoardSolved[row][col]) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Methode um ein teilweise gefülltes Sudoku mit Backtracking zu lösen.
     *
     * @param grid Sudoku Feld
     * @param row  Sollte bei 0 starten und wird rekursiv erhöht
     * @param col  Sollte bei 0 starten und wird rekursiv erhöht
     * @return true wenn das Sudoku gelöst werden konnte, false wenn keine Lösung gefunden werden
     * konnte.
     */
    public boolean solveSudoku(int[][] grid, int row, int col) {

        // Wenn das Ende des Sudoku Feldes erreicht würde, gilt das Sudoku als gelöst.
        if (row == N - 1 && col == N)
            return true;

        // Wenn Ende der Spalte erreicht wurde, gehe in die nächste Reihe.
        if (col == N) {
            row++;
            col = 0;
        }

        // Wenn die Zelle bereits eine Zahl besitzt, gehe rekursiv in die nächste Spalte
        if (grid[row][col] != 0)
            return solveSudoku(grid, row, col + 1);

        for (int num = 1; num < 10; num++) {

            // Wenn eine Zahl ohne Konflikte plaziert werden kann, schreibe die Zahl und gehe rekursiv
            // in die nächste Spalte
            if (isSafe(grid, row, col, num)) {
                grid[row][col] = num;
                if (solveSudoku(grid, row, col + 1))
                    return true;
            }
            // Wenn die Zahl konflikte erzeugt, entferne die Zahl wieder
            grid[row][col] = 0;
        }
        return false;
    }

    /**
     * Überprüft ob gesetzte Zahlen mit anderen Zahlen im Konflikt stehen.
     *
     * @param grid Sudoku Feld
     * @param row  Reihe, wo die Zahl plaziert werden soll
     * @param col  Spalte, wo die Zahl plaziert werden soll
     * @param num  Zahl die plaziert werden soll
     * @return true, wenn Zahl plaziert werden kann, false wenn es ein Konflikt gibt
     */
    public boolean isSafe(int[][] grid, int row, int col,
                          int num) {

        // Check, ob die selbe Zahl in der selben Reihe liegt.
        for (int x = 0; x <= 8; x++)
            if (grid[row][x] == num)
                return false;

        // Check, ob die selbe Zahl in der selben Spalte liegt.
        for (int x = 0; x <= 8; x++)
            if (grid[x][col] == num)
                return false;

        // Check, ob die selbe Zahl in der selben Box liegt.
        int startRow = row - row % 3, startCol
                = col - col % 3;
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                if (grid[i + startRow][j + startCol] == num)
                    return false;

        return true;
    }
}
