/*

 * BoardView
 *
 * Version 1.0
 *
 * Author: Jan Lorenzen
 */

package com.example.sudokuscanner;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Diese Klasse ist ein custom View, welches das Gitter und die Zahlen von dem Sudoku Feld in der
 * activity_sudoku_game.xml zeichnet.
 */
public class BoardView extends View {

    private final String TAG = "BoardView";

    private final int MARGIN = 50;
    private final int ROUNDVALUE = 35;
    private final int N = 9;
    private int boardSize;
    private int cellSize;

    private final int lineColor;
    private final int digitColor;
    private final int highlightCellsColor;
    private final int highlightDigitColor;

    private final Paint gridLinePaint;
    private final Paint highlightCellsPaint;
    private final Paint highlightDigitsPaint;
    private final Paint digitPaint;

    private final Rect digitBounds;

    private final GameLogic game;

    /**
     * Konstruktor der Klasse.
     *
     * @param context Context der Aktivität
     * @param attrs   Attribute, gelesen aus attrs.xml
     */
    public BoardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        // Entpackt die Attribute aus der xml und speichert diese in einem Array
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.SudokuBoard,
                0, 0);

        // Weise alle Farben aus den Attributen zu
        try {
            lineColor = attributes.getColor(R.styleable.SudokuBoard_lineColor, 0);
            highlightCellsColor = attributes.getColor(R.styleable.SudokuBoard_highlightCellsColor, 0);
            digitColor = attributes.getColor(R.styleable.SudokuBoard_digitColor, 0);
            highlightDigitColor = attributes.getColor(R.styleable.SudokuBoard_highlightDigitColor, 0);
        } finally {
            attributes.recycle();
        }

        gridLinePaint = new Paint();
        highlightCellsPaint = new Paint();
        highlightDigitsPaint = new Paint();
        digitPaint = new Paint();
        digitBounds = new Rect();

        game = new GameLogic();
    }

    /**
     * Berechnet und bestimmt die Größe des Spielfelds anhand der Größe des Bildschirms.
     *
     * @param widthMeasureSpec  Breite des View
     * @param heightMeasureSpec Höhe des View
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // Berechne die Größe des Sudoku Feldes und Zellen
        boardSize = Math.min(this.getMeasuredWidth() - MARGIN, this.getMeasuredHeight() - MARGIN);
        cellSize = boardSize / 9;
        setMeasuredDimension(boardSize, boardSize);
    }

    /**
     * Zeichnet das Sudoku Feld und die Zahlen.
     *
     * @param canvas Canvas Objekt zum Zeichnen
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Attribute, zum zeichnen des Sudoku Felds
        gridLinePaint.setStyle(Paint.Style.STROKE);
        gridLinePaint.setColor(lineColor);
        gridLinePaint.setStrokeWidth(16);
        gridLinePaint.setAntiAlias(true);

        // Attribute, zum zeichnen des hervorgehobener Zellen
        highlightCellsPaint.setStyle(Paint.Style.FILL);
        highlightCellsPaint.setColor(highlightCellsColor);
        highlightCellsPaint.setAntiAlias(true);

        // Attribute, zum hervorheben gleicher Zahlen
        highlightDigitsPaint.setStyle(Paint.Style.FILL);
        highlightDigitsPaint.setColor(highlightDigitColor);
        highlightDigitsPaint.setAntiAlias(true);

        // Attribute, zum zeichnen der Zahlen
        digitPaint.setStyle(Paint.Style.FILL);
        digitPaint.setColor(digitColor);
        digitPaint.setAntiAlias(true);

        highlightCells(canvas, game.getSelectedRow(), game.getSelectedCol());
        canvas.drawRoundRect(0, 0, boardSize, boardSize, ROUNDVALUE, ROUNDVALUE, gridLinePaint);
        drawGridLines(canvas);
        drawNumbers(canvas);
    }

    /**
     * Zeichnet die Linien des Sudoku Feldes
     *
     * @param canvas Canvas aus der Methode onDraw()
     */
    private void drawGridLines(Canvas canvas) {
        for (int pos = 1; pos < N; pos++) {
            // Alle 3 Linien zeichne dicke Linien, andernfalls dünne Linien
            if (pos % 3 == 0) {
                gridLinePaint.setStrokeWidth(12);
            } else {
                gridLinePaint.setStrokeWidth(4);
            }
            // Horizontale Linien
            canvas.drawLine(0, cellSize * pos, boardSize, cellSize * pos, gridLinePaint);
            // Vertikale Linien
            canvas.drawLine(cellSize * pos, 0, cellSize * pos, boardSize, gridLinePaint);
        }

        invalidate();
    }

    /**
     * Führt Aktion aus, wenn der Benutzer auf den View tippt.
     *
     * @param event MotionEvent, welche Art von Geste ausgeführt wurde
     * @return true, wenn Action_Down Event empfangen wurde, andernfalls false
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Lese x und y Koordinaten vom TouchEvent aus
        float x = event.getX();
        float y = event.getY();

        int action = event.getAction();

        if (action == MotionEvent.ACTION_DOWN) {
            // Berechne gedrückte Zelle anhand der x und y Koordinaten des TouchEvents
            game.setSelectedRow((int) Math.ceil(y / cellSize));
            game.setSelectedCol((int) Math.ceil(x / cellSize));
            return true;
        }

        return false;
    }

    /**
     * Zeichnet Zahlen aus einem 9x9 Array in die dem jeweiligen Index entsprechenden Zellen.
     *
     * @param canvas Canvas aus der onDraw() Methode
     */
    private void drawNumbers(Canvas canvas) {
        digitPaint.setTextSize(cellSize - 30);

        for (int row = 0; row < N; row++) {
            for (int col = 0; col < N; col++) {
                // Lese die Zahl an dem index aus dem Spielfeld Zellenarray aus
                int digit = game.getSudokuBoard()[row][col];

                // Wenn die Zahl 0 ist, zeichne nichts. Andernfalls Zeichne die Zahl
                if (digit != 0) {
                    String digitString = Integer.toString(digit);

                    // Bestimme die Dimension des Strings
                    digitPaint.getTextBounds(digitString, 0, digitString.length(), digitBounds);

                    float width = digitPaint.measureText(digitString);
                    float height = digitBounds.height();

                    // Berechne die exakten Koordinaten, damit die Ziffer zentriert in einder Zelle
                    // gezeichnet wird.
                    canvas.drawText(digitString, (col * cellSize) + ((cellSize - width) / 2),
                            (row * cellSize + cellSize) - (cellSize - height) / 2, digitPaint);
                }
            }
        }

        invalidate();
    }

    /**
     * Wenn auf eine leere Zelle gtippt wird, hebt die Zellen hervor, die sich in der getippten
     * Reihe und Spalte befinden. Wenn auf eine nicht leere Zelle getippt wird, werden alle Zahlen
     * auf dem Spielfeld hervorgehoben, die gleich der Zahl sind, die sich in der getippten Zelle
     * befinden.
     *
     * @param canvas Canvas aus der onDraw() Methode
     * @param row    Reihen index +1 von der getippten Zelle
     * @param col    Spalten index +1 von der getippten Zelle
     */
    private void highlightCells(Canvas canvas, int row, int col) {
        // Aktiviert, wenn auf eine valide Zelle getippt wurde
        if (game.getSelectedCol() != -1 && game.getSelectedRow() != -1) {
            try {
                int cellContent = game.getSudokuBoard()[row - 1][col - 1];

                // Wenn auf eine leere Zelle getippt wird...
                if (cellContent == 0) {
                    // ...zeichne ein Rechteck über die komplette Spalte
                    canvas.drawRect((col - 1) * cellSize, 0, col * cellSize,
                            cellSize * N, highlightCellsPaint);

                    // ...zeichne ein Rechteck über die komplette Reihe
                    canvas.drawRect(0, (row - 1) * cellSize, cellSize * N,
                            row * cellSize, highlightCellsPaint);

                    // ...zeichne die getippte Zelle mit einer (leicht) anderen Farbe
                    canvas.drawRect((col - 1) * cellSize, (row - 1) * cellSize, col * cellSize,
                            row * cellSize, highlightDigitsPaint);
                } else {
                    // Wenn auf eine nicht leere Zelle getippt wird, finde alle gleichen Zahlen und
                    // zeichne einen Kreis
                    for (int r = 0; r < N; r++) {
                        for (int c = 0; c < N; c++) {
                            if (game.getSudokuBoard()[r][c] == cellContent) {
                                canvas.drawCircle(c * cellSize + cellSize / 2, r * cellSize + cellSize / 2,
                                        cellSize / 2 - 4, highlightCellsPaint);
                            }
                        }
                    }
                    // Markiere die getippte Zelle mit einem Kreis mit einer (leicht) anderen Farbe
                    canvas.drawCircle((col - 1) * cellSize + cellSize / 2, (row - 1) * cellSize + cellSize / 2,
                            cellSize / 2 - 4, highlightDigitsPaint);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }

        invalidate();
    }

    /**
     * Gibt das aktive Spiel zurück
     *
     * @return GameLogic Objekt vom aktiven Spiel
     */
    public GameLogic getGame() {
        return this.game;
    }
}
