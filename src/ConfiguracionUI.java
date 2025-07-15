import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;

public class ConfiguracionUI {

    private static final String CONFIG_PATH = "config.txt";
    private static final String SIMBOLOS_PATH = "simbolos.txt";
    private static final String NUMEROS_PATH = "numeros.txt";

    public static void mostrar() {
        JFrame frame = new JFrame("Configuración");
        frame.setSize(600, 600);
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null);

        JPanel opcionesPanel = new JPanel(new GridLayout(0, 2, 10, 10));

        JCheckBox chkDecimales = new JCheckBox("Permitir Decimales");
        JCheckBox chkNegativos = new JCheckBox("Permitir Números Negativos");
        JCheckBox chkParentesis = new JCheckBox("Permitir Paréntesis");
        JCheckBox chkDespejarX = new JCheckBox("Incluir ejercicios con incógnita X");

        JCheckBox chkSuma = new JCheckBox("Suma");
        JCheckBox chkResta = new JCheckBox("Resta");
        JCheckBox chkMultiplicacion = new JCheckBox("Multiplicación");
        JCheckBox chkDivision = new JCheckBox("División");

        JTextField txtCantidadOperaciones = new JTextField();

        JTextArea areaNumeros = new JTextArea(10, 30);
        JScrollPane scrollNumeros = new JScrollPane(areaNumeros);

        // Cargar configuración actual
        Map<String, String> config = leerArchivoComoMapa(CONFIG_PATH);
        chkDecimales.setSelected("SI".equalsIgnoreCase(config.getOrDefault("DECIMALES", "NO")));
        chkNegativos.setSelected("SI".equalsIgnoreCase(config.getOrDefault("NUMEROS NEGATIVOS", "NO")));
        chkParentesis.setSelected("SI".equalsIgnoreCase(config.getOrDefault("PARENTESIS", "NO")));
        chkDespejarX.setSelected("SI".equalsIgnoreCase(config.getOrDefault("DESPEJAR_X", "NO")));

        txtCantidadOperaciones.setText(config.getOrDefault("OPERACIONES POR EJERCICIO", "2"));

        Map<String, String> simbolos = leerArchivoComoMapa(SIMBOLOS_PATH);
        chkSuma.setSelected("SI".equalsIgnoreCase(simbolos.getOrDefault("SUMA", "NO")));
        chkResta.setSelected("SI".equalsIgnoreCase(simbolos.getOrDefault("RESTA", "NO")));
        chkMultiplicacion.setSelected("SI".equalsIgnoreCase(simbolos.getOrDefault("MULTIPLICACION", "NO")));
        chkDivision.setSelected("SI".equalsIgnoreCase(simbolos.getOrDefault("DIVISION", "NO")));

        String textoNumeros = leerArchivoTexto(NUMEROS_PATH).replaceAll("\\R+", ", ");
        areaNumeros.setText(textoNumeros.trim());

        // Añadir al panel
        opcionesPanel.add(chkDecimales);
        opcionesPanel.add(chkNegativos);
        opcionesPanel.add(chkParentesis);
        opcionesPanel.add(chkDespejarX);

        opcionesPanel.add(new JLabel("Operaciones permitidas:", JLabel.CENTER));
        opcionesPanel.add(new JLabel(""));
        opcionesPanel.add(chkSuma);
        opcionesPanel.add(chkResta);
        opcionesPanel.add(chkMultiplicacion);
        opcionesPanel.add(chkDivision);
        opcionesPanel.add(new JLabel("Cantidad de operaciones por ejercicio:"));
        opcionesPanel.add(txtCantidadOperaciones);

        JButton btnGuardar = new JButton("Guardar Cambios");
        btnGuardar.addActionListener(e -> {
        	guardarConfig(chkDecimales, chkNegativos, chkParentesis, chkDespejarX, txtCantidadOperaciones.getText());
            guardarSimbolos(chkSuma, chkResta, chkMultiplicacion, chkDivision);
            guardarNumeros(areaNumeros.getText());

            JOptionPane.showMessageDialog(frame, "Configuración guardada correctamente.");
            frame.dispose();
            Main.mostrarMenuInicial();
        });


        frame.add(opcionesPanel, BorderLayout.NORTH);
        JPanel panelNumeros = new JPanel(new BorderLayout());
        panelNumeros.setBorder(BorderFactory.createTitledBorder("Lista de números disponibles"));

        JLabel instrucciones = new JLabel("Escribalos numero separos por una coma, por ejemplo: 1, 2, 10, 50", JLabel.LEFT);
        instrucciones.setFont(new Font("Arial", Font.PLAIN, 14));
        panelNumeros.add(instrucciones, BorderLayout.NORTH);
        panelNumeros.add(scrollNumeros, BorderLayout.CENTER);

        frame.add(panelNumeros, BorderLayout.CENTER);

        frame.add(btnGuardar, BorderLayout.PAGE_END);

        frame.setVisible(true);
    }

    private static Map<String, String> leerArchivoComoMapa(String ruta) {
        Map<String, String> mapa = new LinkedHashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(ruta))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(":");
                if (partes.length == 2) {
                    mapa.put(partes[0].trim().toUpperCase(), partes[1].trim().toUpperCase());
                }
            }
        } catch (IOException e) {
            System.out.println("No se pudo leer " + ruta);
        }
        return mapa;
    }

    private static String leerArchivoTexto(String ruta) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(ruta))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                sb.append(linea).append("\n");
            }
        } catch (IOException e) {
            System.out.println("No se pudo leer " + ruta);
        }
        return sb.toString();
    }

    private static void guardarConfig(JCheckBox dec, JCheckBox neg, JCheckBox par, JCheckBox despejarX, String cantOperaciones) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(CONFIG_PATH))) {
            pw.println("DECIMALES: " + (dec.isSelected() ? "SI" : "NO"));
            pw.println("NUMEROS NEGATIVOS: " + (neg.isSelected() ? "SI" : "NO"));
            pw.println("PARENTESIS: " + (par.isSelected() ? "SI" : "NO"));
            pw.println("DESPEJAR_X: " + (despejarX.isSelected() ? "SI" : "NO"));

            pw.println("OPERACIONES POR EJERCICIO: " + cantOperaciones.trim());
        } catch (IOException e) {
            System.out.println("Error al guardar config.txt");
        }
    }

    private static void guardarSimbolos(JCheckBox suma, JCheckBox resta, JCheckBox multi, JCheckBox div) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(SIMBOLOS_PATH))) {
            pw.println("SUMA: " + (suma.isSelected() ? "SI" : "NO"));
            pw.println("RESTA: " + (resta.isSelected() ? "SI" : "NO"));
            pw.println("MULTIPLICACION: " + (multi.isSelected() ? "SI" : "NO"));
            pw.println("DIVISION: " + (div.isSelected() ? "SI" : "NO"));
        } catch (IOException e) {
            System.out.println("Error al guardar simbolos.txt");
        }
    }

    private static void guardarNumeros(String contenido) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(NUMEROS_PATH))) {
            // Separar por comas y limpiar espacios
            String[] numeros = contenido.split(",");
            for (String numero : numeros) {
                numero = numero.trim();
                if (!numero.isEmpty()) {
                    pw.println(numero);
                }
            }
        } catch (IOException e) {
            System.out.println("Error al guardar numeros.txt");
        }
    }


}
