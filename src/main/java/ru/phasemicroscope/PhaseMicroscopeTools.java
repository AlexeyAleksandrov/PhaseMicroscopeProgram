package ru.phasemicroscope;

import com.tambapps.fft4j.FastFourier2d;
import com.tambapps.fft4j.Signal2d;
import org.apache.commons.io.FileUtils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import ru.phasemicroscope.opencv.OpenCV;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.util.Arrays;

import static org.opencv.imgproc.Imgproc.medianBlur;
import static ru.phasemicroscope.window.MainWindow.*;

// из кода от чатбота


public class PhaseMicroscopeTools
{
    public static boolean enableCenterReverse = false;   // включить костыль с инверсией изображения относительно центра
    public static boolean enableLogarithmicScale = true;    // логарифмическое масштабирование для модуля значений пикселей после развёртки
    public static int waveLength = 7920;

    //  public static final double Trash_hold = 0.6;

//    double[][] massive = null;
//    double[][] real = null;
//    double[][] image = null;

    //    public static void main(String[] args) throws IOException
//    {
//        //Loading the OpenCV core library
//        System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
//
//        String inputFileName = "src/main/resources/filter/Interferogramma-2";
//        String inputFileNFormat = ".jpg";
//        String imagePath = inputFileName + inputFileNFormat;
//
//        PhaseMicroscopeTools tools = new PhaseMicroscopeTools();
//
//        // работаем с изображением
//        // загружаем изображение
//        BufferedImage bufferedImage = tools.loadImage(imagePath);   // загружаем изображение
//
//        // обрабатываем изображение
//        tools.processImage(bufferedImage);
//
//        ImageIO.write(bufferedImage, "jpg", new File(inputFileName + "_out" + inputFileNFormat));
//        System.out.println("Готово!");
//    }
    public static int loopsUnwrapCount=50;    // кол-во циклов развёртки
    public static  int loopsDeleteTrendCount = 4;

    public void processImage(BufferedImage bufferedImage, boolean inversePixels) // стандарт с разверткой, без нормализации на длину волны
    {
//        for (int i=1;i==4;i++)
//        {
        //System.out.println("Длина волны - " + waveLength);
        double[][] massive = null;
        double[][] real = null;
        double[][] image = null;


        PhaseMicroscopeTools tools = new PhaseMicroscopeTools();

        massive = tools.getImageMassive(bufferedImage);     // получаем массив пикселей изображения

//        int nn = 255;
//        massive = new double[nn][nn];
//        for (int i = 0; i < nn; i++)
//        {
//            for (int j = 0; j < nn; j++)
//            {
//                massive[i][j] = massiveImg[i][j];
//            }
//        }

        int n = getMatrixSizeForImage(massive);     // считаем размер для матрицы
//        System.out.println("размер матрицы!"+ n + n);
        real = new double[n][n];
        image = new double[n][n];


//          //  massive=
//        }
        //tools.writeMassiveToFile(massive, "src/main/resources/ConvertImg.txt");
        // переводим массив изображения в комплексный вид
        tools.convertToComplex(massive, real, image);   // делаем из массива изображения массив комплексных чисел

        // производим прямой FFT
        tools.FFT_2D(real, image, false);

        // заполняем левую половину нулями
        tools.fillLeftHalfOfImage(real, image, 0);

        // производим обратное FFT
        tools.FFT_2D(real, image, true);

        // делим мнимую часть на действительную
        tools.divideImageToReal(real, image, massive);


        // считаем арктангенс
        tools.atan(massive);
        // tools.writeMassiveToFile(massive, "src/main/resources/image_atan_out.txt");    // записываем текстовый файл


        // нормализуем изображение к значениям от -PI до +PI
        tools.normalizeTo(massive, -Math.PI, Math.PI);

        // делаем развёртку
        tools.unwrapMassive(massive, loopsUnwrapCount);


        //   удаляем тренд
        // tools.deleteTrend(massive, loopsDeleteTrendCount);
        tools.deleteTrend3D(massive);
        tools.normalizeTo(massive, 0, 255); //нужно проверить надобность

        if (inversePixels)
        {
            tools.inversePixelGrayScale(massive);
        }

//        if (medianF)
//        {
//            tools.medianFilter(massive, 20);
//        }
//        tools.medianFilter(massive, 2);

//        Imgproc.medianBlur(massive, matrix, 5);
//           переводим в ангстремы
//            tools.convertToAngstroms(massive, waveLength);   // нужно переделать удаление тренда, появляются проблемы тут из-за этого

        // tools.writeMassiveToFile(massive, "src/main/resources/Final.txt");
        // вывод в файл
//        System.out.println("wh: " + massive.length + " " + massive[0].length);
        setImageFromMassive(massive, bufferedImage);        // из массива создаем изображение

        if (medianF)
        {
            tools.medianFilter(bufferedImage, medianCount);
        }
    }

