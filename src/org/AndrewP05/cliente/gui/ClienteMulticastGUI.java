package org.AndrewP05.cliente.gui;

import org.AndrewP05.dto.MiDatagrama;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ClienteMulticastGUI extends JFrame {

    private static final String MULTICAST_ADDRESS = "230.0.0.0";
    private static final int PORT = 4446;
    
    private JTextField nombreField;
    private JTextField mensajeField;
    private JTextArea mensajesArea;
    private JButton enviarBtn;
    private JButton seleccionarArchivoBtn;
    private File archivoMp3; // archivo seleccionado
    
    private MulticastSocket multicastSocket;
    private InetAddress group;
    
    public ClienteMulticastGUI() {
        initComponents();
        iniciarReceptor(); // Iniciar el hilo que escucha mensajes multicast
    }
    
    private void initComponents() {
        setTitle("Cliente Multicast");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Panel para datos de envío
        JPanel panelEnvio = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        
        // Nombre
        gbc.gridx = 0;
        gbc.gridy = 0;
        panelEnvio.add(new JLabel("Nombre:"), gbc);
        nombreField = new JTextField(20);
        gbc.gridx = 1;
        panelEnvio.add(nombreField, gbc);
        
        // Mensaje
        gbc.gridx = 0;
        gbc.gridy = 1;
        panelEnvio.add(new JLabel("Mensaje:"), gbc);
        mensajeField = new JTextField(20);
        gbc.gridx = 1;
        panelEnvio.add(mensajeField, gbc);
        
        // Botón para seleccionar archivo .mp3
        gbc.gridx = 0;
        gbc.gridy = 2;
        panelEnvio.add(new JLabel("Archivo .mp3:"), gbc);
        seleccionarArchivoBtn = new JButton("Seleccionar Archivo");
        seleccionarArchivoBtn.addActionListener(this::seleccionarArchivoAction);
        gbc.gridx = 1;
        panelEnvio.add(seleccionarArchivoBtn, gbc);
        
        // Botón de enviar
        enviarBtn = new JButton("Enviar Mensaje");
        enviarBtn.addActionListener(this::enviarMensajeAction);
        gbc.gridx = 1;
        gbc.gridy = 3;
        panelEnvio.add(enviarBtn, gbc);
        
        // Área de mensajes recibidos
        mensajesArea = new JTextArea(10, 40);
        mensajesArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(mensajesArea);
        
        // Agregar paneles al frame
        setLayout(new BorderLayout());
        add(panelEnvio, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private void seleccionarArchivoAction(ActionEvent evt) {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("MP3 Files", "mp3");
        fileChooser.setFileFilter(filter);
        int returnVal = fileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            archivoMp3 = fileChooser.getSelectedFile();
            mensajesArea.append("Archivo seleccionado: " + archivoMp3.getAbsolutePath() + "\n");
        }
    }
    
    private void enviarMensajeAction(ActionEvent evt) {
        String nombre = nombreField.getText().trim();
        String mensaje = mensajeField.getText().trim();
        
        if(nombre.isEmpty() || mensaje.isEmpty() || archivoMp3 == null) {
            JOptionPane.showMessageDialog(this, "Debe ingresar nombre, mensaje y seleccionar un archivo .mp3", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            // Leer archivo .mp3
            byte[] archivoBytes = new byte[(int) archivoMp3.length()];
            try (FileInputStream fis = new FileInputStream(archivoMp3)) {
                fis.read(archivoBytes);
            }
            
            // Crear el objeto datagrama
            MiDatagrama datagrama = new MiDatagrama(nombre, mensaje, archivoBytes);
            
            // Serializar el objeto
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(datagrama);
            oos.flush();
            byte[] buffer = baos.toByteArray();
            
            // Crear y enviar el paquete multicast
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            multicastSocket.send(packet);
            
            mensajesArea.append("Mensaje enviado al grupo multicast.\n");
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al enviar mensaje: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void iniciarReceptor() {
        try {
            multicastSocket = new MulticastSocket(PORT);
            group = InetAddress.getByName(MULTICAST_ADDRESS);
            multicastSocket.joinGroup(group);
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al iniciar receptor multicast: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Hilo para recibir mensajes multicast
        Thread receptorThread = new Thread(() -> {
            while (true) {
                try {
                    byte[] buf = new byte[65536];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    multicastSocket.receive(packet);
                    
                    // Deserializar objeto recibido
                    ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    MiDatagrama datagrama = (MiDatagrama) ois.readObject();
                    
                    // Filtrar mensajes que sean enviados por el mismo usuario
                    // Se compara el nombre enviado en el datagrama con el ingresado en la interfaz
                    if(datagrama.getEmisor().equals(nombreField.getText().trim())) {
                        continue;
                    }
                    
                    // Actualizar la ventana de mensajes (ejecutar en el hilo de Swing)
                    SwingUtilities.invokeLater(() -> {
                        mensajesArea.append("Mensaje recibido:\n");
                        mensajesArea.append("Emisor: " + datagrama.getEmisor() + "\n");
                        mensajesArea.append("Mensaje: " + datagrama.getMensaje() + "\n");
                        // Se puede guardar el archivo o reproducirlo según la lógica de la aplicación
                        mensajesArea.append("Archivo .mp3 recibido (tamaño: " + datagrama.getArchivoMp3().length + " bytes)\n\n");
                    });
                    
                } catch (IOException | ClassNotFoundException ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        mensajesArea.append("Error al recibir mensaje: " + ex.getMessage() + "\n");
                    });
                }
            }
        });
        receptorThread.start();
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ClienteMulticastGUI().setVisible(true);
        });
    }
}
