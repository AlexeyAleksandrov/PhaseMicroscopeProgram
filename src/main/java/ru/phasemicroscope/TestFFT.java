package ru.phasemicroscope;

import com.tambapps.fft4j.FastFourier2d;
import com.tambapps.fft4j.Signal2d;
import org.apache.commons.io.FileUtils;
import org.bytedeco.opencv.presets.opencv_core;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;

// из кода от чатбота
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import java.util.Arrays;



public class TestFFT
{
    public static boolean enableCenterReverse = false;   // включить костыль с инверсией изображения относительно центра
    public static boolean enableLogarithmicScale = true;    // логарифмическое масштабирование для модуля значений пикселей после развёртки

    public static void main(String[] args) throws IOException
    {
        String inputFileName = "src/main/resources/Interferogramma";
        String inputFileNFormat = ".bmp";
        String imagePath = inputFileName + inputFileNFormat;
//        BufferedImage bufferedImage = ImageIO.read(new File(imagePath));

//        BufferedImage img2 = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
//        Graphics2D graphics = img2.createGraphics();
//        graphics.drawImage(bufferedImage, null, 0, 0);
//
//        bufferedImage = img2;

        // OpenCV загрузка изображения
        // =======================================================
        //Loading the OpenCV core library
        System.loadLibrary( Core.NATIVE_LIBRARY_NAME );

        //Instantiating the Imagecodecs class
        Imgcodecs imageCodecs = new Imgcodecs();

        //Reading the Image from the file
        Mat matrix = imageCodecs.imread(imagePath);

        BufferedImage bufferedImage = convertMatrixToBufferedImage(matrix);

        System.out.println("Image Loaded");
        // =======================================================

        int n = getMatrixSizeForImage(bufferedImage);   // считаем размер для матрицы
        double[][] massive = getImageMassive(bufferedImage);    // получаем массив пикселей
        double[][] real = new double[n][n];
        double[][] image = new double[n][n];
        double[][] amp = new double[n][n];

//        massive = readTextImage("src/main/resources/Interferogramma_text.txt");

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

        // ПОИСК MIN, MAX
        // нормализация
        double search_min = massive[0][0];
        double search_max = massive[0][0];
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                if(search_min < massive[i][j])
                {
                    search_min = massive[i][j];
                }
                if(search_max > massive[i][j])
                {
                    search_max = massive[i][j];
                }
            }
        }

        System.out.println("Min: " + search_min);
        System.out.println("Max: " + search_max);

        massive = readTextImage("src/main/resources/Result of Imaginary.txt");

        unwrapMassive(massive, n);
//        massive = phaseUnwrap(massive);

        writeMassiveToFile(massive, n, inputFileName + "_unwrapped.txt");

        // перевод в ангстремы
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                double waveLength = 6328;
                massive[i][j] = (massive[i][j] * waveLength) / (4 * Math.PI);
            }
        }

        writeMassiveToFile(massive, n, inputFileName + "_angstrem.txt");

//        massive = phaseUnwrap(massive);
        //   Mat unwrappedPhase = Core.unwrapPhase(massive);
//        writeMassiveToFile(massive, n, inputFileName + "_unwrapped.txt");

        // нормализация
        double min = massive[0][0];
        double max = massive[0][0];
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                if(min < massive[i][j])
                {
                    min = massive[i][j];
                }
                if(max > massive[i][j])
                {
                    max = massive[i][j];
                }
            }
        }

        // производим нормализацию
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                massive[i][j] = (massive[i][j] - min) / (max - min);
            }
        }

        writeMassiveToFile(massive, n, inputFileName + "_normalized.txt");

        massive = phaseUnwrap(massive);


        // конвертируем нормализованное значение в 0 - 255
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                massive[i][j] *= 255;
            }
        }



//        massive = phaseUnwrap(massive);

//        massive = image;
//        massive = image;
//        logarithmicScale(massive, n);



//        System.out.println(Arrays.deepToString(massive));

        // вывод в файл
        setImageFromMassive(massive, bufferedImage);

//        fft(bufferedImage);

        ImageIO.write(bufferedImage, "jpg", new File(inputFileName + "_out" + inputFileNFormat));
        System.out.println("Готово!");

//        System.out.println("atan = " + Math.atan(81.09530240723115));
    }


