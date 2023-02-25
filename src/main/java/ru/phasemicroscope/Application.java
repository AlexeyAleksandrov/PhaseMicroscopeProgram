package ru.phasemicroscope;

import ru.phasemicroscope.window.MainWindow;

public class Application
{
    public static void main(String[] args)
    {
        MainWindow mainWindow = new MainWindow();   // главное окно
        Thread th = new Thread(mainWindow);     // создаем поток для главного окна
        th.start();     // запускаем поток и показываем окно
    }
}
