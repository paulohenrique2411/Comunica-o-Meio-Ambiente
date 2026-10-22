package Server.service;

import Server.repository.IndustriaRepository;
import shared.entity.Industria;
import shared.entity.enums.NiveisPoluicao;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;

public class ProcessarComandos {
    private final IndustriaRepository repository;
    private List<Industria> industrias;
    private String diretorio;

    public ProcessarComandos(IndustriaRepository repository, String diretorio, List<Industria> industrias){
        this.industrias = industrias;
        this.repository = repository;
        this.diretorio = diretorio;
    }

    public String processar(String mensagem, SocketChannel client){
        if(mensagem.startsWith("Cadastrar")){
            return executarCadastro(mensagem);
        } else if (mensagem.startsWith("Consultar")) {
            return executarConsulta(mensagem);
        }
        return "Comando desconhecido.";
    }

    private String executarCadastro(String mensagem){
        String regex = "Cadastrar Nome:(.*?) Local:(.*?) Detalhes:(.*?) NivelPoluição:(.*?) Imagem:(.*?)(?: DadosImagem:(.*))?$";
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
                return "Erro: Nível de poluição inválido." ;

            }

            industria.setNomeImagem(matcher.group(5).trim());

            if(matcher.group(6) != null){
                byte[] imagem = Base64.getDecoder().decode(matcher.group(6).trim());
                String diretorioImagem = FileSystems.getDefault().getPath("").toAbsolutePath() + "\\src\\main\\java\\Server\\dados\\" + industria.getNomeImagem();

                File file = new File(diretorioImagem);

                try {
                    Files.write(file.toPath(), imagem);
                } catch (IOException e) {
                    return "Não foi possivel carregar a imagem da origem";
                }

                industria.setImagem(file);
            }

            if (!industria.validarCampos()) {
                return "Não foi possivel realizar o cadastro, um dos campos estava vazio";
            }

            repository.salvarIndustria(industria);
            industrias.add(industria);
            return "Cadastro realizado com sucesso";
        } else {
            return "Erro: Formato de mensagem inválido. Preencha todos os campos corretamente.";
        }
    }

    private String executarConsulta(String mensagem){
        String[] dados = mensagem.split(" ");
        Industria industria = obterIndustria(dados[1]);

        if (industria == null) {
            return "Não foi possivel achar uma industria com os dados fornecidos.";
        } else {
            File file = new File(diretorio + industria.getNomeImagem());
            industria.setImagem(file);
            return industria.toString();
        }
    }

    public Industria obterIndustria(String nome){
        return industrias.stream().filter(c -> c.getNome().contains(nome)).findFirst().orElse(null);
    }
}