    public void medianFilter(BufferedImage bufferedImage, int radius)
    {
        OpenCV openCV = new OpenCV();
        Mat mat = openCV.bufferedImageToMat(bufferedImage);     // изображение преобразовываем в матрицу
        Mat filtered = new Mat();

        medianBlur(mat, filtered, radius);   // применяем фильтр, 5 - размер окна фильтра

        openCV.convertMatrixToBufferedImage(filtered, bufferedImage);   // создаем из матрицы изображение после фильтрации
    }

    public void medianFilter(double[][] massive, int radius)
    {
        for (int i = radius; i < massive.length - radius; i++)
        {
            for (int j = radius; j < massive[i].length - radius; j++)
            {
                // формируем массив вокруг пикселя
                int size = (radius*2+1)*(radius*2+1);
                double[] mas = new double[size];

                int c = 0;
                for (int k = i-radius; k <= i+radius; k++)
                {
                    for (int l = j-radius; l <= j+radius; l++)
                    {
                        mas[c] = massive[k][l];
                        c++;
                    }
                }

                // сортируем массив
                Arrays.sort(mas);

                massive[i][j] = mas[size/2];
            }
        }
    }

    public void inversePixelGrayScale(double[][] massive)
    {
        for (int i = 0; i < massive.length; i++)
        {
            for (int j = 0; j < massive[0].length; j++)
            {
                massive[i][j] = Math.abs(255 - (massive[i][j]));
            }
        }
    }
    public void processImageANGSTR(BufferedImage bufferedImage)// стандарт с разверткой, с нормализации на длину волны
    {
        double[][] massive = null;
        double[][] real = null;
        double[][] image = null;



        PhaseMicroscopeTools tools = new PhaseMicroscopeTools();

        massive = tools.getImageMassive(bufferedImage);     // получаем массив пикселей изображения

//        int nn = 255;
//        massive = new double[nn][nn];
//        for (int i = 0; i < nn; i++)
//        {
//            for (int j = 0; j < nn; j++)
//            {
//                massive[i][j] = massiveImg[i][j];
//            }
//        }

        int n = getMatrixSizeForImage(massive);     // считаем размер для матрицы
//        System.out.println("размер матрицы!"+ n + n);
        real = new double[n][n];
        image = new double[n][n];
        tools.normalizeTo(massive, -Math.PI, Math.PI);
        //   tools.writeMassiveToFile(massive, "src/main/resources/ConvertImg.txt");
        // переводим массив изображения в комплексный вид
        tools.convertToComplex(massive, real, image);   // делаем из массива изображения массив комплексных чисел

        // производим прямой FFT
        tools.FFT_2D(real, image, false);

        // заполняем левую половину нулями
        tools.fillLeftHalfOfImage(real, image, 0);

        // производим обратное FFT
        tools.FFT_2D(real, image, true);

        // делим мнимую часть на действительную
        tools.divideImageToReal(real, image, massive);



        // считаем арктангенс
        tools.atan(massive);
        // tools.writeMassiveToFile(massive, "src/main/resources/image_atan_out.txt");    // записываем текстовый файл




        // нормализуем изображение к значениям от -PI до +PI
        tools.normalizeTo(massive, -Math.PI, Math.PI);

        // делаем развёртку
        tools.unwrapMassive(massive, loopsUnwrapCount);



        //   удаляем тренд
        tools.deleteTrend(massive, loopsDeleteTrendCount);


        if (invert==true) {
            for (int i = 0; i < massive.length; i++)
            {
                for (int j = 0; j < massive[0].length; j++)
                {
                    massive[i][j] = Math.abs(255 - (massive[i][j]));
                }
            }
        }
//           переводим в ангстремы
        tools.convertToAngstroms(massive, waveLength);   // нужно переделать удаление тренда, появляются проблемы тут из-за этого

        //  tools.writeMassiveToFile(massive, "src/main/resources/photoFromCamera_out.txt");    // записываем текстовый файл
        tools.normalizeTo(massive, 0, 255);
        // вывод в файл
//        System.out.println("wh: " + massive.length + " " + massive[0].length);
        setImageFromMassive(massive, bufferedImage);

    }

