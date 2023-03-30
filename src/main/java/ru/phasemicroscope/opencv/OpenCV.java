package ru.phasemicroscope.opencv;

import org.opencv.core.*;
//import org.bytedeco.opencv.opencv_core.Mat;
import org.opencv.core.Core;
import org.opencv.core.MatOfRect;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

/**
 * Обработчик OpenCV для обнаружения объектов
 */
public class OpenCV
{
    private static final String cascadeClassifierXMLFileName = "src/main/resources/face.xml";

    private int videoCaptureIndex = -1;  // номер камеры
    private VideoCapture capture;   // камера

    public OpenCV()
    {
        // загружаем библиотеку OpenCV core
        System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
    }

    /** Обнаруживает и выделяет объекты на изображении
     * @param imageMatrix матрица изображения
     * @param classifier классификатор, по которому происходит распознавание объектов
     * @param image вывод обработанного изображения с выделенными объектами
     */
    public void detectObjects(Mat imageMatrix, CascadeClassifier classifier, BufferedImage image)
    {
        // выполняем обнаружение объектов
        MatOfRect faceDetections = new MatOfRect();     // матрица прямоугольников
        classifier.detectMultiScale(imageMatrix, faceDetections);   // выполняем обнаружение

        // рисуем прямоугольники
        drawDetectedRects(imageMatrix, faceDetections.toArray());

        // создаем изображение из матрицы
        convertMatrixToBufferedImage(imageMatrix, image);
    }

    /** Обнаруживает и выделяет объекты на изображении
     * @param imageMatrix матрица изображения
     * @param classifier классификатор, по которому происходит распознавание объектов
     * @return обработанное изображение с выделенными объектами
     */
    public BufferedImage detectObjects(Mat imageMatrix, CascadeClassifier classifier)
    {
        BufferedImage image = new BufferedImage(imageMatrix.width(), imageMatrix.height(), BufferedImage.TYPE_3BYTE_BGR);   // создаем изображение из матрицы
        detectObjects(imageMatrix, classifier, image);     // обнаруживаем объекты
        return image;
    }

    /** Конвертация матрицы в изображение
     * @param imageMatrix матрица изображения
     * @return изображение
     */
    public BufferedImage convertMatrixToBufferedImage(Mat imageMatrix)      // создаем изображение из матрицы
    {
        BufferedImage image = new BufferedImage(imageMatrix.width(), imageMatrix.height(), BufferedImage.TYPE_3BYTE_BGR);
        convertMatrixToBufferedImage(imageMatrix, image);
        return  image;
    }

    /** Конвертация матрицы в изображение
     * @param imageMatrix матрица изображения
     * @param image результирующее изображение
     */
    public void convertMatrixToBufferedImage(Mat imageMatrix, BufferedImage image)      // создаем изображение из матрицы
    {
        byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        imageMatrix.get(0, 0, data);
    }

    /** Делает снимок с заданной камеры
     * @return снимок с заданной камеры
     */
    public Mat captureFrame()
    {
        // пробуем сделать кадр, тем самым инициализировав камеру
        Mat matrix = new Mat();
        captureFrame(matrix);
        return matrix;
    }

    /** Делает снимок с заданной камеры
     * @param matrix матрица для сохранения снимка
     */
    public void captureFrame(Mat matrix)
    {
        capture.read(matrix);
    }

    /** Рисует прямоугольники вокруг обнаруженных объектов
     * @param image изображение
     * @param rects координаты объектов
     */
    public void drawDetectedRects(Mat image, Rect[] rects)
    {
        // рисуем прямоугольники
        for (Rect rect : rects) {
            Imgproc.rectangle(
                    image,                                               // изображение, на котором рисуем
                    new Point(rect.x, rect.y),                           // слева снизу
                    new Point(rect.x + rect.width, rect.y + rect.height), // сверху справа
                    new Scalar(0, 255, 0),                                      // цвет в формате BGR
                    3
            );
        }
    }

    /**
     * @return классификатор для распознавания лиц
     */
    public static CascadeClassifier faceClassifier()
    {
        return new CascadeClassifier(cascadeClassifierXMLFileName);     // инициализируем CascadeClassifier
    }

    /**
     * @return текущий номер выбранной камеры
     */
    public int getVideoCaptureIndex()
    {
        return videoCaptureIndex;
    }

    /** Задаёт номер используемой камеры
     * @param videoCaptureIndex номер камеры
     * @throws Exception ошибка, в случае, если не удалось подключиться к камере
     */
    public void setVideoCaptureIndex(int videoCaptureIndex) throws Exception
    {
        if(this.videoCaptureIndex != videoCaptureIndex)
        {
            this.videoCaptureIndex = videoCaptureIndex;
            this.capture = new VideoCapture(videoCaptureIndex);     // создаем подключение к камере

            // смотрим, чтобы камера работала
            if(!capture.isOpened())
            {
                throw new Exception("Camera not detected");
            }

            captureFrame(); // пробуем сделать кадр
        }
    }
}
