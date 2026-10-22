package Client;

import Client.service.ClientService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


public class application extends Application {

    @Override
    public void start(Stage primaryStage){

        ClientService clientService = new ClientService(8080);

        if(!clientService.isConected()) return;


        Label label = new Label();

        clientService.escutarServidor(label);

        TextField campoTexto = new TextField();

        campoTexto.setPromptText("Digite a mensagem aqui...");
        VBox layout = new VBox(10);

        Button bt = new Button();

        bt.setText("Enviar");
        bt.setOnAction(event -> {label.setText(label.getText() + "\n Você: " + campoTexto.getText());
                                             clientService.enviarMensagem(campoTexto.getText());
                                             campoTexto.clear();});

        layout.getChildren().add(label);
        layout.getChildren().add(campoTexto);
        layout.getChildren().add(bt);

        Scene scene = new Scene(layout,300,200);

        primaryStage.setTitle("Chat Secretária Meio Ambiente");
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    public static void main(String[] args) {

        launch(args);


    }

}
