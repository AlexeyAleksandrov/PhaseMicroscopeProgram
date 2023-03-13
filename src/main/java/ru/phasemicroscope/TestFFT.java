package ru.phasemicroscope;

import com.tambapps.fft4j.FastFourier2d;
import com.tambapps.fft4j.Signal2d;
import org.apache.commons.io.FileUtils;
import org.bytedeco.opencv.presets.opencv_core;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;


public class TestFFT
{
    public static boolean enableCenterReverse = false;   // включить костыль с инверсией изображения относительно центра
    public static boolean enableLogarithmicScale = true;    // логарифмическое масштабирование для модуля значений пикселей после развёртки

    public static void main(String[] args) throws IOException
    {
        String inputFileName = "src/main/resources/Interferogramma-1";
        String inputFileNFormat = ".jpg";
        String imagePath = inputFileName + inputFileNFormat;
        BufferedImage bufferedImage = ImageIO.read(new File(imagePath));

        int n = getMatrixSizeForImage(bufferedImage);   // считаем размер для матрицы
        double[][] massive = getImageMassive(bufferedImage);    // получаем массив пикселей
        double[][] real = new double[n][n];
        double[][] image = new double[n][n];
        double[][] amp = new double[n][n];

        massive = readTextImage("src/main/resources/Interferogramma_text.txt");


//        twoDfft(massive, real, image, amp);

//        discrete(massive, real, image);

        Signal2d signal2d = new Signal2d(n, n);
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                signal2d.setReAt(i, j, massive[i][j]);
                signal2d.setImAt(i, j, 0.0);
            }
        }

        writeMassiveToFile(massive, n, inputFileName + "_real_start.txt");
        writeMassiveToFile(image, n, inputFileName + "_image_start.txt");

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
                image[i][j] = signal2d.getImAt(i, j);
            }
        }

        writeMassiveToFile(real, n, inputFileName + "_real_after_FFT.txt");
        writeMassiveToFile(image, n, inputFileName + "_image_after_FFT.txt");

//        shiftImage(real, image);    // шифтинг

        writeMassiveToFile(real, n, inputFileName + "_real_shifting_before_mask.txt");
        writeMassiveToFile(image, n, inputFileName + "_image_shifting_before_mask.txt");

//        // костыль?
//        if(enableCenterReverse)
//        {
//            double[][] mas_real = new double[n][n];
//            double[][] mas_im = new double[n][n];
//            for (int i = 0; i < n; i++)
//            {
//                for (int j = 0; j < n; j++)
//                {
//                    int x = (i <= n/2) ? (n/2 - i) : (3*n/2 - i);
//                    int y = (j <= n/2) ? (n/2 - j) : (3*n/2 - j);
//                    mas_real[i][j] = real[x][y];
//                    mas_im[i][j] = image[x][y];
//                }
//            }
//
//            real = mas_real;
//            image = mas_im;
//        }

//        if(enableLogarithmicScale)
//        {
//            // считаем модуль комплексного числа для каждого пикселя
//            for (int i = 0; i < n; i++)
//            {
//                for (int j = 0; j < n; j++)
//                {
//                    double mod_z = Math.sqrt(Math.pow(real[i][j], 2) + Math.pow(image[i][j], 2));
//                    massive[i][j] = mod_z;
//                }
//            }
//
////            // логарифмическое преобразование
////            logarithmicScale(massive, n);
//        }

        // заполняем левую половину нулями
        final double grayscale = 0;
        for (int x = 0; x < n/2; x++)
        {
            for (int y = 0; y < n; y++)
            {
                real[x][y] = grayscale;
                image[x][y] = grayscale;
            }
        }

        writeMassiveToFile(real, n, inputFileName + "_real_after_mask.txt");
        writeMassiveToFile(image, n, inputFileName + "_image_after_mask.txt");

//        shiftImage(real, image);

        writeMassiveToFile(real, n, inputFileName + "_real_shifting_after_mask.txt");
        writeMassiveToFile(image, n, inputFileName + "_image_shifting_after_mask.txt");

//        real = readTextImage("src/main/resources/Complex of Interferogramma REAL.txt");
//        image = readTextImage("src/main/resources/Complex of Interferogramma IMAGINARY.txt");

        // обратное FFT
        for (int x = 0; x < n; x++)
        {
            for (int y = 0; y < n; y++)
            {
                signal2d.setReAt(x, y, real[x][y]);
                signal2d.setImAt(x, y, image[x][y]);
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
                image[i][j] = signal2d.getImAt(i, j);

//                real[i][j] = Math.atan(real[i][j]);
//                image[i][j] = Math.atan(image[i][j]);
            }
        }

        writeMassiveToFile(real, n, inputFileName + "_real_after_inverse.txt");
        writeMassiveToFile(image, n, inputFileName + "_image_after_inverse.txt");

