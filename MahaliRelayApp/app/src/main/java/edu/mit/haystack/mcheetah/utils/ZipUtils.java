package edu.mit.haystack.mcheetah.utils;
/* The MIT License (MIT)
 * Copyright (c) 2015 Massachusetts Institute of Technology
 *
 * Author: David Mascharka
 * This software is part of the Mahali Project, PI: V. Pankratius
 * http://mahali.mit.edu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author David Mascharka
 *
 * Helper class for handling zip files
 */
public class ZipUtils {
    /**
     * Unzips the given zip from the given path into the given path
     *
     * @param path the path of the zip file and to unzip to
     * @param zipeFileName the name of the zip file
     * @return whether the unzip completed successfully
     */
    public static boolean unzip(String path, String zipeFileName) {
        InputStream inputStream;
        ZipInputStream zipInputStream;

        try {
            inputStream = new FileInputStream(path + zipeFileName);
            zipInputStream = new ZipInputStream(new BufferedInputStream(inputStream));

            ZipEntry zipEntry;
            byte[] buffer = new byte[1024];
            int count;

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                String fileName = zipEntry.getName();

                if (zipEntry.isDirectory()) {
                    File dir = new File(path + fileName);
                    dir.mkdirs();
                    continue;
                }

                FileOutputStream outputStream = new FileOutputStream(path + fileName);

                while ((count = zipInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, count);
                }

                outputStream.close();
                zipInputStream.closeEntry();
            }

            zipInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}