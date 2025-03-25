package org.AndrewP05.dto;

import java.io.Serializable;

public class MiDatagrama implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String emisor;
    private String mensaje;
    private byte[] archivoMp3; // Contenido del archivo .mp3

    public MiDatagrama(String emisor, String mensaje, byte[] archivoMp3) {
        this.emisor = emisor;
        this.mensaje = mensaje;
        this.archivoMp3 = archivoMp3;
    }

    public String getEmisor() {
        return emisor;
    }

    public String getMensaje() {
        return mensaje;
    }

    public byte[] getArchivoMp3() {
        return archivoMp3;
    }
}
