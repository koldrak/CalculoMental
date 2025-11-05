import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;


public class Presentacion {
    private JFrame frame;
    private List<Ejercicio> ejercicios;
    private int indice = 0;
    private Timer timer;
    private int segundos;
    private int indiceCorreccion = 0;
    private List<JLabel> etiquetasResumen = new ArrayList<>();
    private boolean enPantallaIntroduccion = true;

    public Presentacion(List<Ejercicio> ejercicios) {
        this.ejercicios = ejercicios;
        frame = new JFrame("Generador de Ejercicios");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setUndecorated(true);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.addKeyListener(new Navegacion());
        frame.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                avanzar();
            }
        });
    }

    private void avanzar() {
        if (enPantallaIntroduccion) {
            enPantallaIntroduccion = false;
            mostrarPantalla();
        } else if (indice < ejercicios.size()) {
            indice++;
            mostrarPantalla();
        } else if (indiceCorreccion < ejercicios.size()) {
            JLabel etiqueta = etiquetasResumen.get(indiceCorreccion);
            etiqueta.setText(ejercicios.get(indiceCorreccion).getResultadoTexto());
            indiceCorreccion++;
        }
    }

    public void mostrar() {
        mostrarPantalla();
        frame.setVisible(true);
    }
  

    private void mostrarPantalla() {
        frame.getContentPane().removeAll();
        JPanel panel = new JPanel();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = screenSize.width;

        if (enPantallaIntroduccion) {
            if (timer != null) {
                timer.stop();
            }
            panel.setBackground(Color.WHITE);
            panel.setLayout(new GridBagLayout());

            LocalDate fechaActual = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", new Locale("es", "ES"));
            String fechaTexto = fechaActual.format(formatter);

            String mensaje = "<html><div style='text-align: center;'>Antes de comenzar anota la fecha actual" +
                    "<br><span style='font-size: 0.9em;'>\"" + fechaTexto + "\"</span></div></html>";
            JLabel introduccion = new JLabel(mensaje, SwingConstants.CENTER);
            int fontSizeIntroduccion = calcularTamanioFuente("Antes de comenzar anota la fecha actual \"" + fechaTexto + "\"",
                    screenWidth * 0.75);
            introduccion.setFont(new Font("Arial", Font.BOLD, fontSizeIntroduccion));
            introduccion.setForeground(Color.DARK_GRAY);
            introduccion.setHorizontalAlignment(SwingConstants.CENTER);
            introduccion.setVerticalAlignment(SwingConstants.CENTER);

            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.anchor = GridBagConstraints.CENTER;
            panel.add(introduccion, constraints);
        } else if (indice < ejercicios.size()) {
            Color fondo = generarColorPastelAleatorio();
            panel.setBackground(fondo);
            // Usamos BorderLayout para ubicar cronómetro y ejercicio fácilmente
            panel.setLayout(new BorderLayout());

            // Panel Norte con título "Ejercicio N" centrado y cronómetro a la izquierda
            JPanel panelNorte = new JPanel(new BorderLayout());
            panelNorte.setOpaque(false);

            // Título del ejercicio
            JLabel titulo = new JLabel("Ejercicio " + (indice + 1), SwingConstants.CENTER);
            titulo.setFont(new Font("Arial", Font.BOLD, 60));
            titulo.setForeground(Color.DARK_GRAY);
            panelNorte.add(titulo, BorderLayout.CENTER);

            // Cronómetro a la izquierda
            JLabel contador = new JLabel("⏱ 0s");
            contador.setFont(new Font("Arial", Font.PLAIN, 50));
            contador.setForeground(Color.BLUE);
            panelNorte.add(contador, BorderLayout.WEST);

            iniciarContador(contador);
            panel.add(panelNorte, BorderLayout.NORTH);

            // Texto del ejercicio al centro
            JLabel label = new JLabel(ejercicios.get(indice).getEjercicioTexto(), SwingConstants.CENTER);
            int fontSize = calcularTamanioFuente(ejercicios.get(indice).getEjercicioTexto(), screenWidth * 0.7);
            label.setFont(new Font("Arial", Font.BOLD, fontSize));
            label.setVerticalAlignment(SwingConstants.CENTER);
            panel.add(label, BorderLayout.CENTER);
        } else {
            panel.setLayout(new GridLayout(ejercicios.size() + 1, 1));

            if (timer != null) timer.stop();

            JLabel titulo = new JLabel("Corrección", SwingConstants.CENTER);
            titulo.setFont(new Font("Arial", Font.BOLD, 60));
            panel.add(titulo);

            etiquetasResumen.clear();
            indiceCorreccion = 0;

            for (Ejercicio ej : ejercicios) {
                JLabel etiqueta = new JLabel(ej.getEjercicioTexto(), SwingConstants.CENTER);
                etiqueta.setFont(new Font("Arial", Font.PLAIN, 50));
                etiquetasResumen.add(etiqueta);
                panel.add(etiqueta);
            }
        }


        frame.getContentPane().add(panel);
        frame.revalidate();
        frame.repaint();
    }

    private int calcularTamanioFuente(String texto, double anchoObjetivo) {
        int fontSize = 10;
        int anchoTexto;

        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();

        do {
            Font font = new Font("Arial", Font.BOLD, fontSize);
            g2d.setFont(font);
            FontMetrics metrics = g2d.getFontMetrics();
            anchoTexto = metrics.stringWidth(texto);
            fontSize++;
        } while (anchoTexto < anchoObjetivo && fontSize < 300);

        g2d.dispose();
        return fontSize - 1;
    }

    private void iniciarContador(JLabel label) {
        if (timer != null) {
            timer.stop();
        }
        label.setText("⏱ 0s");
        segundos = 0;
        timer = new Timer(1000, e -> {
            segundos++;
            label.setText("⏱ " + segundos + "s");
        });
        timer.start();
    }

    private class Navegacion extends KeyAdapter {
    	public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_SPACE) {
                avanzar();
            } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                if (indice > 0) {
                    indice--;
                    mostrarPantalla();
                } else if (!enPantallaIntroduccion) {
                    enPantallaIntroduccion = true;
                    mostrarPantalla();
                }
            } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                frame.dispose();
            }
        }
    }
    private Color generarColorPastelAleatorio() {
        Random rand = new Random();
        int r = 180 + rand.nextInt(76); // 180 a 255
        int g = 180 + rand.nextInt(76);
        int b = 180 + rand.nextInt(76);
        return new Color(r, g, b);
    }

}