    public void processImageF(BufferedImage bufferedImage)// фазовая картинка без развертки
    {
        double[][] massive = null;
        double[][] real = null;
        double[][] image = null;


        PhaseMicroscopeTools tools = new PhaseMicroscopeTools();

        massive = tools.getImageMassive(bufferedImage);     // получаем массив пикселей изображения

//        int nn = 255;
//        massive = new double[nn][nn];
//        for (int i = 0; i < nn; i++)
//        {
//            for (int j = 0; j < nn; j++)
//            {
//                massive[i][j] = massiveImg[i][j];
//            }
//        }

        int n = getMatrixSizeForImage(massive);     // считаем размер для матрицы
//        System.out.println("размер матрицы!"+ n + n);
        real = new double[n][n];
        image = new double[n][n];

        // tools.writeMassiveToFile(massive, "src/main/resources/ConvertImg.txt");
        // переводим массив изображения в комплексный вид
        tools.convertToComplex(massive, real, image);   // делаем из массива изображения массив комплексных чисел

        // производим прямой FFT
        tools.FFT_2D(real, image, false);

        // заполняем левую половину нулями
        tools.fillLeftHalfOfImage(real, image, 0);

        // производим обратное FFT
        tools.FFT_2D(real, image, true);

        // делим мнимую часть на действительную
        tools.divideImageToReal(real, image, massive);

        // переводим в ангстремы
        // tools.convertToAngstroms(massive, waveLength);   // нужно переделать удаление тренда, появляются проблемы тут из-за этого

        // считаем арктангенс
        tools.atan(massive);
        tools.normalizeTo(massive, 0, 255);
        if (invert==true) {
            for (int i = 0; i < massive.length; i++)
            {
                for (int j = 0; j < massive[0].length; j++)
                {
                    massive[i][j] = Math.abs(255 - (massive[i][j]));
                }
            }
        }
        //  tools.writeMassiveToFile(massive, "src/main/resources/image_atan_out.txt");    // записываем текстовый файл


        // вывод в файл
//        System.out.println("wh: " + massive.length + " " + massive[0].length);
        setImageFromMassive(massive, bufferedImage);

    }
    public void processImageTrend(BufferedImage bufferedImage) // фазовая картинка с разверткой, без удаления тренда
    {
        double[][] massive = null;
        double[][] real = null;
        double[][] image = null;


        PhaseMicroscopeTools tools = new PhaseMicroscopeTools();

        massive = tools.getImageMassive(bufferedImage);     // получаем массив пикселей изображения

//        int nn = 255;
//        massive = new double[nn][nn];
//        for (int i = 0; i < nn; i++)
//        {
//            for (int j = 0; j < nn; j++)
//            {
//                massive[i][j] = massiveImg[i][j];
//            }
//        }

        int n = getMatrixSizeForImage(massive);     // считаем размер для матрицы
//        System.out.println("размер матрицы!"+ n + n);
        real = new double[n][n];
        image = new double[n][n];

        //tools.writeMassiveToFile(massive, "src/main/resources/ConvertImg.txt");
        // переводим массив изображения в комплексный вид
        tools.convertToComplex(massive, real, image);   // делаем из массива изображения массив комплексных чисел

        // производим прямой FFT
        tools.FFT_2D(real, image, false);

        // заполняем левую половину нулями
        tools.fillLeftHalfOfImage(real, image, 0);

        // производим обратное FFT
        tools.FFT_2D(real, image, true);

        // делим мнимую часть на действительную
        tools.divideImageToReal(real, image, massive);



        // считаем арктангенс
        tools.atan(massive);
        // tools.writeMassiveToFile(massive, "src/main/resources/image_atan_out.txt");    // записываем текстовый файл




        // нормализуем изображение к значениям от -PI до +PI
        tools.normalizeTo(massive, -Math.PI, Math.PI);

        // делаем развёртку
        tools.unwrapMassive(massive, loopsUnwrapCount);



        //   удаляем тренд
        // tools.deleteTrend(massive, loopsDeleteTrendCount);
//        tools.deleteTrend3D(massive);
        tools.normalizeTo(massive, 0, 255); //нужно проверить надобность

        if (invert==true) {
            for (int i = 0; i < massive.length; i++)
            {
                for (int j = 0; j < massive[0].length; j++)
                {
                    massive[i][j] = Math.abs(255 - (massive[i][j]));
                }
            }
        }
//           переводим в ангстремы
//            tools.convertToAngstroms(massive, waveLength);   // нужно переделать удаление тренда, появляются проблемы тут из-за этого

        // tools.writeMassiveToFile(massive, "src/main/resources/Final.txt");
        // вывод в файл
//        System.out.println("wh: " + massive.length + " " + massive[0].length);
        setImageFromMassive(massive, bufferedImage);

    }


