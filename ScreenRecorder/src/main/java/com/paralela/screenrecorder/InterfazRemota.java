package com.paralela.screenrecorder;

/**
 * @author Adrian Marin Alcala
 * Desc: Interfaz remota.
 */
public interface InterfazRemota extends java.rmi.Remote{
    public String decodeBase64ToImage(String imageString, String timeStamp) throws java.rmi.RemoteException; 
    public byte[] downloadVideo(int recordingSpeed) throws java.rmi.RemoteException;
    public int videoSize() throws java.rmi.RemoteException;
    public String videoName() throws java.rmi.RemoteException;
}
