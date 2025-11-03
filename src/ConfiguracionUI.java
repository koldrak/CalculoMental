import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        List<RangoNumero> numerosDisponibles = leerNumerosComoLista(NUMEROS_PATH);
        NumerosTableModel numerosModel = new NumerosTableModel(frame, numerosDisponibles);
        JTable tablaNumeros = new JTable(numerosModel);
        tablaNumeros.setFillsViewportHeight(true);
        JScrollPane scrollNumeros = new JScrollPane(tablaNumeros);

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

        tablaNumeros.setPreferredScrollableViewportSize(new Dimension(300, 200));

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
            guardarNumeros(numerosModel);

            JOptionPane.showMessageDialog(frame, "Configuración guardada correctamente.");
            frame.dispose();
            Main.mostrarMenuInicial();
        });


        frame.add(opcionesPanel, BorderLayout.NORTH);
        JPanel panelNumeros = new JPanel(new BorderLayout());
        panelNumeros.setBorder(BorderFactory.createTitledBorder("Lista de números disponibles"));

        JLabel instrucciones = new JLabel("Gestione números individuales o rangos (ej. 1-10).", JLabel.LEFT);
        instrucciones.setFont(new Font("Arial", Font.PLAIN, 14));
        panelNumeros.add(instrucciones, BorderLayout.NORTH);
        panelNumeros.add(scrollNumeros, BorderLayout.CENTER);

        JPanel botonesNumeros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAgregarNumero = new JButton("Agregar número");
        btnAgregarNumero.addActionListener(e -> {
            BigDecimal numero = solicitarNumero(frame, "Ingresa el número a agregar:");
            if (numero != null) {
                numerosModel.agregarNumero(numero);
            }
        });

        JButton btnAgregarRango = new JButton("Agregar rango");
        btnAgregarRango.addActionListener(e -> {
            RangoNumero rango = solicitarRango(frame);
            if (rango != null) {
                numerosModel.agregarRango(rango);
            }
        });

        JButton btnEliminarSeleccion = new JButton("Eliminar selección");
        btnEliminarSeleccion.addActionListener(e -> {
            int[] filasSeleccionadas = tablaNumeros.getSelectedRows();
            if (filasSeleccionadas.length == 0) {
                JOptionPane.showMessageDialog(frame, "Selecciona al menos una fila para eliminar.");
                return;
            }
            numerosModel.eliminarFilas(filasSeleccionadas);
        });

        botonesNumeros.add(btnAgregarNumero);
        botonesNumeros.add(btnAgregarRango);
        botonesNumeros.add(btnEliminarSeleccion);
        panelNumeros.add(botonesNumeros, BorderLayout.SOUTH);

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

    private static void guardarNumeros(NumerosTableModel modelo) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(NUMEROS_PATH))) {
            for (RangoNumero rango : modelo.obtenerRangos()) {
                if (rango.esRango()) {
                    pw.println(formatear(rango.getInicio()) + "-" + formatear(rango.getFin()));
                } else {
                    pw.println(formatear(rango.getInicio()));
                }
            }
        } catch (IOException e) {
            System.out.println("Error al guardar numeros.txt");
        }
    }

    private static List<RangoNumero> leerNumerosComoLista(String ruta) {
        List<RangoNumero> numeros = new ArrayList<>();
        Pattern rangoPattern = Pattern.compile("\\s*(-?\\d+(?:\\.\\d+)?)[\\s]*-[\\s]*(-?\\d+(?:\\.\\d+)?)\\s*");
        try (BufferedReader br = new BufferedReader(new FileReader(ruta))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty()) {
                    continue;
                }
                Matcher matcher = rangoPattern.matcher(linea);
                if (matcher.matches()) {
                    BigDecimal inicio = new BigDecimal(matcher.group(1));
                    BigDecimal fin = new BigDecimal(matcher.group(2));
                    numeros.add(crearRangoOrdenado(inicio, fin));
                } else {
                    try {
                        BigDecimal valor = new BigDecimal(linea);
                        numeros.add(new RangoNumero(valor, valor));
                    } catch (NumberFormatException ex) {
                        System.out.println("Valor de número inválido encontrado en " + ruta + ": " + linea);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("No se pudo leer " + ruta);
        }
        return numeros;
    }

    private static BigDecimal solicitarNumero(Component parent, String mensaje) {
        while (true) {
            String input = JOptionPane.showInputDialog(parent, mensaje);
            if (input == null) {
                return null;
            }
            input = input.trim();
            if (input.isEmpty()) {
                JOptionPane.showMessageDialog(parent, "El campo no puede estar vacío.");
                continue;
            }
            try {
                return new BigDecimal(input);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(parent, "Ingresa un número válido.");
            }
        }
    }

    private static RangoNumero solicitarRango(Component parent) {
        BigDecimal inicio = solicitarNumero(parent, "Ingresa el valor inicial del rango:");
        if (inicio == null) {
            return null;
        }
        BigDecimal fin = solicitarNumero(parent, "Ingresa el valor final del rango:");
        if (fin == null) {
            return null;
        }
        return crearRangoOrdenado(inicio, fin);
    }

    private static RangoNumero crearRangoOrdenado(BigDecimal inicio, BigDecimal fin) {
        if (inicio.compareTo(fin) <= 0) {
            return new RangoNumero(inicio, fin);
        }
        return new RangoNumero(fin, inicio);
    }

    private static String formatear(BigDecimal numero) {
        BigDecimal normalizado = numero.stripTrailingZeros();
        if (normalizado.scale() < 0) {
            normalizado = normalizado.setScale(0);
        }
        return normalizado.toPlainString();
    }

    private static class NumerosTableModel extends AbstractTableModel {

        private final Component parent;
        private final List<RangoNumero> rangos;

        private final String[] columnas = {"Desde", "Hasta"};

        private NumerosTableModel(Component parent, List<RangoNumero> rangosIniciales) {
            this.parent = parent;
            this.rangos = new ArrayList<>(rangosIniciales);
        }

        @Override
        public int getRowCount() {
            return rangos.size();
        }

        @Override
        public int getColumnCount() {
            return columnas.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnas[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            RangoNumero rango = rangos.get(rowIndex);
            if (columnIndex == 0) {
                return formatear(rango.getInicio());
            }
            return rango.esRango() ? formatear(rango.getFin()) : "";
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (aValue == null) {
                return;
            }
            String valor = aValue.toString().trim();
            if (columnIndex == 0 && valor.isEmpty()) {
                JOptionPane.showMessageDialog(parent, "El valor inicial no puede quedar vacío.");
                return;
            }

            RangoNumero rango = rangos.get(rowIndex);
            if (columnIndex == 0) {
                try {
                    BigDecimal inicio = new BigDecimal(valor);
                    rango.setInicio(inicio);
                    if (rango.getFin() == null || inicio.compareTo(rango.getFin()) > 0) {
                        rango.setFin(inicio);
                    }
                    fireTableRowsUpdated(rowIndex, rowIndex);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(parent, "Ingresa un número válido.");
                }
            } else if (columnIndex == 1) {
                if (valor.isEmpty()) {
                    rango.setFin(rango.getInicio());
                    fireTableRowsUpdated(rowIndex, rowIndex);
                    return;
                }
                try {
                    BigDecimal fin = new BigDecimal(valor);
                    if (fin.compareTo(rango.getInicio()) < 0) {
                        JOptionPane.showMessageDialog(parent, "El final del rango no puede ser menor que el inicio.");
                        return;
                    }
                    rango.setFin(fin);
                    fireTableRowsUpdated(rowIndex, rowIndex);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(parent, "Ingresa un número válido.");
                }
            }
        }

        private void agregarNumero(BigDecimal numero) {
            rangos.add(new RangoNumero(numero, numero));
            int nuevaFila = rangos.size() - 1;
            fireTableRowsInserted(nuevaFila, nuevaFila);
        }

        private void agregarRango(RangoNumero rango) {
            rangos.add(rango);
            int nuevaFila = rangos.size() - 1;
            fireTableRowsInserted(nuevaFila, nuevaFila);
        }

        private void eliminarFilas(int[] filas) {
            Arrays.sort(filas);
            for (int i = filas.length - 1; i >= 0; i--) {
                rangos.remove(filas[i]);
            }
            fireTableDataChanged();
        }

        private List<RangoNumero> obtenerRangos() {
            return new ArrayList<>(rangos);
        }
    }

    private static class RangoNumero {
        private BigDecimal inicio;
        private BigDecimal fin;

        private RangoNumero(BigDecimal inicio, BigDecimal fin) {
            this.inicio = inicio;
            this.fin = fin;
        }

        private BigDecimal getInicio() {
            return inicio;
        }

        private void setInicio(BigDecimal inicio) {
            this.inicio = inicio;
        }

        private BigDecimal getFin() {
            return fin;
        }

        private void setFin(BigDecimal fin) {
            this.fin = fin;
        }

        private boolean esRango() {
            return fin != null && inicio.compareTo(fin) != 0;
        }
    }


}
