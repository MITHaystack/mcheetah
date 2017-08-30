package edu.mit.haystack.mcheetah.visualization;

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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import edu.mit.haystack.mcheetah.R;

/**
 * @author David Mascharka
 *
 * Class for handling drawing text on the screen using OpenGL
 * Text is a bitmap font loaded as a texture atlas
 *
 * Most of this class is deriving bounds of characters, so there isn't much interesting logic here
 */
public class Glyphs {

    private FloatBuffer vertexBuffer;

    /**
     * Drawing a title - bigger font
     */
    public static final int TITLE_MODE = 0;

    /**
     * Drawing normal text - smaller font
     */
    public static final int SUBTEXT_MODE = 1;

    private float vertices[] = {
            -1.0f, -1.0f, 0.0f,
            -1.0f, 1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            1.0f, 1.0f, 0.0f
    };
    private int[] textures;

    private FloatBuffer textureBuffer;
    private float texture[] = {
            0.0f, 1.0f, // top left
            0.0f, 0.0f, // bottom left
            1.0f, 1.0f, // top right
            1.0f, 0.0f // bottom right
    };

    private float spacing; // spacing between characters

    public Glyphs() {
        vertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.rewind();

        textureBuffer = ByteBuffer.allocateDirect(texture.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        textureBuffer.put(texture);
        textureBuffer.rewind();

        textures = new int[1];
    }

    /**
     * Draws text on the screen without rotation
     *
     * @param gl a GL10 instance
     * @param text the text to draw
     * @param x the x position to start drawing
     * @param y the y position to start drawing
     * @param mode the mode to draw in (title or subtext)
     */
    public void drawText(GL10 gl, String text, float x, float y, int mode) {
        drawText(gl, text, x, y, mode, 0);
    }

    /**
     * Draws text on the screen rotated {@param rotation} degrees
     * @param gl a GL10 instance
     * @param text the text to draw
     * @param x the x position to start drawing
     * @param y the y position to start drawing
     * @param mode the mode to draw in (title or subtext)
     * @param rotation the rotation amount, in degrees
     */
    public void drawText(GL10 gl, String text, float x, float y, int mode, float rotation) {
        gl.glEnable(GL10.GL_TEXTURE_2D);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);

        spacing = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i); // get the character to draw

            // We need to do some matrix transformations to draw and don't want to change the state in
            // the renderer calling this function
            gl.glPushMatrix();
            gl.glLoadIdentity();
            gl.glRotatef(rotation, 0, 0, 1); // 2d rotation, rotate around the z axis
            gl.glTranslatef(x+spacing, y, 0); // go to the beginning of the text offset by spacing

            // Scale to make text larger if we're drawing a title
            if (mode == TITLE_MODE) {
                gl.glScalef(32, 32, 1);
            } else if (mode == SUBTEXT_MODE) {
                gl.glScalef(16, 16, 1);
            }

            texture = getLetterBounds(c);
            textureBuffer.put(texture);
            textureBuffer.rewind();

