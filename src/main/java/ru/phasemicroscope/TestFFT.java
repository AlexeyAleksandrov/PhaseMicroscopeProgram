package ru.phasemicroscope;

import com.tambapps.fft4j.FastFourier2d;
import com.tambapps.fft4j.Signal2d;
import org.jtransforms.fft.DoubleFFT_2D;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


public class TestFFT
{
    public static void main(String[] args) throws IOException
    {
        String inputFileName = "src/main/resources/obj512";
        String inputFileNFormat = ".jpg";
        BufferedImage image = ImageIO.read(new File(inputFileName + inputFileNFormat));

        int n = getMatrixSizeForImage(image);   // считаем размер для матрицы
        double[][] massive = getImageMassive(image);    // получаем массив пикселей
        double[][] real = new double[n][n];
        double[][] imag = new double[n][n];
        double[][] amp = new double[n][n];

//        twoDfft(massive, real, imag, amp);

//        discrete(massive, real, imag);

        Signal2d signal2d = new Signal2d(n, n);
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                signal2d.setReAt(i, j, massive[i][j]);
            }
        }

        FastFourier2d transformer2D = new FastFourier2d();
        transformer2D.transform(signal2d);

        // do some things with the fourier transform
//        transformer2D.inverse(signal2d);

        // don't forget to shut it down as it uses an executor service
        transformer2D.shutdown();

        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                real[i][j] = signal2d.getReAt(i, j);
                imag[i][j] = signal2d.getImAt(i, j);

                double mod_z = Math.sqrt(Math.pow(real[i][j], 2) + Math.pow(imag[i][j], 2));
                massive[i][j] = mod_z;
            }
        }
//
        setImageFromMassive(massive, image);

//        fft(image);

        ImageIO.write(image, "jpg", new File(inputFileName + "_out" + inputFileNFormat));
        System.out.println("Готово!");
    }

    public static double[][] getImageMassive(BufferedImage image)
    {
        int width = image.getWidth();       // ширина изображения
        int height = image.getHeight();     // высота изображения

        int n = getMatrixSizeForImage(image);   // считаем размер для матрицы

        double[][] massive = new double[n][n];     // создаем массив размера ширина / высота

        for (int x = 0; x < width; x++)     // проходим по каждому столбцу
        {
            for (int y = 0; y < height; y++)    // проходим по каждому значению в столбце
            {
                int rgb = image.getRGB(x, y);   // получаем RGB значения пикселя

                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb & 0xFF);

                // нормализация и гамма-коррекция:
                float rr = (float) Math.pow(r / 255.0, 2.2);
                float gg = (float) Math.pow(g / 255.0, 2.2);
                float bb = (float) Math.pow(b / 255.0, 2.2);

                // рассчитываем яркость:
                float lum = (float) (0.2126 * rr + 0.7152 * gg + 0.0722 * bb);

                // гамма-сложение и масштабирование до диапазона байтов:
                double grayLevel = (255.0 * Math.pow(lum, 1.0 / 2.2));

                massive[x][y] = grayLevel;
            }
        }

        return massive;
    }

    public static void setImageFromMassive(double[][] massive, BufferedImage image)
    {
        int width = image.getWidth();       // ширина изображения
        int height = image.getHeight();     // высота изображения

        for (int x = 0; x < width; x++)     // проходим по каждому столбцу
        {
            for (int y = 0; y < height; y++)    // проходим по каждому значению в столбце
            {
                int grayLevel = (int) massive[x][y];
                int gray = (grayLevel << 16) + (grayLevel << 8) + grayLevel;
                image.setRGB(x, y, gray);   // перерисовываем пиксель в градации серого
            }
        }
    }

    // Now by taking the discrete function
    // This is the declaration of the function
    // This function includes 4 parameters
    // The parameters are the 4 matrices.
    static void discrete(double[][] input, double[][] realOut, double[][] imagOut)
    {

        // Height is the variable of data type int
        // the length of the input variable is stored in
        // variable height
        int height = input.length;

        // The input of the first index length is stored in
        // variable width
        int width = input[0].length;

        // Iterating the input till height stored in
        // variable y
        for (int y = 0; y < height; y++)
        {

            // Taking the input iterating till width in
            // variable x
            for (int x = 0; x < width; x++)
            {

                // Taking another variable y1 which will be
                // the continuation of
                // the variable y
                // This y1 will be iterating till height
                // This index of the variable starts at 0
                for (int y1 = 0; y1 < height; y1++)
                {

                    // This index x1 iterates till width
                    // This x1 is continuation of x
                    // The variables y1 and x1 are the
                    // continuation of summable of x and y
                    for (int x1 = 0; x1 < width; x1++)
                    {

                        // realOut is the variable which
                        // lets us know the real output as
                        // we do the summation of exponential
                        // signal
                        // we get cos as real term and sin
                        // as imaginary term
                        // so taking the consideration of
                        // above properties we write the
                        // formula of real as
                        // summing till x and y and
                        // multiplying it with cos2pie
                        // and then dividing it with width
                        // *height gives us the real term
                        realOut[y][x] += (input[y1][x1] * Math.cos(2 * Math.PI * ((1.0 * x * x1 / width) + (1.0 * y * y1 / height)))) / Math.sqrt(width * height);

                        // Now imagOut is the imaginary term
                        // That is the sine term
                        // This sine term can be obtained
                        // using sin2pie and then we divide
                        // it using width*height The
                        // formulae is same as real

                        imagOut[y][x] -= (input[y1][x1] * Math.sin(2 * Math.PI * ((1.0 * x * x1 / width) + (1.0 * y * y1 / height)))) / Math.sqrt(width * height);
                    }

                    // Now we will print the value of
                    // realOut and imaginary outputn The
                    // ppoutput of imaginary output will end
                    // with value 'i'.
//                    System.out.println(realOut[y][x] + " +" + imagOut[y][x] + "i");
                }
            }
        }
    }

