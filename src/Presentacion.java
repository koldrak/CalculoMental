import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.ArrayList;


public class Presentacion {
    private static final int EJERCICIOS_POR_HOJA = 7;
    private JFrame frame;
    private List<Ejercicio> ejercicios;
    private int indice = 0;
    private Timer timer;
    private int segundos;
    private int indiceCorreccion = 0;
    private int paginaCorreccion = 0;
    private List<JLabel> etiquetasResumen = new ArrayList<>();

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
        if (indice < ejercicios.size()) {
            indice++;
            if (indice == ejercicios.size()) {
                paginaCorreccion = 0;
                indiceCorreccion = 0;
            }
            mostrarPantalla();
        } else if (indiceCorreccion < ejercicios.size()) {
            int paginaDeIndice = indiceCorreccion / EJERCICIOS_POR_HOJA;
            if (paginaDeIndice != paginaCorreccion) {
                paginaCorreccion = paginaDeIndice;
                mostrarPantalla();
            }

            int indiceEnPagina = indiceCorreccion % EJERCICIOS_POR_HOJA;
            if (indiceEnPagina < etiquetasResumen.size()) {
                etiquetasResumen.get(indiceEnPagina).setText(ejercicios.get(indiceCorreccion).getResultadoTexto());
            }
            indiceCorreccion++;

            if (indiceCorreccion < ejercicios.size()) {
                int siguientePagina = indiceCorreccion / EJERCICIOS_POR_HOJA;
                if (siguientePagina != paginaCorreccion) {
                    paginaCorreccion = siguientePagina;
                    mostrarPantalla();
                }
            }
        }
    }

    public void mostrar() {
        mostrarPantalla();
        frame.setVisible(true);
    }
  

    private void mostrarPantalla() {
        frame.getContentPane().removeAll();
        JPanel panel = new JPanel();
        Color fondo = (indice < ejercicios.size()) ? generarColorPastelAleatorio() : Color.WHITE;
        panel.setBackground(fondo);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = screenSize.width;

        if (indice < ejercicios.size()) {
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
        }
        else {
            panel.setLayout(new BorderLayout());

            if (timer != null) timer.stop();

            JLabel titulo = new JLabel("Corrección", SwingConstants.CENTER);
            titulo.setFont(new Font("Arial", Font.BOLD, 60));
            panel.add(titulo, BorderLayout.NORTH);

            etiquetasResumen.clear();

            int totalPaginas = Math.max(1, (int) Math.ceil((double) ejercicios.size() / EJERCICIOS_POR_HOJA));
            paginaCorreccion = Math.min(paginaCorreccion, totalPaginas - 1);

            JPanel hojaPanel = new JPanel(new GridLayout(EJERCICIOS_POR_HOJA, 1));
            int inicio = paginaCorreccion * EJERCICIOS_POR_HOJA;
            int fin = Math.min(inicio + EJERCICIOS_POR_HOJA, ejercicios.size());

            for (int i = inicio; i < fin; i++) {
                boolean resuelto = i < indiceCorreccion;
                String texto = resuelto ? ejercicios.get(i).getResultadoTexto() : ejercicios.get(i).getEjercicioTexto();
                JLabel etiqueta = new JLabel(texto, SwingConstants.CENTER);
                etiqueta.setFont(new Font("Arial", Font.PLAIN, 50));
                etiquetasResumen.add(etiqueta);
                hojaPanel.add(etiqueta);
            }

            for (int i = fin; i < inicio + EJERCICIOS_POR_HOJA; i++) {
                JLabel relleno = new JLabel("", SwingConstants.CENTER);
                relleno.setFont(new Font("Arial", Font.PLAIN, 50));
                hojaPanel.add(relleno);
            }

            panel.add(hojaPanel, BorderLayout.CENTER);

            if (totalPaginas > 1) {
                JLabel indicador = new JLabel("Hoja " + (paginaCorreccion + 1) + " de " + totalPaginas, SwingConstants.CENTER);
                indicador.setFont(new Font("Arial", Font.ITALIC, 30));
                panel.add(indicador, BorderLayout.SOUTH);
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
