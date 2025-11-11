import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class ModoDificultadInteligente {
    private JFrame frame;
    private JPanel panelPrincipal;
    private CardLayout cardLayout;
    private JPanel panelIntro;
    private JPanel panelEjercicio;
    private JPanel panelCorreccion;
    private JLabel lblIntroMensaje;
    private JLabel lblTituloEjercicio;
    private JLabel lblNivel;
    private JLabel lblCronometro;
    private JLabel lblTextoEjercicio;
    private JLabel lblCorreccionTitulo;
    private JLabel lblCorreccionNivel;
    private JPanel panelListaRespuestas;
    private List<JTextField> camposRespuestas = new ArrayList<>();
    private List<JLabel> etiquetasRetroalimentacion = new ArrayList<>();
    private final List<EstadoRespuesta> estadosRespuestas = new ArrayList<>();
    private JButton btnFinalizar;
    private Timer timer;
    private int segundos;

    private EjercicioMultiple ejercicioActual;
    private int ejerciciosTotales;
    private final List<EjercicioMultiple> ejerciciosGenerados = new ArrayList<>();
    private int gradoDificultad;
    private boolean respuestasEvaluadas;

    private enum EstadoPantalla {
        INTRO,
        EJERCICIO,
        HOJA_RESPUESTAS
    }

    private enum EstadoRespuesta {
        SIN_RESPUESTA,
        CORRECTA,
        INCORRECTA,
        INVALIDA
    }

    private EstadoPantalla estadoPantalla = EstadoPantalla.INTRO;

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
        mostrarIntro();
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
                } else if (estadoPantalla == EstadoPantalla.INTRO
                        && (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER)) {
                    iniciarSecuenciaEjercicios();
                } else if (estadoPantalla == EstadoPantalla.EJERCICIO
                        && (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER)) {
                    avanzarEjercicio();
                } else if (estadoPantalla == EstadoPantalla.HOJA_RESPUESTAS
                        && (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER)) {
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
                if (estadoPantalla == EstadoPantalla.INTRO) {
                    iniciarSecuenciaEjercicios();
                } else if (estadoPantalla == EstadoPantalla.EJERCICIO) {
                    avanzarEjercicio();
                }
            }
        });

        cardLayout = new CardLayout();
        panelPrincipal = new JPanel(cardLayout);
        frame.getContentPane().add(panelPrincipal);

        construirPanelIntro();
        construirPanelEjercicio();
        construirPanelCorreccion();
    }

    private void construirPanelIntro() {
        panelIntro = new JPanel(new BorderLayout());
        panelIntro.setBackground(Color.WHITE);

        lblIntroMensaje = new JLabel("", SwingConstants.CENTER);
        lblIntroMensaje.setFont(new Font("Arial", Font.BOLD, 60));
        lblIntroMensaje.setForeground(Color.DARK_GRAY);

        panelIntro.add(lblIntroMensaje, BorderLayout.CENTER);
        panelPrincipal.add(panelIntro, EstadoPantalla.INTRO.name());
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

        btnFinalizar = new JButton("Finalizar");
        btnFinalizar.setFont(new Font("Arial", Font.BOLD, 30));
        btnFinalizar.setEnabled(false);
        btnFinalizar.addActionListener(e -> {
            if (!respuestasEvaluadas) {
                evaluarRespuestas();
            } else {
                cerrarModo();
            }
        });
        panelBotones.add(btnFinalizar);

        panelCorreccion.add(panelBotones, BorderLayout.SOUTH);

        panelPrincipal.add(panelCorreccion, EstadoPantalla.HOJA_RESPUESTAS.name());
    }

    private JLabel crearEtiqueta(String texto, int tamanioFuente, int estilo, int alineacion) {
        JLabel etiqueta = new JLabel(texto, alineacion);
        etiqueta.setFont(new Font("Arial", estilo, tamanioFuente));
        return etiqueta;
    }

    private void mostrarIntro() {
        detenerContador();
        estadoPantalla = EstadoPantalla.INTRO;
        lblIntroMensaje.setText("<html><div style='text-align:center;'>Antes de comenzar anota la fecha actual \""
                + obtenerFechaActualTexto() + "\"</div></html>");
        cardLayout.show(panelPrincipal, EstadoPantalla.INTRO.name());
        frame.getContentPane().revalidate();
        frame.getContentPane().repaint();
    }

    private void iniciarSecuenciaEjercicios() {
        if (estadoPantalla == EstadoPantalla.INTRO) {
            mostrarSiguienteEjercicio();
        }
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
        estadosRespuestas.clear();

        for (int i = 0; i < ejerciciosGenerados.size(); i++) {
            EjercicioMultiple ejercicio = ejerciciosGenerados.get(i);

            JPanel fila = new JPanel();
            fila.setOpaque(false);
            fila.setLayout(new BoxLayout(fila, BoxLayout.Y_AXIS));

            JPanel filaSuperior = new JPanel();
            filaSuperior.setOpaque(false);
            filaSuperior.setLayout(new BoxLayout(filaSuperior, BoxLayout.X_AXIS));

            JLabel lblEjercicio = crearEtiqueta(formatearEjercicioParaHoja(ejercicio.getEjercicioTexto()), 40, Font.BOLD,
                    SwingConstants.LEFT);
            lblEjercicio.setAlignmentY(Component.CENTER_ALIGNMENT);
            filaSuperior.add(lblEjercicio);
            filaSuperior.add(Box.createHorizontalGlue());

            JTextField campoRespuesta = new JTextField();
            campoRespuesta.setFont(new Font("Arial", Font.PLAIN, 36));
            campoRespuesta.setHorizontalAlignment(SwingConstants.CENTER);
            Dimension campoDimension = new Dimension(250, 70);
            campoRespuesta.setPreferredSize(campoDimension);
            campoRespuesta.setMaximumSize(new Dimension(300, 70));
            campoRespuesta.setMinimumSize(new Dimension(200, 60));
            campoRespuesta.setAlignmentY(Component.CENTER_ALIGNMENT);
            filaSuperior.add(Box.createHorizontalStrut(20));
            filaSuperior.add(campoRespuesta);

            fila.add(filaSuperior);
            fila.add(Box.createVerticalStrut(10));

            JLabel lblRetro = crearEtiqueta("", 28, Font.PLAIN, SwingConstants.LEFT);
            lblRetro.setForeground(Color.DARK_GRAY);
            lblRetro.setAlignmentX(Component.LEFT_ALIGNMENT);
            fila.add(lblRetro);

            panelListaRespuestas.add(fila);
            panelListaRespuestas.add(Box.createVerticalStrut(30));

            final int indice = i;
            campoRespuesta.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    evaluarRespuestaInteractiva(indice);
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    evaluarRespuestaInteractiva(indice);
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    evaluarRespuestaInteractiva(indice);
                }
            });

            camposRespuestas.add(campoRespuesta);
            etiquetasRetroalimentacion.add(lblRetro);
            estadosRespuestas.add(EstadoRespuesta.SIN_RESPUESTA);
        }

        btnFinalizar.setText("Finalizar");
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
            EstadoRespuesta estado = evaluarRespuestaFinal(i);
            estadosRespuestas.set(i, estado);

            if (estado == EstadoRespuesta.CORRECTA) {
                gradoDificultad = NivelDificultad.clampGrado(gradoDificultad + 1);
                correctas++;
            } else if (estado == EstadoRespuesta.INCORRECTA || estado == EstadoRespuesta.INVALIDA
                    || estado == EstadoRespuesta.SIN_RESPUESTA) {
                gradoDificultad = NivelDificultad.clampGrado(gradoDificultad - 1);
            }
        }

        ConfigFileHelper.guardarGradoDificultad(gradoDificultad);

        NivelDificultad nivelActual = obtenerNivelActual();
        if (nivelActual != null) {
            lblCorreccionNivel.setText(descripcionNivel(nivelActual));
        }

        respuestasEvaluadas = true;
        btnFinalizar.setEnabled(true);
        btnFinalizar.setText("Cerrar");

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

    private void evaluarRespuestaInteractiva(int indice) {
        if (indice < 0 || indice >= ejerciciosGenerados.size()) {
            return;
        }

        EjercicioMultiple ejercicio = ejerciciosGenerados.get(indice);
        JTextField campo = camposRespuestas.get(indice);
        JLabel etiquetaRetro = etiquetasRetroalimentacion.get(indice);

        String texto = campo.getText().trim().replace(",", ".");

        if (texto.isEmpty()) {
            etiquetaRetro.setText("Ingresa tu respuesta.");
            etiquetaRetro.setForeground(Color.DARK_GRAY);
            estadosRespuestas.set(indice, EstadoRespuesta.SIN_RESPUESTA);
            actualizarEstadoFinalizar();
            return;
        }

        double resultadoEsperado = ejercicio.resultado;

        try {
            double respuestaUsuario = Double.parseDouble(texto);
            boolean esCorrecto = Math.abs(respuestaUsuario - resultadoEsperado) < 1e-6;
            if (esCorrecto) {
                etiquetaRetro.setText("¡Correcto!");
                etiquetaRetro.setForeground(new Color(0, 128, 0));
                estadosRespuestas.set(indice, EstadoRespuesta.CORRECTA);
            } else {
                etiquetaRetro.setText("Incorrecto. La correcta es " + formatearResultado(resultadoEsperado) + ".");
                etiquetaRetro.setForeground(Color.RED);
                estadosRespuestas.set(indice, EstadoRespuesta.INCORRECTA);
            }
        } catch (NumberFormatException ex) {
            etiquetaRetro.setText("Valor inválido. La correcta es " + formatearResultado(resultadoEsperado) + ".");
            etiquetaRetro.setForeground(Color.RED);
            estadosRespuestas.set(indice, EstadoRespuesta.INVALIDA);
        }

        actualizarEstadoFinalizar();
    }

    private EstadoRespuesta evaluarRespuestaFinal(int indice) {
        EjercicioMultiple ejercicio = ejerciciosGenerados.get(indice);
        JTextField campo = camposRespuestas.get(indice);
        JLabel etiquetaRetro = etiquetasRetroalimentacion.get(indice);

        String texto = campo.getText().trim().replace(",", ".");
        double resultadoEsperado = ejercicio.resultado;

        if (texto.isEmpty()) {
            etiquetaRetro.setText("Sin respuesta. La correcta es " + formatearResultado(resultadoEsperado) + ".");
            etiquetaRetro.setForeground(Color.RED);
            return EstadoRespuesta.SIN_RESPUESTA;
        }

        try {
            double respuestaUsuario = Double.parseDouble(texto);
            boolean esCorrecto = Math.abs(respuestaUsuario - resultadoEsperado) < 1e-6;
            if (esCorrecto) {
                etiquetaRetro.setText("¡Correcto!");
                etiquetaRetro.setForeground(new Color(0, 128, 0));
                return EstadoRespuesta.CORRECTA;
            }
            etiquetaRetro.setText("Incorrecto. La correcta es " + formatearResultado(resultadoEsperado) + ".");
            etiquetaRetro.setForeground(Color.RED);
            return EstadoRespuesta.INCORRECTA;
        } catch (NumberFormatException ex) {
            etiquetaRetro.setText("Valor inválido. La correcta es " + formatearResultado(resultadoEsperado) + ".");
            etiquetaRetro.setForeground(Color.RED);
            return EstadoRespuesta.INVALIDA;
        }
    }

    private void actualizarEstadoFinalizar() {
        boolean habilitar = camposRespuestas.stream().anyMatch(c -> !c.getText().trim().isEmpty());
        if (!respuestasEvaluadas) {
            btnFinalizar.setEnabled(habilitar);
        }
    }

    private String formatearEjercicioParaHoja(String texto) {
        return "<html><div style='padding-right:20px;'>" + texto + "</div></html>";
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

    private String obtenerFechaActualTexto() {
        LocalDate hoy = LocalDate.now();
        Locale locale = new Locale("es", "ES");
        String mes = hoy.getMonth().getDisplayName(TextStyle.FULL, locale).toLowerCase(locale);
        return hoy.getDayOfMonth() + " de " + mes + " de " + hoy.getYear();
    }
}