//    public static Complex[] fft(Complex[] x) {
//        int n = x.length;
//
//        // Функция выполняет бпф вектора x, используя рекурсивный алгоритм
//        if (n == 1) {
//            return new Complex[] { x[0] };
//        }
//
//        // считаем значение чётных и нечётных элементов исходного массива
//        Complex[] even = new Complex[n/2];
//        Complex[] odd = new Complex[n/2];
//        for (int i = 0; i < n/2; i++) {
//            even[i] = x[i*2];
//            odd[i] = x[i*2 + 1];
//        }
//
//        // рекурсивно применяем алгоритм к двум половинам
//        Complex[] q = fft(even);
//        Complex[] r = fft(odd);
//
//        // объединяем результаты, чтобы получить исходный массив
//        Complex[] y = new Complex[n];
//        for (int i = 0; i < n/2; i++) {
//            double k = -2 * i * Math.PI / n;
//            Complex wk = new Complex(Math.cos(k), Math.sin(k));
//            y[i] = q[i].add(wk.multiply(r[i]));
//            y[i + n/2] = q[i].subtract(wk.multiply(r[i]));
//        }
//        return y;

    public static void fft(BufferedImage image)
    {
        int width = image.getWidth();       // ширина изображения
        int height = image.getHeight();     // высота изображения

        DoubleFFT_2D fft2D = new DoubleFFT_2D(height+1, width+1);
        double[][] massive = getImageMassive(image);    // получаем массив пикселей
        fft2D.complexForward(massive);
        setImageFromMassive(massive, image);
    }

    public static void twoDfft(double[][] inputData, double[][] realOut,
                               double[][] imagOut, double[][] amplitudeOut)
    {
        int height = inputData.length;
        int width = inputData[0].length;

        // Two outer loops iterate on output data.
        for (int yWave = 0; yWave < height; yWave++)
        {
            for (int xWave = 0; xWave < width; xWave++)
            {
                System.out.println("x = " + xWave + " из " + width + " y = "+ yWave + " из " + height + " Обработано: " + Math.round(((double) xWave + (double)yWave * (double)width) / ((double)height * (double)width) * 1000000.0)/10000.0 + " %");
                // Two inner loops iterate on input data.
                for (int ySpace = 0; ySpace < height; ySpace++)
                {
                    for (int xSpace = 0; xSpace < width; xSpace++)
                    {
                        // Compute real, imag, and ampltude.
                        realOut[yWave][xWave] += (inputData[ySpace][xSpace] * Math
                                .cos(2
                                        * Math.PI
                                        * ((1.0 * xWave * xSpace / width) + (1.0
                                        * yWave * ySpace / height))))
                                / Math.sqrt(width * height);
                        imagOut[yWave][xWave] -= (inputData[ySpace][xSpace] * Math
                                .sin(2
                                        * Math.PI
                                        * ((1.0 * xWave * xSpace / width) + (1.0
                                        * yWave * ySpace / height))))
                                / Math.sqrt(width * height);
                        amplitudeOut[yWave][xWave] = Math
                                .sqrt(realOut[yWave][xWave]
                                        * realOut[yWave][xWave]
                                        + imagOut[yWave][xWave]
                                        * imagOut[yWave][xWave]);
                    }
//                    System.out.println(realOut[yWave][xWave] + " + "
//                            + imagOut[yWave][xWave] + " i");
                }
            }
        }
    }

    public static int findClosestPowerOf2(int n) {
        int power = 1;
        while(power < n) {
            power *= 2;
        }
        return power;
    }

    public static int getMatrixSizeForImage(BufferedImage image)
    {
        int width = image.getWidth();       // ширина изображения
        int height = image.getHeight();     // высота изображения

        int n = Math.max(width, height);    // размер матрицы = максимальному значению из высоты и ширины
        n = findClosestPowerOf2(n);     // дополняем размер до ближайшей степени 2

        return n;
    }
}