import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EditorNivelesDificultadDialog extends JDialog {
    private final NivelesTableModel tableModel;

    public EditorNivelesDificultadDialog(JFrame owner) {
        super(owner, "Editar niveles de dificultad inteligente", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(700, 400);
        setLocationRelativeTo(owner);

        tableModel = new NivelesTableModel(NivelDificultad.obtenerNiveles());

        JTable tabla = new JTable(tableModel);
        tabla.setRowHeight(26);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(220);
        tabla.getColumnModel().getColumn(10).setPreferredWidth(220);

        JScrollPane scrollPane = new JScrollPane(tabla);
        scrollPane.setPreferredSize(new Dimension(650, 220));
        add(scrollPane, BorderLayout.CENTER);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnAgregar = new JButton("Agregar nivel");
        btnAgregar.addActionListener(e -> tableModel.agregarNivel());

        JButton btnEliminar = new JButton("Eliminar seleccionado");
        btnEliminar.addActionListener(e -> {
            int fila = tabla.getSelectedRow();
            if (fila >= 0) {
                tableModel.eliminarNivel(fila);
            } else {
                JOptionPane.showMessageDialog(this, "Selecciona un nivel para eliminar.");
            }
        });

        JButton btnGuardar = new JButton("Guardar");
        btnGuardar.addActionListener(e -> guardar());

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());

        panelBotones.add(btnAgregar);
        panelBotones.add(btnEliminar);
        panelBotones.add(btnCancelar);
        panelBotones.add(btnGuardar);
        add(panelBotones, BorderLayout.SOUTH);
    }

    private void guardar() {
        List<NivelDificultad> niveles = new ArrayList<>();
        for (NivelEditable editable : tableModel.getNiveles()) {
            String descripcion = editable.descripcion.trim();
            if (descripcion.isEmpty()) {
                mostrarError("La descripción no puede estar vacía.");
                return;
            }
            Integer minimo = parseEntero(editable.minimo, "minimo");
            if (minimo == null) {
                return;
            }
            Integer maximo = parseEntero(editable.maximo, "maximo");
            if (maximo == null) {
                return;
            }
            Integer cantidadOperaciones = parseEntero(editable.cantidadOperaciones, "cantidad de operaciones");
            if (cantidadOperaciones == null || cantidadOperaciones <= 0) {
                mostrarError("La cantidad de operaciones debe ser un entero positivo.");
                return;
            }
            Integer cantidadEjercicios = parseEntero(editable.cantidadEjercicios, "cantidad de ejercicios");
            if (cantidadEjercicios == null || cantidadEjercicios <= 0) {
                mostrarError("La cantidad de ejercicios debe ser un entero positivo.");
                return;
            }
            if (minimo > maximo) {
                mostrarError("El mínimo no puede ser mayor que el máximo para " + descripcion + ".");
                return;
            }
            Boolean permitirDecimales = parseBooleano(editable.decimales, "decimales");
            if (permitirDecimales == null) {
                return;
            }
            Boolean permitirNegativos = parseBooleano(editable.negativos, "números negativos");
            if (permitirNegativos == null) {
                return;
            }
            Boolean permitirParentesis = parseBooleano(editable.parentesis, "paréntesis");
            if (permitirParentesis == null) {
                return;
            }
            Boolean permitirDespejarX = parseBooleano(editable.despejarX, "despejar X");
            if (permitirDespejarX == null) {
                return;
            }
            String[] operadores = parseOperadores(editable.operadores);
            if (operadores.length == 0) {
                mostrarError("Debes especificar al menos un operador para " + descripcion + ".");
                return;
            }
            String definicionNumeros = editable.numeros.trim();
            List<Double> numeros;
            try {
                numeros = NivelDificultad.parsearDefinicionNumeros(definicionNumeros, false);
            } catch (IllegalArgumentException ex) {
                mostrarError("Error en números para " + descripcion + ": " + ex.getMessage());
                return;
            }
            if (numeros.isEmpty()) {
                mostrarError("Debes definir al menos un número o rango para " + descripcion + ".");
                return;
            }
            niveles.add(new NivelDificultad(descripcion, minimo, maximo, cantidadOperaciones,
                    cantidadEjercicios, permitirDecimales, permitirNegativos, permitirParentesis,
                    permitirDespejarX, operadores, definicionNumeros));
        }

        if (niveles.isEmpty()) {
            mostrarError("Debes crear al menos un nivel.");
            return;
        }

        niveles.sort(Comparator.comparingInt(NivelDificultad::getMinimo));
        for (int i = 1; i < niveles.size(); i++) {
            NivelDificultad anterior = niveles.get(i - 1);
            NivelDificultad actual = niveles.get(i);
            if (actual.getMinimo() <= anterior.getMaximo()) {
                mostrarError("Los rangos de puntos no pueden solaparse entre "
                        + anterior.getDescripcion() + " y " + actual.getDescripcion() + ".");
                return;
            }
        }

        NivelDificultad.guardarNiveles(niveles);
        JOptionPane.showMessageDialog(this, "Niveles guardados correctamente.");
        dispose();
    }

    private Integer parseEntero(String valor, String campo) {
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            mostrarError("El campo " + campo + " debe ser un número entero válido.");
            return null;
        }
    }

    private String[] parseOperadores(String texto) {
        String[] partes = texto.split(",");
        List<String> operadores = new ArrayList<>();
        for (String parte : partes) {
            String operador = parte.trim();
            if (!operador.isEmpty()) {
                operadores.add(operador);
            }
        }
        return operadores.toArray(new String[0]);
    }

    private Boolean parseBooleano(String valor, String campo) {
        String normalizado = valor.trim().toUpperCase();
        if (normalizado.equals("SI")) {
            return Boolean.TRUE;
        }
        if (normalizado.equals("NO")) {
            return Boolean.FALSE;
        }
        mostrarError("El campo " + campo + " debe contener SI o NO.");
        return null;
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static class NivelesTableModel extends AbstractTableModel {
        private static final String[] COLUMNAS = {
                "Descripción", "Mínimo", "Máximo", "Operaciones", "Ejercicios",
                "Decimales (SI/NO)", "Negativos (SI/NO)", "Paréntesis (SI/NO)",
                "Despejar X (SI/NO)", "Operadores", "Números (separar con ;)"
        };
        private final List<NivelEditable> niveles;

        NivelesTableModel(List<NivelDificultad> nivelesOriginales) {
            niveles = new ArrayList<>();
            for (NivelDificultad nivel : nivelesOriginales) {
                niveles.add(new NivelEditable(nivel));
            }
        }

        public void agregarNivel() {
            niveles.add(new NivelEditable());
            int fila = niveles.size() - 1;
            fireTableRowsInserted(fila, fila);
        }

        public void eliminarNivel(int fila) {
            if (fila >= 0 && fila < niveles.size()) {
                niveles.remove(fila);
                fireTableRowsDeleted(fila, fila);
            }
        }

        public List<NivelEditable> getNiveles() {
            return niveles;
        }

        @Override
        public int getRowCount() {
            return niveles.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNAS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNAS[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            NivelEditable editable = niveles.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return editable.descripcion;
                case 1:
                    return editable.minimo;
                case 2:
                    return editable.maximo;
                case 3:
                    return editable.cantidadOperaciones;
                case 4:
                    return editable.cantidadEjercicios;
                case 5:
                    return editable.decimales;
                case 6:
                    return editable.negativos;
                case 7:
                    return editable.parentesis;
                case 8:
                    return editable.despejarX;
                case 9:
                    return editable.operadores;
                case 10:
                    return editable.numeros;
                default:
                    return "";
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            NivelEditable editable = niveles.get(rowIndex);
            String valor = aValue != null ? aValue.toString() : "";
            switch (columnIndex) {
                case 0:
                    editable.descripcion = valor;
                    break;
                case 1:
                    editable.minimo = valor;
                    break;
                case 2:
                    editable.maximo = valor;
                    break;
                case 3:
                    editable.cantidadOperaciones = valor;
                    break;
                case 4:
                    editable.cantidadEjercicios = valor;
                    break;
                case 5:
                    editable.decimales = valor;
                    break;
                case 6:
                    editable.negativos = valor;
                    break;
                case 7:
                    editable.parentesis = valor;
                    break;
                case 8:
                    editable.despejarX = valor;
                    break;
                case 9:
                    editable.operadores = valor;
                    break;
                case 10:
                    editable.numeros = valor;
                    break;
                default:
                    break;
            }
            fireTableCellUpdated(rowIndex, columnIndex);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }
    }

    private static class NivelEditable {
        String descripcion;
        String minimo;
        String maximo;
        String cantidadOperaciones;
        String cantidadEjercicios;
        String decimales;
        String negativos;
        String parentesis;
        String despejarX;
        String operadores;
        String numeros;

        NivelEditable() {
            this.descripcion = "Nuevo nivel";
            this.minimo = "0";
            this.maximo = "0";
            this.cantidadOperaciones = "1";
            this.cantidadEjercicios = "5";
            this.decimales = "NO";
            this.negativos = "NO";
            this.parentesis = "NO";
            this.despejarX = "NO";
            this.operadores = "+";
            this.numeros = "1-10";
        }

        NivelEditable(NivelDificultad nivel) {
            this.descripcion = nivel.getDescripcion();
            this.minimo = String.valueOf(nivel.getMinimo());
            this.maximo = String.valueOf(nivel.getMaximo());
            this.cantidadOperaciones = String.valueOf(nivel.getCantidadOperaciones());
            this.cantidadEjercicios = String.valueOf(nivel.getCantidadEjercicios());
            this.decimales = nivel.isPermitirDecimales() ? "SI" : "NO";
            this.negativos = nivel.isPermitirNegativos() ? "SI" : "NO";
            this.parentesis = nivel.isPermitirParentesis() ? "SI" : "NO";
            this.despejarX = nivel.isPermitirDespejarX() ? "SI" : "NO";
            this.operadores = nivel.getOperadoresComoCadena();
            this.numeros = nivel.getDefinicionNumeros();
        }
    }
}
