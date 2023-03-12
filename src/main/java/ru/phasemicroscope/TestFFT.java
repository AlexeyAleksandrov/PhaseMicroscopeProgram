package ru.phasemicroscope;

import com.tambapps.fft4j.FastFourier2d;
import com.tambapps.fft4j.Signal2d;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;


public class TestFFT
{
    public static boolean enableCenterReverse = true;   // включить костыль с инверсией изображения относительно центра
    public static boolean enableLogarithmicScale = true;    // логарифмическое масштабирование для модуля значений пикселей после развёртки

    public static void main(String[] args) throws IOException
    {
        String inputFileName = "src/main/resources/obj5050";
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
                signal2d.setImAt(i, j, 0.0);
            }
        }

        FastFourier2d transformer2D = new FastFourier2d();
        transformer2D.transform(signal2d);
//        transformer2D.inverse(signal2d);
//        transformer2D.shutdown();

        // вытаскиваем действительную и мнимую часть
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                real[i][j] = signal2d.getReAt(i, j);
                imag[i][j] = signal2d.getImAt(i, j);
            }
        }

        // костыль?
        if(enableCenterReverse)
        {
            double[][] mas_real = new double[n][n];
            double[][] mas_im = new double[n][n];
            for (int i = 0; i < n; i++)
            {
                for (int j = 0; j < n; j++)
                {
                    int x = (i <= n/2) ? (n/2 - i) : (3*n/2 - i);
                    int y = (j <= n/2) ? (n/2 - j) : (3*n/2 - j);
                    mas_real[i][j] = real[x][y];
                    mas_im[i][j] = imag[x][y];
                }
            }

            real = mas_real;
            imag = mas_im;
        }

        if(enableLogarithmicScale)
        {
            // считаем модуль комплексного числа для каждого пикселя
            for (int i = 0; i < n; i++)
            {
                for (int j = 0; j < n; j++)
                {
                    double mod_z = Math.sqrt(Math.pow(real[i][j], 2) + Math.pow(imag[i][j], 2));
                    massive[i][j] = mod_z;
                }
            }

            // логарифмическое преобразование
            logarithmicScale(massive, n);
        }

        // заполняем левую половину нулями
        final double grayscale = 215;
        for (int x = 0; x < n/2; x++)
        {
            for (int y = 0; y < n; y++)
            {
                real[x][y] = grayscale;
                imag[x][y] = grayscale;
            }
        }

        // обратное FFT
        for (int x = 0; x < n; x++)
        {
            for (int y = 0; y < n; y++)
            {
                signal2d.setReAt(x, y, real[x][y]);
                signal2d.setImAt(x, y, imag[x][y]);
            }
        }

        transformer2D.inverse(signal2d);
        transformer2D.shutdown();

        // вытаскиваем действительную и мнимую часть
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                real[i][j] = signal2d.getReAt(i, j);
                imag[i][j] = signal2d.getImAt(i, j);

//                real[i][j] = Math.atan(real[i][j]);
//                imag[i][j] = Math.atan(imag[i][j]);
            }
        }

        // делим мнимую часть на действительную
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                massive[i][j] = imag[i][j] / real[i][j];
            }
        }

//        massive = real;
//        logarithmicScale(massive, n);

        // считаем арктангенс
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                massive[i][j] = Math.atan(massive[i][j]);
            }
        }

//        massive = imag;

        try(FileWriter fileWriter = new FileWriter(new File(inputFileName + "_data.txt")))
        {
            fileWriter.write(Arrays.deepToString(massive));
        }

        logarithmicScale(massive, n);


        System.out.println(Arrays.deepToString(massive));

        // вывод в файл
        setImageFromMassive(massive, image);

//        fft(image);

        ImageIO.write(image, "jpg", new File(inputFileName + "_out" + inputFileNFormat));
        System.out.println("Готово!");

//        System.out.println("atan = " + Math.atan(81.09530240723115));
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

                massive[x][y] = getGrayScaleLevelFromRGB(r, g, b);
            }
        }

        return massive;
    }

    // получаем градацию серого по RGB
    public static double getGrayScaleLevelFromRGB(int r, int g, int b)
    {
        // нормализация и гамма-коррекция:
        float rr = (float) Math.pow(r / 255.0, 2.2);
        float gg = (float) Math.pow(g / 255.0, 2.2);
        float bb = (float) Math.pow(b / 255.0, 2.2);

        // рассчитываем яркость:
        float lum = (float) (0.2126 * rr + 0.7152 * gg + 0.0722 * bb);

        // гамма-сложение и масштабирование до диапазона байтов:
        return (255.0 * Math.pow(lum, 1.0 / 2.2));  // возвращаем значение градации серого
    }

    public static void setImageFromMassive(double[][] massive, BufferedImage image)
    {
        int width = image.getWidth();       // ширина изображения
        int height = image.getHeight();     // высота изображения

        for (int x = 0; x < width; x++)     // проходим по каждому столбцу
        {
            for (int y = 0; y < height; y++)    // проходим по каждому значению в столбце
            {
                int grayLevel = (int) (massive[x][y]);

                // проверка на корректность данных
                if(grayLevel < 0 || grayLevel > 255)
                {
                    grayLevel = 120;
                }

                int gray = (grayLevel << 16) + (grayLevel << 8) + grayLevel;
                image.setRGB(x, y, gray);   // перерисовываем пиксель в градации серого
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

    public static void logarithmicScale(double[][] massive, int n)
    {
        // ищем максимум
        double max = massive[0][0];
        double min = massive[0][0];
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                if(massive[i][j] > max)
                {
                    max = massive[i][j];
                }
                if(massive[i][j] < min)
                {
                    min = massive[i][j];
                }
            }
        }

        double c =  255 / Math.log10((1 + max));

        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                massive[i][j] = c * Math.log10(1 + massive[i][j] * Math.signum(massive[i][j]));

                if (Double.isNaN(massive[i][j]))
                {
                    System.out.println("NAN: i = " + i + " j = " + j + " c = " + c + " val = " + massive[i][j]);
                }
            }
        }

//        System.out.println("Max = " + max);
//        System.out.println("Min = " + min);
//        System.out.println("C = " + c);
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
                System.out.println("x = " + xWave + " y = " + yWave + "Re = " + realOut[yWave][xWave] + " Im = " + imagOut[yWave][xWave] + " Amp = " + amplitudeOut[yWave][xWave]);
            }
        }
    }
}
