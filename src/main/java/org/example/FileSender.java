package org.example;

import java.io.*;
import java.net.*;

public class FileSender implements Runnable {
    private final Socket socket;
    private final String filePath;  // Variável para armazenar o caminho do arquivo

    // Construtor que recebe o caminho do arquivo
    public FileSender(Socket socket, String filePath) {
        this.socket = socket;
        this.filePath = filePath;
    }

    @Override
    public void run() {
        try (
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                FileInputStream fis = new FileInputStream(filePath)  // Abre o arquivo usando o caminho correto
        ) {
            byte[] buffer = new byte[4096];
            int bytesRead;

            // Lê o arquivo em blocos de 4KB e envia
            while ((bytesRead = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
