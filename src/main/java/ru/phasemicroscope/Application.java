package ru.phasemicroscope;


import org.opencv.core.Mat;
import ru.phasemicroscope.opencv.OpenCV;
import ru.phasemicroscope.window.MainWindow;
import ru.phasemicroscope.window.Render;

import java.awt.image.BufferedImage;

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
                        tools.processImage(img);  // обрабатываем изображение
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
