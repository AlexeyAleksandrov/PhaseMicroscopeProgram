package ru.phasemicroscope;


import org.opencv.core.Mat;
import ru.phasemicroscope.opencv.OpenCV;
import ru.phasemicroscope.window.MainWindow;
import ru.phasemicroscope.window.Render;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.bytedeco.opencv.global.opencv_imgproc.medianBlur;
import static ru.phasemicroscope.PhaseMicroscopeTools.waveLength;
import static ru.phasemicroscope.window.MainWindow.*;

public class Application {

    public static void main(String[] args) throws Exception {
        MainWindow mainWindow = new MainWindow();   // главное окно
        mainWindow.show();  // показываем окно

        BufferedImage image = mainWindow.getImage();    // главное изображение
        Render render = new Render(image, mainWindow.getFrame());   // рендер
        mainWindow.setRender(render);

        Thread appThread = new Thread(() ->
        {
            OpenCV openCV = new OpenCV();
            try {
                openCV.setVideoCaptureIndex(0);   // задаем номер камеры = 0
            } catch (Exception e) {
                e.printStackTrace();
            }

            Mat matrix = openCV.captureFrame();   // делаем снимок с камеры
            BufferedImage img = openCV.convertMatrixToBufferedImage(matrix);    // конвертируем в изображение

            //        render.draw(img);  // рисуем

            int savedFramesCount = 0;   // на данный момент 0 сохранённых кадров
            //  int MAX_SAVED_FRAMES_COUNT = streamCount;   // максимальное кол-во кадров, которое нужно сохранить, при достижении 150 следующие кадры не сохраняются

            PhaseMicroscopeTools tools = new PhaseMicroscopeTools();
            // рисуем постоянно видеопоток

            String streamJpgFileName = "";
            String streamCsvFileName = "";

            long startTime = 0;

            while (mainWindow.isVisible()) {


                if (mainWindow.isVideoRunning)   // если видео должно идти
                {
                    openCV.captureFrame(matrix);   // делаем снимок с камеры
                    openCV.convertMatrixToBufferedImage(matrix, img);   // конвертируем в изображение

                    if (!mainWindow.showVideoOriginal)   // если не надо показывать оригинал
                    {

                        if (mainWindow.oneCameraShot) {  // если нужно сохранить снимок

                            if (!Stream) {
                                {
                                    {

                                        try {

                                            double[][] massive = tools.processImage(img, invert);   // обрабатыываем изображение с камеры, в massive находится массив высот
                                            //                                System.out.println("Остановка видеопотока");
                                            //                                isVideoRunning = false;

                                            JFileChooser fileChooser = new JFileChooser();
                                            fileChooser.setDialogTitle("Specify a file to save");
                                            fileChooser.setName("decryptImage");
                                            fileChooser.setFileFilter(new FileFilter() {
                                                @Override
                                                public boolean accept(File file) {
                                                    if (file.getName().endsWith(".jpg")) {
                                                        return true;
                                                    }
                                                    return false;
                                                }

                                                @Override
                                                public String getDescription() {
                                                    return ".jpg";
                                                }
                                            });
                                            JFrame parentFrame = new JFrame();
                                            int userSelection = fileChooser.showSaveDialog(parentFrame);

                                            if (userSelection == JFileChooser.APPROVE_OPTION) {
                                                System.out.println("Запись файла...");
                                                ImageIO.write(img, "jpg",
                                                        fileChooser.getSelectedFile().getName().endsWith(".jpg")
                                                                ? fileChooser.getSelectedFile()
                                                                : new File(fileChooser.getSelectedFile() + ".jpg"));

//                                                tools.convertToAngstroms(massive, waveLength);

                                                double min = massive[0][0];
                                                for (int i = 0; i < massive.length; i++) {
                                                    for (int j = 0; j < massive[i].length; j++) {

                                                        if (massive[i][j] < min) {
                                                            min = massive[i][j];
                                                            // System.out.println("минимум - "+ min);

                                                        }

                                                    }
                                                }
                                                for (int i = 0; i < massive.length; i++) {
                                                    for (int j = 0; j < massive[i].length; j++) {

                                                        massive[i][j] = massive[i][j] - min;

                                                    }
                                                }



                                                File textFile = fileChooser.getSelectedFile().getName().endsWith(".jpg")
                                                        ? fileChooser.getSelectedFile()
                                                        : new File(fileChooser.getSelectedFile() + ".jpg");

                                                String textFileName = textFile.getAbsolutePath().replace("jpg", "csv");
                                                tools.writeMassiveToFile(massive, textFileName);
                                                System.out.println("Файл записан");

                                                //   tools.writeMassiveToFile(massive, String.valueOf(fileChooser.getSelectedFile()));
                                                //    File fileToSave = fileChooser.getSelectedFile();
                                                //  System.out.println("Save as file: " + fileToSave.getAbsolutePath());
                                            }
                                            //  tools.writeMassiveToFile(massive, "src/main/resources/photoFromCamera_out.txt");    // записываем текстовый файл
                                            //            ImageIO.write(bufferedImage, "jpg", new File("src/main/resources/photoFromCamera.jpg"));    // записываем изображение ??
                                        } catch (IOException e) //?
                                        {
                                            e.printStackTrace();
                                        }

                                        //       mainWindow.oneCameraShot = false;

                                    }
                                }
                            } //сохранение 1 кадра
                        }
                        else if (Stream && !mainWindow.oneCameraShot) {

                            if(savedFramesCount == 0)   // если мы только начинаем делать сохранение потока
                            {
                                JFileChooser fileChooser = new JFileChooser();
                                fileChooser.setDialogTitle("Specify a file to save");
                                fileChooser.setName("decryptImage");

                                JFrame parentFrame = new JFrame();
                                int userSelection = fileChooser.showSaveDialog(parentFrame);

                                if (userSelection == JFileChooser.APPROVE_OPTION) {
                                    streamJpgFileName = fileChooser.getSelectedFile().getName().endsWith(".jpg")
                                            ? fileChooser.getSelectedFile().getName()
                                            : new File(fileChooser.getSelectedFile() + ".jpg").getAbsolutePath();

                                    streamCsvFileName = streamJpgFileName.replace("jpg", "csv");
                                }
                            }

                            // сохраняем кадры
                            if (savedFramesCount < streamCount) {

                                if(savedFramesCount == 0)
                                {
                                    startTime = System.nanoTime();
                                }
                                // сохраняем изображение кадра в JPG
                                double[][] massive = tools.processImage(img, invert);   // обрабатыываем изображение с камеры, в massive находится массив высот
                                //  tools.convertToAngstroms(massive, waveLength);
                                //   double[][] min_massive = new double[massive.length][massive[0].length];
                                double min = massive[0][0];

                                for (int i = 0; i < massive.length; i++) {
                                    for (int j = 0; j < massive[i].length; j++) {

                                        if (massive[i][j] < min) {
                                            min = massive[i][j];
                                            // System.out.println("минимум - "+ min);

                                        }

                                    }
                                }
                                for (int i = 0; i < massive.length; i++) {
                                    for (int j = 0; j < massive[i].length; j++) {

                                        massive[i][j] = massive[i][j] - min;

                                    }
                                }

                                String streamCsvFileNameSave = streamCsvFileName.replace(".csv", "_" + (savedFramesCount + 1) + ".csv");
                                tools.writeMassiveToFile(massive, streamCsvFileNameSave);

                                savedFramesCount++;     // кадр сохранён, увеличиваем значение счётчика

                            }
                            if (savedFramesCount == streamCount) {
                                elapsed = (System.nanoTime() - startTime)/1000000;
                                Stream = false;
                                try {
                                    streamJpgFileName = streamJpgFileName.replace(".jpg", "_" + (savedFramesCount) + ".jpg");
                                    ImageIO.write(img, "jpg", new File(streamJpgFileName));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                String valueFileName = new File(streamJpgFileName).getParent() + "\\value.txt";
                                tools.ToFile(valueFileName);
                                System.out.println("записано кадров - " + streamCount);
                                savedFramesCount = 0;

                            }
                        }
                        else if (phase == false && trend == false) {
                            tools.processImage(img, invert);


                        }
                        else if (phase) {
                            tools.processImageF(img);
                        }

                        else if (trend) {
                            tools.processImageTrend(img);
                        }
//                        mainWindow.oneCameraShot = false;
                        //  Stream = false;
                        // tools.processImage(img);  // обрабатываем изображение
                    }



                    mainWindow.oneCameraShot = false;
                    render.draw(img);  // рисуем

                }

                // делаем задержку
                try {
                    Thread.sleep(17);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        appThread.start();  // запускаем поток для камеры
    }
}