    public void processImageF_ANGSTR(BufferedImage bufferedImage)// фазовая картинка без развертки, с нормализации на длину волны
    {
        double[][] massive = null;
        double[][] real = null;
        double[][] image = null;


        PhaseMicroscopeTools tools = new PhaseMicroscopeTools();

        massive = tools.getImageMassive(bufferedImage);     // получаем массив пикселей изображения

//        int nn = 255;
//        massive = new double[nn][nn];
//        for (int i = 0; i < nn; i++)
//        {
//            for (int j = 0; j < nn; j++)
//            {
//                massive[i][j] = massiveImg[i][j];
//            }
//        }

        int n = getMatrixSizeForImage(massive);     // считаем размер для матрицы
//        System.out.println("размер матрицы!"+ n + n);
        real = new double[n][n];
        image = new double[n][n];
        tools.normalizeTo(massive, -Math.PI, Math.PI);
        // tools.writeMassiveToFile(massive, "src/main/resources/ConvertImg.txt");
        // переводим массив изображения в комплексный вид
        tools.convertToComplex(massive, real, image);   // делаем из массива изображения массив комплексных чисел

        // производим прямой FFT
        tools.FFT_2D(real, image, false);

        // заполняем левую половину нулями
        tools.fillLeftHalfOfImage(real, image, 0);

        // производим обратное FFT
        tools.FFT_2D(real, image, true);

        // делим мнимую часть на действительную
        tools.divideImageToReal(real, image, massive);



        // считаем арктангенс
        tools.atan(massive);

        tools.normalizeTo(massive, 0, 255);
        if (invert==true) {
            for (int i = 0; i < massive.length; i++)
            {
                for (int j = 0; j < massive[0].length; j++)
                {
                    massive[i][j] = Math.abs(255 - (massive[i][j]));
                }
            }
        }
        tools.writeMassiveToFile(massive, "src/main/resources/image_atan_out.txt");    // записываем текстовый файл
        // переводим в ангстремы
        tools.convertToAngstroms(massive, waveLength);   // нужно пероверить необходимость добавления перед инвертированием

        // вывод в файл
//        System.out.println("wh: " + massive.length + " " + massive[0].length);
        setImageFromMassive(massive, bufferedImage);

    }

