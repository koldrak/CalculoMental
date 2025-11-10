import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> mostrarMenuInicial());
    }

    public static void mostrarMenuInicial() {
        JFrame frame = new JFrame("Menú Principal");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new GridLayout(3, 1, 20, 20));

        JButton btnEjercicios = new JButton("Comenzar Ejercicios");
        btnEjercicios.setFont(new Font("Arial", Font.BOLD, 30));
        btnEjercicios.addActionListener(e -> {
            frame.dispose();
            Generador generador = new Generador();
            Presentacion presentacion = new Presentacion(generador.generarEjercicios());
            presentacion.mostrar();
        });

        JButton btnDificultadInteligente = new JButton("Dificultad Inteligente");
        btnDificultadInteligente.setFont(new Font("Arial", Font.BOLD, 30));
        btnDificultadInteligente.addActionListener(e -> {
            frame.dispose();
            new ModoDificultadInteligente().iniciar();
        });

        JButton btnConfiguracion = new JButton("Configurar Opciones");
        btnConfiguracion.setFont(new Font("Arial", Font.BOLD, 30));
        btnConfiguracion.addActionListener(e -> {
            frame.dispose();
            ConfiguracionUI.mostrar(); // lanzamos una nueva ventana de configuración
        });

        frame.add(btnEjercicios);
        frame.add(btnDificultadInteligente);
        frame.add(btnConfiguracion);

        frame.setVisible(true);
    }
}

