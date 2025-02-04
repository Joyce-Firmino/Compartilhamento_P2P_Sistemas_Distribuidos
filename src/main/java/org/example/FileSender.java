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

            // Receber o nome do arquivo solicitado e o range
            String request = in.readLine();
            String[] requestParts = request.split(" ");
            String fileName = requestParts[1];
            String range = requestParts.length > 2 ? requestParts[2] : "0-"; // Padrão de range

            System.out.println("Cliente solicitou o arquivo: " + fileName);

            // Pega o arquivo
            File file = new File(directory, fileName);
            if (!file.exists() || !file.isFile()) {
                System.out.println("Arquivo não encontrado: " + fileName);
                out.write("ERRO: Arquivo não encontrado.\n".getBytes());
                return;
            }

            // Processa os valores do range
            String[] rangeParts = range.split("-");
            long offsetStart = Long.parseLong(rangeParts[0]);
            long offsetEnd = rangeParts.length > 1 && !rangeParts[1].isEmpty()
                    ? Math.min(Long.parseLong(rangeParts[1]), file.length())
                    : file.length();  // Se não foi passado um offsetEnd, usa o tamanho total do arquivo

            //System.out.println("Enviando bytes de " + offsetStart + " até " + offsetEnd);

            try (RandomAccessFile fileIn = new RandomAccessFile(file, "r")) {
                fileIn.seek(offsetStart); // Vai direto para o ponto de leitura inicial

                byte[] buffer = new byte[4096];
                long bytesLeft = offsetEnd - offsetStart;
                int bytesRead;

                while (bytesLeft > 0 && (bytesRead = fileIn.read(buffer, 0, (int) Math.min(buffer.length, bytesLeft))) != -1) {
                    out.write(buffer, 0, bytesRead);
                    bytesLeft -= bytesRead;
                    out.flush();
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