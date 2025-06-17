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
    private static String Retry(DatagramSocket socket, InetAddress address,int port,String message) throws IOException
    {
        byte[] sendData = message.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
        for (int i = 0; i < 3; i++)
        {
            socket.send(sendPacket);
            socket.setSoTimeout(1000 * (i+1));
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);
            return new String(receivePacket.getData(), 0, receivePacket.getLength());
        }
        return null;
    }
    private static void showProgress(long bytesReceived, long fileSize)
    {
        System.out.print("\rDownloading: [");
        int progress = (int)((double)bytesReceived/fileSize * 50);
        for (int i = 0; i < 50; i++)
        {
            System.out.print(i < progress ? "=" : " ");
        }
        System.out.printf("] %d%%", (int)((double)bytesReceived/fileSize * 100));
    }
}