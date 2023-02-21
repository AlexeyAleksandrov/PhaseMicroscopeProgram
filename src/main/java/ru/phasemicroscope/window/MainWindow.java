package ru.phasemicroscope.window;

import com.github.sarxos.webcam.Webcam;
import ru.phasemicroscope.processing.MinMaxSearcher;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Главное окно
 */
public class MainWindow
{
    private JFrame frame;   // окно программы
    private int screenWidth = 1200;
    private int screenHeight = 800;

    // виджеты
    private JComboBox<String> comboBox;
    private JButton buttonChose;

    private BufferedImage image;    // изображение, которое будет находиться в Label
    private Render render;  // рендер изображения

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

        render = new Render(image, frame);  // создаём рисовальщик

        // создаем блок отображения картинок
        ImageIcon imageIcon = new ImageIcon(image);
        JLabel imageLabel = new JLabel(imageIcon);

        comboBox = new JComboBox<>();
        buttonChose = new JButton();
        buttonChose.setText("Кнопка");

        // layout - чтобы элементы не расползались
        BorderLayout borderLayout = new BorderLayout();
        Container mainWindow = frame.getContentPane();      // получаем область контента
        mainWindow.setLayout(borderLayout);                 // задаем layout

        mainWindow.add(comboBox, BorderLayout.PAGE_START);
        mainWindow.add(buttonChose, BorderLayout.PAGE_END);
        mainWindow.add(imageLabel, BorderLayout.CENTER);    // задаем расположение по центру

        // отображаем окно
        frame.setSize(image.getWidth(), image.getHeight());
        frame.setVisible(true);

        // выводим список камер
        List<Webcam> webcamList = Webcam.getWebcams();
        webcamList.forEach(webcam ->
        {
            comboBox.addItem(webcam.getName());
        });

        // добавляем обработчик
        buttonChose.addActionListener(this::onButtonChoseClicked);
    }

    private void onButtonChoseClicked(ActionEvent e)
    {
        String webcamName = (String) comboBox.getSelectedItem();
        Webcam webcam = Webcam.getWebcamByName(webcamName);
        Dimension dimension = new Dimension();
        dimension.setSize(640, 480);
        webcam.setViewSize(dimension);
        webcam.open();
        if (!webcam.isOpen())
        {
            System.out.println("Нет доступа!");
            return;
        }
        BufferedImage image = webcam.getImage();
        webcam.close();
//        BufferedImage image = null;
//        try
//        {
//            image = ImageIO.read(new File("src/main/resources/lines2.jpg"));
//        }
//        catch (IOException ex)
//        {
//            throw new RuntimeException(ex);
//        }

        MinMaxSearcher minMaxSearcher = new MinMaxSearcher();
        minMaxSearcher.drawMinMax(image);
        render.draw(image);
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

    public Render getRender() {
        return render;
    }
}
