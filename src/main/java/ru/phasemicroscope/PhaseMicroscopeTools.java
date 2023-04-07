package ru.phasemicroscope;

import com.tambapps.fft4j.FastFourier2d;
import com.tambapps.fft4j.Signal2d;
import org.apache.commons.io.FileUtils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;

// из кода от чатбота


public class PhaseMicroscopeTools
{
    public static boolean enableCenterReverse = false;   // включить костыль с инверсией изображения относительно центра
    public static boolean enableLogarithmicScale = true;    // логарифмическое масштабирование для модуля значений пикселей после развёртки

    private double[][] massive = null;
    private double[][] real = null;
    private double[][] image = null;

    public static void main(String[] args) throws IOException
    {
        //Loading the OpenCV core library
        System.loadLibrary( Core.NATIVE_LIBRARY_NAME );

        PhaseMicroscopeTools tools = new PhaseMicroscopeTools();
        tools.onStart();
    }

    public void onStart() throws IOException
    {
        String inputFileName = "src/main/resources/img[1]-2";
        String inputFileNFormat = ".jpg";
        String imagePath = inputFileName + inputFileNFormat;

        // OpenCV загрузка изображения
        // =======================================================

        //Instantiating the Imagecodecs class
        Imgcodecs imageCodecs = new Imgcodecs();

        //Reading the Image from the file
        Mat matrix = imageCodecs.imread(imagePath);

        BufferedImage bufferedImage = convertMatrixToBufferedImage(matrix);

        System.out.println("Image Loaded");
        // =======================================================

        int n = getMatrixSizeForImage(bufferedImage);   // считаем размер для матрицы
        massive = getImageMassive(bufferedImage);    // получаем массив пикселей
        real = new double[n][n];
        image = new double[n][n];

        Signal2d signal2d = new Signal2d(n, n);
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                signal2d.setReAt(i, j, massive[i][j]);
                signal2d.setImAt(i, j, 0.0);
            }
        }

//        writeMassiveToFile(massive, n, inputFileName + "_real_start.txt");
//        writeMassiveToFile(image, n, inputFileName + "_image_start.txt");

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

        // считаем арктангенс
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                massive[i][j] = Math.atan(massive[i][j]);
            }
        }

        writeMassiveToFile(massive, n, inputFileName + "_atan.txt");


        //        // ========================
        //
        normalize(massive);
        denormalize(massive, -1 * Math.PI, Math.PI);
        writeMassiveToFile(massive, n, inputFileName + "_atan_normal.txt");
        //
        //        // ========================

        unwrapMassive(massive, n);

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

        normalize(massive, 1, 1, n-1, n-1);

        writeMassiveToFile(massive, n, inputFileName + "_normalized.txt");

        // конвертируем нормализованное значение в 0 - 255
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                massive[i][j] *= 255;
            }
        }

        // считаем каким должен быть градиент
        double[] trendMassive = new double[n];

        for (int i = 0; i < n; i++)
        {
            double k = (((double)(n - 1 - i) / (double) (n - 1)));
            trendMassive[i] = 255 * k;
        }

        // вычитаем из градиента получившееся значение
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                massive[i][j] = trendMassive[i] - massive[i][j];

            }
        }

        normalize(massive, 1, 1, n-1, n-1);

        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                massive[i][j] = Math.abs(255 - (massive[i][j] * 255));

