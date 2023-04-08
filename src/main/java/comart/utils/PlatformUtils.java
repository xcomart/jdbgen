/*
 * MIT License
 * 
 * Copyright (c) 2020 Dennis Soungjin Park
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package comart.utils;

import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlatformUtils {
    private static OSType detectedOS = null;
    private static final Logger logger = Logger.getLogger(PlatformUtils.class.getName());

    public PlatformUtils() {
    }

    public static OSType getOSType() {
        if (detectedOS == null) {
            String OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
            if (!OS.contains("mac") && !OS.contains("darwin")) {
                if (OS.contains("win")) {
                    detectedOS = OSType.Windows;
                } else if (!OS.contains("ux") && !OS.contains("ix")) {
                    detectedOS = OSType.Other;
                } else {
                    detectedOS = OSType.Unix;
                }
            } else {
                detectedOS = OSType.MacOS;
            }
        }

        return detectedOS;
    }

    public static void openURL(String url) {
        Runtime rt = Runtime.getRuntime();

        try {
            String[] cmd = null;
            if (isWindows()) {
                cmd = new String[]{"rundll32", "url.dll,FileProtocolHandler", url};
            } else if (isMac()) {
                cmd = new String[]{"open", url};
            } else if (isUnix()) {
                cmd = new String[]{"xdg-open", url};
            } else {
                try {
                    throw new IllegalStateException();
                } catch (IllegalStateException var4) {
                    logger.log(Level.SEVERE, (String)null, var4);
                }
            }

            if (cmd != null) {
                rt.exec(cmd).waitFor();
            }
        } catch (InterruptedException | IOException e) {
            logger.log(Level.SEVERE, (String)null, e);
        }

    }

    public static boolean isWindows() {
        return getOSType() == OSType.Windows;
    }

    public static boolean isMac() {
        return getOSType() == OSType.MacOS;
    }

    public static boolean isUnix() {
        return getOSType() == OSType.Unix;
    }

    public static enum OSType {
        Windows,
        MacOS,
        Unix,
        Other;

        private OSType() {
        }
    }
}
