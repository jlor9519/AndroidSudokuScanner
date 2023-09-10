/*

 * SudokuAnalyser
 *
 * Version 1.0
 *
 * Author: Jan Lorenzen
 */

package com.example.sudokuscanner;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Diese Klasse beinhaltet alle Methoden um ein Bild zu Analysieren und anhand der Konturen
 * zuzuschneiden.
 */
public class SudokuAnalyser {

    private final String TAG = "SudokuAnalyser";
    private Boolean debugMode = false;
    private final File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

    /**
     * Komplette Pipeline, um das Sudoku in einem Bild zu finden, die einzelnen Zellen zuzuschneiden,
     * für die Klassifikation vorzubereiten und in einem 2d-Array abzuspeichern.
     *
     * @param bitmap Bild Datei die analysiert werden soll.
     * @param debug  Checks if debug mode is on
     * @return 2-dimensionales Mat Array, welches alle 81 Zellen beinhaltet.
     */
    public Mat[][] analyseSudokuPipeline(Bitmap bitmap, Boolean debug) throws IOException {
        debugMode = debug;
        Mat image = imageToMat(bitmap);
        Mat preprocessedImage = preprocessImage(image);
        List<Point> sudokuContour = findSudoku(preprocessedImage);

        if (sudokuContour.isEmpty()) {
            return new Mat[9][9];
        }

        List<Point> corners = findCorners(sudokuContour);
        Mat warpedImage = fourPointTransform(image, corners);
        saveMatAsImage("zugeschnitten.png", warpedImage);

        return extractCells(warpedImage);
    }

    /**
     * Wandelt eine Bilddatei in ein Mat-Objekt um, was für das Arbeiten mit OpenCV notwendig ist.
     *
     * @param bitmap Zu konvertierende Bilddatei
     * @return Mat representation des ursprünglichen Bildes
     */
    public Mat imageToMat(Bitmap bitmap) {
        Mat mat = new Mat();
        // Wandelt die Bitmap in ein Mat Objekt um, welches dieselben Eigenschaften wie das Bitmap Objekt besitzt
        Utils.bitmapToMat(bitmap, mat);
        return mat;
    }

    /**
     * Invertiert die Pixel eines schwarz-weiß Bildes
     *
     * @param threshold Mat Obejkt, welches nur Schwarze (0) und Weiße (255) Pixel besitzt
     * @return Mat Objekt, welches das Invertierte Bild enthält.
     */
    private Mat invert(Mat threshold) {
        Mat inverted = new Mat(threshold.height(), threshold.width(), CvType.CV_8UC1);
        Core.bitwise_not(threshold, inverted);

        Mat kernel = new Mat(3, 3, CvType.CV_8UC1, new Scalar(0));
        kernel.row(1).setTo(new Scalar(1));
        kernel.col(1).setTo(new Scalar(1));
        Imgproc.dilate(inverted, inverted, kernel);

        return inverted;
    }

    /**
     * Wendet den Threshold-Algorithmus auf ein schwarz-weiß Bild an.
     *
     * @param blur Schwarz-weiß Mat Objekt
     * @return Mat Objekt
     */
    private Mat threshold(Mat blur) {
        Mat threshold = new Mat(blur.height(), blur.width(), CvType.CV_8UC1);
        Imgproc.adaptiveThreshold(blur, threshold, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY, 101, 10);
        return threshold;
    }

    /**
     * Wendet einen Blur-Filter auf das Bild an, um das Bild zu glätten
     *
     * @param gray Schwarz-weiß Mat Objekt
     * @return Mat Objekt
     */
    private Mat gaussianBlur(Mat gray, int size) {
        Mat blur = new Mat(gray.height(), gray.width(), CvType.CV_8UC1);
        Imgproc.GaussianBlur(gray, blur, new Size(size, size), 0);
        return blur;
    }

