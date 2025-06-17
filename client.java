import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
public class client
{
    public static void main(String[] args) throws IOException
    {
        DatagramSocket socket = new DatagramSocket();
        InetAddress address = InetAddress.getByName(args[0]);
        String message = "Hi";
        DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), address, Integer.parseInt(args[1]));
        socket.send(packet);
    }
}