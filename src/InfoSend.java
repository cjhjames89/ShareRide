import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

public class InfoSend {
    private Socket socket = null;
    private ObjectInputStream inputStream = null;
    private ObjectOutputStream outputStream = null;
    private boolean isConnected = false;

    public InfoSend() {

    }

    public void sendParams(String ip,int port,ParamData sentData) {

        while (!isConnected) {
            try {
                socket = new Socket(ip, port);
                System.out.println("Connected");
                isConnected = true;
                outputStream = new ObjectOutputStream(socket.getOutputStream());
                System.out.println("Object to be written = " + sentData);
                outputStream.writeObject(sentData);
                System.out.println("Data Sent");
                socket.close();
            } catch (SocketException se) {
                se.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
