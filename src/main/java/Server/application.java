package Server;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class application {

    public static void main(String[] args) {

        try {
            ServerSocket server = new ServerSocket(8080);

            if(!server.isBound()){
                System.out.println("Não foi possivel vincular o servidor.");
            }

            System.out.println("Servidor online");

            Socket client = server.accept();

            System.out.println("Clente conectado: " + client.getInetAddress().getHostAddress());

            Scanner entrada = new Scanner(client.getInputStream());

            while (entrada.hasNextLine()){
                System.out.println(entrada.nextLine().substring(2));
            }

            entrada.close();
            server.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

}
