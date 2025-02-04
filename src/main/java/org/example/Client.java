package org.example;

import java.io.*;
import java.net.*;
import java.util.*;

public class Client {

    private static final int SERVER_PORT = 1234;
    private static final int CLIENT_PORT = 1235;
    private static String serverIP;
    private static final Set<String> processedResponses = new HashSet<>();

    // Mapa para armazenar os IPs de clientes que possuem um arquivo
    private static Map<String, List<String>> fileOwners = new HashMap<>();

    public Client() throws NumberFormatException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.print("Digite o IP do servidor: ");
            serverIP = br.readLine().trim();  // Lê o IP digitado pelo usuário

            try (Socket socket = new Socket(serverIP, SERVER_PORT);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {

                Scanner scanner = new Scanner(System.in); // 🔹 Instancia Scanner corretamente

                // Iniciar servidor para downloads em uma thread separada
                new Thread(Client::listenForDownloads).start();

                while (true) {

                    processServerResponses(in); // 🔹 Lê todas as respostas pendentes antes de mostrar o menu
                    displayMenu();
                    String userChoice = scanner.nextLine().trim();
                    handleMenuChoice(userChoice, out, socket, in, scanner);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            System.out.println("Erro ao obter o IP do servidor: " + e.getMessage());
        }
    }

    private void displayMenu() {

        System.out.println("\033[1;34m===================================="); // Azul
        System.out.println("         \033[1;32mBEM-VINDO AO CLIENTE\033[0m       "); // Verde
        System.out.println("\033[1;34m====================================");
        System.out.println("\033[1;33m1. \033[0mJOIN"); // Amarelo
        System.out.println("\033[1;33m2. \033[0mCREATEFILE (Todos)");
        System.out.println("\033[1;33m3. \033[0mCREATEFILE (Individual)");
        System.out.println("\033[1;33m4. \033[0mSEARCH");
        System.out.println("\033[1;33m5. \033[0mGET"); // Amarelo
        System.out.println("\033[1;33m6. \033[0mDELETEFILE");
        System.out.println("\033[1;33m7. \033[0mLEAVE");
        System.out.println("\033[1;33m8. \033[0mSair");
        System.out.println("\033[1;34m====================================\033[0m");
        System.out.print("\033[1;37mEscolha uma opção: \033[0m"); // Branco
    }


    /**
     * Processa comandos do cliente.
     */
    private void handleMenuChoice(String choice, PrintWriter out, Socket socket, BufferedReader in, Scanner scanner) throws IOException {
        switch (choice) {
            case "1":
                handleJoinCommand(out, socket);
                break;
            case "2":
                shareFiles(out);
                break;
            case "3":
                handleCreateFileCommand(out, scanner);
                break;
            case "4":
                System.out.print("Digite o nome do arquivo para buscar: ");
                String searchQuery = scanner.nextLine().trim();
                handleSearchCommand("SEARCH " + searchQuery, out, in);
                break;
            case "5":
                System.out.print("Digite o nome do arquivo para baixar: ");
                String fileName = scanner.nextLine().trim();
                System.out.print("Digite o range do arquivo (formato <OFFSET START>-<OFFSET END>, por exemplo, 0-51200): ");
                String range = scanner.nextLine().trim();  // Agora pedindo o range
                handleGetCommand("GET " + fileName + " " + range, out, in);
                break;
            case "6":
                System.out.print("Digite o nome do arquivo para deletar: ");
                String deleteFileName = scanner.nextLine().trim();
                handleDeleteFileCommand("DELETEFILE " + deleteFileName, out);
                break;
            case "7":
                handleLeaveCommand(out);
                break;
            case "8":
                System.out.println("Saindo...");
                System.exit(0);
                break;
            default:
                System.out.println("Opção inválida. Tente novamente.");
        }
        // Processar respostas após cada comando
        processServerResponses(in);
    }

    /**
     * Processa respostas do servidor.
     */
    private void processServerResponses(BufferedReader in) throws IOException {
        while (in.ready()) {
            String response = in.readLine();
            if (response != null && processedResponses.add(response)) {
                System.out.println(response);

                // Caso seja uma confirmação de DELETEFILE, remove do mapa de donos de arquivos
                if (response.startsWith("CONFIRMDELETEFILE")) {
                    String[] parts = response.split(" ");
                    if (parts.length >= 2) {
                        String fileName = parts[1];
                        fileOwners.remove(fileName);
                    }
                }
            }
        }

        // Adiciona um pequeno delay para dar tempo ao sistema de processar respostas pendentes
        try {
            Thread.sleep(100);  // Pausa de 100ms (ajuste conforme necessário)
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * Lida com o comando JOIN.
     */
    private void joinServer(PrintWriter out, Socket socket) {
        out.println("JOIN " + socket.getLocalAddress().getHostAddress());
    }

    /**
     * Lida com o comando GET.
     */
    private void handleGetCommand(String userCommand, PrintWriter out, BufferedReader in) throws IOException {
        String[] parts = userCommand.split(" ");
        if (parts.length >= 2) {
            String fileName = parts[1];
            String range = parts.length > 2 ? parts[2] : "0-51200"; // Exemplo de range

            // Executa SEARCH antes do GET
            out.println("SEARCH " + fileName);

            // Aguarda resposta por até 3 segundos
            long startTime = System.currentTimeMillis();
            while (!fileOwners.containsKey(fileName) && (System.currentTimeMillis() - startTime) < 3000) {
                if (in.ready()) {
                    String response = in.readLine();
                    if (response != null && processedResponses.add(response)) {
                        //System.out.println(response);

                        if (response.startsWith("FILE")) {
                            // Se o SEARCH foi feito automaticamente antes do GET, apenas armazena sem exibir
                            storeFileInfo(response);
                        }
                    }
                }
            }

            // Verifica se encontrou algum cliente com o arquivo
            List<String> availableIPs = fileOwners.get(fileName);
            if (availableIPs != null && !availableIPs.isEmpty()) {
                String clientIP = availableIPs.get(0); // Seleciona o primeiro IP disponível
                downloadFile(fileName, clientIP, CLIENT_PORT, range);
            } else {
                System.out.println("Erro: Nenhum cliente com o arquivo encontrado. Tente novamente.");
            }
        } else {
            System.out.println("Uso correto: GET <filename> <offset-start>-<offset-end>");
        }
    }

    /**
     * Lida com o comando SEARCH.
     */
    private void handleSearchCommand(String userCommand, PrintWriter out, BufferedReader in) throws IOException {
        String[] parts = userCommand.split(" ");
        if (parts.length >= 2) {
            String searchQuery = parts[1];
            processedResponses.clear();
            out.println("SEARCH " + searchQuery);


            long startTime = System.currentTimeMillis();
            boolean foundFiles = false;

            while ((System.currentTimeMillis() - startTime) < 3000) {
                if (in.ready()) {
                    String response = in.readLine();
                    if (response != null && processedResponses.add(response)) {
                        System.out.println(response);

                        if (response.startsWith("FILE")) {
                            storeFileInfo(response);
                            foundFiles = true; // ✅ Agora ele detecta corretamente os arquivos
                        }
                    }
                }
            }

            // ✅ Se nenhum arquivo foi encontrado, exibir mensagem corretamente
            if (!foundFiles) {
                System.out.println("Nenhum arquivo encontrado para: " + searchQuery);
            }
        } else {
            System.out.println("Uso correto: SEARCH <nome_do_arquivo>");
        }
    }

    /**
     * Lida com o comando DELETEFILE.
     */
    private void handleDeleteFileCommand(String userCommand, PrintWriter out) {
        String[] parts = userCommand.split(" ");
        if (parts.length >= 2) {
            String fileName = parts[1];
            out.println("DELETEFILE " + fileName);
        } else {
            System.out.println("Uso correto: DELETEFILE <filename>");
        }
    }

    /**
     * Lida com o comando LEAVE.
     */
    private void handleLeaveCommand(PrintWriter out) {
        out.println("LEAVE");
        System.out.println("Você saiu do sistema. Digite JOIN para reconectar.");
    }

    /**
     * Lida com o comando JOIN.
     */
    private void handleJoinCommand(PrintWriter out, Socket socket) {
        joinServer(out, socket);
    }

    // Compartilha informações sobre os arquivos na pasta ./public
    private static void shareFiles(PrintWriter out) {
        File folder = new File("./public");
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                out.println("CREATEFILE " + file.getName() + " " + file.length());
            }
        }
    }

    // Escuta por solicitações de download em uma porta específica
    private static void listenForDownloads() {
        try (ServerSocket serverSocket = new ServerSocket(CLIENT_PORT)) {
            //System.out.println("🔵 Cliente pronto para enviar arquivos na porta " + CLIENT_PORT);
            while (true) {
                Socket sockett = serverSocket.accept();
                new Thread(new FileSender(sockett, "./public/")).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void downloadFile(String fileName, String clientIP, int port, String range) {
        System.out.println("Tentando baixar do cliente: " + clientIP + " na porta: " + port);
        File downloadsDir = new File("./downloads");

        if (!downloadsDir.exists() && !downloadsDir.mkdir()) {
            System.err.println("Não foi possível criar o diretório 'downloads'.");
            return;
        }

        File fileToSave = new File(downloadsDir, fileName);

        try (Socket downloadSocket = new Socket(clientIP, port);
             PrintWriter out = new PrintWriter(downloadSocket.getOutputStream(), true);
             InputStream in = downloadSocket.getInputStream();
             RandomAccessFile fileOut = new RandomAccessFile(fileToSave, "rw")) {

            // Ajusta o range caso o usuário tenha enviado apenas "0-"
            if (range.endsWith("-")) {
                range += "999999999"; // Um valor alto para garantir que o servidor envia tudo
            }

            // Enviar comando GET com range para o cliente
            out.println("GET " + fileName + " " + range);

            // Processar o range
            long offsetStart = Long.parseLong(range.split("-")[0]);
            fileOut.seek(offsetStart); // Começa a escrever no ponto certo

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

    // Apenas armazena os IPs dos clientes que possuem o arquivo, sem exibir no terminal
    private static void storeFileInfo(String response) {
        String[] parts = response.split(" ");
        if (parts.length >= 4) {
            String fileName = parts[1];
            String fileIP = parts[2];

            // Armazena no mapa sem exibir no terminal
            fileOwners.putIfAbsent(fileName, new ArrayList<>());
            fileOwners.get(fileName).add(fileIP);
        }
    }

    //Lida com o comando CREATEFILE individualmente
    private void handleCreateFileCommand(PrintWriter out, Scanner scanner) {
        System.out.print("Digite o nome do arquivo a ser criado: ");
        String fileName = scanner.nextLine().trim();
        System.out.print("Digite o tamanho do arquivo (em bytes): ");
        String fileSizeStr = scanner.nextLine().trim();

        try {
            long fileSize = Long.parseLong(fileSizeStr);
            if (fileSize > 0) {
                // Envia o comando CREATEFILE para o servidor
                out.println("CREATEFILE " + fileName + " " + fileSize);
                System.out.println("Arquivo " + fileName + " criado com sucesso e enviado ao servidor.");
            } else {
                System.out.println("Tamanho inválido para o arquivo. Tente novamente.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Erro: Tamanho do arquivo deve ser um número válido.");
        }
    }

}