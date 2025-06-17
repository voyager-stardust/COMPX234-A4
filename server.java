import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
public class server
{
    public static void main(String[] args) throws IOException
    {
        DatagramSocket socket = new DatagramSocket(Integer.parseInt(args[0]));
        System.out.println("Server started on port " + args[0]);
        while (true)
        {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            System.out.println("Received: " + new String(packet.getData()));
        }
    }
}