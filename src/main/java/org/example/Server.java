package org.example;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 1234;
    private static final Map<String, List<Map<String, Object>>> allFiles = new HashMap<>();

    public Server() throws NumberFormatException {
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
        private boolean isJoined = false;

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
                            //if (!isJoined) {  // Se o cliente ainda não foi 'joined', responde com 'CONFIRMJOIN'
                                out.println("CONFIRMJOIN");
                                //isJoined = true;
                           // }
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
                            }
                            break;

                        case "SEARCH":
                            String pattern = parts[1].trim().toLowerCase();
                            List<String> searchResults = search(pattern);
                            System.out.println("array " + searchResults);
                            for (String result : searchResults) {
                                System.out.println("individual " + result);
                                out.println(result);
                            }

                            break;

                        case "DELETEFILE":
                            if (parts.length >= 2) {
                                String filename = parts[1];

                                // Remover o arquivo da lista de arquivos daquele cliente
                                if (removeFile(ipAddress, filename)) {
                                    out.println("CONFIRMDELETEFILE " + filename);
                                    System.out.println("Arquivo " + filename + " removido com sucesso.");
                                }
                            }
                            break;


                        case "LEAVE":
                            isJoined = false;
                            allFiles.remove(ipAddress);
                            System.out.println("Cliente " + ipAddress + " desconectado. Arquivos removidos.");
                            out.println("CONFIRMLEAVE");
                            break;

                        default:
                            out.println("Comando inválido");
                    }
                }
            } catch (IOException e) {
                System.err.println("Erro de conexão: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("Conexão com cliente encerrada.");
            }
        }
    }

    private static List<String> search(String pattern) {
        List<String> result = new ArrayList<>();
        for (String ip : allFiles.keySet()) {
            System.out.println("Arquivos disponíveis: " + allFiles);
            for (Map<String, Object> file : allFiles.get(ip)) {
                String filename = file.get("filename").toString();
                if (filename.equalsIgnoreCase(pattern) || filename.contains(pattern)) {
                    String fileInfo = String.format("FILE %s %s %d",
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

    private static boolean removeFile(String ipAddress, String filename) {
        List<Map<String, Object>> files = allFiles.get(ipAddress);
        if (files != null) {
            // Filtra a lista removendo o arquivo correspondente
            boolean fileRemoved = files.removeIf(file -> file.get("filename").equals(filename));

            // Se a lista estiver vazia após a remoção, remove a entrada do cliente
            if (files.isEmpty()) {
                allFiles.remove(ipAddress);
            }

            return fileRemoved;
        }
        return false;  // Retorna false se o cliente não tem esse arquivo
    }
}
