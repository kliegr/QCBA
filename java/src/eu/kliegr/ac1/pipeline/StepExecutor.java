/*
 * Monotonicity Exploiting Association Rule Classification (MARC)
 *
 *     Copyright (C)2014-2017 Tomas Kliegr
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.kliegr.ac1.pipeline;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Deprecated
public class StepExecutor {

    /**
     *
     * @param args
     * @throws IOException
     * @throws InterruptedException
     */
    public static void executor(String[] args) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        Process process = processBuilder.start();

        InputStream outputStream = null, errorStream = null;
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errorBuffer = new ByteArrayOutputStream();
        try {
            outputStream = process.getInputStream();
            errorStream = process.getErrorStream();

            byte[] tmp = new byte[1024];

            while (true) {
                int outputBytes = readAvailablOnce(outputStream, outputBuffer, tmp);
                int errorBytes = readAvailablOnce(errorStream, errorBuffer, tmp);
                if (outputBytes == 0 && errorBytes == 0) {
                    try {
                        process.exitValue();
                        break;
                    } catch (IllegalThreadStateException e) {
                        // keep on looping
                    }
                }
            }
            readAvailableAll(outputStream, outputBuffer, tmp);
            readAvailableAll(errorStream, errorBuffer, tmp);

        } finally {
            closeQuietly(outputStream);
            closeQuietly(errorStream);
        }

        System.out.println(outputBuffer.toString("ASCII"));
        System.err.println(errorBuffer.toString("ASCII"));
    }

    private static void closeQuietly(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                // ignored
            }
        }
    }

    private static int readAvailablOnce(
            InputStream inputStream, OutputStream outputStream, byte[] buffer)
            throws IOException {
        int bytesRead = 0;
        if (inputStream.available() > 0) {
            bytesRead = inputStream.read(buffer);
            outputStream.write(buffer, 0, bytesRead);
        }
        return bytesRead;
    }

    private static void readAvailableAll(
            InputStream inputStream, OutputStream outputStream, byte[] buffer)
            throws IOException {
        if (inputStream.available() > 0) {
            int bytesRead = 0;
            while ((bytesRead = inputStream.read(buffer)) >= 0) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    private StepExecutor() {
    }
}