//                if  (i == 0 || j == 0)
//                {
//                    massive[i][j] = 0;
//                }
//                if  (j == n-1 )
//                {
//                    massive[i][j] = 0;
//                }
//                if  (i == n-1 )
//                {
//                    massive[i][j] = 0;
//                }
            }
        }

        // зануляем граничные значения
        for (int i = 0; i < n; i++)
        {
            massive[0][i] = 0;      // верхняя горизонтальная линия
            massive[n-1][i] = 0;    // нижняя горизонтальная линия
            massive[i][0] = 0;      // левая вертикальная линия
            massive[i][n-1] = 0;    // правая вертикальная линия
        }

        MaxMin(massive);
        writeMassiveToFile(massive, n, inputFileName + "_final.txt");

        // вывод в файл
        setImageFromMassive(massive, bufferedImage);

        ImageIO.write(bufferedImage, "jpg", new File(inputFileName + "_out" + inputFileNFormat));
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

    //    public static void twoDfft(double[][] inputData, double[][] realOut,
    //                               double[][] imagOut, double[][] amplitudeOut)
    //    {
    //        int height = inputData.length;
    //        int width = inputData[0].length;
    //
    //        // Two outer loops iterate on output data.
    //        for (int yWave = 0; yWave < height; yWave++)
    //        {
    //            for (int xWave = 0; xWave < width; xWave++)
    //            {
    //                System.out.println("x = " + xWave + " из " + width + " y = "+ yWave + " из " + height + " Обработано: " + Math.round(((double) xWave + (double)yWave * (double)width) / ((double)height * (double)width) * 1000000.0)/10000.0 + " %");
    //                // Two inner loops iterate on input data.
    //                for (int ySpace = 0; ySpace < height; ySpace++)
    //                {
    //                    for (int xSpace = 0; xSpace < width; xSpace++)
    //                    {
    //                        // Compute real, imag, and ampltude.
    //                        realOut[yWave][xWave] += (inputData[ySpace][xSpace] * Math
    //                                .cos(2
    //                                        * Math.PI
    //                                        * ((1.0 * xWave * xSpace / width) + (1.0
    //                                        * yWave * ySpace / height))))
    //                                / Math.sqrt(width * height);
    //                        imagOut[yWave][xWave] -= (inputData[ySpace][xSpace] * Math
    //                                .sin(2
    //                                        * Math.PI
    //                                        * ((1.0 * xWave * xSpace / width) + (1.0
    //                                        * yWave * ySpace / height))))
    //                                / Math.sqrt(width * height);
    //                        amplitudeOut[yWave][xWave] = Math
    //                                .sqrt(realOut[yWave][xWave]
    //                                        * realOut[yWave][xWave]
    //                                        + imagOut[yWave][xWave]
    //                                        * imagOut[yWave][xWave]);
    //                    }
    //                    //                    System.out.println(realOut[yWave][xWave] + " + "
    //                    //                            + imagOut[yWave][xWave] + " i");
    //                }
    //                System.out.println("x = " + xWave + " y = " + yWave + "Re = " + realOut[yWave][xWave] + " Im = " + imagOut[yWave][xWave] + " Amp = " + amplitudeOut[yWave][xWave]);
    //            }
    //        }
    //    }

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
        double c = 0.7;

        double[] unwrapped = new double[n];

        for (int i = 0; i < n; i++)
        {
            if(i==n-1)
            {
                unwrapped[n-1] = massive[n-1] + (2 * Math.PI * k);
                return unwrapped;
            }
            unwrapped[i] = massive[i] + (2 * Math.PI * k);
            if(Math.abs(massive[i+1] - massive[i]) > (Math.PI * c))
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
        for (int y = 0; y < n; y++)
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
        for (int x = 0; x < n; x++)
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
        int n = massive.length;
        normalize(massive, 0, 0, n, n);
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

        System.out.println("min = " + min + "  max = " + max);

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
        int n = massive.length;

        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                massive[i][j] = massive[i][j] * (max - min) + min;
            }
        }
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

    public static void deleteTrend3D(double[][] massive) {
        int imgWidth = massive.length;
        int imgHeight = massive[0].length;
        //определяем координаты 3-х точек плоскости
        int x1 = 1;
        int y1 = 1;
        int z1 = (int) massive[x1][y1];
        int x3 = imgWidth - 1;
        int y3 = imgHeight - 1;
        int z3 = (int) massive[x3][y3];
        int x2 = imgWidth - 1;
        int y2 = 1;
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
        for (int j = 0; j < imgHeight; j++) {
            for (int i = 0; i < imgWidth; i++) {

                int Z = (B * j - A * i - D) / C;
                massive[i][j] = Z;
            }
        }

    }
}
