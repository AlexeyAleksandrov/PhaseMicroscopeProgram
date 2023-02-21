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

        Render render = mainWindow.getRender();   // рендер


//
//        render.draw(image);  // рисуем
    }
}
