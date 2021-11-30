package com.paralela.screenrecorder;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


/**
 * @author: Adrian Marin Alcala
 * @see:    Controlador de la toma, carga y reproduccion de video.
 */

public class ScreenshotController implements ActionListener {

    JFrame Master;
    JLabel imageContainer;
    JButton triggerBtn;
    JButton controllerBtn;

    private Robot robot;
    private Rectangle rec;

    public static int recordingSpeed;
    private static LinkedList<BufferedImage> screenShot;
    private static Queue<BufferedImage> screenShot2Save;
    private static Queue<String> screenshotTimeStamp;
    private BufferedImage screenshotTaken;
    private int proccessLevel = 0;

    private String date;
    private SimpleDateFormat formatter;
    private ThreadPoolExecutor takeSSThread;
    private ThreadPoolExecutor showThread;
    private ThreadPoolExecutor saveThread;

    private static boolean isRecording = false;
    private ImageIcon replayImage;
    
    private InterfazRemota mir;
    
    private String dataEncoded;
    private String serverSaveStatus;
    private String videoDirectory;

    public ScreenshotController(JFrame root, JLabel srcLbl, JButton actionBtn,
            JButton trigger) {
        this.Master = root;
        this.imageContainer = srcLbl;
        this.controllerBtn = actionBtn;
        this.triggerBtn = trigger;

        initVars();
        initRobot();
        initClient();
    }

    private void initRobot() {
        try {
            robot = new Robot();
        } catch (AWTException ex) {
            JOptionPane.showMessageDialog(null,
                    "Ocurrio un error:" + ex.getMessage(),
                    "Error al iniciar Robot", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initVars() {
        formatter = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss-SSS");

        recordingSpeed = 12;
        screenShot = new LinkedList<>();
        screenShot2Save = new LinkedList<>();
        screenshotTimeStamp = new LinkedList<>();
        
        takeSSThread = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        showThread = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
        saveThread = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

        rec = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        videoDirectory = System.getProperty("user.dir") + "\\Videos\\";
        File vD = new File(videoDirectory);
        if (!vD.isDirectory())
            new File(videoDirectory).mkdirs();
    }
    
    private void initClient(){
        try {
            mir = (InterfazRemota)java.rmi.Naming.lookup("//"
                    + "192.168.200.189:1234/PruebaRMI");
        }catch (MalformedURLException | NotBoundException | RemoteException e){
            System.out.println("Error, no encuentro: " + e.getMessage());
        }        
    }

    protected double getMemoryUsage() {
        return Runtime.getRuntime().totalMemory() / (1024.0 * 1024.0 * 1024.0);
    }

    public void takeScreenshotController() {
        while (isRecording) {
            try {
                takeScreenshot();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null,
                        "Ocurrio un error:" + ex.getMessage(),
                        "Error al tomar SS", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void takeScreenshot() throws IOException {
        if (getMemoryUsage() > 2.85) {
            System.gc();
            System.err.println(
                    "\n[!]Sistema de emergencia activado."
                    + "screenshotTimeStamp: " + screenshotTimeStamp.size()
                    + "screenShot: " + screenShot.size()
                    + "screenShot2Save: " + screenShot2Save.size());
        }
        screenshotTaken = robot.createScreenCapture(rec);
        date = formatter.format(Calendar.getInstance().getTime());
        screenshotTimeStamp.add(date + ".jpg");

        screenShot.add(screenshotTaken);
        screenShot2Save.add(screenshotTaken);
        System.out.println("SS tomada");
    }
    
    private void downloadVideo(){
        try {
            System.out.println("[!]Descargando video...");
            byte[] video = mir.downloadVideo(recordingSpeed);
            System.out.println("[./]Video Descargado: " + video.length);
            System.out.println("[./]Video Encodeado: " + mir.videoSize());
            File filePath = new File(videoDirectory);
            if(!filePath.isDirectory())
                new File(videoDirectory).mkdirs();
            try (FileOutputStream out = new FileOutputStream(mir.videoName())){
                out.write(video);
                out.flush();
            }            
        } catch (RemoteException ex) {
            System.out.println("Excepcion Remota: " + ex.getMessage());
        } catch (FileNotFoundException ex) {
            System.out.println("Excepcion Archivo no encontrado: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("Excepcion de Escritura: " + ex.getMessage());
        }
        System.out.println("[./]Video guardado correctamente.");
        controllerBtn.setEnabled(true);
    }

    public void saveScreenshotsController() {
        while (true) {
            saveScreenshots();
            if (proccessLevel == 2) {
                controllerBtn.setEnabled(false);
                if (screenShot2Save.isEmpty()) {
                    downloadVideo();
                    screenShot.clear();
                    screenShot2Save.clear();
                    proccessLevel = 0;
                    break;
                }
            }
        }
    }

    private void saveScreenshots() {
        System.out.print("");
        if (screenShot2Save.peek() != null && screenshotTimeStamp.peek() != null) {
            try {
                dataEncoded = encodeImageToString(screenShot2Save.poll());
                serverSaveStatus = mir.decodeBase64ToImage(dataEncoded,
                        screenshotTimeStamp.poll());
                System.out.println(serverSaveStatus);
            } catch (RemoteException ex) {
                Logger.getLogger(ScreenshotController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private String encodeImageToString(BufferedImage image) {
        String imageString = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try {
            ImageIO.write(image, "JPG", bos);
            byte[] imageBytes = bos.toByteArray();

            Base64.Encoder encoder = Base64.getEncoder();
            imageString = encoder.encodeToString(imageBytes);
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageString;
    }

    public void showVideoController() {
        while (true) {
            showVideo();
            if(!isRecording){
                break;
            }
        }
    }

    public void showVideo() {
        System.out.print("");
        if (screenShot.peek() != null) {
            try {
                //Esta variable forza al sistema
                replayImage = new ImageIcon(screenShot.poll());
                imageContainer.setIcon(replayImage);
                Thread.sleep((long) 16.6666667);
            } catch(InterruptedException ex) {
                System.out.println("Ocurrio una interrupcion al mostrar: " 
                        + ex.getMessage());
            }
        }
    }

    private void submitTasks() throws InterruptedException {
        takeSSThread = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        showThread = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
        saveThread = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        
        for (int i = 0; i < 10; i++) {
            takeSSThread.submit(() -> {
                takeScreenshotController();
                return 0;
            });
        }
        
        for (int i = 0; i < 5; i++) {
            showThread.submit(() -> {
                showVideoController();
                return 0;
            });
        }

        saveThread.submit(() -> {
            saveScreenshotsController();
            return 0;
        });
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        controllerBtn.setText((isRecording ? "Iniciar Grabación" : "Detener Grabación"));
        isRecording = !isRecording;

        proccessLevel++;

        triggerBtn.setEnabled(true);
        triggerBtn.setBackground((isRecording
                ? new Color(230, 20, 20) : new Color(189, 189, 189)));
        triggerBtn.setEnabled(false);
        
        if(isRecording){
            System.out.println("Creando tareas..");
            try {
                submitTasks();
            } catch (InterruptedException ex) {
                Logger.getLogger(ScreenshotController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        // Al inicia pesa 248.046875mb
        System.out.println(getMemoryUsage() + "gb");
    }
}