//        real = readTextImage("src/main/resources/Real.txt");
//        image = readTextImage("src/main/resources/Imaginary.txt");

        // делим мнимую часть на действительную
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                massive[i][j] = image[i][j] / real[i][j];
            }
        }

        writeMassiveToFile(massive, n, inputFileName + "_after_divide.txt");
//
////        massive = real;
////        logarithmicScale(massive, n);
//
//        massive = readTextImage("src/main/resources/Result of Imaginary_befire_atan.txt");

        // считаем арктангенс
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                massive[i][j] = Math.atan(massive[i][j]);
            }
        }
//
////        massive = image;
//
//        try(FileWriter fileWriter = new FileWriter(new File(inputFileName + "_data.txt")))
//        {
//            for (int i = 0; i < n; i++)
//            {
//                for (int j = 0; j < n; j++)
//                {
//                    fileWriter.write(Double.toString(massive[i][j]) + "\t");
//                }
//                fileWriter.write("\r\n");
//            }
//        }

        writeMassiveToFile(massive, n, inputFileName + "_atan.txt");

//        massive = phaseUnwrap(massive);

//        massive = image;
//        massive = image;
        logarithmicScale(massive, n);


//        System.out.println(Arrays.deepToString(massive));

        // вывод в файл
        setImageFromMassive(massive, bufferedImage);

//        fft(bufferedImage);

        ImageIO.write(bufferedImage, "jpg", new File(inputFileName + "_out" + inputFileNFormat));
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

    public static void shiftImage(double[][] real, double[][] image)
    {
        int n = real.length;
        double[][] mas_real = new double[n][n];
        double[][] mas_im = new double[n][n];
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                int x = (i <= n/2) ? (n/2 - i) : (3*n/2 - i);
                int y = (j <= n/2) ? (n/2 - j) : (3*n/2 - j);
                mas_real[i][j] = real[x][y];
                mas_im[i][j] = image[x][y];
            }
        }

        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                real[i][j] = mas_real[i][j];
                image[i][j] = mas_im[i][j];
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

    public static double[][] phaseUnwrap(double[][] phase) {
        int height = phase.length;
        int width = phase[0].length;

        double[][] unwrappedPhase = new double[height][width];

// Step 1: Compute phase of image
// Phase computation code here

// Step 2: Normalize phase to [-0.5, 0.5]
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                unwrappedPhase[i][j] = phase[i][j] / (2 * Math.PI);
                if (unwrappedPhase[i][j] > 0.5) {
                    unwrappedPhase[i][j] -= 1;
                } else if (unwrappedPhase[i][j] < -0.5) {
                    unwrappedPhase[i][j] += 1;
                }
            }
        }

// Step 3-5: Unwrap phase
        boolean isUnwrapped = false;
        while (!isUnwrapped) {
            isUnwrapped = true;
            for (int i = 1; i < height - 1; i++) {
                for (int j = 1; j < width - 1; j++) {
                    double diff1 = unwrappedPhase[i][j] - unwrappedPhase[i-1][j];
                    double diff2 = unwrappedPhase[i][j] - unwrappedPhase[i][j-1];
                    if (diff1 > Math.PI) {
                        unwrappedPhase[i-1][j] += 1;
                        isUnwrapped = false;
                    } else if (diff1 < -Math.PI) {
                        unwrappedPhase[i-1][j] -= 1;
                        isUnwrapped = false;
                    }
                    if (diff2 > Math.PI) {
                        unwrappedPhase[i][j-1] += 1;
                        isUnwrapped = false;
                    } else if (diff2 < -Math.PI) {
                        unwrappedPhase[i][j-1] -= 1;
                        isUnwrapped = false;
                    }
                }
            }
        }

// Step 6: Multiply phase by 2*pi
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                unwrappedPhase[i][j] *= (2 * Math.PI);
            }
        }

        return unwrappedPhase;
    }

    public static double[][] readTextImage(String fileName)
    {
        double[][] massive = null;
        File file = new File(fileName);
        try
        {
            String content = FileUtils.readFileToString(file);  // считываем файл

            String[] lines = content.split("\r\n");
            int n = lines.length;       // размер массива

            massive = new double[n][n];
            for (int i = 0; i < lines.length; i++)      // проходим по строкам
            {
                String line = lines[i];     // берем строку
                String[] values = line.split("\t");     // делим на значения
                for (int j = 0; j < values.length; j++)     // перебираем все значения в строке
                {
                    massive[j][i] = Double.parseDouble(values[j]);      // записываем значение в массив
                }
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        return massive;
    }

    public static void writeMassiveToFile(double[][] massive, int n, String fileName)
    {
        try(FileWriter fileWriter = new FileWriter(new File(fileName)))
        {
            for (int i = 0; i < n; i++)
            {
                for (int j = 0; j < n; j++)
                {
                    fileWriter.write(Double.toString(massive[i][j]) + "\t");
                }
                fileWriter.write("\r\n");
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
