package org.example;

import java.io.*;
import java.net.Socket;

public class FileSender implements Runnable {
    private final Socket socket;
    private final String directory;

    public FileSender(Socket socket, String directory) {
        this.socket = socket;
        this.directory = directory;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             OutputStream out = socket.getOutputStream()) {

            // Receber o nome do arquivo solicitado
            String fileName = in.readLine();
            System.out.println("Cliente solicitou o arquivo: " + fileName);

            File file = new File(directory, fileName);
            if (!file.exists() || !file.isFile()) {
                System.out.println("Arquivo não encontrado: " + fileName);
                out.write("ERRO: Arquivo não encontrado.\n".getBytes());
                return;
            }

            // Enviar o arquivo
            try (FileInputStream fileIn = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                System.out.println("Arquivo enviado com sucesso: " + fileName);
            }
        } catch (IOException e) {
            System.err.println("Erro ao enviar o arquivo: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
