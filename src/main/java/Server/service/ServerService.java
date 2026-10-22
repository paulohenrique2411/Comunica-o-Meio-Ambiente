package Server.service;

import shared.entity.Industria;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import shared.entity.enums.NiveisPoluicao;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.*;

import static shared.Utils.SerializationUtils.deserialize;
import static shared.Utils.SerializationUtils.serialize;

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

    public boolean verificaRequisicao(SocketChannel client) throws ClassNotFoundException, IOException {

        String mensagem = receberMensagem(client);


        if (mensagem != null && mensagem.startsWith("Cadastrar")) {

            cadastrarIndustria(mensagem, client);
            return true;
        }else if (mensagem != null && mensagem.startsWith("Consultar")){
            consultarindustria(mensagem, client);
            return true;
        } else if (mensagem == null) {
            return false;
        }

        enviarMensagem("Não foi possivel indentificar o motivo da requisição", client);
        return true;
    }

    public void consultarindustria(String mensagem, SocketChannel client){
        String[] dados = mensagem.split(" ");
        Industria industria = obterIndustria(dados[1]);

        if (industria == null) {
            enviarMensagem("Não foi possivel achar uma industria com os dados fornecidos.", client);
        } else {
            File file = new File(diretorio + industria.getNomeImagem());
            industria.setImagem(file);
            enviarIndustria(industria, client);
        }
    }

    public void cadastrarIndustria(String mensagem, SocketChannel client) throws IOException {
        String regex = "Cadastrar Nome:(.*?) Local:(.*?) Detalhes:(.*?) NivelPoluição:(.*?) Imagem:(.*?)(?: DadosImagem:(.*))?";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(mensagem);

        if (matcher.find()) {
            Industria industria = new Industria();

            industria.setNome(matcher.group(1).trim());
            industria.setLocal(matcher.group(2).trim());
            industria.setDetalhes(matcher.group(3).trim());

            try {
                String nivelStr = matcher.group(4).trim().toUpperCase();
                industria.setNivelPoluicao(NiveisPoluicao.valueOf(nivelStr));
            } catch (IllegalArgumentException e) {
                enviarMensagem("Erro: Nível de poluição inválido.", client);
                return;
            }

            industria.setNomeImagem(matcher.group(5).trim());

            if(matcher.group(6) != null){
                byte[] imagem = Base64.getDecoder().decode(matcher.group(6).trim());
                String diretorioImagem = FileSystems.getDefault().getPath("").toAbsolutePath() + "\\src\\main\\java\\Server\\dados\\" + industria.getNomeImagem();

                File file = new File(diretorioImagem);

                Files.write(file.toPath(), imagem);

                industria.setImagem(file);
            }

            if (!industria.validarCampos()) {
                enviarMensagem("Não foi possivel realizar o cadastro, um dos campos estava vazio", client);
                return;
            }

            InserirIndustriaJson(industria);
            industrias.add(industria);
            enviarMensagem("Cadastro realizado com sucesso", client);
        } else {
            enviarMensagem("Erro: Formato de mensagem inválido. Preencha todos os campos corretamente.", client);
        }
    }

    public void InserirIndustriaJson(Industria industria){

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
                novaIndustria.put("nomeImagem", industria.getNomeImagem());

                listaIndustrias.add(novaIndustria);

                mapper.writeValue(file, listaIndustrias);

            }
            catch (IOException e){
            e.printStackTrace();
            }

    }

    public Industria obterIndustria(String nome){
        return industrias.stream().filter(c -> c.getNome().contains(nome)).findFirst().orElse(null);
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
