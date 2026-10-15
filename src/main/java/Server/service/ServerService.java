package Server.service;

import Server.entity.Industria;
import Server.entity.enums.NiveisPoluicao;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ServerService implements AutoCloseable {

    private String diretorio;
    private static int PORTA = 8080;
    private Selector selector;
    private ServerSocketChannel server;
    private List<SocketChannel> clients = new ArrayList<>();
    private List<Industria>    industrias = new ArrayList<>();

    public ServerService(String diretorio){
        this.diretorio = diretorio;
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
    }

    public void carregarIndustrias(){
        try(FileReader file = new FileReader(diretorio)){

            ObjectMapper mapper = new ObjectMapper();

            industrias = mapper.readValue(file, new TypeReference<List<Industria>>() {});


        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch(IOException e){
            e.printStackTrace();
        }
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
                            "Cadastrar {nome da industria} {local} {detalhes} \n " +
                            "{NivelPoluicao digite Baixo ou MEDIO ou ALTO} {imagem diretório (opcional)}";

                    enviarMensagem(mensagem,clientChannel);

                    }else if(key.isReadable()) {

                        SocketChannel clientChannel = (SocketChannel) key.channel();

                        String mensagem = receberMensagem(clientChannel);

                        verificaRequisicao(mensagem, clientChannel);
                    }

                }

                Thread.sleep(1000);
            }
        } catch (IOException e){
            e.printStackTrace();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    public void verificaRequisicao(String mensagem, SocketChannel client){

        if(mensagem.contains("Consultar")){
            String[] nome = mensagem.split(" ");
            Industria  industria = consultaIndustria(nome[1]);
            if(industria.equals(null)){
                enviarMensagem("Não foi possivel achar uma industria com os dados fornecidos.", client);
            }else{
                enviarMensagem(industria.toString(), client);
            }
        } else if (mensagem.contains("Cadastrar")) {
            String[] dados = mensagem.split(" ");
            Industria novaIndustria = new Industria();
            novaIndustria.setNome(dados[1]);
            novaIndustria.setLocal(dados[2]);
            novaIndustria.setDetalhes(dados[3]);
            novaIndustria.setNivelPoluicao(NiveisPoluicao.valueOf(dados[4].toUpperCase()));
            cadastrarIndustria(novaIndustria);
            enviarMensagem("Cadastro realizado com sucesso", client);
        }

    }

    public void cadastrarIndustria(Industria industria){

            File file = new File(diretorio);

            ObjectMapper mapper = new ObjectMapper();

            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            try{
                ArrayNode listaIndustrias;

                if(file.exists() && file.length() > 0){
                    listaIndustrias = (ArrayNode) mapper.readTree(file);
                }else{
                    listaIndustrias = mapper.createArrayNode();
                }
                ObjectNode novaIndustria = mapper.createObjectNode();

                novaIndustria.put("nome", industria.getNome());
                novaIndustria.put("local", industria.getLocal());
                novaIndustria.put("detalhes", industria.getDetalhes());
                novaIndustria.put("nivelPoluicao", industria.getNivelPoluicao().ordinal());
                novaIndustria.put("imagem", "vazio");

                listaIndustrias.add(novaIndustria);

                mapper.writeValue(file, listaIndustrias);

            }
            catch (IOException e){
            e.printStackTrace();
            }

    }

    public Industria consultaIndustria(String nome){
        return industrias.stream().filter(c -> c.getNome().contains(nome)).findFirst().orElse(null);
    }
    public String receberMensagem(SocketChannel client) throws IOException {
        try{
            ByteBuffer responseBuffer = ByteBuffer.allocate(1024);

            int byteRead = client.read(responseBuffer);

            if(byteRead == -1){
                System.out.println("Cliente desconectado: " + client.getRemoteAddress());
                client.close();
            }
            else if(byteRead > 0){
                responseBuffer.flip();
                String mensagem = StandardCharsets.UTF_8.decode(responseBuffer).toString();
                return mensagem;
            }


        }catch (IOException e){
            e.printStackTrace();
                client.close();
        }

        return null;
    }

    public void enviarMensagem(String mensagem, SocketChannel client){
        try {
            ByteBuffer outBuffer = ByteBuffer.wrap((mensagem).getBytes());
            client.write(outBuffer);
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
