package ru.phasemicroscope.window;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Главное окно
 */
public class MainWindow
{
    private JFrame frame;   // окно программы

    private int screenWidth = 1200;
    private int screenHeight = 800;

    private BufferedImage image;    // изображение, которое будет находиться в Label

    public MainWindow()
    {
        init();
    }

    /** Создает окно программы
     * @param screenWidth ширина окна
     * @param screenHeight высота окна
     */
    public MainWindow(int screenWidth, int screenHeight)
    {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        init();
    }

    /**
     * Инициализация, создает белое изображение на фон
     */
    private void init()
    {
        // создаем пустое изображение
        image = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_3BYTE_BGR);

        // заполняем его белым цветом
        Graphics2D graphics = image.createGraphics();
        graphics.setPaint(Color.WHITE);
        graphics.fillRect(0,0, image.getWidth(), image.getHeight());
    }

    /**
     * Показывает окно программы
     */
    public void show()
    {
        // создаем окно
        frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        // создаем блок отображения картинок
        ImageIcon imageIcon = new ImageIcon(image);
        JLabel imageLabel = new JLabel(imageIcon);

        // layout - чтобы элементы не расползались
        BorderLayout borderLayout = new BorderLayout();
        Container mainWindow = frame.getContentPane();      // получаем область контента
        mainWindow.setLayout(borderLayout);                 // задаем layout
        mainWindow.add(imageLabel, BorderLayout.CENTER);    // задаем расположение по центру

        // отображаем окно
        frame.setSize(image.getWidth(), image.getHeight());
        frame.setVisible(true);
    }

    public JFrame getFrame()
    {
        return frame;
    }

    public void setFrame(JFrame frame)
    {
        this.frame = frame;
    }

    public BufferedImage getImage()
    {
        return image;
    }

    public void setImage(BufferedImage image)
    {
        this.image = image;
    }

    public boolean isVisible()
    {
        return frame.isVisible();
    }
}
