package Client.service;

import Server.entity.Industria;
import javafx.application.Platform;
import javafx.scene.control.Label;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
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
                System.out.println("Não foi possivel estabelecer conexão");
                return;
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


    public void escutarServidor(Label label){
        Thread threadEscuta = new Thread( () -> {

            try{
                while (true){
                    Thread.sleep(1000);

                    try {
                        String mensagem = receberMensagem();
                        if(mensagem.isEmpty()) continue;
                        Platform.runLater(() -> {
                            label.setText(label.getText() + "\n======Mensagem recebida======\n" + mensagem + "\n");
                        });
                    }catch (IOException e){
                        Industria industria = receberIndustria();
                        Platform.runLater(() -> {
                            label.setText(label.getText() + "\n======Mensagem recebida======\n" + industria.toString() + "\n");
                        });
                    }



                }
            }catch (IOException e){
                e.printStackTrace();
            }catch (InterruptedException e){
                e.printStackTrace();
            }catch (ClassNotFoundException e){
                e.printStackTrace();
            }


        });
        threadEscuta.start();
    }

    public String receberMensagem() throws IOException, ClassNotFoundException {

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


    public Industria receberIndustria() throws IOException, ClassNotFoundException {

        InputStream inStream = Channels.newInputStream(socketChannel);
        ObjectInputStream in = new ObjectInputStream(inStream);

        return (Industria) in.readObject();
    }

    public void enviarIndustria(Industria industria){
        try {
            OutputStream outputStream = Channels.newOutputStream(socketChannel);
            ObjectOutputStream out = new ObjectOutputStream(outputStream);

            out.writeObject(industria);
            out.flush();
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


