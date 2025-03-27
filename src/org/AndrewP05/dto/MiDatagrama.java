package org.AndrewP05.dto;

import java.io.Serializable;

public class MiDatagrama implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String emisor;
    private String mensaje;
    
    // Campos para la fragmentación del archivo
    private String idArchivo;         // Identificador único del archivo
    private int fragmentoActual;      // Número de fragmento actual (1-indexado)
    private int totalFragmentos;      // Total de fragmentos que componen el archivo
    
    private byte[] archivoFragmento;  // Contenido del fragmento del archivo .mp3

    public MiDatagrama(String emisor, String mensaje, String idArchivo, int fragmentoActual, int totalFragmentos, byte[] archivoFragmento) {
        this.emisor = emisor;
        this.mensaje = mensaje;
        this.idArchivo = idArchivo;
        this.fragmentoActual = fragmentoActual;
        this.totalFragmentos = totalFragmentos;
        this.archivoFragmento = archivoFragmento;
    }

    public String getEmisor() {
        return emisor;
    }

    public String getMensaje() {
        return mensaje;
    }
    
    public String getIdArchivo() {
        return idArchivo;
    }

    public int getFragmentoActual() {
        return fragmentoActual;
    }

    public int getTotalFragmentos() {
        return totalFragmentos;
    }

    public byte[] getArchivoFragmento() {
        return archivoFragmento;
    }
}
