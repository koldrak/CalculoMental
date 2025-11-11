import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

public class ModoDificultadInteligente {
    private JFrame frame;
    private JPanel panelPrincipal;
    private CardLayout cardLayout;
    private JPanel panelEjercicio;
    private JPanel panelCorreccion;
    private JLabel lblTituloEjercicio;
    private JLabel lblNivel;
    private JLabel lblCronometro;
    private JLabel lblTextoEjercicio;
    private JLabel lblCorreccionTitulo;
    private JLabel lblCorreccionNivel;
    private JLabel lblCorreccionEjercicio;
    private JLabel lblRetroalimentacion;
    private JTextField txtRespuesta;
    private JButton btnVerificar;
    private JButton btnContinuar;
    private Timer timer;
    private int segundos;

    private EjercicioMultiple ejercicioActual;
    private int ejerciciosTotales;
    private int ejerciciosCompletados;
    private int gradoDificultad;
    private Color fondoEjercicioActual;

    private enum EstadoPantalla {
        EJERCICIO,
        CORRECCION
    }

    private EstadoPantalla estadoPantalla = EstadoPantalla.EJERCICIO;

    public void iniciar() {
        LinkedHashMap<String, String> config = ConfigFileHelper.leerConfig();
        gradoDificultad = parseEntero(config.getOrDefault("GRADO_DIFICULTAD", "0"), 0);
        gradoDificultad = NivelDificultad.clampGrado(gradoDificultad);

        NivelDificultad nivelInicial = NivelDificultad.desdeGrado(gradoDificultad);
        if (nivelInicial != null) {
            ejerciciosTotales = nivelInicial.getCantidadEjercicios();
        } else {
            ejerciciosTotales = parseEntero(config.getOrDefault("CANTIDAD EJERCICIOS", "7"), 7);
        }

        construirUI();
        mostrarSiguienteEjercicio();
        frame.setVisible(true);
    }

