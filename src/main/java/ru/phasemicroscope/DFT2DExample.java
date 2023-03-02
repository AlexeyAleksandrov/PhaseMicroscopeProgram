package ru.phasemicroscope;

//import com.tambapps.fft4j.FastFourier2d;
//import org.apache.commons.math3.complex.Complex;
//import org.apache.commons.math3.transform.*;
//import org.apache.commons.math4.transform.FastFourierTransform

public class DFT2DExample {
//    public static void main(String[] args) {
//        int rows = 4;
//        int cols = 6;
//        double[][] input = new double[][]{{1, 1, 1, 1, 1, 1},
//                {2, 2, 2, 2, 2, 2},
//                {3, 3, 3, 3, 3, 3},
//                {4, 4, 4, 4, 4, 4}};
//        double[][] output = new double[rows][cols];
//
//        // Создаем объект DTFT2 на основе библиотеки FFT
//        FastFourierTransform fourierTransform = new FastFourierTransform(rows, cols, Transforms.TransformType.FORWARD);
////        FastFourier2d fourierTransform = new FastFourierTransformer(rows, cols, TransformType FORWARD);
//
//        // Создаем массив комплексных чисел для хранения результата трансформации
//        Complex[][] transformedData = new Complex[rows][cols];
//
//        // Применяем бпф
//        for (int i = 0; i < rows; ++i) {
//            transformedData[i] = fourierTransform.apply(input[i]);
//        }
//        for (int j = 0; j < cols; ++j) {
//            double[] column = new double[rows];
//            for (int i = 0; i < rows; ++i) {
//                column[i] = transformedData[i][j].getReal();
//            }
//            transformedData[j] = fourierTransform.apply(column);
//        }
//
//        // Получаем действительную часть результата и записываем в массив output
//        for (int i = 0; i < rows; ++i) {
//            for (int j = 0; j < cols; ++j) {
//                output[i][j] = transformedData[i][j].getReal();
//            }
//
//            // Выводим результат
//            System.out.println(Arrays.toString(output[i]));
//        }
//    }
}