//    public class unwrapPhaseMap (double phase,double Uphase)
//     {
//        public static void main(String[] args) {
//            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
//
//            Mat phase = Mat.zeros(5, 5, CvType.CV_32FC1);
//            phase.put(0, 0, 2.0f);
//            phase.put(0, 1, 2.1f);
//            phase.put(0, 2, 1.9f);
//            phase.put(0, 3, -3.0f);
//            phase.put(0, 4, -2.7f);
//            phase.put(1, 0, -2.7f);
//            phase.put(1, 1, -3.0f);
//            phase.put(1, 2, 2.0f);
//            phase.put(1, 3, 2.1f);
//            phase.put(1, 4, 1.9f);
//            phase.put(2, 0, 1.9f);
//            phase.put(2, 1, -2.7f);
//            phase.put(2, 2, -3.0f);
//            phase.put(2, 3, 2.0f);
//            phase.put(2, 4, 2.1f);
//            phase.put(3, 0, 2.1f);
//            phase.put(3, 1, 1.9f);
//            phase.put(3, 2, -2.7f);
//            phase.put(3, 3, -3.0f);
//            phase.put(3, 4, 2.0f);
//            phase.put(4, 0, -3.0f);
//            phase.put(4, 1, 2.0f);
//            phase.put(4, 2, 2.1f);
//            phase.put(4, 3, 1.9f);
//            phase.put(4, 4, -2.7f);
//
//            Mat unwrappedPhase =
//            System.out.println(phase.dump());
//            System.out.println(unwrappedPhase.dump());
//        }
//    }
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

                float r = new Color(rgb).getRed();
                float g = new Color(rgb).getGreen();
                float b = new Color(rgb).getBlue();
                int grayScaled = (int)(r+g+b)/3;

//                int r = (rgb >> 16) & 0xFF;
//                int g = (rgb >> 8) & 0xFF;
//                int b = (rgb & 0xFF);
//
//                massive[x][y] = getGrayScaleLevelFromRGB(r, g, b);
                massive[x][y] = grayScaled;
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

        System.out.println("step 1");
// Step 1: Compute phase of image
// Phase computation code here

        System.out.println("step 2");
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

        System.out.println("step 3-5");
// Step 3-5: Unwrap phase
        boolean isUnwrapped = false;
        while (!isUnwrapped) {
            System.out.println("image not unwrapped!");
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

        System.out.println("step 6");
// Step 6: Multiply phase by 2*pi
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                unwrappedPhase[i][j] *= (2 * Math.PI);
            }
        }
        System.out.println("--");

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

    /** Конвертация матрицы в изображение
     * @param imageMatrix матрица изображения
     * @return изображение
     */
    public static BufferedImage convertMatrixToBufferedImage(Mat imageMatrix)      // создаем изображение из матрицы
    {
        BufferedImage image = new BufferedImage(imageMatrix.width(), imageMatrix.height(), BufferedImage.TYPE_3BYTE_BGR);
        convertMatrixToBufferedImage(imageMatrix, image);
        return  image;
    }

    /** Конвертация матрицы в изображение
     * @param imageMatrix матрица изображения
     * @param image результирующее изображение
     */
    public static void convertMatrixToBufferedImage(Mat imageMatrix, BufferedImage image)      // создаем изображение из матрицы
    {
        byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        imageMatrix.get(0, 0, data);
    }

    public static double[] unwrap2(double[] massive)
    {
        int k = 0;
        int n = massive.length;

        double[] unwrapped = new double[n];

        for (int i = 1; i < (n-1); i++)
        {
            unwrapped[i] = massive[i] + (2 * Math.PI * k);
            if(Math.abs(massive[i+1] - massive[i]) > Math.PI)
            {
                if(massive[i+1] < massive[i])
                {
                    k++;
                }
                else
                {
                    k--;
                }
            }
        }

        unwrapped[n-1] = massive[n-1] + (2 * Math.PI * k);

        return unwrapped;
    }

    public static void unwrapMassive(double[][] massive, int n)
    {
        // развёртка по Y
        for (int y = 1; y < n-1; y++)
        {
            double[] row = new double[n];    // создаём стобец

            for (int x = 0; x < n; x++)     // заполняем столбец
            {
                row[x] = massive[x][y];
            }

            row = unwrap2(row);   // выполняем развёртку столбца

            for (int x = 0; x < n; x++)     // заполняем матрицу
            {
                massive[x][y] = row[x];
            }
        }

        // развёртка по X
        for (int x = 1; x < n-1; x++)
        {
            double[] column = new double[n];    // создаём стобец

            for (int y = 0; y < n; y++)
            {
                column[y] = massive[x][y];
            }

            column = unwrap2(column);

            for (int y = 0; y < n; y++)
            {
                massive[x][y] = column[y];
            }
        }
    }
}
