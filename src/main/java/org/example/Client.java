package org.example;

import java.io.*;
import java.net.*;

public class Client {
    private static final int SERVER_PORT = 1234;
    private static final int CLIENT_PORT = 1235;
    private static boolean isConnected = true;
    private static boolean waitingForDownloadResponse = false;
    private static String fileToDownload = "";
    private static String fileIP = "";
    private static boolean receivedFileFoundResponse = false;  // Nova variável de controle

    public Client() throws NumberFormatException {
        try (Socket socket = new Socket("localhost", SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {

            // Enviar JOIN automaticamente ao iniciar
            out.println("JOIN " + socket.getLocalAddress().getHostAddress());
            shareFiles(out);

            // Iniciar servidor para downloads em uma thread separada
            new Thread(Client::listenForDownloads).start();

            // Loop principal
            while (true) {
                // Receber mensagens do servidor
                if (in.ready()) {
                    String response = in.readLine();
                    if (response != null) {
                        System.out.println("Servidor: " + response);

                        // Tratar resposta para DOWNLOAD
                        if (response.startsWith("FILE_FOUND") && !receivedFileFoundResponse) {
                            handleFileFound(response);
                            receivedFileFoundResponse = true;  // Marca que já processou a resposta
                        }
                    }
                }

                // Enviar comandos do usuário
                if (userInput.ready()) {
                    String userCommand = userInput.readLine();

                    if (waitingForDownloadResponse) {
                        // Se o cliente está esperando pela resposta do download
                        if ("s".equalsIgnoreCase(userCommand)) {
                            // Baixar arquivo
                            downloadFile(fileToDownload, fileIP, CLIENT_PORT);
                        } else if ("n".equalsIgnoreCase(userCommand)) {
                            System.out.println("Você escolheu não baixar o arquivo.");
                        }
                        waitingForDownloadResponse = false;
                        receivedFileFoundResponse = false;  // Permite processar novas respostas de FILE_FOUND
                    } else {
                        out.println(userCommand);

                        if ("LEAVE".equalsIgnoreCase(userCommand.trim())) {
                            isConnected = false;
                            System.out.println("Você saiu do sistema. Digite JOIN para reconectar.");
                        }

                        if ("JOIN".equalsIgnoreCase(userCommand.trim()) && !isConnected) {
                            isConnected = true;
                            System.out.println("Reconectando e enviando arquivos novamente...");
                            out.println("JOIN " + socket.getLocalAddress().getHostAddress());
                            shareFiles(out);
                        }

                        if (userCommand.startsWith("DOWNLOAD")) {
                            String[] parts = userCommand.split(" ");
                            if (parts.length == 3) {
                                String clientIP = parts[1];
                                String fileName = parts[2];
                                downloadFile(fileName, clientIP, CLIENT_PORT);
                            } else {
                                System.out.println("Uso correto: DOWNLOAD <ipAddress> <filename>");
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Compartilha informações sobre os arquivos presentes na pasta "./public" com o servidor.
    private static void shareFiles(PrintWriter out) {
        File folder = new File("./public");
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                out.println("CREATEFILE " + file.getName() + " " + file.length());
            }
        }
    }

    // Escuta por solicitações de download de outros clientes em uma porta específica.
    private static void listenForDownloads() {
        try (ServerSocket serverSocket = new ServerSocket(CLIENT_PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new FileSender(socket, "./downloads")).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Faz o download de um arquivo de outro cliente especificado pelo IP e porta.
    private static void downloadFile(String fileName, String clientIP, int port) {
        File downloadsDir = new File("./downloads");

        // Garantir que o diretório de downloads existe
        if (!downloadsDir.exists()) {
            if (downloadsDir.mkdir()) {
                System.out.println("Diretório 'downloads' criado.");
            } else {
                System.err.println("Não foi possível criar o diretório 'downloads'.");
                return;
            }
        }

        // Caminho completo para salvar o arquivo
        File fileToSave = new File(downloadsDir, fileName);

        try (Socket downloadSocket = new Socket(clientIP, port);
             InputStream in = downloadSocket.getInputStream();
             FileOutputStream fileOut = new FileOutputStream(fileToSave)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                fileOut.write(buffer, 0, bytesRead);
            }

            System.out.println("Download concluído: " + fileToSave.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Erro ao baixar o arquivo: " + e.getMessage());
        }
    }

    // Lida com a resposta "FILE_FOUND" do servidor, indicando que um arquivo foi encontrado e perguntando se deseja fazer download
    private static void handleFileFound(String response) {
        // Formato esperado: FILE_FOUND <fileName> <ipAddress> <fileSize>
        String[] parts = response.split(" ");
        if (parts.length >= 4) {
            String fileName = parts[1];
            fileIP = parts[2];
            long fileSize = Long.parseLong(parts[3]);

            System.out.println("Arquivo encontrado: " + fileName);
            System.out.println("Local: " + fileIP + " | Tamanho: " + fileSize + " bytes");

            // Armazenar o nome do arquivo a ser baixado
            fileToDownload = fileName;

            // Definir que está aguardando a resposta do usuário
            waitingForDownloadResponse = true;

            // Agora, espera-se a resposta do usuário para decidir se o download será feito
            System.out.print("Deseja baixar este arquivo? (s/n): ");
        }
    }
}
