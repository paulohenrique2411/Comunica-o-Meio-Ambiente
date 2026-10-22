package Server.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import shared.entity.Industria;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IndustriaRepository {

    private final String caminhoArquivo;
    private final ObjectMapper mapper;

    public IndustriaRepository(String caminhoArquivo) {
        this.caminhoArquivo = caminhoArquivo + "Informacoes.json";
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }


    public List<Industria> carregarIndustrias(){

        File file = new File(caminhoArquivo);

        if(!file.exists()) return new ArrayList<>();

        try{
            return mapper.readValue(file, new TypeReference<List<Industria>>() {});
        }catch(Exception e){
            return new ArrayList<>();
        }
    }

    public void salvarIndustria(Industria industria){
        File file = new File(caminhoArquivo);

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

}
