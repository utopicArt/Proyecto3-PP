package Server;

/**
 * @author Adrian Marin Alcala
 * Desc: Interfaz remota.
 */
public interface InterfazRemota extends java.rmi.Remote{
    public String decodeBase64ToImage(String imageString, String timeStamp) throws java.rmi.RemoteException;
    //public void saveImage(BufferedImage image, String timeStamp) throws java.rmi.RemoteException;
}
