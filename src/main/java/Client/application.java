package Client;

import Client.service.ClientService;

public class application {
    public static void main(String[] args) {

        ClientService clientService = new ClientService(8080);

        clientService.run();

    }

}
