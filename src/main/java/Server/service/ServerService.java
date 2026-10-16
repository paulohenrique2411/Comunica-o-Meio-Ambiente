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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ServerService implements AutoCloseable {

    private String arquivoInfo = "Informacoes.json";
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
        try(FileReader file = new FileReader(diretorio + arquivoInfo)){

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
                            "Cadastrar Nome:{Instituição} Local:{local} Detalhes:{detalhes} NivelPoluição:{nivelpoluição} Imagem:{imagem}";

                    enviarMensagem(mensagem,clientChannel);

                    }else if(key.isReadable()) {

                        SocketChannel clientChannel = (SocketChannel) key.channel();

                        verificaRequisicao(clientChannel);
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

    public void verificaRequisicao(SocketChannel client) throws ClassNotFoundException, IOException {


        try{
            String mensagem = receberMensagem(client);
            String[] nome = mensagem.split(" ");
            Industria industria = consultaIndustria(nome[1]);
            File file = new File(diretorio + industria.getNomeImagem());
            industria.setImagem(file);

            if (industria.equals(null)) {
                enviarMensagem("Não foi possivel achar uma industria com os dados fornecidos.", client);
            } else {
                enviarIndustria(industria, client);
            }
        }catch (IOException e){
            Industria industria = receberIndustria(client);

            if (industria.validarCampos()){
                enviarMensagem("Não foi possivel realizar o cadastro, um dos campos estava vazio", client);
            }
            cadastrarIndustria(industria);
            enviarMensagem("Cadastro realizado com sucesso", client);
        }

    }

    public String obterValor(String valor, String mensagem){
        if(mensagem.contains(valor)){
            int index = mensagem.indexOf(valor);
            return mensagem.substring(index+valor.length()+1, (mensagem.indexOf(" ", index) == -1)? mensagem.length() : mensagem.indexOf(" ", index) );
        }
        return "";
    }

    public void cadastrarIndustria(Industria industria){

            File file = new File(diretorio + arquivoInfo);

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
                novaIndustria.put("imagem", industria.getNomeImagem());

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


        return "";
    }

    public void enviarMensagem(String mensagem, SocketChannel client){
        try {
            ByteBuffer outBuffer = ByteBuffer.wrap((mensagem).getBytes());
            client.write(outBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Industria receberIndustria(SocketChannel client) throws IOException, ClassNotFoundException {

        InputStream inStream = Channels.newInputStream(client);
        ObjectInputStream in = new ObjectInputStream(inStream);

        return (Industria) in.readObject();
    }

    public void enviarIndustria(Industria industria, SocketChannel client){
        try {
            OutputStream outputStream = Channels.newOutputStream(client);
            ObjectOutputStream out = new ObjectOutputStream(outputStream);

            out.writeObject(industria);
            out.flush();
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
