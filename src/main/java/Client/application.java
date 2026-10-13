package Client;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class application {
    public static void main(String[] args) {

        try{
            Socket cliente = new Socket(InetAddress.getLocalHost(),8080);

            if(!cliente.isConnected()){
                System.out.println("Não foi possível estabelecer conexão com o servidor");
            }

            System.out.println("Conexão com o servidor estabelecida");

            DataOutputStream dt = new DataOutputStream(cliente.getOutputStream());

            String mensagem = "Opa meu querido";
            cliente.setSendBufferSize(mensagem.length());
            dt.writeUTF(mensagem);
            dt.flush();
            dt.close();

            cliente.close();
        }catch( IOException e){
            e.printStackTrace();
        }

    }

}
