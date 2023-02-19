package ru.phasemicroscope;

import ru.phasemicroscope.window.MainWindow;
import ru.phasemicroscope.window.Render;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class Application
{
    public static void main(String[] args) throws Exception
    {
        MainWindow mainWindow = new MainWindow();   // главное окно
        mainWindow.show();  // показываем окно

        BufferedImage image = mainWindow.getImage();    // главное изображение
        Render render = new Render(image, mainWindow.getFrame());   // рендер

        image = ImageIO.read(new File("src/main/resources/lines2.jpg"));

        int width = image.getWidth();       // ширина изображения
        int height = image.getHeight();     // высота изображения

        double[] avgGrayValues = new double[width];     // массив среднего арифметического значения в градациях серого по каждой вертикале

        for (int x = 0; x < width; x++)     // проходим по каждому столбцу
        {
            for (int y = 0; y < height; y++)    // проходим по каждому значению в столбце
            {
                int rgb = image.getRGB(x, y);   // получаем RGB значения пикселя

                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb & 0xFF);

                // нормализация и гамма-коррекция:
                float rr = (float) Math.pow(r / 255.0, 2.2);
                float gg = (float) Math.pow(g / 255.0, 2.2);
                float bb = (float) Math.pow(b / 255.0, 2.2);

                // рассчитываем яркость:
                float lum = (float) (0.2126 * rr + 0.7152 * gg + 0.0722 * bb);

                // гамма-сложение и масштабирование до диапазона байтов:
                int grayLevel = (int) (255.0 * Math.pow(lum, 1.0 / 2.2));
                int gray = (grayLevel << 16) + (grayLevel << 8) + grayLevel;
                image.setRGB(x, y, gray);   // перерисовываем пиксель в градации серого

                avgGrayValues[x] += grayLevel;   // суммируем значения для этой позиции х
            }

            avgGrayValues[x] /= height;  // считаем среднее арифметическое по этой вертикали
        }

        // ищем границы между минимумами и максимумами
        double min = Arrays.stream(avgGrayValues).min().orElse(0);    // считаем минимальное значение
        double max = Arrays.stream(avgGrayValues).max().orElse(255);  // ищем максимальное значение

        double minMaxAvg = (min + max) / 2.0;   // считаем среднее между минимальным и максимальным значением

        // считаем кол-во переходов графика через среднее значение - ищем кол-во минимумов/максимумов
        ArrayList<Integer> changePoints = new ArrayList<>();    // список Х точек, в которых происходит переход между минимумами и максимумами

        for (int i = 0; i < avgGrayValues.length-1; i++)    // проходим по каждому среднему значению
        {
            if((avgGrayValues[i] > minMaxAvg && avgGrayValues[i+1] <= minMaxAvg)
                    || (avgGrayValues[i] < minMaxAvg && avgGrayValues[i+1] > minMaxAvg))    // если одно значение больше среднего, а другое меньше, значит мы на границе
            {
                changePoints.add(i);    // добавляем текущий Х в список
            }
        }

        // формируем участки, на которых будем искать точки максимума и минимума
        ArrayList<Integer> minimumsCoordinates = new ArrayList<>();     // список минимумов
        ArrayList<Integer> maximumsCoordinates = new ArrayList<>();     // список минимумов

        int x1 = 0;     // для нулевого участка
        for (int x2 : changePoints)     // перебираем все координаты
        {
            double[] subarray = Arrays.copyOfRange(avgGrayValues, x1 + 1, x2);      // берём участок, на котором ищем минимум или максимум
            double maxGray = Arrays.stream(subarray).max().orElse(minMaxAvg);   // находим максимум
            double minGray = Arrays.stream(subarray).min().orElse(minMaxAvg);   // находим минимум

            double foundValue;  // найденное значение минимума или максимума

            if (maxGray > minMaxAvg)    // если найденный максимум больше, чем среднее значение
            {
                foundValue = maxGray;   // сохраняем максимальное значение
            }
            else    // если найденный минимум меньше, чем среднее значение
            {
                foundValue = minGray;   // сохраняем минимальное значение
            }

            // ищем позицию, на которой находится найденное значение
            int position = x1;  // позиция Х, на которой находится найденный минимум или максимум
            for (int i = x1+1; i < x2; i++)   // берём участок на котором искали минимум и максимум
            {
                if(avgGrayValues[i] == foundValue)  // если значение совпадает с искомым
                {
                    position = i;   // записываем координату
                    break;
                }
            }

            // добавляем найденную координату в список
            if (maxGray > minMaxAvg)    // если найденный максимум больше, чем среднее значение
            {
                maximumsCoordinates.add(position);      // добавляем позицию, на которой находится максимум
            }
            else    // если найденный минимум меньше, чем среднее значение
            {
                minimumsCoordinates.add(position);      // добавляем позицию, на которой находится минимум
            }

            x1 = x2;    // приравниваем следующее значение X к текущему, для следующей итерации
        }

        // рисуем минимумы и максимумы
        Graphics2D graphics = image.createGraphics();

        // рисуем минимумы
        graphics.setColor(Color.red);   // цвет рисования
        for (int x : minimumsCoordinates)
        {
            int y1 = (int) ((int) ((double) height / 2) + ((double) height * 0.05));    // вертикальный центр +5% вверх
            int y2 = (int) ((int) ((double) height / 2) - ((double) height * 0.05));    // вертикальный центр -5% вниз
            graphics.drawLine(x, y1, x, y2);    // рисуем линию
        }

        // рисуем максимумы
        graphics.setColor(Color.green);     // цвет рисования
        for (int x : maximumsCoordinates)
        {
            int y1 = (int) ((int) ((double) height / 2) + ((double) height * 0.05));    // вертикальный центр +5% вверх
            int y2 = (int) ((int) ((double) height / 2) - ((double) height * 0.05));    // вертикальный центр -5% вниз
            graphics.drawLine(x, y1, x, y2);    // рисуем линию
        }

        render.draw(image);  // рисуем
    }
}