    public void onStart() throws IOException
    {


////        massive = loadImage(imagePath);     // получаем массив пикселей изображения
////        int n = getMatrixSizeForImage(massive);     // считаем размер для матрицы
////
////        real = new double[n][n];
////        image = new double[n][n];
//
//        Signal2d signal2d = new Signal2d(n, n);
//        for (int i = 0; i < n; i++)
//        {
//            for (int j = 0; j < n; j++)
//            {
//                signal2d.setReAt(i, j, massive[i][j]);
//                signal2d.setImAt(i, j, image[i][j]);
//            }
//        }
//
////        writeMassiveToFile(massive, n, inputFileName + "_real_start.txt");
////        writeMassiveToFile(image, n, inputFileName + "_image_start.txt");
//
//        FastFourier2d transformer2D = new FastFourier2d();
//        transformer2D.transform(signal2d);
//        //        transformer2D.inverse(signal2d);
//        //        transformer2D.shutdown();
//
//        // вытаскиваем действительную и мнимую часть
//        for (int i = 0; i < n; i++)
//        {
//            for (int j = 0; j < n; j++)
//            {
//                real[i][j] = signal2d.getReAt(i, j);
//                image[i][j] = signal2d.getImAt(i, j);
//            }
//        }
//
//        writeMassiveToFile(real, n, inputFileName + "_real_after_FFT.txt");
//        writeMassiveToFile(image, n, inputFileName + "_image_after_FFT.txt");
//
//        //        shiftImage(real, image);    // шифтинг
//
//        writeMassiveToFile(real, n, inputFileName + "_real_shifting_before_mask.txt");
//        writeMassiveToFile(image, n, inputFileName + "_image_shifting_before_mask.txt");

//        // заполняем левую половину нулями
//        final double grayscale = 0;
//        for (int x = 0; x < n/2; x++)
//        {
//            for (int y = 0; y < n; y++)
//            {
//                real[x][y] = grayscale;
//                image[x][y] = grayscale;
//            }
//        }
//
//        writeMassiveToFile(real, n, inputFileName + "_real_after_mask.txt");
//        writeMassiveToFile(image, n, inputFileName + "_image_after_mask.txt");

        //        shiftImage(real, image);

        //        real = readTextImage("src/main/resources/Complex of Interferogramma REAL.txt");
        //        image = readTextImage("src/main/resources/Complex of Interferogramma IMAGINARY.txt");

//        // обратное FFT
//        for (int x = 0; x < n; x++)
//        {
//            for (int y = 0; y < n; y++)
//            {
//                signal2d.setReAt(x, y, real[x][y]);
//                signal2d.setImAt(x, y, image[x][y]);
//            }
//        }
//
//        transformer2D.inverse(signal2d);
//        transformer2D.shutdown();
//
//        // вытаскиваем действительную и мнимую часть
//        for (int i = 0; i < n; i++)
//        {
//            for (int j = 0; j < n; j++)
//            {
//                real[i][j] = signal2d.getReAt(i, j);
//                image[i][j] = signal2d.getImAt(i, j);
//
//                //                real[i][j] = Math.atan(real[i][j]);
//                //                image[i][j] = Math.atan(image[i][j]);
//            }
//        }
//
//        writeMassiveToFile(real, n, inputFileName + "_real_after_inverse.txt");
//        writeMassiveToFile(image, n, inputFileName + "_image_after_inverse.txt");

        //        real = readTextImage("src/main/resources/Real.txt");
        //        image = readTextImage("src/main/resources/Imaginary.txt");

//        // делим мнимую часть на действительную
//        for (int i = 0; i < n; i++)
//        {
//            for (int j = 0; j < n; j++)
//            {
//                massive[i][j] = image[i][j] / real[i][j];
//            }
//        }
//
//        writeMassiveToFile(massive, n, inputFileName + "_after_divide.txt");

//        // считаем арктангенс
//        for (int i = 0; i < n; i++)
//        {
//            for (int j = 0; j < n; j++)
//            {
//                massive[i][j] = Math.atan(massive[i][j]);
//            }
//        }
//
//        writeMassiveToFile(massive, n, inputFileName + "_atan.txt");


        //        // ========================
        //
//        normalize(massive);
//        denormalize(massive, -1 * Math.PI, Math.PI);
//        writeMassiveToFile(massive, n, inputFileName + "_atan_normal.txt");
        //
        //        // ========================

//        unwrapMassive(massive, n);
//
//        writeMassiveToFile(massive, n, inputFileName + "_unwrapped.txt");

//        // перевод в ангстремы
//        for (int i = 0; i < n; i++)
//        {
//            for (int j = 0; j < n; j++)
//            {
//                double waveLength = 6328;
//                massive[i][j] = (massive[i][j] * waveLength) / (4 * Math.PI);
//            }
//        }
//
//        writeMassiveToFile(massive, n, inputFileName + "_angstrem.txt");

//        normalize(massive, 1, 1, n-1, n-1);
//
//        writeMassiveToFile(massive, n, inputFileName + "_normalized.txt");
//
//        // конвертируем нормализованное значение в 0 - 255
//        for (int i = 0; i < n; i++)
//        {
//            for (int j = 0; j < n; j++)
//            {
//                massive[i][j] *= 255;
//            }
//        }
//
//        // считаем каким должен быть градиент
//        double[] trendMassive = new double[n];
//
//        for (int i = 0; i < n; i++)
//        {
//            double k = (((double)(n - 1 - i) / (double) (n - 1)));
//            trendMassive[i] = 255 * k;
//        }
//
//        // вычитаем из градиента получившееся значение
//        for (int i = 0; i < n; i++)
//        {
//            for (int j = 0; j < n; j++)
//            {
//                massive[i][j] = trendMassive[i] - massive[i][j];
//
//            }
//        }
//
//        normalize(massive, 1, 1, n-1, n-1);
//
//        for (int i = 0; i < n; i++)
//        {
//            for (int j = 0; j < n; j++)
//            {
//                massive[i][j] = Math.abs(255 - (massive[i][j] * 255));
//            }
//        }
//
//        // зануляем граничные значения
//        for (int i = 0; i < n; i++)
//        {
//            massive[0][i] = 0;      // верхняя горизонтальная линия
//            massive[n-1][i] = 0;    // нижняя горизонтальная линия
//            massive[i][0] = 0;      // левая вертикальная линия
//            massive[i][n-1] = 0;    // правая вертикальная линия
//        }

//        MaxMin(massive);
//        writeMassiveToFile(massive, n, inputFileName + "_final.txt");

//        // вывод в файл
//        BufferedImage bufferedImage = new BufferedImage(massive.length, massive[0].length, BufferedImage.TYPE_3BYTE_BGR);
//        setImageFromMassive(massive, bufferedImage);
//
//        ImageIO.write(bufferedImage, "jpg", new File(inputFileName + "_out" + inputFileNFormat));
//        System.out.println("Готово!");
    }

    public BufferedImage loadImage(String imagePath)
    {
        // OpenCV загрузка изображения
        // =======================================================

        //Instantiating the Imagecodecs class
        Imgcodecs imageCodecs = new Imgcodecs();

        //Reading the Image from the file
        Mat matrix = imageCodecs.imread(imagePath);

        BufferedImage bufferedImage = convertMatrixToBufferedImage(matrix);

        System.out.println("Image Loaded");
        // =======================================================

//        int n = getMatrixSizeForImage(bufferedImage);   // считаем размер для матрицы
        return bufferedImage;
    }

