package ru.phasemicroscope.window;

import ru.phasemicroscope.PhaseMicroscopeTools;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Главное окно
 */
public class MainWindow
{
    private JFrame frame;   // окно программы

    private int screenWidth = 1200;
    private int screenHeight = 800;

    private BufferedImage image;    // изображение, которое будет находиться в Label

    private Render render;

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

        // кнопка изображение
        JButton buttonImage = new JButton("Изображение");
        buttonImage.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                on_buttonImage_clicked();   // при нажатии вызываем функцию обработки изображения
            }
        });

        // кнопка видеопоток
        JButton buttonVideo = new JButton("Видеопоток");
        buttonVideo.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                ob_buttonVideo_clicked();   // при нажатии вызываем функцию обработки видео
            }
        });

        // переключатель видео
        JCheckBox checkBoxVideo = new JCheckBox("Показать оригинал");
        checkBoxVideo.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                on_checkBoxVideo_clicked();
            }
        });

        // layout - чтобы элементы не расползались
        BorderLayout borderLayout = new BorderLayout();
        Container mainWindow = frame.getContentPane();      // получаем область контента
        mainWindow.setLayout(borderLayout);                 // задаем layout
        mainWindow.add(buttonImage, BorderLayout.BEFORE_LINE_BEGINS);   // добавляем кнопку обработки изображения
        mainWindow.add(buttonVideo, BorderLayout.AFTER_LINE_ENDS);      // добавляем кнопку обработки видеопотока
        mainWindow.add(imageLabel, BorderLayout.CENTER);    // задаем расположение по центру
        mainWindow.add(checkBoxVideo, BorderLayout.AFTER_LAST_LINE);      // добавляем переключатель режима видео

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

    public void on_buttonImage_clicked()
    {
        init();     // сбрасываем кадр, чтобы был белый фон
        render.draw(image);     // рисуем пустое изображение

        String inputFileName = "src/main/resources/filter/Interferogramma";
        String inputFileNFormat = ".bmp";
        String imagePath = inputFileName + inputFileNFormat;

        PhaseMicroscopeTools tools = new PhaseMicroscopeTools();

        // работаем с изображением
        // загружаем изображение
        BufferedImage bufferedImage = tools.loadImage(imagePath);   // загружаем изображение

        // обрабатываем изображение
        tools.processImage(bufferedImage);

        try
        {
            ImageIO.write(bufferedImage, "jpg", new File(inputFileName + "_out" + inputFileNFormat));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        render.draw(bufferedImage);
//        System.out.println("Готово!");
    }

    public boolean isVideoRunning = false;

    public void ob_buttonVideo_clicked()
    {
        init();     // сбрасываем кадр, чтобы был белый фон
        render.draw(image);     // рисуем пустое изображение

        if(!isVideoRunning)
        {
            System.out.println("Запуск видеопотока");
            isVideoRunning = true;
        }
        else
        {
            System.out.println("Остановка видеопотока");
            isVideoRunning = false;
        }
    }

    public boolean showVideoOriginal = false;

    public void on_checkBoxVideo_clicked()
    {
        showVideoOriginal = !showVideoOriginal;
    }

    public Render getRender()
    {
        return render;
    }

    public void setRender(Render render)
    {
        this.render = render;
    }
}
