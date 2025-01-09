package org.example;

import java.io.*;
import java.net.*;

public class FileSender implements Runnable {
    private final Socket socket;

    public FileSender(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                FileInputStream fis = new FileInputStream("./public/musica.mp3")
        ) {
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
