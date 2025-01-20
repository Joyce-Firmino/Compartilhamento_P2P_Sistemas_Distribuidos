package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class

Main {
    public static void main(String args[]) throws Exception{

        System.out.println("Escolha uma das opções abaixo:");
        System.out.println("1. Para iniciar o Servidor");
        System.out.println("2. Para iniciar como Cliente");


        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        int opcao = Integer.parseInt(br.readLine());

        if(opcao == 1){
            Server s = new Server();
        }
        else if(opcao == 2){
            Client c = new Client();
        }
        else{
            System.out.println("Opção inválida!");
        }
    }
}

