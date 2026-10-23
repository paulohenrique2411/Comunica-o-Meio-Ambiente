package Client.service;

import javafx.application.Platform;
import javafx.scene.control.Label;
import shared.entity.Industria;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Base64;

import static shared.Utils.SerializationUtils.serialize;

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
        } catch (ConnectException e){
            System.out.println("Não foi possível estabelecer conexão com o servidor");
        } catch (IOException e){
            e.printStackTrace();
        }

    }

    public boolean isConected(){
        return socketChannel.isConnected();
    }

    public void escutarServidor(MensagemListener listener){
        Thread threadEscuta = new Thread( () -> {

            try{
                while (true){
                    Thread.sleep(1000);

                        if(!socketChannel.isConnected()) return;

                        String mensagem = receberMensagem();
                        if(mensagem.isEmpty() ) continue;

                        listener.onMensagemrecebida(mensagem);

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

        ByteBuffer tipoBuffer = ByteBuffer.allocate(1);
        int bytesRead = 0;
        try{
            bytesRead = socketChannel.read(tipoBuffer);
        } catch (IOException e) {
            System.out.println("Conexão com o servidor foi perdida.");
            socketChannel.close();
            return null;
        }

        if(bytesRead <= 0){
            return "";
        }
        tipoBuffer.flip();
        byte tipomensagem = tipoBuffer.get();

        System.out.println("Mensagem recebida tipo: " + tipomensagem);

        if(tipomensagem == 1){

          ByteBuffer tamanhoBuffer = ByteBuffer.allocate(4);

          while (tamanhoBuffer.hasRemaining()) socketChannel.read(tamanhoBuffer);

          tamanhoBuffer.flip();
          int tamanhoTexto = tamanhoBuffer.getInt();

          ByteBuffer textoBuffer = ByteBuffer.allocate(tamanhoTexto);

          while (textoBuffer.hasRemaining()) socketChannel.read(textoBuffer);
          textoBuffer.flip();


          return StandardCharsets.UTF_8.decode(textoBuffer).toString();

        }else if(tipomensagem == 2){

            ByteBuffer tamanhoBuffer = ByteBuffer.allocate(4);

            while (tamanhoBuffer.hasRemaining()) socketChannel.read(tamanhoBuffer);
            tamanhoBuffer.flip();

            int tamanhoObjeto = tamanhoBuffer.getInt();

            Industria industria = receberIndustria(tamanhoObjeto);

            if(industria.getImagem().exists()){
                String textoDestino = FileSystems.getDefault().getPath("").toAbsolutePath() + "\\src\\main\\java\\Client\\" + industria.getNomeImagem();

                Path destino = Path.of(textoDestino);

                Files.copy(industria.getImagem().toPath(), destino, StandardCopyOption.REPLACE_EXISTING);
            }

            return industria.toString();

        }else{
            return "Erro: Tipo de mensagem desconhecido (" + tipomensagem + ")";
        }

    }

    public void enviarMensagem(String mensagem){
        try {
            byte[] dadosImagem =  verificaAnexo(mensagem);
            byte[] textoBytes;
            if(dadosImagem != null){
                String imagemBase64 = Base64.getEncoder().encodeToString(dadosImagem);
                mensagem += " DadosImagem:" + imagemBase64;
                textoBytes = mensagem.getBytes(StandardCharsets.UTF_8);
            }else{
                textoBytes = mensagem.getBytes(StandardCharsets.UTF_8);
            }




            ByteBuffer outBuffer = ByteBuffer.allocate(1 + 4 + textoBytes.length);

            outBuffer.put((byte) 1);
            outBuffer.putInt(textoBytes.length);
            outBuffer.put(textoBytes);
            outBuffer.flip();

            while (outBuffer.hasRemaining()) socketChannel.write(outBuffer);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public byte[] verificaAnexo(String mensagem) throws IOException {
        int idxImagem = mensagem.indexOf("Imagem");

        if(idxImagem == -1){
            return null;
        }

        String origem = mensagem.substring(idxImagem + 7);

        File file = new File(Path.of("").toAbsolutePath().toString()+ "\\src\\main\\java\\Client\\"+origem);

        byte[] dadosImagem;
        try {
            dadosImagem = Files.readAllBytes(file.toPath());
        }catch (NoSuchFileException e){
            dadosImagem = null;
        }

        return dadosImagem;
    }


    public Industria receberIndustria(int objectSize) throws IOException, ClassNotFoundException {

        ByteBuffer dataBuffer = ByteBuffer.allocate(objectSize);

        while (dataBuffer.hasRemaining()){
            if(socketChannel.read(dataBuffer) == -1) throw new IOException("Conexão interrompida");
        }
        dataBuffer.flip();

        ByteArrayInputStream bais = new ByteArrayInputStream(dataBuffer.array());
        ObjectInputStream ois = new ObjectInputStream(bais);

        return (Industria) ois.readObject();
    }

    public void enviarIndustria(Industria industria){
        try {
            byte [] objectBytes = serialize(industria);

            ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + objectBytes.length);
            buffer.put((byte) 2);
            buffer.putInt(objectBytes.length);
            buffer.put(objectBytes);
            buffer.flip();

            while (buffer.hasRemaining()){
                socketChannel.write(buffer);
            }

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


