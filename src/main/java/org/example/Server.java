package org.example;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 1234;
    private static final Map<String, List<Map<String, Object>>> allFiles = new HashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor iniciado na porta " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                String ipAddress = socket.getInetAddress().getHostAddress();
                System.out.println("Cliente " + ipAddress + " se conectou!");

                String message;

                while ((message = in.readLine()) != null) {
                    System.out.println("Recebido do cliente " + ipAddress + ": " + message);

                    String[] parts = message.split(" ");
                    switch (parts[0]) {
                        case "JOIN":
                            out.println("CONFIRMJOIN");
                            break;

                        case "CREATEFILE":
                            if (parts.length >= 3) {
                                String filename = parts[1];
                                long size = Long.parseLong(parts[2]);
                                Map<String, Object> file = new HashMap<>();
                                file.put("filename", filename);
                                file.put("size", size);
                                allFiles.computeIfAbsent(ipAddress, k -> new ArrayList<>()).add(file);
                                out.println("CONFIRMCREATEFILE " + filename);
                            } else {
                                out.println("Erro: parâmetros inválidos para CREATEFILE.");
                            }
                            break;

                        case "SEARCH":
                            if (parts.length >= 2) {
                                String pattern = parts[1];
                                List<String> results = search(pattern);
                                if (results.isEmpty()) {
                                    out.println("Nenhum arquivo correspondente encontrado.");
                                } else {
                                    for (String result : results) {
                                        out.println(result);
                                    }
                                }
                            } else {
                                out.println("Erro: padrão de busca ausente.");
                            }
                            break;

                        case "LEAVE":
                            allFiles.remove(ipAddress);
                            out.println("CONFIRMLEAVE");
                            break;

                        default:
                            out.println("Comando inválido");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static List<String> search(String pattern) {
        List<String> result = new ArrayList<>();
        for (String ip : allFiles.keySet()) {
            for (Map<String, Object> file : allFiles.get(ip)) {
                String filename = file.get("filename").toString();
                if (filename.contains(pattern)) {
                    String fileInfo = String.format("File: %s, IP: %s, Bytes: %d",
                            filename,
                            ip,
                            (long) file.get("size")
                    );
                    result.add(fileInfo);
                }
            }
        }
        return result;
    }
}