    public double[][] getImageMassive(BufferedImage image)
    {
        int width = image.getWidth();       // ширина изображения
        int height = image.getHeight();     // высота изображения

//        int n = getMatrixSizeForImage(image);   // считаем размер для матрицы

        double[][] massive = new double[width][height];     // создаем массив размера ширина / высота

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

    /** Переводим массив изображения в комплексное
     * @param massive исходный массив изображения
     * @param real действительная часть
     * @param image мнимая часть
     */
    public void convertToComplex(double[][] massive, double[][] real, double[][] image)
    {
        for (int i = 0; i < massive.length; i++)
        {
            for (int j = 0; j < massive[i].length; j++)
            {
                real[i][j] = massive[i][j];
                image[i][j] = 0.0;
            }
        }
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

    public static int getMatrixSizeForImage(double[][] massive)
    {
        int width = massive.length;       // ширина изображения
        int height = massive[0].length;     // высота изображения

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

    public void writeMassiveToFile(double[][] massive, String fileName)
    {
        try(FileWriter fileWriter = new FileWriter(new File(fileName)))
        {
            for (int i = 0; i < massive.length; i++)
            {
                for (int j = 0; j < massive[i].length; j++)
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

        for (int i = 0; i < n; i++)
        {
            if(i==n-1)
            {
                unwrapped[n-1] = massive[n-1] + (2 * Math.PI * k);
                return unwrapped;
            }
            unwrapped[i] = massive[i] + (2 * Math.PI * k);
            if(Math.abs(massive[i+1] - massive[i]) > (Math.PI * Trash_hold))
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

    public void unwrapMassive(double[][] massive, int loopsUnwrapCount)
    {
        int width = massive.length;
        int height = massive[0].length;

        // развёртка по Y
        for (int y = 0; y < height; y++)
        {
            double[] row = new double[width];    // создаём стобец

            for (int x = 0; x < width; x++)     // заполняем столбец
            {
                row[x] = massive[x][y];
            }

            for (int i = 0; i < loopsUnwrapCount; i++)
            {
                row = unwrap2(row);   // выполняем развёртку столбца
            }

            for (int x = 0; x < width; x++)     // заполняем матрицу
            {
                massive[x][y] = row[x];
            }
        }

        // развёртка по X
        for (int x = 0; x < width; x++)
        {
            double[] column = new double[height];    // создаём стобец

            for (int y = 0; y < height; y++)
            {
                column[y] = massive[x][y];
            }

            for (int i = 0; i < loopsUnwrapCount; i++)
            {
                column = unwrap2(column);
            }

            for (int y = 0; y < height; y++)
            {
                massive[x][y] = column[y];
            }
        }
    }

    public static void Normalize1(double[] massive)
    {
        int n = massive.length;

        // нормализация
        double min = massive[0];
        double max = massive[0];
        for (int i = 0; i < n; i++)
        {
            if(min < massive[i])
            {
                min = massive[i];
            }
            if(max > massive[i])
            {
                max = massive[i];
            }
        }

        // производим нормализацию
        for (int i = 0; i < n; i++)
        {
            massive[i] = (massive[i] - min) / (max - min);
        }
    }

    public static void normalize(double[][] massive)
    {
        int width = massive.length;
        int height = massive[0].length;
        normalize(massive, 0, 0, width, height);
    }

    public static void normalize(double[][] massive, int start_i, int start_j, int end_i, int end_j)
    {
        // нормализация
        double min = massive[start_i][start_j];
        double max = massive[start_i][start_j];
        for (int i = start_i; i < end_i; i++)
        {
            for (int j = start_j; j < end_j; j++)
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

//        System.out.println("min = " + min + "  max = " + max);

        // производим нормализацию
        for (int i = start_i; i < end_i; i++)
        {
            for (int j = start_j; j < end_j; j++)
            {
                massive[i][j] = (massive[i][j] - min) / (max - min);
            }
        }
    }

    public static void MaxMin(double[][] massive) {
        // нормализация
        int n = massive.length;
        double min = massive[0][0];
        double max = massive[0][0];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (min < massive[i][j]) {
                    min = massive[i][j];
                }
                if (max > massive[i][j]) {
                    max = massive[i][j];
                }
            }
        }

        System.out.println("min эл. = " + min + "  max эл. = " + max);
    }


    public static void denormalize(double[][] massive, double min, double max)
    {
//        int n = massive.length;

        for (int i = 0; i < massive.length; i++)
        {
            for (int j = 0; j < massive[i].length; j++)
            {
                massive[i][j] = massive[i][j] * (max - min) + min;
            }
        }
    }

    public void normalizeTo(double[][] massive, double min, double max)
    {
        normalize(massive);
        denormalize(massive, min, max);
    }




    ////////////////////////////////////////////////////

    public static double[] linReg(double[] inputMassive) {

        double[] outputMassive=new double[inputMassive.length];

        double xmean=0;
        double ymean=0;

        for (int x=0; x<inputMassive.length; x++)
        {
            ymean+=inputMassive[x];
            xmean+=x;
        }

        xmean/=inputMassive.length;
        ymean/=inputMassive.length;

        double sum1=0;
        double sum2=0;
        for (int i=0; i<inputMassive.length; i++)
        {
            sum1+=(i-xmean)*(inputMassive[i]-ymean);
            sum2+=(i-xmean)*(i-xmean);
        }

        double m=sum1/sum2;
        double c=ymean-m*xmean;

        for (int i=0; i<inputMassive.length; i++)
        {
            outputMassive[i]=m*i+c;
        }

        return outputMassive;
    }

    //    public static double[][] linReg(double[][] p) {
    //
    //        double[][] line=new double[p.length][p[0].length];
    //        double xmean=0;
    //        double ymean=0;
    //        for (int x=0; x<p.length; x++) {
    //            for (int y=0; y<p[x].length; y++) {
    //                ymean+=p[x][y];
    //                xmean+=y;
    //            }
    //        }
    //        xmean/=(p.length*p[0].length);
    //        ymean/=(p.length*p[0].length);
    //        double sum1=0;
    //        double sum2=0;
    //        for (int i=0; i<p.length; i++) {
    //            for (int j=0; j<p[i].length; j++) {
    //                sum1+=(j-xmean)*(p[i][j]-ymean);
    //                sum2+=(j-xmean)*(j-xmean);
    //            }
    //        }
    //        double m=sum1/sum2;
    //        double c=ymean-m*xmean;
    //
    //        for (int i=0; i<p.length; i++) {
    //            for (int j=0; j<p[i].length; j++) {
    //                line[i][j]=m*j+c;
    //            }
    //        }
    //        System.out.println("число эл. " + m);
    //        return line;
    //    }

    public static void trendDeletion(double[][] p)
    {
        int imgWidth = p.length;
        int imgHeight = p[0].length;

        double[] horizontal = new double[imgWidth];
        double[] vertical = new double[imgHeight];

        final int lineIndex = 10;
        final double c = 0.1;

        for (int x = 0; x < imgWidth; x++)
        {
            horizontal[x] = p[x][lineIndex]*(1.0 - c);
        }

        for (int y = 0; y < imgHeight; y++)
        {
            vertical[y] = p[lineIndex][y]*(1.0 - c);
        }

        vertical = linReg(vertical);
        horizontal = linReg(horizontal);

        //            for (int x=0; x<imgWidth; x++) horizontal[x]=imgWidth-1-x;  //проверить, что изображение ровное
        //            for (int y=0; y<imgHeight; y++) vertical[y]=imgHeight-1-y;

        for (int y = 0; y < imgHeight; y++)
            for (int x = 0; x < imgWidth; x++) {
                p[x][y] -= horizontal[x];
                // System.out.println("1 " + horizontal[x]);
            }

        for (int x = 0; x < imgWidth; x++)
            for (int y = 0; y < imgHeight; y++) {
                p[x][y] -= vertical[y];
                // System.out.println("2 " + vertical[y]);
            }
    }

    public void deleteTrend3D(double[][] massive) {
        int imgWidth = massive.length;
        int imgHeight = massive[0].length;
        //for (int a = 0; a < loopsDeleteTrendCount; a++) {
            //определяем координаты 3-х точек плоскости
            int x1 = deleteTrendX;
            int y1 = deleteTrendY;
            int z1 = (int) massive[0][0];
            int x3 = imgWidth - 1;
            int y3 = imgHeight - 1;
            int z3 = (int) massive[x3][y3];
            int x2 = imgWidth - 1;
            int y2 = 0;
            int z2 = (int) massive[x2][y2];
            // Задание уравнения плоскости
            int A = (y2 - y1) * (z3 - z1) - (z2 - z1) * (y3 - y1);
            int B = (x2 - x1) * (z3 - z1) - (z2 - z1) * (x3 - x1);
            int C = (x2 - x1) * (y3 - y1) - (y2 - y1) * (x3 - x1);
            int D = -((A * x1) + (B * y1) + (C * z1));

            A = A / 3;
            B = B / 3;
            C = C / 3;
            D = D / 3;

            // Cоздание массива корректирующей плоскости
            double[][] datatrand = new double[imgWidth][imgHeight];
            for (int j = 0; j < imgHeight; j++) {
                for (int i = 0; i < imgWidth; i++) {

                    int Z = (B * j - A * i - D) / C;
                    datatrand[i][j] = Z;
                }
            }

            // double [][] massive = new double[imgWidth][imgHeight];
            for (int j = 0; j < imgHeight; j++) {
                for (int i = 0; i < imgWidth; i++) {


                    massive[i][j] = massive[i][j] - datatrand[i][j];
                }
            }
        }
    //}

    public void FFT_2D(double[][] real, double[][] image, boolean inverse)
    {
        int n = real.length;
        Signal2d signal2d = new Signal2d(n, n);
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                signal2d.setReAt(i, j, real[i][j]);
                signal2d.setImAt(i, j, image[i][j]);
            }
        }

        //        writeMassiveToFile(massive, n, inputFileName + "_real_start.txt");
        //        writeMassiveToFile(image, n, inputFileName + "_image_start.txt");

        FastFourier2d transformer2D = new FastFourier2d();
        if (!inverse)    // если выбрано прямое преобразование
        {
            transformer2D.transform(signal2d);
        }
        else    // если выбрано обратное преобразование
        {
            transformer2D.inverse(signal2d);
        }
        transformer2D.shutdown();

        // вытаскиваем действительную и мнимую часть
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                real[i][j] = signal2d.getReAt(i, j);
                image[i][j] = signal2d.getImAt(i, j);
            }
        }
    }

    public void fillLeftHalfOfImage(double[][] real, double[][] image, double value)
    {
        int n = real.length;

        // заполняем левую половину нулями
        for (int x = 0; x < n/2; x++)
        {
            for (int y = 0; y < n; y++)
            {
                real[x][y] = value;
                image[x][y] = value;
            }
        }

//        writeMassiveToFile(real, n, inputFileName + "_real_after_mask.txt");
//        writeMassiveToFile(image, n, inputFileName + "_image_after_mask.txt");
    }

    public void divideImageToReal(double[][] real, double[][] image, double[][] massive)
    {
        // делим мнимую часть на действительную
        for (int i = 0; i < massive.length; i++)
        {
            for (int j = 0; j < massive[i].length; j++)
            {
                massive[i][j] = image[i][j] / real[i][j];
            }
        }
    }

    public void atan(double[][] massive)
    {
        // считаем арктангенс
        for (int i = 0; i < massive.length; i++)
        {
            for (int j = 0; j < massive[i].length; j++)
            {
                massive[i][j] = Math.atan(massive[i][j]);
            }
        }
    }

    public void convertToAngstroms(double[][] massive, double waveLength)
    {
        // перевод в ангстремы
        for (int i = 0; i < massive.length; i++)
        {
            for (int j = 0; j < massive[i].length; j++)
            {
                massive[i][j] = (massive[i][j] * waveLength) / (4 * Math.PI);
            }
        }

//        writeMassiveToFile(massive, n, inputFileName + "_angstrem.txt");
    }

    public void deleteTrend(double[][] massive, int loopsDeleteTrendCount)
    {
        normalize(massive, 1, 1, massive.length-1, massive[0].length-1);

//        writeMassiveToFile(massive, n, inputFileName + "_normalized.txt");

        for (int a = 0; a < loopsDeleteTrendCount; a++)
        {
            // конвертируем нормализованное значение в 0 - 255
            for (int i = 0; i < massive.length; i++)
            {
                for (int j = 0; j < massive[0].length; j++)
                {
                    massive[i][j] *= 255;
                }
            }

            // считаем каким должен быть градиент
            final int width = massive.length;
            final int height = massive[0].length;
            double[] trendMassive = new double[width];
            double[] trendMassiveH = new double[height];

            for (int i = 0; i < width; i++)
            {
                double k = (((double)(width - 1 - i) / (double) (width - 1)));
                trendMassive[i] = 255 * k;
            }

            for (int j= 0; j < height; j++)
            {
                double k1 = (((double)(height - 1 - j) / (double) (height - 1)));
                trendMassiveH[j] = 255 * k1;
            }

            // вычитаем из градиента получившееся значение
            for (int i = 0; i < massive.length; i++)
            {
                for (int j = 0; j < massive[i].length; j++)
                {
                    massive[i][j] =  trendMassive[i]-massive[i][j];
//                    massive[i][j] =  trendMassiveH[j]- massive[i][j];
                }
            }




            normalize(massive, 1, 1, massive.length-1, massive[0].length-1);

            for (int i = 0; i < massive.length; i++)
            {
                for (int j = 0; j < massive[0].length; j++)
                {
                    massive[i][j] = Math.abs(255 - (massive[i][j] * 255));
                }
            }

            // зануляем граничные значения
            for (int i = 0; i < massive.length; i++)
            {
                massive[i][0] = 0;      // левая вертикальная линия
                massive[i][massive[0].length-1] = 0;    // правая вертикальная линия
            }
            for (int j = 0; j < massive[0].length; j++)
            {
                massive[0][j] = 0;      // верхняя горизонтальная линия
                massive[massive.length-1][j] = 0;    // нижняя горизонтальная линия
            }
        }
    }
}
