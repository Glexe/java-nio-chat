package zad1;

public class TestMain {
    public static void main(String[] args) throws InterruptedException {
        ChatServer server = new ChatServer("localhost",8000);
        ChatClient client1 = new ChatClient("localhost",8000,"Asia");
        ChatClient client2 = new ChatClient("localhost",8000,"Sara");
        ChatClient client3 = new ChatClient("localhost",8000,"Adam");
        server.startServer();
        client1.login();
        client2.login();
        client3.login();
        client1.send("hello");
        client2.send("hello2");
        client3.send("hello3");
        client1.logout();
        client2.logout();
        client3.logout();
        Thread.sleep(800);
        System.out.println(client1.getChatView());
        Thread.sleep(800);
        server.stopServer();
        System.out.println(server.getServerLog());
    }
}
