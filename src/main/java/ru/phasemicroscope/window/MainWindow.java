package ru.phasemicroscope.window;
import org.opencv.core.Mat;
import ru.phasemicroscope.PhaseMicroscopeTools;
import ru.phasemicroscope.opencv.OpenCV;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.*;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Scanner;

import static ru.phasemicroscope.PhaseMicroscopeTools.loopsUnwrapCount;
import static ru.phasemicroscope.PhaseMicroscopeTools.waveLength;

/**
 * Главное окно
 */
public class MainWindow
{
    public static int INPUT_CAMERA_WIDTH = 1024;   // ширина кадра камеры
    public static int INPUT_CAMERA_HEIGHT = 576;  // высота кадра камеры
    public static int Trash_hold_int;
    public static int REAL_WIDTH = 103;   // ширина кадра
    public static int REAL_HEIGHT = 58;  // высота кадра
    public static boolean phase = false;
    public static boolean trend = false;
    public static double Trash_hold=0.6;
    public static boolean invert = false;
    public static boolean medianF = false;
    private JFrame frame;   // окно программы
    public static int medianCount = 3;

    public static int deleteTrendX;

    public static int deleteTrendY;
    private int screenWidth = 1150;
    private int screenHeight = 800;

//    JFrame parentFrame = new JFrame();
//    JFileChooser fileChooser = new JFileChooser();

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
        JButton buttonImage = new JButton("Изображение"); // не нужно
        buttonImage.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e)
            {

                on_buttonImage_clicked();   // при нажатии вызываем функцию обработки изображения
            }
        });


//        JLabel label;
//        Dictionary <Integer, JLabel> labels = new Hashtable<Integer, JLabel>();   //попытка создать шкалу
//        labels.put(new Integer(0), new JLabel("<html><font color=gray size=3>30"));
        JSlider Slider;
        BoundedRangeModel model = new DefaultBoundedRangeModel( (int)Trash_hold, 0, 0, 100); //слайдер для регулировки трешхолда
        Slider = new JSlider(model);

        Slider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                // меняем надпись
                Trash_hold = (((JSlider)e.getSource()).getValue());
                Trash_hold=Trash_hold/100;
                System.out.println("Трашхолд - " + Trash_hold);
            }
        });

////////////////////////////////////////////////////////

        SpinnerModel value =
                new SpinnerNumberModel(waveLength, //спиннер с выбором длины волны
                        0, //minimum value
                        10000, //maximum value
                        1); //step
        JSpinner spinnerWaveLength = new JSpinner(value);
        spinnerWaveLength.setBounds(100,100,50,30);

////////////////////////////////////////////////////////
        SpinnerModel Weight =
                new SpinnerNumberModel(INPUT_CAMERA_WIDTH, //спиннер с выбором ширины кадра
                        0, //minimum value
                        10000, //maximum value
                        1); //step
        JSpinner spinnerWeight = new JSpinner(Weight);
    //    Weight.setValue("длина волны (А)");
        // String[] label = {"длина волны (А) "};
        spinnerWeight.setBounds(100,100,50,30);
////////////////////////////////////////////////////////
        SpinnerModel Height =
                new SpinnerNumberModel(INPUT_CAMERA_HEIGHT , //спиннер с выбором ширины кадра
                        0, //minimum value
                        10000, //maximum value
                        1); //step
        JSpinner spinnerHeight = new JSpinner(Height);
        // spinner.setName("длина волны (А)");
        // String[] label = {"длина волны (А) "};
        spinnerHeight.setBounds(100,100,50,30);
////////////////////////////////////////////////////////


        ////////////////////////////////////////////////////////
        SpinnerModel Real_Weight =
                new SpinnerNumberModel(REAL_WIDTH , ///информационный спиннер для установки действительной ширины кадра
                        0, //minimum value
                        1000, //maximum value
                        1); //step
        JSpinner Real_spinnerWeight = new JSpinner(Real_Weight);
        // spinner.setName("длина волны (А)");
        // String[] label = {"длина волны (А) "};
        Real_spinnerWeight.setBounds(100,100,50,30);
