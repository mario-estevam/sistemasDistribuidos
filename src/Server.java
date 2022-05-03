import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {

    private ArrayList<ConncetionHanlder> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;

    public Server(){
        connections = new ArrayList<>();
        done = false;
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while(!done){
                Socket client = server.accept();
                ConncetionHanlder handler = new ConncetionHanlder(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (IOException e) {
            shutdown();
        }
    }

    public void broadcast(String message){
        for(ConncetionHanlder ch : connections){
            if(ch != null){
                ch.sendMessage(message);
            }
        }
    }


    public void shutdown(){

        try {

            done = true;
            pool.shutdown();
            if(!server.isClosed()){
                server.close();
            }
            for(ConncetionHanlder ch : connections){
                ch.shutdown();
            }

        } catch (IOException e) {
            //ignore
        }
    }

    class  ConncetionHanlder implements Runnable{
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String name;

        public ConncetionHanlder(Socket client){
            this.client = client;
        }



        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader((new InputStreamReader(client.getInputStream())));
                out.println("Por favor insira seu nome:");
                name = in.readLine();
                broadcast(name+" Entrou no chat!");
                String message;
                while ((message = in.readLine())!=null){
                    if(message.startsWith("/nick")){
                        String[] messageSplit = message.split(" ",2);
                                if(messageSplit.length==2){
                                    broadcast(name+ " mudou o nome para " + messageSplit[1]);
                                    System.out.println(name + " mudou o nome para "+ messageSplit[1]);
                                    name = messageSplit[1];
                                    out.println("nome alterado "+ name);
                                }else{
                                    out.println("Nenhum nome fornecido");
                                }

                    }
                   else if(message.startsWith("/quit")){
                        broadcast(name+ " saiu do chat");
                        System.out.println(name + "saiu do chat");
                        shutdown();
                    }else{
                        broadcast(name+": "+ message);
                    }
                }
            }catch (IOException e){
                shutdown();
            }
        }

        public void sendMessage(String message){
            out.println(message);
        }

        public void shutdown(){
            try {
                broadcast(name+ " saiu do chat");
                System.out.println(name + "saiu do chat");
                in.close();
                out.close();
                if(!client.isClosed()){
                    client.close();
                }
            } catch (IOException e) {
                shutdown();
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }

}
