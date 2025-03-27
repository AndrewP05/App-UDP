package org.AndrewP05.servidor.gui;

import org.AndrewP05.dto.MiDatagrama;
import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servidor Multicast que recibe fragmentos de archivos y los reconstruye.
 */
public class PrincipalSrv extends JFrame {

    // Debe coincidir con el del cliente
    private static final String MULTICAST_ADDRESS = "230.0.0.0";
    private static final int PORT = 4446;
    
    private JTextArea mensajesTxt;
    private JButton bIniciar;
    
    // Estructura para almacenar fragmentos por archivo (clave: idArchivo)
    private Map<String, MiFragmento> archivosEnProceso = new HashMap<>();
    
    // Clase interna para almacenar los fragmentos de un archivo
    private class MiFragmento {
        int totalFragmentos;
        byte[][] fragmentos; // Índice 0 corresponde al fragmento 1
        int recibidos = 0;
        
        public MiFragmento(int total) {
            this.totalFragmentos = total;
            fragmentos = new byte[total][];
        }
        
        public void agregarFragmento(int numFragmento, byte[] datos) {
            if(fragmentos[numFragmento - 1] == null) {
                fragmentos[numFragmento - 1] = datos;
                recibidos++;
            }
        }
        
        public boolean completo() {
            return recibidos == totalFragmentos;
        }
        
        public byte[] reconstruir() throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for (byte[] frag : fragmentos) {
                baos.write(frag);
            }
            return baos.toByteArray();
        }
    }
    
    public PrincipalSrv() {
        initComponents();
        this.mensajesTxt.setEditable(false);
    }
    
    private void initComponents() {
        this.setTitle("Servidor UDP - Multicast");
        bIniciar = new JButton();
        JLabel jLabel1 = new JLabel();
        mensajesTxt = new JTextArea();
        JScrollPane jScrollPane1 = new JScrollPane();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(null);

        bIniciar.setFont(new java.awt.Font("Segoe UI", 0, 18));
        bIniciar.setText("INICIAR SERVIDOR");
        bIniciar.addActionListener(evt -> bIniciarActionPerformed(evt));
        getContentPane().add(bIniciar);
        bIniciar.setBounds(150, 50, 250, 40);

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 14));
        jLabel1.setForeground(new java.awt.Color(204, 0, 0));
        jLabel1.setText("SERVIDOR MULTICAST");
        getContentPane().add(jLabel1);
        jLabel1.setBounds(150, 10, 200, 17);

        mensajesTxt.setColumns(25);
        mensajesTxt.setRows(5);
        jScrollPane1.setViewportView(mensajesTxt);
        getContentPane().add(jScrollPane1);
        jScrollPane1.setBounds(20, 150, 500, 120);

        setSize(new java.awt.Dimension(570, 320));
        setLocationRelativeTo(null);
    }
    
    private void bIniciarActionPerformed(java.awt.event.ActionEvent evt) {
        iniciar();
    }
    
    public void iniciar(){
        mensajesTxt.append("Servidor Multicast iniciado en el puerto " + PORT + "\n");

        new Thread(() -> {
            MulticastSocket socketudp = null;
            try {
                // Usamos MulticastSocket para unirnos al mismo grupo del cliente
                socketudp = new MulticastSocket(PORT);
                InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
                socketudp.joinGroup(group);
                
                bIniciar.setEnabled(false);
                
                // Bucle infinito para recibir fragmentos
                while (true) {
                    mensajesTxt.append("Escuchando...\n");
                    
                    byte[] buf = new byte[65536];
                    DatagramPacket dp = new DatagramPacket(buf, buf.length);
                    socketudp.receive(dp);
                    
                    // Deserializar objeto MiDatagrama
                    ByteArrayInputStream bais = new ByteArrayInputStream(dp.getData(), 0, dp.getLength());
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    MiDatagrama datagrama = (MiDatagrama) ois.readObject();
                    
                    mensajesTxt.append("Recibido fragmento " + datagrama.getFragmentoActual() +
                            " de " + datagrama.getTotalFragmentos() +
                            " para el archivo " + datagrama.getIdArchivo() + "\n");
                    
                    // Procesar el fragmento: agruparlo por idArchivo
                    MiFragmento mf = archivosEnProceso.get(datagrama.getIdArchivo());
                    if(mf == null) {
                        mf = new MiFragmento(datagrama.getTotalFragmentos());
                        archivosEnProceso.put(datagrama.getIdArchivo(), mf);
                    }
                    mf.agregarFragmento(datagrama.getFragmentoActual(), datagrama.getArchivoFragmento());
                    
                    // Verificar si se han recibido todos los fragmentos
                    if(mf.completo()){
                        mensajesTxt.append("Archivo " + datagrama.getIdArchivo() + " recibido completamente.\n");
                        byte[] archivoCompleto = mf.reconstruir();
                        
                        // Si deseas que se guarde siempre con extensión .mp3, puedes:
                        // (OJO: si datagrama.getIdArchivo() ya tiene .mp3, no es necesario)
                        String nombreArchivo = datagrama.getIdArchivo();
                        // Fuerza extensión .mp3 (opcional):
                        // nombreArchivo = nombreArchivo + ".mp3";
                        
                        // Guardar en un directorio relativo
                        File directorio = new File(System.getProperty("user.dir"), "archivos_recibidos");
                        if (!directorio.exists()) {
                            if (directorio.mkdirs()) {
                                mensajesTxt.append("Directorio creado: " + directorio.getAbsolutePath() + "\n");
                            } else {
                                mensajesTxt.append("No se pudo crear el directorio: " + directorio.getAbsolutePath() + "\n");
                            }
                        }
                        
                        // Crear el archivo
                        File archivoGuardado = new File(directorio, nombreArchivo);
                        try (FileOutputStream fos = new FileOutputStream(archivoGuardado)) {
                            fos.write(archivoCompleto);
                            fos.flush(); // Asegurar escritura
                        }
                        mensajesTxt.append("Archivo guardado en: " + archivoGuardado.getAbsolutePath() + "\n");
                        
                        // Eliminar la entrada del mapa, ya que se completó la recepción
                        archivosEnProceso.remove(datagrama.getIdArchivo());
                    }
                }
            } catch (IOException | ClassNotFoundException ex) {
                Logger.getLogger(PrincipalSrv.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                if (socketudp != null && !socketudp.isClosed()) {
                    socketudp.close(); // Evita la advertencia de "Resource leak"
                }
            }
        }).start();
    }
    
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> {
            new PrincipalSrv().setVisible(true);
        });
    }
}
