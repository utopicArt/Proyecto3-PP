package Server;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.rmi.UnknownHostException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * @author: Adrian Marin Alcala
 * @see:    Clase que guarda las SS y crea el video
 * @since:  03/11/2021
 */

public class ClaseRemota extends java.rmi.server.UnicastRemoteObject 
        implements InterfazRemota{
    
    private String statusInfo = "[X]No se pudo guardar la imagen";
    private final File folder;
    private final String directory;
    
    public ClaseRemota() throws java.rmi.RemoteException{
        directory = "C:\\rmi\\servidor2\\Screenshots\\";
        folder = new File(directory);
        if (!folder.isDirectory()) {
            new File(directory).mkdirs();
        }    
    }

    @Override
    public String decodeBase64ToImage(String imageString, String timeStamp) throws java.rmi.RemoteException {
        BufferedImage image = null;
        byte[] imageByte;
        
        try {
            Base64.Decoder decoder = Base64.getDecoder();
            imageByte = decoder.decode(imageString);
            try (ByteArrayInputStream bis = new ByteArrayInputStream(imageByte)) {
                image = ImageIO.read(bis);
            }finally{
                saveImage(image, timeStamp);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this.statusInfo;
    }

    private void saveImage(BufferedImage image, String timeStamp) throws RemoteException {
        try {
            //directory + date + ".jpg"
            ImageIO.write(image, "JPG", new File(directory + timeStamp));
        } catch (IOException e) {
            this.statusInfo = "[X]No se pudo guardar la imagen";
            System.err.println("Error al guardar: " + e.getMessage());
        } finally {
            this.statusInfo = "Imagen Guardada en el Servidor";
            System.out.println("Imagen Guardada en el Servidor");
        }
    }
    
    public static void main(String[] args) throws RemoteException, MalformedURLException {
        try{
            InterfazRemota mri = new ClaseRemota();
            try {
                java.rmi.Naming.rebind("//" + java.net.InetAddress.getLocalHost()
                        .getHostAddress() + ":1234/PruebaRMI", mri);
            } catch (java.net.UnknownHostException ex) {
                Logger.getLogger(ClaseRemota.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        } catch (UnknownHostException  e) {
            e.printStackTrace();
        }
        //System.exit(0);
    }    
}
