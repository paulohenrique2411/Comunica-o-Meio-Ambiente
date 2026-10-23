package Server.service;

import Server.repository.IndustriaRepository;
import shared.entity.Industria;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static shared.Utils.SerializationUtils.deserialize;
import static shared.Utils.SerializationUtils.serialize;

public class ServerService implements AutoCloseable {

    private IndustriaRepository industriaRepository;
    private String diretorio;
    private static int PORTA = 8080;
    private Selector selector;
    private ServerSocketChannel server;
    private List<SocketChannel> clients = new ArrayList<>();
    private List<Industria>    industrias = new ArrayList<>();
    private ProcessarComandos processarComandos;

    public ServerService(String diretorio){
        this.diretorio = diretorio;
        industriaRepository = new IndustriaRepository(diretorio);
        industrias = industriaRepository.carregarIndustrias();

        try {
            selector = Selector.open();
            server = ServerSocketChannel.open();

            server.bind(new InetSocketAddress(PORTA));

            server.configureBlocking(false);
            server.register(selector, SelectionKey.OP_ACCEPT);

            if(!server.isOpen()){
                System.out.println("Não foi possivel criar o servidor na porta");
            }

            System.out.println("Server Online");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.processarComandos = new ProcessarComandos(industriaRepository, diretorio, industrias);
    }

    public void run(){
        try{
            while (true){

                selector.select();

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();

                while (iterator.hasNext()) {

                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if(!key.isValid()) continue;

                    if(key.isAcceptable()) {
                    ServerSocketChannel serverConection = (ServerSocketChannel) key.channel();
                    SocketChannel clientChannel = server.accept();
                    clientChannel.configureBlocking(false);

                    clientChannel.register(selector, SelectionKey.OP_READ);

                    System.out.println("Nova conexão: " + clientChannel.getLocalAddress());

                    String mensagem = "Bem vindo ao Servidor da Secretária. \n" +
                            "Para solicitar mais informações siga esse padrão: \n" +
                            "Consultar {nome da industria} \n" +
                            "Para cadastrar mais industrias siga esse padrão: \n"+
                            "Cadastrar Nome:{Instituição} Local:{local} Detalhes:{detalhes} NivelPoluição:{nivelpoluição} Imagem:{imagem}";

                    enviarMensagem(mensagem,clientChannel);

                    }else if(key.isReadable()) {

                        SocketChannel clientChannel = (SocketChannel) key.channel();

                        if(!verificaRequisicao(clientChannel)){
                            fecharConexao(key, clientChannel);
                        }
                    }

                }

                Thread.sleep(1000);
            }
        } catch (IOException e){
            e.printStackTrace();
        }catch (InterruptedException e){
            e.printStackTrace();
        }catch (ClassNotFoundException e){
            e.printStackTrace();
        }
    }

    public void fecharConexao(SelectionKey key, SocketChannel client){

        try{
            key.cancel();

            if(client != null){
                client.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public boolean  verificaRequisicao(SocketChannel client) throws ClassNotFoundException, IOException {

        String mensagem = receberMensagem(client);

        if(mensagem == null) return false;

        Object resposta = processarComandos.processar(mensagem, client);

        if(resposta instanceof  String){
            enviarMensagem((String) resposta,client);
        } else if (resposta instanceof Industria) {
            enviarIndustria((Industria) resposta, client);
        }

        return true;
    }

    public String receberMensagem(SocketChannel client) {

        try{
            ByteBuffer tipoBuffer = ByteBuffer.allocate(1);

            int byteRead = client.read(tipoBuffer);

            if(byteRead == -1){
                System.out.println("Cliente desconectado: " + client.getRemoteAddress());
                client.close();
            }
            else if(byteRead > 0){
                tipoBuffer.flip();

                byte tipoMensagem = tipoBuffer.get();

                if(tipoMensagem == 1){
                    ByteBuffer tamanhoBuffer = ByteBuffer.allocate(4);
                    while (tamanhoBuffer.hasRemaining()) client.read(tamanhoBuffer);
                    tamanhoBuffer.flip();
                    int tamanho = tamanhoBuffer.getInt();

                    ByteBuffer textoBuffer = ByteBuffer.allocate(tamanho);
                    while (textoBuffer.hasRemaining()) client.read(textoBuffer);
                    textoBuffer.flip();

                    return StandardCharsets.UTF_8.decode(textoBuffer).toString();
                }
            }
        } catch (SocketException e){
            System.out.println("Conexão perdida com o usuário");
            return null;
        } catch (IOException e){
            e.printStackTrace();
        }


        return null;
    }

    public void enviarMensagem(String mensagem, SocketChannel client){
        try {
            byte[] textoBytes = mensagem.getBytes(StandardCharsets.UTF_8);
            ByteBuffer outBuffer = ByteBuffer.allocate(1 + 4 + textoBytes.length);
            outBuffer.put((byte) 1);
            outBuffer.putInt(textoBytes.length);
            outBuffer.put(textoBytes);

            outBuffer.flip();

            while(outBuffer.hasRemaining()) client.write(outBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Industria receberIndustria(SocketChannel client) throws IOException, ClassNotFoundException {
        ByteBuffer sizeBuffer = ByteBuffer.allocate(4);
        while (sizeBuffer.hasRemaining()){
            if(client.read(sizeBuffer) == -1) throw new IOException("Conexão encerrada.");
        }
        sizeBuffer.flip();
        int objectSize = sizeBuffer.getInt();

        ByteBuffer dataBuffer = ByteBuffer.allocate(objectSize);
        while (dataBuffer.hasRemaining()){
            if(client.read(dataBuffer) == -1) throw new IOException("Conexão interrompida.");
        }

        dataBuffer.flip();

        return (Industria) deserialize(dataBuffer.array());
    }

    public void enviarIndustria(Industria industria, SocketChannel client){
        try {
            byte [] objectBytes = serialize(industria);

            ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + objectBytes.length);
            buffer.put((byte) 2);
            buffer.putInt(objectBytes.length);
            buffer.put(objectBytes);
            buffer.flip();

            while (buffer.hasRemaining()){
                client.write(buffer);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public ServerSocketChannel getServer() {
        return server;
    }

    public void setServer(ServerSocketChannel server) {
        this.server = server;
    }

    public List<SocketChannel> getClients() {
        return clients;
    }

    public void setClients(List<SocketChannel> clients) {
        this.clients = clients;
    }

    public List<Industria> getIndustrias() {
        return industrias;
    }

    public void setIndustrias(List<Industria> industrias) {
        this.industrias = industrias;
    }

    @Override
    public void close() throws Exception{
        if(!clients.isEmpty()){
            for(SocketChannel c: clients){
                c.close();
            }
        }
        if(server.isOpen()){
            server.close();
        }
    }
}