    /**
     * Wandelt ein Farbbild in ein schwarz-weiß Bild um
     *
     * @param image Farbiges Bild
     * @return Schwarz-weiß Bild als Mat Objekt
     */
    private Mat toGrayscale(Mat image) {
        Mat gray = new Mat(image.height(), image.width(), CvType.CV_8UC1);
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_RGB2GRAY);
        return gray;
    }

    /**
     * Bereitet das übergebene Bild so vor, dass das Sudoku gefunden und weiterverarbeitet werden
     * kann.
     *
     * @param image Ein Bild als Bitmap Obejekt
     * @return Mat Objekt, welches bereit zur weiteren Analyse ist.
     */
    public Mat preprocessImage(Mat image) throws IOException {
        Mat gray = toGrayscale(image);
        saveMatAsImage("gray.png", gray);
        Mat blur = gaussianBlur(gray, 23);
        saveMatAsImage("gauss.png", blur);
        Mat threshold = threshold(blur);
        saveMatAsImage("thresh.png", threshold);
        Mat inverted = invert(threshold);
        saveMatAsImage("invert.png", inverted);
        return inverted;
    }

    /**
     * Findet die Kontur eines Sudokus in einem gegebenen Bild.
     *
     * @param preprocessedImage Bild, welches für die Konturenfinung vorbereitet wurde
     * @return Punkte der größten (vom Flächeninhalt) quadratische Kontur als List
     */
    public List<Point> findSudoku(Mat preprocessedImage) {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchey = new Mat();
        Imgproc.findContours(preprocessedImage, contours, hierarchey, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        // Konturen nach Flächengröße sortieren
        contours.sort(new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint c1, MatOfPoint c2) {
                return (int) (Imgproc.contourArea(c2) - Imgproc.contourArea(c1));
            }
        });

        drawContours(preprocessedImage, contours, 3, "allConturs.png");

        MatOfPoint2f sudokuContour = new MatOfPoint2f();

        // Über alle Konturen iterieren. Die Kontur mit der größten Fläche und 4 zusammenhängenden
        // Seiten ist (vermutlich) ein Sudoku
        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint boxPoints = contours.get(i);
            MatOfPoint2f approx = new MatOfPoint2f(boxPoints.toArray());
            double peri = Imgproc.arcLength(approx, true);
            Imgproc.approxPolyDP(approx, approx, 0.015 * peri, true);

            if (approx.height() == 4) {
                sudokuContour = approx;
                break;
            }
        }

        MatOfPoint convertedContour = new MatOfPoint();
        sudokuContour.convertTo(convertedContour, CvType.CV_32S);
        List<MatOfPoint> contourList = new ArrayList<>();
        contourList.add(convertedContour);

        drawContours(preprocessedImage, contourList, 12, "sudokuContour.png");

        return contourList.get(0).toList();
    }

    /**
     * Findet und sortiert die Eckpunkte einer übergebenen Kontur.
     *
     * @param cornerCoordinates Liste mit den Eckpunkten einer Kontur.
     * @return Sortierte Liste der Eckpunkte
     */
    public List<Point> findCorners(List<Point> cornerCoordinates) {
        List<Point> corners = new ArrayList<>(4);

        // kleines X kleines Y
        Point topLeft = null;
        // großes X kleines Y
        Point topRight = null;
        // kleines X großes Y
        Point bottomLeft = null;
        // großes X großes Y
        Point bottomRight = null;

        Collections.sort(cornerCoordinates, new Comparator<Point>() {
            @Override
            public int compare(Point p1, Point p2) {
                return Double.compare(p1.x, p2.x);
            }
        });

        // sortiert Eckpunkte in eine Liste der Form {oben Links, oben Rechts, unten Links, unten Rechts}
        for (int i = 0; i < cornerCoordinates.size(); i += 2) {
            double currentX = cornerCoordinates.get(i).x;
            double currentY = cornerCoordinates.get(i).y;
            double nextX = cornerCoordinates.get(i + 1).x;
            double nextY = cornerCoordinates.get(i + 1).y;

            if (i <= 1) {
                if (currentY < nextY) {
                    topLeft = new Point(currentX, currentY);
                    bottomLeft = new Point(nextX, nextY);
                } else {
                    topLeft = new Point(nextX, nextY);
                    bottomLeft = new Point(currentX, currentY);
                }
            } else {
                if (currentY < nextY) {
                    topRight = new Point(currentX, currentY);
                    bottomRight = new Point(nextX, nextY);
                } else {
                    topRight = new Point(nextX, nextY);
                    bottomRight = new Point(currentX, currentY);
                }
            }
        }

        corners.add(topLeft);
        corners.add(topRight);
        corners.add(bottomLeft);
        corners.add(bottomRight);

        return corners;
    }

    /**
     * Schneidet das gefundene Sudoku zu, sodass ein quadratisches Bild erstellt wird, welches
     * nur das Sudoku beinhaltet.
     *
     * @param image   Mat Objekt vom ursprünglichen Bild
     * @param corners Liste mit den Koordinaten der vier Ecken des Sudokus
     * @return Mat Objekt vom zugeschnittenen Sudoku
     */
    public Mat fourPointTransform(Mat image, List<Point> corners) {
        Point topLeft = corners.get(0);
        Point topRight = corners.get(1);
        Point bottomLeft = corners.get(2);
        Point bottomRight = corners.get(3);

        // Breite und Höhe der Kontur
        int maxWidth = Math.max((int) bottomRight.x - (int) bottomLeft.x, (int) topRight.x - (int) topLeft.x);
        int maxHeight = Math.max((int) bottomRight.y - (int) topRight.y, (int) bottomLeft.y - (int) topLeft.y);

        List<Point> target = new ArrayList<>();
        target.add(new Point(0, 0));
        target.add(new Point(maxWidth - 1, 0));
        target.add(new Point(0, maxHeight - 1));
        target.add(new Point(maxWidth - 1, maxHeight - 1));

        Mat warp = Imgproc.getPerspectiveTransform(Converters.vector_Point2f_to_Mat(corners),
                Converters.vector_Point2f_to_Mat(target));
        Mat destImage = new Mat();

        Imgproc.warpPerspective(image, destImage, warp, new Size(maxWidth, maxHeight));
        System.out.println(destImage);
        return destImage;
    }

    /**
     * Schneidet alle Zellen aus einem Sudoku-Bild zu.
     *
     * @param warpedImage Zugeschnittenes Bild von einem Sudoku, indem NUR das Sudoku enthalten ist.
     * @return 2d Array mit allen zugeschnittenen Zellen.
     */
    public Mat[][] extractCells(Mat warpedImage) {
        Mat[][] cells = new Mat[9][9];

        int cellWidth = warpedImage.width() / 9;
        int cellHeight = warpedImage.height() / 9;

        int x, y;

        for (int col = 0; col < 9; col++) {
            for (int row = 0; row < 9; row++) {
                x = row * cellWidth;
                y = col * cellHeight;

                Rect cellArea = new Rect(x, y, cellWidth, cellHeight);
                Mat cell = new Mat(warpedImage, cellArea);

                // Wenn kein Sudoku gefunden wurde, ist die Zelle leer.
                // Gib ein leeres Mat Array zurück, um zu Signalisieren, dass kein Sudoku
                // gefunden wurde.
                if (cell.height() == 0 || cell.width() == 0) {
                    return new Mat[9][9];
                }

                cell = preprocessCell(cell);
                cells[col][row] = cell;
            }
        }
        Log.d(TAG, "Double for loop: OK");
        return cells;
    }

    /**
     * Bereitet jede einzelne Zelle für die Klassifikation vor
     * - wandelt Zelle zu schwarz-weiß um
     * - wendet einen gaussian Filter an
     * - berechnet den threshold
     * - invertiert die Farben
     * - schneidet die Zelle so zu, dass nur die Zahl enthalten ist und
     * nicht die Ränder einzelner Zellen und andere störende Merkmale
     *
     * @param cell Mat einer einzelnen Sudoku Zelle.
     * @return Mat von einer Sudoku Zelle, die bereit für die Klassifizierung ist.
     */
    private Mat preprocessCell(Mat cell) {
        // Vorverarbeitungsschritte, um die Zelle zur Konturenfindung der Zahlen vorzubereiten
        Mat gray = new Mat(cell.height(), cell.width(), CvType.CV_8UC1);
        Imgproc.cvtColor(cell, gray, Imgproc.COLOR_BGR2GRAY);
        Mat blur = gaussianBlur(gray, 7);
        Imgproc.adaptiveThreshold(blur, blur, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 101, 10);
        Core.bitwise_not(blur, blur);

        Mat kernel = new Mat(5, 5, CvType.CV_8UC1, new Scalar(0));
        kernel.row(1).setTo(new Scalar(1));
        kernel.col(1).setTo(new Scalar(1));
        Imgproc.erode(blur, blur, kernel);

        //Konturen der Zahl finden und den Bereich, wo die Zahl liegt
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.resize(blur, blur, new Size(28, 28));
        Imgproc.findContours(blur, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        Mat cleanedCell = new Mat(blur.height(), blur.width(), CvType.CV_8UC1, new Scalar(0));

        // Finde Bereich der Kontur der Zahlen
        for (MatOfPoint c : contours) {
            Rect roi = Imgproc.boundingRect(c);
            int x = roi.x;
            int y = roi.y;
            int h = roi.height;
            int w = roi.width;

            // Keine Zahl, wenn die Kontur kleiner h und w ist oder die x und y Koordinaten sehr nah am Rand liegen
            if (x < 3 || y < 3 || h < 4 || w < 4) {
                continue;
            }

            // Kopiere nur die Zahl in eine neue Zelle mit schwarzem Hintergrund
            Mat mask = new Mat(blur.height(), blur.width(), CvType.CV_8UC1, new Scalar(0));
            Imgproc.rectangle(mask, new Point(x, y), new Point(x + w, y + h), new Scalar(255, 255, 255), -1);
            blur.copyTo(cleanedCell, mask);
        }

        return cleanedCell;
    }

    private void drawContours(Mat image, List<MatOfPoint> contours, int stroke, String saveString) {
        Mat contourImg = image.clone();
        Imgproc.cvtColor(contourImg, contourImg, Imgproc.COLOR_GRAY2RGB);

        for (int i = 0; i < contours.size(); i++) {
            Imgproc.drawContours(contourImg, contours, i, new Scalar(0, 255, 0), stroke);
        }

        saveMatAsImage(saveString, contourImg);
    }

    /**
     * Speichert eine Mat als Bild auf dem System, wenn die Einstellung aktiviert ist.
     * @param filename name und Dateiendung die das gespeicherte Bild haben soll
     * @param mat      Mat welches als Bild gespeichert werden soll
     */
    private void saveMatAsImage(String filename, Mat mat) {
        if (debugMode) {
            Log.d(TAG, "DEBUG MODE ON");
            File file = new File(path, filename);

            filename = file.toString();
            Imgcodecs.imwrite(filename, mat);
            System.out.println("SAVED");
        }
    }
}
