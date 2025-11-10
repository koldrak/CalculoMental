import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

public class ModoDificultadInteligente {
    private final Random random = new Random();
    private JFrame frame;
    private JLabel lblProgreso;
    private JLabel lblNivel;
    private JLabel lblEjercicio;
    private JLabel lblRetroalimentacion;
    private JTextField txtRespuesta;
    private JButton btnVerificar;
    private JButton btnContinuar;

    private EjercicioMultiple ejercicioActual;
    private int ejerciciosTotales;
    private int ejerciciosCompletados;
    private int gradoDificultad;

    public void iniciar() {
        LinkedHashMap<String, String> config = ConfigFileHelper.leerConfig();
        gradoDificultad = parseEntero(config.getOrDefault("GRADO_DIFICULTAD", "0"), 0);
        ejerciciosTotales = parseEntero(config.getOrDefault("CANTIDAD EJERCICIOS", "7"), 7);
        gradoDificultad = NivelDificultad.clampGrado(gradoDificultad);

        construirUI();
        mostrarSiguienteEjercicio();
        frame.setVisible(true);
    }

    private void construirUI() {
        frame = new JFrame("Modo dificultad inteligente");
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout(10, 10));
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                Main.mostrarMenuInicial();
            }
        });

        lblProgreso = crearEtiqueta("", 28, Font.BOLD, SwingConstants.CENTER);
        lblNivel = crearEtiqueta("", 20, Font.PLAIN, SwingConstants.CENTER);

        JPanel panelSuperior = new JPanel(new GridLayout(2, 1));
        panelSuperior.add(lblProgreso);
        panelSuperior.add(lblNivel);
        frame.add(panelSuperior, BorderLayout.NORTH);

        lblEjercicio = crearEtiqueta("", 48, Font.BOLD, SwingConstants.CENTER);
        txtRespuesta = new JTextField();
        txtRespuesta.setFont(new Font("Arial", Font.PLAIN, 32));
        txtRespuesta.setHorizontalAlignment(SwingConstants.CENTER);

        btnVerificar = new JButton("Verificar");
        btnVerificar.setFont(new Font("Arial", Font.BOLD, 26));
        btnVerificar.addActionListener(e -> verificarRespuesta());

        btnContinuar = new JButton("Siguiente");
        btnContinuar.setFont(new Font("Arial", Font.BOLD, 26));
        btnContinuar.setEnabled(false);
        btnContinuar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                continuar();
            }
        });

        JPanel panelCentro = new JPanel();
        panelCentro.setLayout(new BoxLayout(panelCentro, BoxLayout.Y_AXIS));
        panelCentro.add(Box.createVerticalStrut(40));
        panelCentro.add(lblEjercicio);
        panelCentro.add(Box.createVerticalStrut(20));
        panelCentro.add(txtRespuesta);
        panelCentro.add(Box.createVerticalStrut(20));

        JPanel panelBotones = new JPanel();
        panelBotones.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelBotones.add(btnVerificar);
        panelBotones.add(Box.createHorizontalStrut(20));
        panelBotones.add(btnContinuar);
        panelCentro.add(panelBotones);

        lblRetroalimentacion = crearEtiqueta("", 22, Font.PLAIN, SwingConstants.CENTER);
        lblRetroalimentacion.setForeground(Color.DARK_GRAY);
        panelCentro.add(Box.createVerticalStrut(20));
        panelCentro.add(lblRetroalimentacion);

        frame.add(panelCentro, BorderLayout.CENTER);
    }

    private JLabel crearEtiqueta(String texto, int tamanioFuente, int estilo, int alineacion) {
        JLabel etiqueta = new JLabel(texto, alineacion);
        etiqueta.setFont(new Font("Arial", estilo, tamanioFuente));
        return etiqueta;
    }

    private void mostrarSiguienteEjercicio() {
        if (ejerciciosCompletados >= ejerciciosTotales) {
            cerrarModo();
            return;
        }

        NivelDificultad nivelActual = obtenerNivelActual();
        if (nivelActual == null) {
            return;
        }
        ejercicioActual = generarEjercicio(nivelActual);

        lblProgreso.setText("Ejercicio " + (ejerciciosCompletados + 1) + " de " + ejerciciosTotales);
        lblNivel.setText(descripcionNivel(nivelActual));
        lblEjercicio.setText(ejercicioActual.getEjercicioTexto());
        lblRetroalimentacion.setText("");

        txtRespuesta.setText("");
        txtRespuesta.setEditable(true);
        txtRespuesta.requestFocusInWindow();

        btnVerificar.setEnabled(true);
        btnContinuar.setEnabled(false);
        btnContinuar.setText("Siguiente");
    }

    private EjercicioMultiple generarEjercicio(NivelDificultad nivel) {
        int cantidadNumeros = nivel.getCantidadOperaciones();
        int maximoNumero = nivel.getMaximoNumero();
        String[] operadores = nivel.getOperadoresPermitidos();

        StringBuilder expresion = new StringBuilder();
        int numeroInicial = numeroAleatorio(1, maximoNumero);
        expresion.append(numeroInicial);
        double resultado = numeroInicial;

        for (int i = 1; i < cantidadNumeros; i++) {
            String operador = operadores[random.nextInt(operadores.length)];
            int siguiente = numeroAleatorio(1, maximoNumero);
            expresion.append(" ").append(operador).append(" ").append(siguiente);
            if ("-".equals(operador)) {
                resultado -= siguiente;
            } else {
                resultado += siguiente;
            }
        }

        return new EjercicioMultiple(expresion.toString(), resultado);
    }

    private int numeroAleatorio(int minimo, int maximo) {
        return random.nextInt(maximo - minimo + 1) + minimo;
    }

    private void verificarRespuesta() {
        String texto = txtRespuesta.getText().trim().replace(",", ".");
        if (texto.isEmpty()) {
            mostrarMensaje("Ingresa una respuesta antes de verificar.", Color.RED);
            return;
        }

        double respuestaUsuario;
        try {
            respuestaUsuario = Double.parseDouble(texto);
        } catch (NumberFormatException e) {
            mostrarMensaje("La respuesta debe ser un número válido.", Color.RED);
            return;
        }

        double resultadoEsperado = ejercicioActual.resultado;
        boolean esCorrecto = Math.abs(respuestaUsuario - resultadoEsperado) < 1e-6;

        if (esCorrecto) {
            gradoDificultad = NivelDificultad.clampGrado(gradoDificultad + 1);
            mostrarMensaje("¡Correcto!", new Color(0, 128, 0));
        } else {
            gradoDificultad = NivelDificultad.clampGrado(gradoDificultad - 1);
            mostrarMensaje("Incorrecto. La respuesta correcta es " + formatearResultado(resultadoEsperado) + ".", Color.RED);
        }

        ConfigFileHelper.guardarGradoDificultad(gradoDificultad);
        ejerciciosCompletados++;

        NivelDificultad nivelActual = obtenerNivelActual();
        if (nivelActual != null) {
            lblNivel.setText(descripcionNivel(nivelActual));
        }

        btnVerificar.setEnabled(false);
        txtRespuesta.setEditable(false);
        btnContinuar.setEnabled(true);
        if (ejerciciosCompletados >= ejerciciosTotales) {
            btnContinuar.setText("Finalizar");
        }
    }

    private String descripcionNivel(NivelDificultad nivelActual) {
        return "Nivel actual: " + nivelActual.getDescripcion() + " - Grado " + gradoDificultad + " pts";
    }

    private NivelDificultad obtenerNivelActual() {
        NivelDificultad nivel = NivelDificultad.desdeGrado(gradoDificultad);
        if (nivel == null) {
            List<NivelDificultad> niveles = NivelDificultad.obtenerNiveles();
            if (niveles.isEmpty()) {
                JOptionPane.showMessageDialog(frame,
                        "No hay niveles de dificultad configurados. Configura al menos uno antes de continuar.",
                        "Niveles no disponibles", JOptionPane.ERROR_MESSAGE);
                cerrarModo();
                return null;
            }
            nivel = niveles.get(0);
        }
        return nivel;
    }

    private void mostrarMensaje(String mensaje, Color color) {
        lblRetroalimentacion.setText(mensaje);
        lblRetroalimentacion.setForeground(color);
    }

    private String formatearResultado(double valor) {
        if (Math.abs(valor - Math.rint(valor)) < 1e-6) {
            return String.valueOf((long) Math.round(valor));
        }
        return String.valueOf(valor);
    }

    private void continuar() {
        if (!btnContinuar.isEnabled()) {
            return;
        }
        if (ejerciciosCompletados >= ejerciciosTotales) {
            cerrarModo();
        } else {
            mostrarSiguienteEjercicio();
        }
    }

    private void cerrarModo() {
        JOptionPane.showMessageDialog(frame,
                "Has terminado el modo dificultad inteligente. Grado final: " + gradoDificultad + " pts.");
        frame.dispose();
    }

    private int parseEntero(String valor, int predeterminado) {
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            return predeterminado;
        }
    }
}