////////////////////////////////////////////////////////
        SpinnerModel Real_Height =
                new SpinnerNumberModel(REAL_HEIGHT, //информационный спиннер для установки действительной высоты кадра
                        0, //minimum value
                        1000, //maximum value
                        1); //step
        JSpinner Real_spinnerHeight = new JSpinner(Real_Height);
        // spinner.setName("длина волны (А)");
        // String[] label = {"длина волны (А) "};
        Real_spinnerHeight.setBounds(100,100,50,30);
////////////////////////////////////////////////////////
        SpinnerModel medianF_Count =
                new SpinnerNumberModel(medianCount , //установка радиуса медианного фильтра
                        1, //minimum value
                        19, //maximum value
                        2); //step

//        if (medianCount % 2 == 0){
//            medianCount++;
//        }

        JSpinner spinner_medianF_Count = new JSpinner(medianF_Count);
        // spinner.setName("длина волны (А)");
        // String[] label = {"длина волны (А) "};
        spinner_medianF_Count.setBounds(100,100,50,30);

//        String[] items = {    //комбо бокс со списком камер
//                "камера 1",
//                "камера 2",
//                "камера 3"
//        };
//        JComboBox editComboBox = new JComboBox(items);
//        editComboBox.setEditable(true);
////////////////////////////////////////////////////////
        SpinnerModel loopsUnwrapC =
                new SpinnerNumberModel(loopsUnwrapCount, //спинер для задания кол-ва циклов развертки
                        0, //minimum value
                        100, //maximum value
                        1); //step
        JSpinner spinnerloopsUnwrapC = new JSpinner(loopsUnwrapC);
        // spinner.setName("длина волны (А)");
        // String[] label = {"длина волны (А) "};
        spinnerloopsUnwrapC.setBounds(100,100,50,30);

////////////////////////////////////////////////////////

////////////////////////////////////////////////////////
        SpinnerModel delete_TrendX =
                new SpinnerNumberModel(deleteTrendX, // для задания координаты Х при удалении тренда
                        0, //minimum value
                        100, //maximum value
                        1); //step
        JSpinner spinner_deleteTrendX = new JSpinner(delete_TrendX);
        // spinner.setName("длина волны (А)");
        // String[] label = {"длина волны (А) "};
        spinner_deleteTrendX.setBounds(100,100,50,30);

////////////////////////////////////////////////////////


////////////////////////////////////////////////////////
        SpinnerModel delete_TrendY =
                new SpinnerNumberModel(deleteTrendY, // для задания координаты Y при удалении тренда
                        0, //minimum value
                        100, //maximum value
                        1); //step
        JSpinner spinner_deleteTrendY = new JSpinner(delete_TrendY);
        // spinner.setName("длина волны (А)");
        // String[] label = {"длина волны (А) "};
        spinner_deleteTrendY.setBounds(100,100,50,30);

////////////////////////////////////////////////////////

        // кнопка видеопоток
        JButton buttonVideo = new JButton("Видеопоток");
        buttonVideo.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                //  INPUT_CAMERA_WIDTH = (int) spinnerWaveLength.getValue();
                ob_buttonVideo_clicked();   // при нажатии вызываем функцию обработки видео
            }
        });

        JButton buttonSET = new JButton("Применить установки");
        buttonSET.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                //  INPUT_CAMERA_WIDTH = (int) spinnerWaveLength.getValue();

                deleteTrendX  = (int) spinner_deleteTrendX.getValue();
                deleteTrendY  = (int) spinner_deleteTrendY.getValue();
                loopsUnwrapCount = (int) spinnerloopsUnwrapC.getValue();
                spinnerWaveLength.setValue(Integer.valueOf(waveLength));

                Real_spinnerWeight.setValue(Integer.valueOf(REAL_WIDTH));
                Real_spinnerHeight.setValue(Integer.valueOf(REAL_HEIGHT));

                spinnerWeight.setValue(Integer.valueOf(INPUT_CAMERA_WIDTH));
                spinnerHeight.setValue(Integer.valueOf(INPUT_CAMERA_HEIGHT));

                Slider.setValue((int) (Trash_hold * 100)); //не меняет ползунок

               // waveLength = (int) spinnerWaveLength.getValue();
                REAL_WIDTH = (int) Real_spinnerWeight.getValue();
                REAL_HEIGHT = (int) Real_spinnerHeight.getValue(); //не работает установка размера кадра, ничего не меняется