    private void construirUI() {
        frame = new JFrame("Modo dificultad inteligente");
        frame.setUndecorated(true);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cerrarModo();
                } else if (estadoPantalla == EstadoPantalla.EJERCICIO && (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER)) {
                    mostrarCorreccion();
                } else if (estadoPantalla == EstadoPantalla.CORRECCION && btnContinuar.isEnabled() && (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER)) {
                    continuar();
                }
            }
        });
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                Main.mostrarMenuInicial();
            }
        });

        frame.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (estadoPantalla == EstadoPantalla.EJERCICIO) {
                    mostrarCorreccion();
                }
            }
        });

        cardLayout = new CardLayout();
        panelPrincipal = new JPanel(cardLayout);
        frame.getContentPane().add(panelPrincipal);

        construirPanelEjercicio();
        construirPanelCorreccion();
    }

    private void construirPanelEjercicio() {
        panelEjercicio = new JPanel(new BorderLayout());
        panelEjercicio.setBackground(Color.WHITE);

        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.setOpaque(false);

        lblCronometro = crearEtiqueta("⏱ 0s", 50, Font.PLAIN, SwingConstants.LEFT);
        lblCronometro.setForeground(Color.BLUE);
        panelSuperior.add(lblCronometro, BorderLayout.WEST);

        lblTituloEjercicio = crearEtiqueta("", 60, Font.BOLD, SwingConstants.CENTER);
        lblTituloEjercicio.setForeground(Color.DARK_GRAY);
        panelSuperior.add(lblTituloEjercicio, BorderLayout.CENTER);

        lblNivel = crearEtiqueta("", 24, Font.PLAIN, SwingConstants.RIGHT);
        lblNivel.setForeground(Color.DARK_GRAY);
        panelSuperior.add(lblNivel, BorderLayout.EAST);

        panelEjercicio.add(panelSuperior, BorderLayout.NORTH);

        lblTextoEjercicio = crearEtiqueta("", 70, Font.BOLD, SwingConstants.CENTER);
        lblTextoEjercicio.setVerticalAlignment(SwingConstants.CENTER);
        panelEjercicio.add(lblTextoEjercicio, BorderLayout.CENTER);

        panelPrincipal.add(panelEjercicio, EstadoPantalla.EJERCICIO.name());
    }

    private void construirPanelCorreccion() {
        panelCorreccion = new JPanel(new BorderLayout(20, 20));
        panelCorreccion.setBackground(Color.WHITE);

        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.setOpaque(false);

        lblCorreccionTitulo = crearEtiqueta("Corrección", 60, Font.BOLD, SwingConstants.CENTER);
        lblCorreccionTitulo.setForeground(Color.DARK_GRAY);
        panelSuperior.add(lblCorreccionTitulo, BorderLayout.CENTER);

        lblCorreccionNivel = crearEtiqueta("", 24, Font.PLAIN, SwingConstants.RIGHT);
        lblCorreccionNivel.setForeground(Color.DARK_GRAY);
        panelSuperior.add(lblCorreccionNivel, BorderLayout.EAST);

        panelCorreccion.add(panelSuperior, BorderLayout.NORTH);

        JPanel panelCentro = new JPanel();
        panelCentro.setOpaque(false);
        panelCentro.setLayout(new BoxLayout(panelCentro, BoxLayout.Y_AXIS));

        lblCorreccionEjercicio = crearEtiqueta("", 50, Font.BOLD, SwingConstants.CENTER);
        lblCorreccionEjercicio.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelCentro.add(lblCorreccionEjercicio);
        panelCentro.add(Box.createVerticalStrut(30));

        txtRespuesta = new JTextField();
        txtRespuesta.setFont(new Font("Arial", Font.PLAIN, 36));
        txtRespuesta.setHorizontalAlignment(SwingConstants.CENTER);
        txtRespuesta.setMaximumSize(new Dimension(600, 70));
        panelCentro.add(txtRespuesta);
        panelCentro.add(Box.createVerticalStrut(20));

        lblRetroalimentacion = crearEtiqueta("", 28, Font.PLAIN, SwingConstants.CENTER);
        lblRetroalimentacion.setForeground(Color.DARK_GRAY);
        lblRetroalimentacion.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelCentro.add(lblRetroalimentacion);
        panelCentro.add(Box.createVerticalStrut(20));

        JPanel panelBotones = new JPanel();
        panelBotones.setOpaque(false);

        btnVerificar = new JButton("Verificar");
        btnVerificar.setFont(new Font("Arial", Font.BOLD, 30));
        btnVerificar.addActionListener(e -> verificarRespuesta());
        panelBotones.add(btnVerificar);

        btnContinuar = new JButton("Siguiente");
        btnContinuar.setFont(new Font("Arial", Font.BOLD, 30));
        btnContinuar.setEnabled(false);
        btnContinuar.addActionListener(e -> continuar());
        panelBotones.add(Box.createHorizontalStrut(20));
        panelBotones.add(btnContinuar);

        panelCentro.add(panelBotones);

        panelCorreccion.add(panelCentro, BorderLayout.CENTER);

        panelPrincipal.add(panelCorreccion, EstadoPantalla.CORRECCION.name());
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
        if (ejercicioActual == null) {
            return;
        }

        lblTituloEjercicio.setText("Ejercicio " + (ejerciciosCompletados + 1) + " de " + ejerciciosTotales);
        lblNivel.setText(descripcionNivel(nivelActual));
        lblTextoEjercicio.setText(ejercicioActual.getEjercicioTexto());

        fondoEjercicioActual = generarColorPastelAleatorio();
        panelEjercicio.setBackground(fondoEjercicioActual);

        segundos = 0;
        lblCronometro.setText("⏱ 0s");
        iniciarContador();

        estadoPantalla = EstadoPantalla.EJERCICIO;
        cardLayout.show(panelPrincipal, EstadoPantalla.EJERCICIO.name());
        frame.getContentPane().revalidate();
        frame.getContentPane().repaint();
    }

    private EjercicioMultiple generarEjercicio(NivelDificultad nivel) {
        try {
            Generador generador = new Generador(nivel);
            return generador.generarEjercicioUnico();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(frame,
                    "No se pudo generar un ejercicio para el nivel actual: " + ex.getMessage(),
                    "Configuración inválida", JOptionPane.ERROR_MESSAGE);
        }
        cerrarModo();
        return null;
    }

    private void verificarRespuesta() {
        if (estadoPantalla != EstadoPantalla.CORRECCION) {
            return;
        }

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
            lblCorreccionNivel.setText(descripcionNivel(nivelActual));
        }

        btnVerificar.setEnabled(false);
        txtRespuesta.setEditable(false);
        btnContinuar.setEnabled(true);
        if (ejerciciosCompletados >= ejerciciosTotales) {
            btnContinuar.setText("Finalizar");
        }
    }

    private void mostrarCorreccion() {
        if (estadoPantalla != EstadoPantalla.EJERCICIO) {
            return;
        }

        detenerContador();
        estadoPantalla = EstadoPantalla.CORRECCION;

        lblCorreccionTitulo.setText("Corrección - Ejercicio " + (ejerciciosCompletados + 1) + " de " + ejerciciosTotales);
        lblCorreccionNivel.setText(lblNivel.getText());
        lblCorreccionEjercicio.setText(ejercicioActual.getEjercicioTexto());
        lblRetroalimentacion.setText("");

        txtRespuesta.setText("");
        txtRespuesta.setEditable(true);
        btnVerificar.setEnabled(true);
        btnContinuar.setEnabled(false);
        btnContinuar.setText("Siguiente");

        panelCorreccion.setBackground(fondoEjercicioActual != null ? fondoEjercicioActual : Color.WHITE);

        cardLayout.show(panelPrincipal, EstadoPantalla.CORRECCION.name());
        SwingUtilities.invokeLater(() -> txtRespuesta.requestFocusInWindow());
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
        detenerContador();
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

    private void iniciarContador() {
        detenerContador();
        timer = new Timer(1000, e -> {
            segundos++;
            lblCronometro.setText("⏱ " + segundos + "s");
        });
        timer.start();
    }

    private void detenerContador() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    private Color generarColorPastelAleatorio() {
        Random random = new Random();
        int red = (random.nextInt(128) + 127);
        int green = (random.nextInt(128) + 127);
        int blue = (random.nextInt(128) + 127);
        return new Color(red, green, blue);
    }
}
