package ru.phasemicroscope.window;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Класс для отрисовки изображения
 */
public class Render
{
    private BufferedImage image;    // рисуемое изображение
    private JFrame frame;   // окно, в котором рисуем

    private volatile static boolean isFrameReadyToDraw = true;      // переменная для синхронизации с обновлением экрана

    /**
     * @param image изображение, на котором будет происходить рисование
     * @param frame окно, в котором происходит рисование
     */
    public Render(BufferedImage image, JFrame frame)
    {
        this.image = image;
        this.frame = frame;
    }

    /** Нарисовать изображение в окне
     * @param img изображение
     */
    public void draw(BufferedImage img)
    {
        // масштабируем изображение до размеров окна
        Image imgScaled = getScaledImage(img);

        // рисуем изображение
        Graphics2D graphics = image.createGraphics();
        graphics.drawImage(imgScaled, 0, 0, imgScaled.getWidth(null), imgScaled.getHeight(null), null);

        // перерисовываем кадр
        isFrameReadyToDraw = false;
        SwingUtilities.invokeLater(() -> {
            frame.repaint();
            isFrameReadyToDraw = true;
        });
    }

    /** Масштабирует изображение до размеров окна, до максимальной ширины, или максимальной высоты
     * @param img изображение
     * @return масштабированное изображение
     */
    private Image getScaledImage(BufferedImage img)
    {
        double screenWidth = image.getWidth();
        double screenHeight = image.getHeight();

        double imgWidth = img.getWidth();
        double imgHeight = img.getHeight();

        double widthScaleFactor = screenWidth/imgWidth;
        double heightScaleFactor = screenHeight/imgHeight;

        double scaleFactor = heightScaleFactor;

        if(widthScaleFactor < heightScaleFactor)
        {
            scaleFactor = widthScaleFactor;
        }

        imgWidth *= scaleFactor;
        imgHeight *= scaleFactor;

        return img.getScaledInstance((int) imgWidth, (int) imgHeight, Image.SCALE_FAST);
    }
}