//
                //  System.out.println("длина волны - " + waveLength);
//                System.out.println("ширина - " + spinnerWeight);
//                System.out.println("высота - " + spinnerHeight);

            }
        });


        JCheckBox checkBoxPhase = new JCheckBox("Фазовая картинка");
        checkBoxPhase.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                phase = !phase;

                System.out.println("фаза - " + phase);
            }
        });

//        // кнопка видеопоток
//        JButton buttonPhase = new JButton("Фазовая картинка");
//        buttonPhase.addActionListener(new AbstractAction() {
//            @Override
//            public void actionPerformed(ActionEvent e)
//            {
//                 // при нажатии выводим фазовую картинку
//                phase = !phase;
//
//                System.out.println("фаза - " + phase);
//            }
//        });



        // кнопка создания фото
        JButton buttonPhoto = new JButton("Снимок");
        buttonPhoto.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                // при нажатии выводим фазовую картинку
                // phase = !phase;
                ob_buttonPhoto_clicked();
                System.out.println("фото готово - " + phase);

            }
        });


        JButton buttonInvert = new JButton("Инвертировать");
        buttonInvert.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                // при нажатии выводим фазовую картинку
                // phase = !phase;
                invert = !invert;

                System.out.println("инвертирование - " + invert);
            }
        });

        JButton buttonSafe = new JButton("Сохранить настройки");  // при нажатии сохраняем параметры из файла
        buttonSafe.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                ob_buttonSafe_settings();
                System.out.println("Сохранение завершена");
            }
        });


        JButton buttonLoading = new JButton("Загрузить настройки");  // при нажатии загружаем параметры из файла
        buttonLoading.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                ob_buttonLoad_settings();
                System.out.println("Загрузка завершена");
            }
        });


        // переключатель видео
        JCheckBox checkBoxVideo = new JCheckBox("Режим видео");
        checkBoxVideo.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                on_checkBoxVideo_clicked();
            }
        });

        JCheckBox checkBoxMedianF = new JCheckBox("Медианный фильтр");
        checkBoxMedianF.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e)
            {

               // spinner_medianF_Count.setValue(medianCount);
                if((medianCount%2)==0)
                {
                    medianF_Count.setValue(medianCount+1); //РАБОТАЕТ С НАТЯЖКОЙ, НУЖНО СМОТРЕТЬ
                }
                medianCount = (int) medianF_Count.getValue();
                medianF = !medianF;
            }
        });

        JCheckBox checkBoxPhotoFromCamera = new JCheckBox("Удаление тренда", true); //нужно сделать чтобы он был активен сразу
        checkBoxPhotoFromCamera.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                trend = !trend;

                System.out.println("Удаление тренда - " + trend);
                //on_checkBoxPhotoFromCamera_clicked();
            }
        });

        JLabel TrashHoldLabel = new JLabel("TrashHold"); // создаем лейбл трешхолда
         JLabel WavelengthLabel = new JLabel("Wavelength"); // создаем лейбл трешхолда

        JLabel spinnerWeightLabel = new JLabel("Weight"); // создаем лейбл ширины камеры
        JLabel spinnerHeightLabel = new JLabel("Height"); // создаем лейбл высоты камеры

        JLabel Real_spinnerWeightLabel = new JLabel("ширина (мкм) "); // создаем лейбл реальной ширины камеры
        JLabel Real_spinnerHeightLabel = new JLabel("высота (мкм) "); // создаем лейбл реальной высоты камеры

        JLabel medianFLabel = new JLabel("Радиус фильтра"); // создаем лейбл Радиус медианного фильтра
        JLabel spinnerUnwrapCLabel = new JLabel("цикл развертки"); // создаем лейбл

        JLabel deleteTrendXLabel = new JLabel("X");
        JLabel deleteTrendYLabel = new JLabel("Y");
        // layout - чтобы элементы не расползались

        FlowLayout FlowLayout = new FlowLayout();
        Container mainWindow = frame.getContentPane();      // получаем область контента
