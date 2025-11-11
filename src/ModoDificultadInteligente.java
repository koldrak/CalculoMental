import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
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
    private JPanel panelListaRespuestas;
    private List<JTextField> camposRespuestas = new ArrayList<>();
    private List<JLabel> etiquetasRetroalimentacion = new ArrayList<>();
    private JButton btnEvaluar;
    private JButton btnFinalizar;
    private Timer timer;
    private int segundos;

    private EjercicioMultiple ejercicioActual;
    private int ejerciciosTotales;
    private final List<EjercicioMultiple> ejerciciosGenerados = new ArrayList<>();
    private int gradoDificultad;
    private boolean respuestasEvaluadas;

    private enum EstadoPantalla {
        EJERCICIO,
        HOJA_RESPUESTAS
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
                    avanzarEjercicio();
                } else if (estadoPantalla == EstadoPantalla.HOJA_RESPUESTAS && (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER)) {
                    if (!respuestasEvaluadas) {
                        evaluarRespuestas();
                    } else {
                        cerrarModo();
                    }
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
                    avanzarEjercicio();
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

        lblCorreccionTitulo = crearEtiqueta("Hoja de respuestas", 60, Font.BOLD, SwingConstants.CENTER);
        lblCorreccionTitulo.setForeground(Color.DARK_GRAY);
        panelSuperior.add(lblCorreccionTitulo, BorderLayout.CENTER);

        lblCorreccionNivel = crearEtiqueta("", 24, Font.PLAIN, SwingConstants.RIGHT);
        lblCorreccionNivel.setForeground(Color.DARK_GRAY);
        panelSuperior.add(lblCorreccionNivel, BorderLayout.EAST);

        panelCorreccion.add(panelSuperior, BorderLayout.NORTH);

        panelListaRespuestas = new JPanel();
        panelListaRespuestas.setOpaque(false);
        panelListaRespuestas.setLayout(new BoxLayout(panelListaRespuestas, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(panelListaRespuestas);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        panelCorreccion.add(scrollPane, BorderLayout.CENTER);

        JPanel panelBotones = new JPanel();
        panelBotones.setOpaque(false);

        btnEvaluar = new JButton("Verificar respuestas");
        btnEvaluar.setFont(new Font("Arial", Font.BOLD, 30));
        btnEvaluar.addActionListener(e -> evaluarRespuestas());
        panelBotones.add(btnEvaluar);

        btnFinalizar = new JButton("Finalizar");
        btnFinalizar.setFont(new Font("Arial", Font.BOLD, 30));
        btnFinalizar.setEnabled(false);
        btnFinalizar.addActionListener(e -> cerrarModo());
        panelBotones.add(Box.createHorizontalStrut(20));
        panelBotones.add(btnFinalizar);

        panelCorreccion.add(panelBotones, BorderLayout.SOUTH);

        panelPrincipal.add(panelCorreccion, EstadoPantalla.HOJA_RESPUESTAS.name());
    }

    private JLabel crearEtiqueta(String texto, int tamanioFuente, int estilo, int alineacion) {
        JLabel etiqueta = new JLabel(texto, alineacion);
        etiqueta.setFont(new Font("Arial", estilo, tamanioFuente));
        return etiqueta;
    }

    private void mostrarSiguienteEjercicio() {
        if (ejerciciosGenerados.size() >= ejerciciosTotales) {
            mostrarHojaRespuestas();
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

        ejerciciosGenerados.add(ejercicioActual);

        lblTituloEjercicio.setText("Ejercicio " + ejerciciosGenerados.size() + " de " + ejerciciosTotales);
        lblNivel.setText(descripcionNivel(nivelActual));
        lblTextoEjercicio.setText(ejercicioActual.getEjercicioTexto());

        panelEjercicio.setBackground(generarColorPastelAleatorio());

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

    private void mostrarHojaRespuestas() {
        detenerContador();
        estadoPantalla = EstadoPantalla.HOJA_RESPUESTAS;
        respuestasEvaluadas = false;

        lblCorreccionTitulo.setText("Hoja de respuestas");
        NivelDificultad nivelActual = obtenerNivelActual();
        if (nivelActual != null) {
            lblCorreccionNivel.setText(descripcionNivel(nivelActual));
        }

        panelCorreccion.setBackground(Color.WHITE);
        panelListaRespuestas.removeAll();
        camposRespuestas.clear();
        etiquetasRetroalimentacion.clear();

        for (int i = 0; i < ejerciciosGenerados.size(); i++) {
            EjercicioMultiple ejercicio = ejerciciosGenerados.get(i);

            JPanel fila = new JPanel();
            fila.setOpaque(false);
            fila.setLayout(new BoxLayout(fila, BoxLayout.Y_AXIS));

            JLabel lblEjercicio = crearEtiqueta("Ejercicio " + (i + 1) + ": " + ejercicio.getEjercicioTexto(), 40, Font.BOLD, SwingConstants.CENTER);
            lblEjercicio.setAlignmentX(Component.CENTER_ALIGNMENT);
            fila.add(lblEjercicio);
            fila.add(Box.createVerticalStrut(10));

            JTextField campoRespuesta = new JTextField();
            campoRespuesta.setFont(new Font("Arial", Font.PLAIN, 36));
            campoRespuesta.setHorizontalAlignment(SwingConstants.CENTER);
            campoRespuesta.setMaximumSize(new Dimension(600, 70));
            campoRespuesta.setAlignmentX(Component.CENTER_ALIGNMENT);
            fila.add(campoRespuesta);
            fila.add(Box.createVerticalStrut(10));

            JLabel lblRetro = crearEtiqueta("", 28, Font.PLAIN, SwingConstants.CENTER);
            lblRetro.setForeground(Color.DARK_GRAY);
            lblRetro.setAlignmentX(Component.CENTER_ALIGNMENT);
            fila.add(lblRetro);

            panelListaRespuestas.add(fila);
            panelListaRespuestas.add(Box.createVerticalStrut(30));

            camposRespuestas.add(campoRespuesta);
            etiquetasRetroalimentacion.add(lblRetro);
        }

        btnEvaluar.setEnabled(true);
        btnFinalizar.setEnabled(false);

        cardLayout.show(panelPrincipal, EstadoPantalla.HOJA_RESPUESTAS.name());
        panelListaRespuestas.revalidate();
        panelListaRespuestas.repaint();

        if (!camposRespuestas.isEmpty()) {
            SwingUtilities.invokeLater(() -> camposRespuestas.get(0).requestFocusInWindow());
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

    private String formatearResultado(double valor) {
        if (Math.abs(valor - Math.rint(valor)) < 1e-6) {
            return String.valueOf((long) Math.round(valor));
        }
        return String.valueOf(valor);
    }

    private void avanzarEjercicio() {
        if (estadoPantalla != EstadoPantalla.EJERCICIO) {
            return;
        }

        detenerContador();
        if (ejerciciosGenerados.size() >= ejerciciosTotales) {
            mostrarHojaRespuestas();
        } else {
            mostrarSiguienteEjercicio();
        }
    }

    private void evaluarRespuestas() {
        if (respuestasEvaluadas) {
            return;
        }

        int correctas = 0;

        for (int i = 0; i < ejerciciosGenerados.size(); i++) {
            EjercicioMultiple ejercicio = ejerciciosGenerados.get(i);
            JTextField campo = camposRespuestas.get(i);
            JLabel etiquetaRetro = etiquetasRetroalimentacion.get(i);

            String texto = campo.getText().trim().replace(",", ".");
            double resultadoEsperado = ejercicio.resultado;

            if (texto.isEmpty()) {
                etiquetaRetro.setText("Sin respuesta. La correcta es " + formatearResultado(resultadoEsperado) + ".");
                etiquetaRetro.setForeground(Color.RED);
                gradoDificultad = NivelDificultad.clampGrado(gradoDificultad - 1);
                continue;
            }

            double respuestaUsuario;
            try {
                respuestaUsuario = Double.parseDouble(texto);
            } catch (NumberFormatException ex) {
                etiquetaRetro.setText("Valor inválido. La correcta es " + formatearResultado(resultadoEsperado) + ".");
                etiquetaRetro.setForeground(Color.RED);
                gradoDificultad = NivelDificultad.clampGrado(gradoDificultad - 1);
                continue;
            }

            boolean esCorrecto = Math.abs(respuestaUsuario - resultadoEsperado) < 1e-6;
            if (esCorrecto) {
                etiquetaRetro.setText("¡Correcto!");
                etiquetaRetro.setForeground(new Color(0, 128, 0));
                gradoDificultad = NivelDificultad.clampGrado(gradoDificultad + 1);
                correctas++;
            } else {
                etiquetaRetro.setText("Incorrecto. La correcta es " + formatearResultado(resultadoEsperado) + ".");
                etiquetaRetro.setForeground(Color.RED);
                gradoDificultad = NivelDificultad.clampGrado(gradoDificultad - 1);
            }
        }

        ConfigFileHelper.guardarGradoDificultad(gradoDificultad);

        NivelDificultad nivelActual = obtenerNivelActual();
        if (nivelActual != null) {
            lblCorreccionNivel.setText(descripcionNivel(nivelActual));
        }

        respuestasEvaluadas = true;
        btnEvaluar.setEnabled(false);
        btnFinalizar.setEnabled(true);

        JOptionPane.showMessageDialog(frame,
                "Respuestas correctas: " + correctas + " de " + ejerciciosGenerados.size()
                        + "\nGrado final: " + gradoDificultad + " pts.");
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
