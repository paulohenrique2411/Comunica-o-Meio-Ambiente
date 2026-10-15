package Server;
import Server.service.ServerService;

public class application {

    public static void main(String[] args) {

        ServerService sv = new ServerService("C:\\Users\\paulo.dahsilva\\IdeaProjects\\Comunicacao-Meio-Ambiente\\src\\main\\java\\Server\\dados\\Informacoes.json");

        sv.carregarIndustrias();

        sv.run();

        System.out.println(sv.getIndustrias().size());

    }

}
