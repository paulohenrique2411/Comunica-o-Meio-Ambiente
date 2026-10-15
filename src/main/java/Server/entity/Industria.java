package Server.entity;

import Server.entity.enums.NiveisPoluicao;

import java.io.File;
import java.io.Serializable;

public class Industria implements Serializable {

    private String nome;
    private String local;
    private String detalhes;
    private NiveisPoluicao nivelPoluicao;
    private File imagem;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getLocal() {
        return local;
    }

    public void setLocal(String local) {
        this.local = local;
    }

    public String getDetalhes() {
        return detalhes;
    }

    public void setDetalhes(String detalhes) {
        this.detalhes = detalhes;
    }

    public NiveisPoluicao getNivelPoluicao() {
        return nivelPoluicao;
    }

    public void setNivelPoluicao(NiveisPoluicao nivelPoluicao) {
        this.nivelPoluicao = nivelPoluicao;
    }

    public File getImagem() {
        return imagem;
    }

    public void setImagem(File imagem) {
        this.imagem = imagem;
    }

    @Override
    public String toString() {
        return "Nome = " + nome + '\n' +
                "Local = " + local + '\n' +
                "Detalhes = " + detalhes + '\n' +
                "Nivel Poluicao = " + nivelPoluicao.toString() + '\n' +
                "imagem = " + imagem;
    }
}
