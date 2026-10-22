package Server;
import Server.service.ServerService;

public class ServerApplication {

    public static void main(String[] args) {

        ServerService sv = new ServerService(System.getProperty("user.dir") + "\\src\\main\\java\\Server\\dados\\");

        sv.run();

        System.out.println(sv.getIndustrias().size());

    }

}
