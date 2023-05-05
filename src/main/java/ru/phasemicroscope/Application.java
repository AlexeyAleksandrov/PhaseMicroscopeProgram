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

public class Application
{

    public static void main(String[] args) throws Exception
    {
        MainWindow mainWindow = new MainWindow();   // главное окно
        mainWindow.show();  // показываем окно

        BufferedImage image = mainWindow.getImage();    // главное изображение
        Render render = new Render(image, mainWindow.getFrame());   // рендер
        mainWindow.setRender(render);

        Thread appThread = new Thread(() ->
        {
            OpenCV openCV = new OpenCV();
            try
            {
                openCV.setVideoCaptureIndex(0);   // задаем номер камеры = 0
            }

            catch (Exception e)
            {
                e.printStackTrace();
            }

            Mat matrix = openCV.captureFrame();   // делаем снимок с камеры
            BufferedImage img = openCV.convertMatrixToBufferedImage(matrix);    // конвертируем в изображение

            //        render.draw(img);  // рисуем

            // рисуем постоянно видеопоток
            while(mainWindow.isVisible())
            {
                PhaseMicroscopeTools tools = new PhaseMicroscopeTools();

                if(mainWindow.isVideoRunning)   // если видео должно идти
                {
                    openCV.captureFrame(matrix);   // делаем снимок с камеры
                    openCV.convertMatrixToBufferedImage(matrix, img);   // конвертируем в изображение

                    if(!mainWindow.showVideoOriginal)   // если не надо показывать оригинал
                    {

                        if (phase==false&&trend==false) {
                            tools.processImage(img, mainWindow.invert);



                        }
                        if (phase==true) {
                            tools.processImageF(img);
                        }

                        if (trend==true) {
                            tools.processImageTrend(img);
                        }

                        if(mainWindow.oneCameraShot)    // если нужно сохранить снимок
                        {
                            try
                            {
                                double[][] massive = tools.getImageMassive(img);
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
                                    tools.convertToAngstroms(massive, waveLength);
                                    if(medianF) {
                                        tools.medianFilter(massive, medianCount);
                                    }
                                    File textFile = fileChooser.getSelectedFile().getName().endsWith(".jpg")
                                            ? fileChooser.getSelectedFile()
                                            : new File(fileChooser.getSelectedFile() + ".jpg");

                                    String textFileName = textFile.getAbsolutePath().replace("jpg", "txt");
                                    tools.writeMassiveToFile(massive, textFileName);
                                    System.out.println("Файл записан");

                                    //   tools.writeMassiveToFile(massive, String.valueOf(fileChooser.getSelectedFile()));
                                    //    File fileToSave = fileChooser.getSelectedFile();
                                    //  System.out.println("Save as file: " + fileToSave.getAbsolutePath());
                                }
                                //  tools.writeMassiveToFile(massive, "src/main/resources/photoFromCamera_out.txt");    // записываем текстовый файл
//            ImageIO.write(bufferedImage, "jpg", new File("src/main/resources/photoFromCamera.jpg"));    // записываем изображение ??
                            }
                            catch (IOException e) //?
                            {
                                e.printStackTrace();
                            }

                            mainWindow.oneCameraShot = false;
                        }
                        // tools.processImage(img);  // обрабатываем изображение
                    }

                    render.draw(img);  // рисуем
                }

                // делаем задержку
                try
                {
                    Thread.sleep(17);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        });

        appThread.start();  // запускаем поток для камеры
    }
}
