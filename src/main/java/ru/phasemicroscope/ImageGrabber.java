package ru.phasemicroscope;

import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.IplImage;

import static org.bytedeco.opencv.global.opencv_core.cvFlip;
import static org.bytedeco.opencv.helper.opencv_imgcodecs.cvSaveImage;

public class ImageGrabber implements Runnable
{
    final int INTERVAL = 100;///you may use interval
    CanvasFrame canvas = new CanvasFrame("Web Cam");

    boolean isGrabbing = false;

    public ImageGrabber()
    {
        canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
    }

    public void run()
    {
//        new File("images").mkdir();
        FrameGrabber grabber = new OpenCVFrameGrabber(0); // 1 for next camera
        OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
        IplImage img;
//        int i = 0;
        try
        {
            grabber.start();
            while (true)
            {
                if(!isGrabbing)
                {
                    continue;
                }

                Frame frame = grabber.grab();
                img = converter.convert(frame);
                // захваченный кадр перевернут, поэтому переворачиваем его на 180 градусов, чтобы сделать его правильным
                cvFlip(img, img, 1);// l-r = 90_degrees_steps_anti_clockwise
                // сохранение
//                cvSaveImage("images" + File.separator + (i++) + "-aa.jpg", img);
                canvas.showImage(converter.convert(img));
                Thread.sleep(INTERVAL);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
//
//    public static void main(String[] args)
//    {
//        ImageGrabber gs = new ImageGrabber();
//        Thread th = new Thread(gs);
//        th.start();
//    }


    public boolean isGrabbing()
    {
        return isGrabbing;
    }

    public void setGrabbing(boolean grabbing)
    {
        isGrabbing = grabbing;
    }
}