            gl.glFrontFace(GL10.GL_CW);
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
            gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer);
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, vertices.length / 3);
            gl.glPopMatrix(); // restore the matrix

            // Get the spacing to account for the width of the character we just drew and kerning
            if (mode == TITLE_MODE) {
                spacing += getTitleSpace(c);
            } else if (mode == SUBTEXT_MODE) {
                spacing += getSubtextSpace(c);
            }
        }

        gl.glDisable(GL10.GL_BLEND);
        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisable(GL10.GL_TEXTURE_2D);
    }

    /**
     * Gets the length of the string in the given mode
     *
     * Useful for centering text horizontally
     *
     * @param text the text to query
     * @param mode the drawing mode (title or subtext)
     * @return the pixel length of the string
     */
    public int getStringLength(String text, int mode) {
        switch (mode) {
            case TITLE_MODE:
                return getTitleStringLength(text);
            case SUBTEXT_MODE:
                return getSubtextStringLength(text);
            default:
                return 0;
        }
    }

    // This is slightly off since it accounts for the spacing for kerning after the last character
    private int getTitleStringLength(String text) {
        int length = 0;
        for (char c : text.toCharArray()) {
            length += getTitleSpace(c);
        }
        return length;
    }

    // Again, slightly off because it accounts for kerning at the end of the string
    private int getSubtextStringLength(String text) {
        int length = 0;
        for (char c : text.toCharArray()) {
            length += getSubtextSpace(c);
        }
        return length;
    }

    /**
     * Gets the height of each character in the given mode
     *
     * Useful for centering text vertically
     * All characters are uppercase and equal sizes, which makes this easy
     *
     * @param mode the mode to draw in (title or subtext)
     * @return the height of each character
     */
    public int getHeight(int mode) {
        switch (mode) {
            case TITLE_MODE:
                return 40;
            case SUBTEXT_MODE:
                return 20;
            default:
                return 0;
        }
    }

    public void loadGlyphs(GL10 gl, Context context) {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.text_atlas);
        gl.glGenTextures(1, textures, 0);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);

        bitmap.recycle();
    }

    private int getTitleSpace(char c) {
        int space = 0;

        switch (c) {
            case 'A':
                space = 38;
                break;
            case 'B':
                space = 31;
                break;
            case 'C':
                space = 32;
                break;
            case 'D':
                space = 35;
                break;
            case 'E':
                space = 26;
                break;
            case 'F':
                space = 25;
                break;
            case 'G':
                space = 36;
                break;
            case 'H':
                space = 34;
                break;
            case 'I':
                space = 14;
                break;
            case 'J':
                space = 19;
                break;
            case 'K':
                space = 30;
                break;
            case 'L':
                space = 24;
                break;
            case 'M':
                space = 48;
                break;
            case 'N':
                space = 36;
                break;
            case 'O':
                space = 40;
                break;
            case 'P':
                space = 29;
                break;
            case 'Q':
                space = 43;
                break;
            case 'R':
                space = 32;
                break;
            case 'S':
                space = 29;
                break;
            case 'T':
                space = 33;
                break;
            case 'U':
                space = 34;
                break;
            case 'V':
                space = 37;
                break;
            case 'W':
                space = 55;
                break;
            case 'X':
                space = 34;
                break;
            case 'Y':
                space = 33;
                break;
            case 'Z':
                space = 30;
                break;
            case '0':
                space = 31;
                break;
            case '1':
                space = 27;
                break;
            case '2':
                space = 29;
                break;
            case '3':
                space = 29;
                break;
            case '4':
                space = 31;
                break;
            case '5':
                space = 28;
                break;
            case '6':
                space = 30;
                break;
            case '7':
                space = 29;
                break;
            case '8':
                space = 30;
                break;
            case '9':
                space = 29;
                break;
            case '!':
                space = 11;
                break;
            case '?':
                space = 26;
                break;
            case '+':
                space = 31;
                break;
            case '-':
                space = 19;
                break;
            case '=':
                space = 29;
                break;
            case ':':
                space = 12;
                break;
            case '.':
                space = 12;
                break;
            case ',':
                space = 15;
                break;
            case ' ':
                space = 25;
        }

        return space;
    }

    private int getSubtextSpace(char c) {
        int space = 0;
        switch (c) {
            case 'A':
                space = 20;
                break;
            case 'B':
                space = 17;
                break;
            case 'C':
                space = 17;
                break;
            case 'D':
                space = 18;
                break;
            case 'E':
                space = 14;
                break;
            case 'F':
                space = 13;
                break;
            case 'G':
                space = 19;
                break;
            case 'H':
                space = 17;
                break;
            case 'I':
                space = 7;
                break;
            case 'J':
                space = 10;
                break;
            case 'K':
                space = 16;
                break;
            case 'L':
                space = 13;
                break;
            case 'M':
                space = 25;
                break;
            case 'N':
                space = 18;
                break;
            case 'O':
                space = 21;
                break;
            case 'P':
                space = 15;
                break;
            case 'Q':
                space = 23;
                break;
            case 'R':
                space = 17;
                break;
            case 'S':
                space = 15;
                break;
            case 'T':
                space = 18;
                break;
            case 'U':
                space = 18;
                break;
            case 'V':
                space = 20;
                break;
            case 'W':
                space = 29;
                break;
            case 'X':
                space = 18;
                break;
            case 'Y':
                space = 18;
                break;
            case 'Z':
                space = 15;
                break;
            case '0':
                space = 17;
                break;
            case '1':
                space = 14;
                break;
            case '2':
                space = 16;
                break;
            case '3':
                space = 15;
                break;
            case '4':
                space = 16;
                break;
            case '5':
                space = 15;
                break;
            case '6':
                space = 16;
                break;
            case '7':
                space = 16;
                break;
            case '8':
                space = 16;
                break;
            case '9':
                space = 15;
                break;
            case '!':
                space = 7;
                break;
            case '?':
                space = 13;
                break;
            case '+':
                space = 15;
                break;
            case '-':
                space = 9;
                break;
            case '=':
                space = 15;
                break;
            case ':':
                space = 7;
                break;
            case '.':
                space = 7;
                break;
            case ',':
                space = 8;
                break;
            case ' ':
                space = 12;
                break;
        }
        return space;
    }

    private float[] getLetterBounds(char c) {
        float[] f = new float[8];
        switch (c) {
            case 'A':
                f[0] = 0;
                f[1] = 1.0f/8.0f;
                f[2] = 0;
                f[3] = 0;
                f[4] = 1.0f/8.0f;
                f[5] = 1.0f/8.0f;
                f[6] = 1.0f/8.0f;
                f[7] = 0;
                break;
            case 'B':
                f[0] = 1.0f/8.0f;
                f[1] = 1.0f/8.0f;
                f[2] = 1.0f/8.0f;
                f[3] = 0;
                f[4] = 2.0f/8.0f;
                f[5] = 1.0f/8.0f;
                f[6] = 2.0f/8.0f;
                f[7] = 0;
                break;
            case 'C':
                f[0] = 2.0f/8.0f;
                f[1] = 1.0f/8.0f;
                f[2] = 2.0f/8.0f;
                f[3] = 0;
                f[4] = 3.0f/8.0f;
                f[5] = 1.0f/8.0f;
                f[6] = 3.0f/8.0f;
                f[7] = 0;
                break;
            case 'D':
                f[0] = 3.0f/8.0f;
                f[1] = 1.0f/8.0f;
                f[2] = 3.0f/8.0f;
                f[3] = 0;
                f[4] = 4.0f/8.0f;
                f[5] = 1.0f/8.0f;
                f[6] = 4.0f/8.0f;
                f[7] = 0;
                break;
            case 'E':
                f[0] = 4.0f/8.0f;
                f[1] = 1.0f/8.0f;
                f[2] = 4.0f/8.0f;
                f[3] = 0;
                f[4] = 5.0f/8.0f;
                f[5] = 1.0f/8.0f;
                f[6] = 5.0f/8.0f;
                f[7] = 0;
                break;
            case 'F':
                f[0] = 5.0f/8.0f;
                f[1] = 1.0f/8.0f;
                f[2] = 5.0f/8.0f;
                f[3] = 0;
                f[4] = 6.0f/8.0f;
                f[5] = 1.0f/8.0f;
                f[6] = 6.0f/8.0f;
                f[7] = 0;
                break;
            case 'G':
                f[0] = 6.0f/8.0f;
                f[1] = 1.0f/8.0f;
                f[2] = 6.0f/8.0f;
                f[3] = 0;
                f[4] = 7.0f/8.0f;
                f[5] = 1.0f/8.0f;
                f[6] = 7.0f/8.0f;
                f[7] = 0;
                break;
            case 'H':
                f[0] = 7.0f/8.0f;
                f[1] = 1.0f/8.0f;
                f[2] = 7.0f/8.0f;
                f[3] = 0;
                f[4] = 8.0f/8.0f;
                f[5] = 1.0f/8.0f;
                f[6] = 8.0f/8.0f;
                f[7] = 0;
                break;
            case 'I':
                f[0] = 0.0f/8.0f;
                f[1] = 2.0f/8.0f;
                f[2] = 0.0f/8.0f;
                f[3] = 1.0f/8.0f;
                f[4] = 1.0f/8.0f;
                f[5] = 2.0f/8.0f;
                f[6] = 1.0f/8.0f;
                f[7] = 1.0f/8.0f;
                break;
            case 'J':
                f[0] = 1.0f/8.0f;
                f[1] = 2.0f/8.0f;
                f[2] = 1.0f/8.0f;
                f[3] = 1.0f/8.0f;
                f[4] = 2.0f/8.0f;
                f[5] = 2.0f/8.0f;
                f[6] = 2.0f/8.0f;
                f[7] = 1.0f/8.0f;
                break;
            case 'K':
                f[0] = 2.0f/8.0f;
                f[1] = 2.0f/8.0f;
                f[2] = 2.0f/8.0f;
                f[3] = 1.0f/8.0f;
                f[4] = 3.0f/8.0f;
                f[5] = 2.0f/8.0f;
                f[6] = 3.0f/8.0f;
                f[7] = 1.0f/8.0f;
                break;
            case 'L':
                f[0] = 3.0f/8.0f;
                f[1] = 2.0f/8.0f;
                f[2] = 3.0f/8.0f;
                f[3] = 1.0f/8.0f;
                f[4] = 4.0f/8.0f;
                f[5] = 2.0f/8.0f;
                f[6] = 4.0f/8.0f;
                f[7] = 1.0f/8.0f;
                break;
            case 'M':
                f[0] = 4.0f/8.0f;
                f[1] = 2.0f/8.0f;
                f[2] = 4.0f/8.0f;
                f[3] = 1.0f/8.0f;
                f[4] = 5.0f/8.0f;
                f[5] = 2.0f/8.0f;
                f[6] = 5.0f/8.0f;
                f[7] = 1.0f/8.0f;
                break;
            case 'N':
                f[0] = 5.0f/8.0f;
                f[1] = 2.0f/8.0f;
                f[2] = 5.0f/8.0f;
                f[3] = 1.0f/8.0f;
                f[4] = 6.0f/8.0f;
                f[5] = 2.0f/8.0f;
                f[6] = 6.0f/8.0f;
                f[7] = 1.0f/8.0f;
                break;
            case 'O':
                f[0] = 6.0f/8.0f;
                f[1] = 2.0f/8.0f;
                f[2] = 6.0f/8.0f;
                f[3] = 1.0f/8.0f;
                f[4] = 7.0f/8.0f;
                f[5] = 2.0f/8.0f;
                f[6] = 7.0f/8.0f;
                f[7] = 1.0f/8.0f;
                break;
            case 'P':
                f[0] = 7.0f/8.0f;
                f[1] = 2.0f/8.0f;
                f[2] = 7.0f/8.0f;
                f[3] = 1.0f/8.0f;
                f[4] = 8.0f/8.0f;
                f[5] = 2.0f/8.0f;
                f[6] = 8.0f/8.0f;
                f[7] = 1.0f/8.0f;
                break;
            case 'Q':
                f[0] = 0.0f/8.0f;
                f[1] = 3.0f/8.0f;
                f[2] = 0.0f/8.0f;
                f[3] = 2.0f/8.0f;
                f[4] = 1.0f/8.0f;
                f[5] = 3.0f/8.0f;
                f[6] = 1.0f/8.0f;
                f[7] = 2.0f/8.0f;
                break;
            case 'R':
                f[0] = 1.0f/8.0f;
                f[1] = 3.0f/8.0f;
                f[2] = 1.0f/8.0f;
                f[3] = 2.0f/8.0f;
                f[4] = 2.0f/8.0f;
                f[5] = 3.0f/8.0f;
                f[6] = 2.0f/8.0f;
                f[7] = 2.0f/8.0f;
                break;
            case 'S':
                f[0] = 2.0f/8.0f;
                f[1] = 3.0f/8.0f;
                f[2] = 2.0f/8.0f;
                f[3] = 2.0f/8.0f;
                f[4] = 3.0f/8.0f;
                f[5] = 3.0f/8.0f;
                f[6] = 3.0f/8.0f;
                f[7] = 2.0f/8.0f;
                break;
            case 'T':
                f[0] = 3.0f/8.0f;
                f[1] = 3.0f/8.0f;
                f[2] = 3.0f/8.0f;
                f[3] = 2.0f/8.0f;
                f[4] = 4.0f/8.0f;
                f[5] = 3.0f/8.0f;
                f[6] = 4.0f/8.0f;
                f[7] = 2.0f/8.0f;
                break;
            case 'U':
                f[0] = 4.0f/8.0f;
                f[1] = 3.0f/8.0f;
                f[2] = 4.0f/8.0f;
                f[3] = 2.0f/8.0f;
                f[4] = 5.0f/8.0f;
                f[5] = 3.0f/8.0f;
                f[6] = 5.0f/8.0f;
                f[7] = 2.0f/8.0f;
                break;
            case 'V':
                f[0] = 5.0f/8.0f;
                f[1] = 3.0f/8.0f;
                f[2] = 5.0f/8.0f;
                f[3] = 2.0f/8.0f;
                f[4] = 6.0f/8.0f;
                f[5] = 3.0f/8.0f;
                f[6] = 6.0f/8.0f;
                f[7] = 2.0f/8.0f;
                break;
            case 'W':
                f[0] = 6.0f/8.0f;
                f[1] = 3.0f/8.0f;
                f[2] = 6.0f/8.0f;
                f[3] = 2.0f/8.0f;
                f[4] = 7.0f/8.0f;
                f[5] = 3.0f/8.0f;
                f[6] = 7.0f/8.0f;
                f[7] = 2.0f/8.0f;
                break;
            case 'X':
                f[0] = 7.0f/8.0f;
                f[1] = 3.0f/8.0f;
                f[2] = 7.0f/8.0f;
                f[3] = 2.0f/8.0f;
                f[4] = 8.0f/8.0f;
                f[5] = 3.0f/8.0f;
                f[6] = 8.0f/8.0f;
                f[7] = 2.0f/8.0f;
                break;
            case 'Y':
                f[0] = 0.0f/8.0f;
                f[1] = 4.0f/8.0f;
                f[2] = 0.0f/8.0f;
                f[3] = 3.0f/8.0f;
                f[4] = 1.0f/8.0f;
                f[5] = 4.0f/8.0f;
                f[6] = 1.0f/8.0f;
                f[7] = 3.0f/8.0f;
                break;
            case 'Z':
                f[0] = 1.0f/8.0f;
                f[1] = 4.0f/8.0f;
                f[2] = 1.0f/8.0f;
                f[3] = 3.0f/8.0f;
                f[4] = 2.0f/8.0f;
                f[5] = 4.0f/8.0f;
                f[6] = 2.0f/8.0f;
                f[7] = 3.0f/8.0f;
                break;
            case '0':
                f[0] = 2.0f/8.0f;
                f[1] = 4.0f/8.0f;
                f[2] = 2.0f/8.0f;
                f[3] = 3.0f/8.0f;
                f[4] = 3.0f/8.0f;
                f[5] = 4.0f/8.0f;
                f[6] = 3.0f/8.0f;
                f[7] = 3.0f/8.0f;
                break;
            case '1':
                f[0] = 3.0f/8.0f;
                f[1] = 4.0f/8.0f;
                f[2] = 3.0f/8.0f;
                f[3] = 3.0f/8.0f;
                f[4] = 4.0f/8.0f;
                f[5] = 4.0f/8.0f;
                f[6] = 4.0f/8.0f;
                f[7] = 3.0f/8.0f;
                break;
            case '2':
                f[0] = 4.0f/8.0f;
                f[1] = 4.0f/8.0f;
                f[2] = 4.0f/8.0f;
                f[3] = 3.0f/8.0f;
                f[4] = 5.0f/8.0f;
                f[5] = 4.0f/8.0f;
                f[6] = 5.0f/8.0f;
                f[7] = 3.0f/8.0f;
                break;
            case '3':
                f[0] = 5.0f/8.0f;
                f[1] = 4.0f/8.0f;
                f[2] = 5.0f/8.0f;
                f[3] = 3.0f/8.0f;
                f[4] = 6.0f/8.0f;
                f[5] = 4.0f/8.0f;
                f[6] = 6.0f/8.0f;
                f[7] = 3.0f/8.0f;
                break;
            case '4':
                f[0] = 6.0f/8.0f;
                f[1] = 4.0f/8.0f;
                f[2] = 6.0f/8.0f;
                f[3] = 3.0f/8.0f;
                f[4] = 7.0f/8.0f;
                f[5] = 4.0f/8.0f;
                f[6] = 7.0f/8.0f;
                f[7] = 3.0f/8.0f;
                break;
            case '5':
                f[0] = 7.0f/8.0f;
                f[1] = 4.0f/8.0f;
                f[2] = 7.0f/8.0f;
                f[3] = 3.0f/8.0f;
                f[4] = 8.0f/8.0f;
                f[5] = 4.0f/8.0f;
                f[6] = 8.0f/8.0f;
                f[7] = 3.0f/8.0f;
                break;
            case '6':
                f[0] = 0.0f/8.0f;
                f[1] = 5.0f/8.0f;
                f[2] = 0.0f/8.0f;
                f[3] = 4.0f/8.0f;
                f[4] = 1.0f/8.0f;
                f[5] = 5.0f/8.0f;
                f[6] = 1.0f/8.0f;
                f[7] = 4.0f/8.0f;
                break;
            case '7':
                f[0] = 1.0f/8.0f;
                f[1] = 5.0f/8.0f;
                f[2] = 1.0f/8.0f;
                f[3] = 4.0f/8.0f;
                f[4] = 2.0f/8.0f;
                f[5] = 5.0f/8.0f;
                f[6] = 2.0f/8.0f;
                f[7] = 4.0f/8.0f;
                break;
            case '8':
                f[0] = 2.0f/8.0f;
                f[1] = 5.0f/8.0f;
                f[2] = 2.0f/8.0f;
                f[3] = 4.0f/8.0f;
                f[4] = 3.0f/8.0f;
                f[5] = 5.0f/8.0f;
                f[6] = 3.0f/8.0f;
                f[7] = 4.0f/8.0f;
                break;
            case '9':
                f[0] = 3.0f/8.0f;
                f[1] = 5.0f/8.0f;
                f[2] = 3.0f/8.0f;
                f[3] = 4.0f/8.0f;
                f[4] = 4.0f/8.0f;
                f[5] = 5.0f/8.0f;
                f[6] = 4.0f/8.0f;
                f[7] = 4.0f/8.0f;
                break;
            case '!':
                f[0] = 4.0f/8.0f;
                f[1] = 5.0f/8.0f;
                f[2] = 4.0f/8.0f;
                f[3] = 4.0f/8.0f;
                f[4] = 5.0f/8.0f;
                f[5] = 5.0f/8.0f;
                f[6] = 5.0f/8.0f;
                f[7] = 4.0f/8.0f;
                break;
            case '?':
                f[0] = 5.0f/8.0f;
                f[1] = 5.0f/8.0f;
                f[2] = 5.0f/8.0f;
                f[3] = 4.0f/8.0f;
                f[4] = 6.0f/8.0f;
                f[5] = 5.0f/8.0f;
                f[6] = 6.0f/8.0f;
                f[7] = 4.0f/8.0f;
                break;
            case '+':
                f[0] = 6.0f/8.0f;
                f[1] = 5.0f/8.0f;
                f[2] = 6.0f/8.0f;
                f[3] = 4.0f/8.0f;
                f[4] = 7.0f/8.0f;
                f[5] = 5.0f/8.0f;
                f[6] = 7.0f/8.0f;
                f[7] = 4.0f/8.0f;
                break;
            case '-':
                f[0] = 7.0f/8.0f;
                f[1] = 5.0f/8.0f;
                f[2] = 7.0f/8.0f;
                f[3] = 4.0f/8.0f;
                f[4] = 8.0f/8.0f;
                f[5] = 5.0f/8.0f;
                f[6] = 8.0f/8.0f;
                f[7] = 4.0f/8.0f;
                break;
            case '=':
                f[0] = 0.0f/8.0f;
                f[1] = 6.0f/8.0f;
                f[2] = 0.0f/8.0f;
                f[3] = 5.0f/8.0f;
                f[4] = 1.0f/8.0f;
                f[5] = 6.0f/8.0f;
                f[6] = 1.0f/8.0f;
                f[7] = 5.0f/8.0f;
                break;
            case ':':
                f[0] = 1.0f/8.0f;
                f[1] = 6.0f/8.0f;
                f[2] = 1.0f/8.0f;
                f[3] = 5.0f/8.0f;
                f[4] = 2.0f/8.0f;
                f[5] = 6.0f/8.0f;
                f[6] = 2.0f/8.0f;
                f[7] = 5.0f/8.0f;
                break;
            case '.':
                f[0] = 2.0f/8.0f;
                f[1] = 6.0f/8.0f;
                f[2] = 2.0f/8.0f;
                f[3] = 5.0f/8.0f;
                f[4] = 3.0f/8.0f;
                f[5] = 6.0f/8.0f;
                f[6] = 3.0f/8.0f;
                f[7] = 5.0f/8.0f;
                break;
            case ',':
                f[0] = 3.0f/8.0f;
                f[1] = 6.0f/8.0f;
                f[2] = 3.0f/8.0f;
                f[3] = 5.0f/8.0f;
                f[4] = 4.0f/8.0f;
                f[5] = 6.0f/8.0f;
                f[6] = 4.0f/8.0f;
                f[7] = 5.0f/8.0f;
                break;
        }

        return f;
    }
}