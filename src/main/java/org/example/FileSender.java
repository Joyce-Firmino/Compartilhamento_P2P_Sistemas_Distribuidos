package org.example;

import java.io.*;
import java.net.*;

public class FileSender implements Runnable {
    private Socket socket;

    private String local;
    private String fileName;

    public FileSender(Socket socket, String local, String fileName) {
        this.socket = socket;
        this.local = local;
        this.fileName = fileName;
    }

    @Override
    public void run() {
        try (OutputStream out = socket.getOutputStream();
             BufferedInputStream fileIn = new BufferedInputStream(
                     new FileInputStream(new File(local, fileName)))) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();

        } catch (IOException e) {
            System.err.println("Erro ao enviar arquivo: " + e.getMessage());
        }
    }
}

