/* Copyright 2017 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.example.android.tflitecamerademo.RecognitionObjectsTensorFlow;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Classifies images with Tensorflow Lite.
 */
public class ImageClassifier {

    static final int DIM_IMG_SIZE_X = 224;
    static final int DIM_IMG_SIZE_Y = 224;
    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = "HandgestureRecognition";
    /**
     * Nombre del archivo de modelo almacenado en activos.
     */
    private static final String MODEL_PATH = "hand_graph.lite";
    /**
     * Nombre del archivo de etiqueta almacenado en activos.
     */
    private static final String LABEL_PATH = "graph_label_strings.txt";
    /**
     * Número de resultados que se muestran en la interfaz de usuario.
     */
    private static final int RESULTS_TO_SHOW = 5;
    /**
     * Dimensiones de las entradas.
     */
    private static final int DIM_BATCH_SIZE = 1;
    private static final int DIM_PIXEL_SIZE = 3;
    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128.0f;
    private static final int FILTER_STAGES = 3;
    private static final float FILTER_FACTOR = 0.4f;
    /* Búferes preasignados para almacenar datos de imagen */
    private int[] intValues = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];
    /**
     * Una instancia de la clase de controlador para ejecutar la inferencia del modelo con Tensorflow Lite.
     */
    private Interpreter tflite;
    /**
     * Etiquetas correspondientes a la salida del modelo Vision.
     */
    private List<String> labelList;
    /**
     * Un ByteBuffer para contener datos de imagen, para ser feed en Tensorflow Lite como entradas.
     */
    private ByteBuffer imgData = null;
    /**
     * Una matriz para mantener los resultados de inferencia, que se alimentan en Tensorflow Lite como salidas.
     */
    private float[][] labelProbArray = null;
    /**
     * filtro de paso bajo multi-etapa
     **/
    private float[][] filterLabelProbArray = null;
    private PriorityQueue<Map.Entry<String, Float>> sortedLabels =
            new PriorityQueue<>(
                    RESULTS_TO_SHOW,
                    new Comparator<Map.Entry<String, Float>>() {
                        @Override
                        public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
                            return (o1.getValue()).compareTo(o2.getValue());
                        }
                    });

    /**
     * Inicializa un {@code ImageClassifier}.
     */
    ImageClassifier(Activity activity) throws IOException {
        tflite = new Interpreter(loadModelFile(activity));
        labelList = loadLabelList(activity);
        imgData =
                ByteBuffer.allocateDirect(
                        4 * DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE);
        imgData.order(ByteOrder.nativeOrder());
        labelProbArray = new float[1][labelList.size()];
        filterLabelProbArray = new float[FILTER_STAGES][labelList.size()];
        Log.d(TAG, "Creado un clasificador de imagen Tensorflow Lite.");
    }

    /**
     * Clasifica un fotograma de la secuencia de vista previa.
     */
    String classifyFrame(Bitmap bitmap) {
        if (tflite == null) {
            Log.e(TAG, "No se ha inicializado el clasificador de imágenes; Saltamos.");
            return "Clasificador no inicializado.";
        }
        convertBitmapToByteBuffer(bitmap);
        // Aquí es donde ocurre la magia!!!
        long startTime = SystemClock.uptimeMillis();
        tflite.run(imgData, labelProbArray);
        long endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "Timecost para ejecutar inferencia de modelo: " + Long.toString(endTime - startTime));

        //  suavizar los resultados
        applyFilter();

        // imprimir los resultados
        String textToShow = printTopKLabels();
        textToShow = Long.toString(endTime - startTime) + "ms" + textToShow;
        return textToShow;
    }

    void applyFilter() {
        int num_labels = labelList.size();

        //  Filtro de paso bajo ' labelProbArray ' en la primera etapa del filtro.
        for (int j = 0; j < num_labels; ++j) {
            filterLabelProbArray[0][j] += FILTER_FACTOR * (labelProbArray[0][j] -
                    filterLabelProbArray[0][j]);
        }
        // Filtro de paso bajo cada etapa en la siguiente.
        for (int i = 1; i < FILTER_STAGES; ++i) {
            for (int j = 0; j < num_labels; ++j) {
                filterLabelProbArray[i][j] += FILTER_FACTOR * (
                        filterLabelProbArray[i - 1][j] -
                                filterLabelProbArray[i][j]);

            }
        }

        // Copie la salida del filtro de la última etapa de vuelta a ' labelProbArray '.
        for (int j = 0; j < num_labels; ++j) {
            labelProbArray[0][j] = filterLabelProbArray[FILTER_STAGES - 1][j];
        }
    }

    /**
     * Cierra tflite para liberar recursos.
     */
    public void close() {
        tflite.close();
        tflite = null;
    }

    /**
     * Lee la lista de etiquetas de los activos.
     */
    private List<String> loadLabelList(Activity activity) throws IOException {
        List<String> labelList = new ArrayList<String>();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(activity.getAssets().open(LABEL_PATH)));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    /**
     * Memoria-mapear el archivo modelo en activos.
     */
    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * Escribe datos de imagen en un  {@code ByteBuffer}.
     */
    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (imgData == null) {
            return;
        }
        imgData.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        // Convertir la imagen a punto flotante.
        int pixel = 0;
        long startTime = SystemClock.uptimeMillis();
        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                final int val = intValues[pixel++];
                imgData.putFloat((((val >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                imgData.putFloat((((val >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                imgData.putFloat((((val) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
            }
        }
        long endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "Timecost para poner valores en ByteBuffer: " + Long.toString(endTime - startTime));
    }

    /**
     * Imprime las etiquetas Top-K, que se mostrarán en UI como los resultados.
     */
    private String printTopKLabels() {
        for (int i = 0; i < labelList.size(); ++i) {
            sortedLabels.add(
                    new AbstractMap.SimpleEntry<>(labelList.get(i), labelProbArray[0][i]));
            if (sortedLabels.size() > RESULTS_TO_SHOW) {
                sortedLabels.poll();
            }
        }
        String textToShow = "";
        final int size = sortedLabels.size();
        for (int i = 0; i < size; ++i) {
            Map.Entry<String, Float> label = sortedLabels.poll();
            switch (label.getKey()) {
                case "0":
                    textToShow = String.format("\nNumero (%s) = %4.2f", label.getKey(), label.getValue()) + textToShow;
                    break;
                case "1":
                    textToShow = String.format("\nNumero (%s) = %4.2f", label.getKey(), label.getValue()) + textToShow;
                    break;
                case "2":
                    textToShow = String.format("\nNumero (%s) = %4.2f", label.getKey(), label.getValue()) + textToShow;
                    break;
                case "3":
                    textToShow = String.format("\nNumero (%s) = %4.2f", label.getKey(), label.getValue()) + textToShow;
                    break;
                case "4":
                    textToShow = String.format("\nNumero (%s) = %4.2f", label.getKey(), label.getValue()) + textToShow;
                    break;
                case "5":
                    textToShow = String.format("\nNumero (%s) = %4.2f", label.getKey(), label.getValue()) + textToShow;
                    break;
                case "a":
                    textToShow = String.format("\nVocal (%s) = %4.2f", label.getKey(), label.getValue()) + textToShow;
                    break;
                case "e":
                    textToShow = String.format("\nVocal (%s) = %4.2f", label.getKey(), label.getValue()) + textToShow;
                    break;
                case "i":
                    textToShow = String.format("\nVocal (%s) = %4.2f", label.getKey(), label.getValue()) + textToShow;
                    break;
                case "o":
                    textToShow = String.format("\nVocal (%s) = %4.2f", label.getKey(), label.getValue()) + textToShow;
                    break;
                case "u":
                    textToShow = String.format("\nVocal (%s) = %4.2f", label.getKey(), label.getValue()) + textToShow;
                    break;
            }

        }
        return textToShow;
    }
}
