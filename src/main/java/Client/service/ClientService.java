package Client.service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ClientService implements  AutoCloseable{

    private SocketChannel socketChannel;
    private Selector selector;

    public ClientService(int PORTA) {
        try{
            selector = Selector.open();
            socketChannel = SocketChannel.open();

            if(!socketChannel.isOpen()){
                System.out.println("Não foi possivel estabelecer conexão com o Servidor da Secretária");
            }

            socketChannel.connect(new InetSocketAddress(InetAddress.getLocalHost().getHostAddress().toString(),PORTA));

            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);

            System.out.println("Conexão Estabelecida com o Servidor da Secretária");
        }catch (UnknownHostException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void run(){

        Scanner sc = new Scanner(System.in);

        escutarServidor();

        while (true){

            System.out.print("Digite a mensagem que deseja enviar: ");
            String mensagem = sc.nextLine();
            enviarMensagem(mensagem);

            if(mensagem.equals("sair")) break;

        }


    }

    public void escutarServidor(){
        Thread threadEscuta = new Thread( () -> {

            try{
                while (true){
                    Thread.sleep(1000);
                    String mensagem = receberMensagem();
                    if(mensagem.isEmpty()) continue;
                    System.out.println("\nMensagem recebida: " + mensagem + "\n");
                }
            }catch (IOException e){
                e.printStackTrace();
            }catch (InterruptedException e){
                e.printStackTrace();
            }


        });
        threadEscuta.start();
    }

    public String receberMensagem() throws IOException {
        try{
            ByteBuffer responseBuffer = ByteBuffer.allocate(1024);

            int byteRead = socketChannel.read(responseBuffer);

            if(byteRead == -1){
                System.out.println("Cliente desconectado: " + socketChannel.getRemoteAddress());
                socketChannel.close();
            }
            else if(byteRead > 0){
                responseBuffer.flip();
                String mensagem = StandardCharsets.UTF_8.decode(responseBuffer).toString();
                return mensagem;
            }


        }catch (IOException e){
            e.printStackTrace();
            socketChannel.close();
        }

        return "";
    }

    public void enviarMensagem(String mensagem){
        try {
            ByteBuffer outBuffer = ByteBuffer.wrap((mensagem).getBytes());
            socketChannel.write(outBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws Exception{
        if(socketChannel.isOpen()){
            socketChannel.close();
        }
    }
}
