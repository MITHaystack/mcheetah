package edu.mit.haystack.example.mcheetah;

import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

/**
 * @author David Mascharka
 *
 * Sets up this example by creating 3 data files
 */
public class Setup {
    public static void createDataFiles() {
        (new Thread(new Runnable() {
            @Override
            public void run() {
                File dataFile1 = new File("/sdcard/mcheetah_example_1.txt");
                File dataFile2 = new File("/sdcard/mcheetah_example_2.txt");
                File dataFile3 = new File("/sdcard/mcheetah_example_3.txt");

                try {
                    FileOutputStream outputStream = new FileOutputStream(dataFile1, true);
                    PrintWriter writer = new PrintWriter(outputStream);

                    for (int i = 0; i < 100; i++) {
                        writer.write(i + "\t" + (i % 10) + "\n");
                    }
                    writer.flush();
                    writer.close();

                    outputStream = new FileOutputStream(dataFile2, true);
                    writer = new PrintWriter(outputStream);
                    for (int i = 100; i < 200; i++) {
                        writer.write(i + "\t" + (i%10) + "\n");
                    }
                    writer.flush();
                    writer.close();

                    outputStream = new FileOutputStream(dataFile3, true);
                    writer = new PrintWriter(outputStream);
                    for (int i = 200; i < 300; i++) {
                        writer.write(i + "\t" + (i%10) + "\n");
                    }
                    writer.flush();
                    writer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        })).start();
    }
}
