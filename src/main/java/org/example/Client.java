package org.example;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.net.Socket;
public class Client {
    private static final int SERVER_PORT = 1234;
    private static final int CLIENT_PORT = 1235;
    static boolean isConnected;

    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {

            // Enviar JOIN automaticamente
            out.println("JOIN " + socket.getLocalAddress().getHostAddress());

            // Enviar arquivos (CREATEFILE) automaticamente
            shareFiles(out);

            // Iniciar servidor para downloads em uma thread separada
            new Thread(Client::listenForDownloads).start();

            // Loop para envio e recepção de dados
            String response;
            while (true) {
                // Verificar se há resposta do servidor
                if (in.ready()) {
                    response = in.readLine();
                    if (response != null) {
                        System.out.println("Servidor: " + response);
                    }
                }

                // Verificar se há entrada do usuário
                if (userInput.ready()) {
                    String userCommand = userInput.readLine();

                    // Enviar comando ao servidor
                    out.println(userCommand);

                    // Verificar se o comando é LEAVE

                    if ("LEAVE".equalsIgnoreCase(userCommand.trim())) {
                        isConnected = false; // Cliente foi desconectado
                        System.out.println("Você saiu do sistema. Digite JOIN para reconectar.");
                    }

                    // Verificar se o comando é JOIN e o cliente não está conectado
                    if ("JOIN".equalsIgnoreCase(userCommand.trim()) && !isConnected) {
                        isConnected = true; // Cliente está reconectado
                        System.out.println("Reconectando e enviando arquivos novamente...");
                        out.println("JOIN " + socket.getLocalAddress().getHostAddress());
                        shareFiles(out); // Enviar arquivos da pasta public novamente
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void shareFiles(PrintWriter out) {
        File folder = new File("./public");
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                out.println("CREATEFILE " + file.getName() + " " + file.length());
            }
        }
    }

    private static void listenForDownloads() {
        try (var serverSocket = new java.net.ServerSocket(CLIENT_PORT)) {
            while (true) {
                var socket = serverSocket.accept();
                new Thread(new FileSender(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
