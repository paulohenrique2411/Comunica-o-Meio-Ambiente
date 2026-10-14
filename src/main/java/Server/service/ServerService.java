package Server.service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public class ServerService implements AutoCloseable {

    private static int PORTA = 8080;
    private ServerSocketChannel server;
    private List<SocketChannel> clients = new ArrayList<>();

    public void ServerSocket(){
        try {
            server = new ServerSocketChannel.open();

            server.bind(new InetSocketAddress(PORTA));

            server.configureBlocking(false);

            if(!server.isBound()){
                System.out.println("Não foi possivel criar o servidor na porta");
            }

            System.out.println("Server Online");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void run(){
        try{
            while (true){

                SocketChannel clientSocket = server.accept();
                if(clientSocket != null) {
                    System.out.println("Novo cliente conectado " + clientSocket.getInetAddress().getHostAddress());
                    clients.add(clientSocket);
                }





                Thread.sleep(1000);
            }
        } catch (IOException e){
            e.printStackTrace();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    public void tratarRequisicoes(){

    }


    @Override
    public void close() throws Exception{
        if(!clients.isEmpty()){
            for(SocketChannel c: clients){
                c.close();
            }
        }
        if(!server.isClosed()){
            server.close();
        }
    }
}