//        получаем область контента
        mainWindow.setLayout(FlowLayout );                 // задаем layout
        //  mainWindow.add(buttonImage, BorderLayout.BEFORE_LINE_BEGINS);   // добавляем кнопку обработки изображения
        mainWindow.add(buttonVideo, BorderLayout.AFTER_LINE_ENDS);      // добавляем кнопку обработки видеопотока
        mainWindow.add(imageLabel, BorderLayout.CENTER);    // задаем расположение по центру
        mainWindow.add(checkBoxVideo, BorderLayout.AFTER_LAST_LINE);      // добавляем переключатель режима видео
        mainWindow.add(checkBoxPhotoFromCamera, BorderLayout.BEFORE_FIRST_LINE);    // добавляем переключатель выбора изображения без/с удалением тренда

        mainWindow.add(spinner_deleteTrendX);//коорд. нач. точки
        mainWindow.add(deleteTrendXLabel);

        mainWindow.add(spinner_deleteTrendY);//коорд.нач. точки
        mainWindow.add(deleteTrendYLabel);

        mainWindow.add(checkBoxPhase); //переключение режима показа (фазовая картинка или развернутая фазовая картинка)
        mainWindow.add(buttonPhoto, BorderLayout.BEFORE_FIRST_LINE);  // кнопка снимока с камеры
        mainWindow.add(buttonInvert, BorderLayout.BEFORE_FIRST_LINE);  // кнопка инвертирования
        // mainWindow.add(editComboBox, BorderLayout.BEFORE_FIRST_LINE);  // создаем комбо бокс со списком камер
        mainWindow.add(Slider);  // создаем слайдер трешхолда
        mainWindow.add(TrashHoldLabel);// добавляем Label трешхолда
        mainWindow.add(spinnerWaveLength);// СПИНЕР ДЛЯ ИЗМЕНЕНИЯ ДЛИНЫ ВОЛНЫ
        mainWindow.add(WavelengthLabel);// добавляем Label трешхолда

        mainWindow.add(spinnerWeight);
        mainWindow.add(spinnerWeightLabel); // лейблы и спиннеры для установки разрешения камеры
        mainWindow.add(spinnerHeight);
        mainWindow.add(spinnerHeightLabel);

        mainWindow.add(checkBoxMedianF);//добавляем переключаетль медианного фильтра

        mainWindow.add(spinner_medianF_Count);//добавляем переключаетль радиуса медианного фильтра
        mainWindow.add(medianFLabel);//лейбл фильтра

        mainWindow.add(spinnerloopsUnwrapC);
        mainWindow.add(spinnerUnwrapCLabel);//лейбл кол-ва циклов развертки
        mainWindow.add(buttonSET); //примерить установки разрешения камеры + длины волны
        mainWindow.add(buttonSafe);  //добавляем кнопку сохранения настроек
        mainWindow.add(buttonLoading); //добавляем кнопку загрузки настроек


        mainWindow.add(Real_spinnerWeight);
        mainWindow.add(Real_spinnerWeightLabel); // лейблы и спиннеры реального размера кадра
        mainWindow.add(Real_spinnerHeight);
        mainWindow.add(Real_spinnerHeightLabel);


        //  mainWindow.add(spinnerWeight);
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

    public void on_buttonImage_clicked() //КОСТЫЛЬ, не особо нужный
    {
        init();     // сбрасываем кадр, чтобы был белый фон
        render.draw(image);     // рисуем пустое изображение

        BufferedImage bufferedImage = null;
        PhaseMicroscopeTools tools = new PhaseMicroscopeTools();

        if(isCapturePhotoFromCamera)    // если считывать изображение нужно с камеры
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
            bufferedImage = openCV.convertMatrixToBufferedImage(matrix);    // конвертируем в изображение

//             обрабатываем изображение
            if (phase==false) {
                tools.processImage(bufferedImage, invert);
            }
            if (phase==true) {
                tools.processImageF(bufferedImage);
            }


            try
            {
                double[][] massive = tools.getImageMassive(bufferedImage);
                System.out.println("Остановка видеопотока");
                isVideoRunning = false;
                tools.writeMassiveToFile(massive, "src/main/resources/photoFromCamera_out.txt");    // записываем текстовый файл
                tools.normalizeTo(massive, 0, 255);
                ImageIO.write(bufferedImage, "jpg", new File("src/main/resources/photoFromCamera.jpg"));    // записываем изображение
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        else    // если из файла
        {
            String inputFileName = "src/main/resources/Snap";
            String inputFileNFormat = ".bmp";
            String imagePath = inputFileName + inputFileNFormat;

            // работаем с изображением
            // загружаем изображение
            bufferedImage = tools.loadImage(imagePath);   // загружаем изображение

            // обрабатываем изображение
            tools.processImage(bufferedImage, invert);

            try
            {
                double[][] massive = tools.getImageMassive(bufferedImage);
                tools.writeMassiveToFile(massive, inputFileName + "_out" + inputFileNFormat);    // записываем текстовый файл
                ImageIO.write(bufferedImage, "jpg", new File(inputFileName + "_out" + inputFileNFormat));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
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


    public boolean oneCameraShot = false;   // необходимость сделать стнимок
    public void ob_buttonPhoto_clicked()
    {
        oneCameraShot = true;
//        init();     // сбрасываем кадр, чтобы был белый фон
//        render.draw(image);     // рисуем пустое изображение
//
//        BufferedImage bufferedImage = null;
//        PhaseMicroscopeTools tools = new PhaseMicroscopeTools();
//        OpenCV openCV = new OpenCV();
//        try
//        {
//            openCV.setVideoCaptureIndex(0);   // задаем номер камеры = 0
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//        }
//
//
//        Mat matrix = openCV.captureFrame();   // делаем снимок с камеры
//        bufferedImage = openCV.convertMatrixToBufferedImage(matrix);    // конвертируем в изображение
//
////             обрабатываем изображение
//        if (phase==false) {
//            tools.processImageANGSTR(bufferedImage);
//        }
//        if (phase==true) {
//            tools.processImageF_ANGSTR(bufferedImage); //нужно сделать тоже самое для простой фотки
//        }
//        if (trend==true) {
//            tools.processImageTrend(bufferedImage);
//        }
//        try
//        {
//            double[][] massive = tools.getImageMassive(bufferedImage);
//            System.out.println("Остановка видеопотока");
//            isVideoRunning = false;
//
//            fileChooser.setDialogTitle("Specify a file to save");
//            fileChooser.setName("decryptImage");
//            fileChooser.setFileFilter(new FileFilter() {
//                @Override
//                public boolean accept(File file) {
//                    if (file.getName().endsWith(".jpg")) {
//                        return true;
//                    }
//                    return false;
//                }
//
//                @Override
//                public String getDescription() {
//                    return ".jpg";
//                }
//            });
//             int userSelection = fileChooser.showSaveDialog(parentFrame);
//
//            if (userSelection == JFileChooser.APPROVE_OPTION) {
//                ImageIO.write(bufferedImage, "jpg",
//                        fileChooser.getSelectedFile().getName().endsWith(".jpg")
//                        ? fileChooser.getSelectedFile()
//                        : new File(fileChooser.getSelectedFile() + ".jpg"));
//                tools.convertToAngstroms(massive, waveLength);
//
//                File textFile = fileChooser.getSelectedFile().getName().endsWith(".jpg")
//                        ? fileChooser.getSelectedFile()
//                        : new File(fileChooser.getSelectedFile() + ".jpg");
//
//                String textFileName = textFile.getAbsolutePath().replace("jpg", "txt");
//                tools.writeMassiveToFile(massive, textFileName);
//
//                //   tools.writeMassiveToFile(massive, String.valueOf(fileChooser.getSelectedFile()));
//                //    File fileToSave = fileChooser.getSelectedFile();
//                //  System.out.println("Save as file: " + fileToSave.getAbsolutePath());
//            }
//            //  tools.writeMassiveToFile(massive, "src/main/resources/photoFromCamera_out.txt");    // записываем текстовый файл
////            ImageIO.write(bufferedImage, "jpg", new File("src/main/resources/photoFromCamera.jpg"));    // записываем изображение ??
//        }
//        catch (IOException e) //?
//        {
//            e.printStackTrace();
//        }
    }

    public void ob_buttonSafe_settings()
    {
        JFileChooser fileChooser1 = new JFileChooser();
        fileChooser1.setDialogTitle("Specify a file to save");
        JFrame parentFrame1 = new JFrame();
             int userSelection1 = fileChooser1.showSaveDialog(parentFrame1);

            if (userSelection1 == JFileChooser.APPROVE_OPTION) {

                File textFile = fileChooser1.getSelectedFile().getName().endsWith(".txt")
                        ? fileChooser1.getSelectedFile()
                        : new File(fileChooser1.getSelectedFile() + ".txt");

                String textFileName = textFile.getAbsolutePath().replace("txt", "txt");

                try(FileWriter writer = new FileWriter(textFileName, false))
                {
                    // запись всей строки
                    String text = String.valueOf(waveLength);
                    writer.write(text);
                    writer.append(' ');
                 //  Trash_hold = Trash_hold * 100;
                    Trash_hold_int = (int) (Trash_hold * 100);
                    String text1 = String.valueOf(Trash_hold_int);
                    writer.write(text1);
                    writer.append(' ');

                    String text2 = String.valueOf(INPUT_CAMERA_WIDTH);
                    writer.write(text2);
                    writer.append(' ');

                    String text3 = String.valueOf(INPUT_CAMERA_HEIGHT);
                    writer.write(text3);
                    writer.append(' ');

                    String text4 = String.valueOf(REAL_WIDTH);
                    writer.write(text4);
                    writer.append(' ');

                    String text5 = String.valueOf(REAL_HEIGHT);
                    writer.write(text5);
                    writer.append(' ');

                }
                catch(IOException ex){

                    System.out.println(ex.getMessage());
                }
                System.out.println("Файл записан");
                //   tools.writeMassiveToFile(massive, String.valueOf(fileChooser.getSelectedFile()));
                //    File fileToSave = fileChooser.getSelectedFile();
                //  System.out.println("Save as file: " + fileToSave.getAbsolutePath());
            }
            //  tools.writeMassiveToFile(massive, "src/main/resources/photoFromCamera_out.txt");    // записываем текстовый файл
//            ImageIO.write(bufferedImage, "jpg", new File("src/main/resources/photoFromCamera.jpg"));    // записываем изображение ??
        }

    public void ob_buttonLoad_settings() {
        JFileChooser fileChooser1 = new JFileChooser();
        fileChooser1.setDialogTitle("Specify a file to save");
        JFrame parentFrame1 = new JFrame();
        int userSelection1 = fileChooser1.showOpenDialog(parentFrame1);

        if (userSelection1 == JFileChooser.APPROVE_OPTION) {

            File textFile = new File(fileChooser1.getSelectedFile().toURI());
            //String textFileName = textFile.getAbsolutePath();

            try (Scanner scanner = new Scanner(textFile)) {

                String line = scanner.nextLine();
                String[] numbers = line.split(" ");
                int[] numbers_int = new int[6];
                int counter = 0;
                for (String number : numbers) {
                    numbers_int[counter++] = Integer.parseInt(number);

                   // numbers_int[counter++] = Double.parseDouble(number);

                    System.out.println(Arrays.toString(numbers));

                }
                waveLength  = numbers_int[0];


                Trash_hold =  numbers_int[1];
                Trash_hold = Trash_hold/100;
                INPUT_CAMERA_WIDTH = numbers_int[2];
                INPUT_CAMERA_HEIGHT = numbers_int[3];
                REAL_WIDTH = numbers_int[4];
                REAL_HEIGHT=numbers_int[5];

                //  System.out.println(fileContent);


                System.out.println("waveLength " + waveLength);
                System.out.println("Trash_hold " + Trash_hold);
                System.out.println("INPUT_CAMERA_WIDTH " + INPUT_CAMERA_WIDTH);
                System.out.println("INPUT_CAMERA_HEIGHT " + INPUT_CAMERA_HEIGHT);
                System.out.println("REAL_WIDTH " + REAL_WIDTH);
                System.out.println("REAL_HEIGHT " + REAL_HEIGHT);

            } catch (IOException ex) {

                System.out.println(ex.getMessage());
            }
        }
    }

    public boolean showVideoOriginal = false;

    public void on_checkBoxVideo_clicked()
    {
        showVideoOriginal = !showVideoOriginal;
    }

    public boolean isCapturePhotoFromCamera = false;    // переключатель выбора изображения из файла или с камеры
    public void on_checkBoxPhotoFromCamera_clicked()
    {
        isCapturePhotoFromCamera = !isCapturePhotoFromCamera;

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
