package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        int opcao = -1;

        while (opcao != 1 && opcao != 2) {
            System.out.println("\u001B[33m====================================");
            System.out.println("   Bem-vindo ao Sistema de Rede!   ");
            System.out.println("====================================\u001B[0m");
            System.out.println("Escolha uma das opções abaixo:");
            System.out.println("1. 🌐 Iniciar o Servidor");
            System.out.println("2. 👤 Iniciar como Cliente");  // Texto azul para 'Iniciar como Cliente'
            System.out.println("\u001B[33m====================================\u001B[0m");
            System.out.print("Digite sua opção (1 ou 2): ");

            try {
                opcao = Integer.parseInt(br.readLine());
                if (opcao == 1) {
                    System.out.println("\n🔧 Iniciando o servidor...");
                    Server s = new Server();
                } else if (opcao == 2) {
                    System.out.println("\n🔗 Iniciando como cliente...");
                    Client c = new Client();
                } else {
                    System.out.println("\n❌ Opção inválida! Tente novamente.\n");
                }
            } catch (NumberFormatException e) {
                System.out.println("\n⚠ Entrada inválida! Digite apenas números.\n");
            }
        }
    }
}
