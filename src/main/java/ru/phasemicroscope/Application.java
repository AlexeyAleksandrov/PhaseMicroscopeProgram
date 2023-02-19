package ru.phasemicroscope;

import ru.phasemicroscope.window.MainWindow;
import ru.phasemicroscope.window.Render;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

public class Application
{
    public static void main(String[] args) throws Exception
    {
        MainWindow mainWindow = new MainWindow();   // главное окно
        mainWindow.show();  // показываем окно

        BufferedImage image = mainWindow.getImage();    // главное изображение
        Render render = new Render(image, mainWindow.getFrame());   // рендер

        image = ImageIO.read(new File("src/main/resources/lines2.jpg"));

        int width = image.getWidth();
        int height = image.getHeight();

        double[] avgGrayValues = new double[width];

        for (int x = 0; x < width; x++)
        {
            for (int y = 0; y < height; y++)
            {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb & 0xFF);

                // Normalize and gamma correct:
                float rr = (float) Math.pow(r / 255.0, 2.2);
                float gg = (float) Math.pow(g / 255.0, 2.2);
                float bb = (float) Math.pow(b / 255.0, 2.2);

                // Calculate luminance:
                float lum = (float) (0.2126 * rr + 0.7152 * gg + 0.0722 * bb);

                // Gamma compand and rescale to byte range:
                int grayLevel = (int) (255.0 * Math.pow(lum, 1.0 / 2.2));
                int gray = (grayLevel << 16) + (grayLevel << 8) + grayLevel;
                image.setRGB(x, y, gray);

                avgGrayValues[x] += grayLevel;   // суммируем значения для этой позиции х
            }

            avgGrayValues[x] /= height;  // считаем среднее арифметическое по этой вертикали
        }

        double min = Arrays.stream(avgGrayValues).min().orElse(0);    // считаем минимальное значение
        double max = Arrays.stream(avgGrayValues).max().orElse(255);  // ищем максимальное значение

        double minMaxAvg = (min + max) / 2.0;   // считаем среднее между минимальным и максимальным значением

        // считаем кол-во переходов графика через среднее значение - ищем кол-во минимумов/максимумов
        ArrayList<Integer> changePoints = new ArrayList<>();    // список Х точек, в которых происходит переход между минимумами и максимумами

        for (int i = 0; i < avgGrayValues.length-1; i++)
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

            double foundValue = 0;  // найденное значение минимума или максимума

            if (maxGray > minMaxAvg)    // если найденный максимум больше, чем среднее значение
            {
                foundValue = maxGray;
            }
            else    // если найденный минимум меньше, чем среднее значение
            {
                foundValue = minGray;
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
                maximumsCoordinates.add(position);
            }
            else    // если найденный минимум меньше, чем среднее значение
            {
                minimumsCoordinates.add(position);
            }

            x1 = x2;    // приравниваем следующее значение X к текущему, для следующей итерации
        }

        // рисуем минимумы и максимумы
        Graphics2D graphics = image.createGraphics();

        // рисуем минимумы
        graphics.setColor(Color.red);
        for (int x : minimumsCoordinates)
        {
            int y1 = (int) ((int) ((double) height / 2) + ((double) height * 0.05));
            int y2 = (int) ((int) ((double) height / 2) - ((double) height * 0.05));
            graphics.drawLine(x, y1, x, y2);
        }

        // рисуем максимумы
        graphics.setColor(Color.green);
        for (int x : maximumsCoordinates)
        {
            int y1 = (int) ((int) ((double) height / 2) + ((double) height * 0.05));
            int y2 = (int) ((int) ((double) height / 2) - ((double) height * 0.05));
            graphics.drawLine(x, y1, x, y2);
        }

        render.draw(image);  // рисуем
    }
}
