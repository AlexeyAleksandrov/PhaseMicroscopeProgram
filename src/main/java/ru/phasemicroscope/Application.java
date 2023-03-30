package ru.phasemicroscope;


import org.opencv.core.Mat;
import org.opencv.objdetect.CascadeClassifier;
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

        OpenCV openCV = new OpenCV();     // обнаружение лиц
        openCV.setVideoCaptureIndex(0);   // задаем номер камеры = 0

        Mat matrix = openCV.captureFrame();   // делаем снимок с камеры
        BufferedImage img = openCV.convertMatrixToBufferedImage(matrix);    // конвертируем в изображение

        render.draw(img);  // рисуем

        // рисуем постоянно видеопоток
        while(mainWindow.isVisible())
        {
            openCV.captureFrame(matrix);   // делаем снимок с камеры
            openCV.convertMatrixToBufferedImage(matrix, img);   // конвертируем в изображение
            render.draw(img);  // рисуем
        }
    }
}
