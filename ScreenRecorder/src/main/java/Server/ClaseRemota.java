package Server;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.rmi.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.media.MediaLocator;

/**
 * @author: Adrian Marin Alcala
 * @see:    Clase que guarda las SS, crea y envia el video
 * @since:  03/11/2021
 */

public class ClaseRemota extends java.rmi.server.UnicastRemoteObject 
        implements InterfazRemota{
    
    private SimpleDateFormat formatter;
    
    private String statusInfo = "[X]No se pudo guardar la imagen";
    private File folder;
    private File videoFolder;
    private String directory;
    private String videoDirectory;
    private String outputFile;
    private boolean videoWasCreated = false;
    private int videoEncodedSize = 0;
    
    static JpegImagesToMovie imageToMovie;
    
    public ClaseRemota() throws java.rmi.RemoteException{
        initVars();
    }
    
    protected final void initVars(){
        imageToMovie = new JpegImagesToMovie();
        formatter = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
        directory = "C:\\rmi\\servidor2\\Screenshots\\";
        videoDirectory = "C:\\rmi\\servidor2\\Videos\\";
        folder = new File(directory);
        videoFolder = new File(videoDirectory);
        
        if (!folder.isDirectory())
            new File(directory).mkdirs();
        if (!videoFolder.isDirectory())
            new File(videoDirectory).mkdirs();
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
    
    private void createVideo(int recordingSpeed) {
        if (folder.listFiles().length > 1) {
            System.out.println("[CREANDO VIDEO]");
            File[] listOfFiles = folder.listFiles();

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int screenWidth = (int) screenSize.getWidth();
            int screenHeight = (int) screenSize.getHeight();

            outputFile = formatter.format(Calendar.getInstance()
                    .getTime()) + ".mp4";

            ArrayList<String> imgLst = new ArrayList<>();

            System.out.println("Obteniendo imagenes de: " + this.directory);
            System.out.println("Imagenes encontradas: " + listOfFiles.length);

            for (File listOfFile : listOfFiles) {
                imgLst.add(listOfFile.getAbsolutePath());
            }
            imgLst.forEach((name) -> {
                System.out.println("Procesando: " + name);
            });

            MediaLocator oml;
            if ((oml = JpegImagesToMovie.createMediaLocator(outputFile)) == null) {
                System.err.println("No se puede construir media locator de: " + outputFile);
                System.exit(0);
            }

            videoWasCreated = imageToMovie.doIt(screenWidth, screenHeight,
                    recordingSpeed, imgLst, oml);
        } else {
            System.out.println("Error al crear video:" 
                    + "Hay muy pocas imágenes en el directorio");
        }
    }

    @Override
    public byte[] downloadVideo(int recordingSpeed) throws RemoteException {
        createVideo(recordingSpeed);
        
        new Thread(() -> {
            while (!videoWasCreated) {
                try {
                    System.out.println("[!]El video se esta creando");
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    System.out.println("Hilo interrumpido");
                }
            }
        }).start();
        
        byte[] videoEncoded;
        File filePath = new File(outputFile);
        videoEncoded = new byte[(int) filePath.length()];
        FileInputStream in;
        try {
            in = new FileInputStream(filePath);
            try {
                in.read(videoEncoded, 0, videoEncoded.length);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    in.close();
                } catch (IOException ex) {
                    Logger.getLogger(ClaseRemota.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("[./]El Cliente descargó el video");
        videoEncodedSize = videoEncoded.length;
        return videoEncoded;
    }

    @Override
    public int videoSize() throws RemoteException {
        return videoEncodedSize;
    }

    @Override
    public String videoName() throws RemoteException {
        return outputFile;
    }
}
