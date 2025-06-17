import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Base64;
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
            String request = new String(packet.getData(), 0, packet.getLength()).trim();
            if (request.startsWith("DOWNLOAD"))
            {
                String filename = request.substring(9).trim();
                File file = new File(filename);
                if (file.exists())
                {
                    String response = "OK " + filename;
                    sendResponse(packet.getAddress(), packet.getPort(), response);
                }
                else
                {
                    String response = "ERR " + filename + " NOT_FOUND";
                    sendResponse(packet.getAddress(), packet.getPort(), response);
                }
            }
            else if (request.startsWith("FILE") && request.contains("GET"))
            {
                String[] parts = request.split(" ");
                long start = Long.parseLong(parts[4]);
                long end = Long.parseLong(parts[6]);
                try (RandomAccessFile file = new RandomAccessFile(parts[1], "r"))
                {
                    byte[] data = new byte[(int)(end-start+1)];
                    file.seek(start);
                    file.read(data);
                    String response = "FILE " + parts[1] + " DATA " + Base64.getEncoder().encodeToString(data);
                    sendResponse(packet.getAddress(), packet.getPort(), response);
                }
            }
            System.out.println("Received: " + new String(packet.getData()));
        }
    }
    private static void sendResponse(InetAddress address, int port, String message) throws IOException
    {
        try (DatagramSocket tempSocket = new DatagramSocket())
        {
            byte[] data = message.getBytes();
            DatagramPacket packet=new DatagramPacket(data,data.length,address,port);
            tempSocket.send(packet);
        }
    }
}
