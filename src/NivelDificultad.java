import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NivelDificultad {
    private static final String NIVELES_PATH = "niveles_dificultad.txt";
    private static List<NivelDificultad> cache;

    private final String descripcion;
    private final int minimo;
    private final int maximo;
    private final int cantidadOperaciones;
    private final int maximoNumero;
    private final String[] operadoresPermitidos;

    public NivelDificultad(String descripcion, int minimo, int maximo, int cantidadOperaciones,
            int maximoNumero, String[] operadoresPermitidos) {
        this.descripcion = descripcion;
        this.minimo = minimo;
        this.maximo = maximo;
        this.cantidadOperaciones = cantidadOperaciones;
        this.maximoNumero = maximoNumero;
        this.operadoresPermitidos = operadoresPermitidos.clone();
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getMinimo() {
        return minimo;
    }

    public int getMaximo() {
        return maximo;
    }

    public int getCantidadOperaciones() {
        return cantidadOperaciones;
    }

    public int getMaximoNumero() {
        return maximoNumero;
    }

    public String[] getOperadoresPermitidos() {
        return operadoresPermitidos.clone();
    }

    public String getOperadoresComoCadena() {
        return String.join(",", operadoresPermitidos);
    }

    public static synchronized List<NivelDificultad> obtenerNiveles() {
        if (cache == null) {
            cache = Collections.unmodifiableList(new ArrayList<>(cargarDesdeArchivo()));
        }
        return cache;
    }

    public static synchronized void guardarNiveles(List<NivelDificultad> niveles) {
        List<NivelDificultad> ordenados = new ArrayList<>(niveles);
        ordenados.sort(Comparator.comparingInt(NivelDificultad::getMinimo));
        escribirNiveles(ordenados);
        cache = Collections.unmodifiableList(new ArrayList<>(ordenados));
    }

    public static synchronized void recargar() {
        cache = null;
    }

    public static NivelDificultad desdeGrado(int grado) {
        List<NivelDificultad> niveles = obtenerNiveles();
        if (niveles.isEmpty()) {
            return null;
        }
        for (NivelDificultad nivel : niveles) {
            if (grado <= nivel.getMaximo()) {
                return nivel;
            }
        }
        return niveles.get(niveles.size() - 1);
    }

    public static int clampGrado(int grado) {
        List<NivelDificultad> niveles = obtenerNiveles();
        if (niveles.isEmpty()) {
            return grado;
        }
        int minimo = niveles.get(0).getMinimo();
        int maximo = niveles.get(niveles.size() - 1).getMaximo();
        if (grado < minimo) {
            return minimo;
        }
        if (grado > maximo) {
            return maximo;
        }
        return grado;
    }

    private static List<NivelDificultad> cargarDesdeArchivo() {
        List<NivelDificultad> niveles = new ArrayList<>();
        File archivo = new File(NIVELES_PATH);
        if (archivo.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
                String linea;
                while ((linea = reader.readLine()) != null) {
                    linea = linea.trim();
                    if (linea.isEmpty() || linea.startsWith("#")) {
                        continue;
                    }
                    String[] partes = linea.split("\\|");
                    if (partes.length < 6) {
                        continue;
                    }
                    String descripcion = partes[0].trim();
                    Integer minimo = parseEntero(partes[1]);
                    Integer maximo = parseEntero(partes[2]);
                    Integer cantidadOperaciones = parseEntero(partes[3]);
                    Integer maximoNumero = parseEntero(partes[4]);
                    String[] operadores = parseOperadores(partes[5]);
                    if (descripcion.isEmpty() || minimo == null || maximo == null
                            || cantidadOperaciones == null || maximoNumero == null
                            || operadores.length == 0) {
                        continue;
                    }
                    niveles.add(new NivelDificultad(descripcion, minimo, maximo, cantidadOperaciones,
                            maximoNumero, operadores));
                }
            } catch (IOException e) {
                System.out.println("No se pudo leer " + NIVELES_PATH + ". Se utilizarán niveles predeterminados.");
            }
        }

        if (niveles.isEmpty()) {
            niveles = nivelesPredeterminados();
            escribirNiveles(niveles);
        }

        niveles.sort(Comparator.comparingInt(NivelDificultad::getMinimo));
        return niveles;
    }

    private static void escribirNiveles(List<NivelDificultad> niveles) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(NIVELES_PATH))) {
            for (NivelDificultad nivel : niveles) {
                writer.println(formatearLinea(nivel));
            }
        } catch (IOException e) {
            System.out.println("No se pudo guardar " + NIVELES_PATH + ".");
        }
    }

    private static List<NivelDificultad> nivelesPredeterminados() {
        List<NivelDificultad> predeterminados = new ArrayList<>();
        predeterminados.add(new NivelDificultad("Nivel 1 (0 a 5 pts)", 0, 5, 2, 10, new String[]{"+"}));
        predeterminados.add(new NivelDificultad("Nivel 2 (6 a 10 pts)", 6, 10, 2, 50, new String[]{"+"}));
        predeterminados.add(new NivelDificultad("Nivel 3 (11 a 15 pts)", 11, 15, 3, 50, new String[]{"+", "-"}));
        return predeterminados;
    }

    private static String formatearLinea(NivelDificultad nivel) {
        return String.join("|",
                Arrays.asList(
                        nivel.getDescripcion(),
                        String.valueOf(nivel.getMinimo()),
                        String.valueOf(nivel.getMaximo()),
                        String.valueOf(nivel.getCantidadOperaciones()),
                        String.valueOf(nivel.getMaximoNumero()),
                        nivel.getOperadoresComoCadena()));
    }

    private static Integer parseEntero(String valor) {
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String[] parseOperadores(String valor) {
        return Arrays.stream(valor.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
