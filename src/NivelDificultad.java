import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NivelDificultad {
    private static final String NIVELES_PATH = "niveles_dificultad.csv";
    private static final Pattern PATRON_RANGO = Pattern.compile(
            "\\s*(-?\\d+(?:\\.\\d+)?)[\\s]*-[\\s]*(-?\\d+(?:\\.\\d+)?)(?:\\s*@\\s*(\\d+))?\\s*");
    private static List<NivelDificultad> cache;

    private final String descripcion;
    private final int minimo;
    private final int maximo;
    private final int cantidadOperaciones;
    private final int cantidadEjercicios;
    private final boolean permitirDecimales;
    private final boolean permitirNegativos;
    private final boolean permitirParentesis;
    private final boolean permitirDespejarX;
    private final String[] operadoresPermitidos;
    private final String definicionNumeros;

    public NivelDificultad(String descripcion, int minimo, int maximo, int cantidadOperaciones,
            int cantidadEjercicios, boolean permitirDecimales, boolean permitirNegativos,
            boolean permitirParentesis, boolean permitirDespejarX, String[] operadoresPermitidos,
            String definicionNumeros) {
        this.descripcion = descripcion;
        this.minimo = minimo;
        this.maximo = maximo;
        this.cantidadOperaciones = cantidadOperaciones;
        this.cantidadEjercicios = cantidadEjercicios;
        this.permitirDecimales = permitirDecimales;
        this.permitirNegativos = permitirNegativos;
        this.permitirParentesis = permitirParentesis;
        this.permitirDespejarX = permitirDespejarX;
        this.operadoresPermitidos = operadoresPermitidos.clone();
        this.definicionNumeros = definicionNumeros;
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

    public int getCantidadEjercicios() {
        return cantidadEjercicios;
    }

    public boolean isPermitirDecimales() {
        return permitirDecimales;
    }

    public boolean isPermitirNegativos() {
        return permitirNegativos;
    }

    public boolean isPermitirParentesis() {
        return permitirParentesis;
    }

    public boolean isPermitirDespejarX() {
        return permitirDespejarX;
    }

    public String[] getOperadoresPermitidos() {
        return operadoresPermitidos.clone();
    }

    public String getOperadoresComoCadena() {
        return String.join(",", operadoresPermitidos);
    }

    public String getDefinicionNumeros() {
        return definicionNumeros;
    }

    public List<Double> obtenerNumeros() {
        return parsearDefinicionNumeros(definicionNumeros, false);
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
                    if (linea.trim().isEmpty() || linea.trim().startsWith("#")) {
                        continue;
                    }
                    String[] partes = parseLineaCsv(linea);
                    if (partes.length == 0 || esEncabezado(partes)) {
                        continue;
                    }
                    if (partes.length < 11) {
                        continue;
                    }

                    String descripcion = partes[0].trim();
                    Integer minimo = parseEntero(partes[1]);
                    Integer maximo = parseEntero(partes[2]);
                    Integer cantidadOperaciones = parseEntero(partes[3]);
                    Integer cantidadEjercicios = parseEntero(partes[4]);
                    Boolean permitirDecimales = parseBooleano(partes[5]);
                    Boolean permitirNegativos = parseBooleano(partes[6]);
                    Boolean permitirParentesis = parseBooleano(partes[7]);
                    Boolean permitirDespejarX = parseBooleano(partes[8]);
                    String[] operadores = parseOperadores(partes[9]);
                    String definicionNumeros = partes[10].trim();

                    if (descripcion.isEmpty() || minimo == null || maximo == null
                            || cantidadOperaciones == null || cantidadEjercicios == null
                            || permitirDecimales == null || permitirNegativos == null
                            || permitirParentesis == null || permitirDespejarX == null
                            || operadores.length == 0 || definicionNumeros.isEmpty()) {
                        continue;
                    }
                    try {
                        parsearDefinicionNumeros(definicionNumeros, false);
                    } catch (IllegalArgumentException e) {
                        continue;
                    }
                    niveles.add(new NivelDificultad(descripcion, minimo, maximo, cantidadOperaciones,
                            cantidadEjercicios, permitirDecimales, permitirNegativos,
                            permitirParentesis, permitirDespejarX, operadores, definicionNumeros));
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
            writer.println("descripcion,min,max,operaciones,ejercicios,decimales,negativos,parentesis,despejarX,operadores,numeros");
            for (NivelDificultad nivel : niveles) {
                writer.println(formatearLinea(nivel));
            }
        } catch (IOException e) {
            System.out.println("No se pudo guardar " + NIVELES_PATH + ".");
        }
    }

    private static List<NivelDificultad> nivelesPredeterminados() {
        List<NivelDificultad> predeterminados = new ArrayList<>();
        predeterminados.add(new NivelDificultad("Nivel 1 (0 a 5 pts)", 0, 5, 2, 7,
                false, false, false, false, new String[]{"+"}, "1-10"));
        predeterminados.add(new NivelDificultad("Nivel 2 (6 a 10 pts)", 6, 10, 2, 7,
                false, false, false, false, new String[]{"+"}, "1-50"));
        predeterminados.add(new NivelDificultad("Nivel 3 (11 a 15 pts)", 11, 15, 3, 7,
                false, false, false, false, new String[]{"+", "-"}, "1-50"));
        return predeterminados;
    }

    private static String formatearLinea(NivelDificultad nivel) {
        return String.join(",",
                Arrays.asList(
                        escaparCsv(nivel.getDescripcion()),
                        String.valueOf(nivel.getMinimo()),
                        String.valueOf(nivel.getMaximo()),
                        String.valueOf(nivel.getCantidadOperaciones()),
                        String.valueOf(nivel.getCantidadEjercicios()),
                        escaparCsv(nivel.isPermitirDecimales() ? "SI" : "NO"),
                        escaparCsv(nivel.isPermitirNegativos() ? "SI" : "NO"),
                        escaparCsv(nivel.isPermitirParentesis() ? "SI" : "NO"),
                        escaparCsv(nivel.isPermitirDespejarX() ? "SI" : "NO"),
                        escaparCsv(nivel.getOperadoresComoCadena()),
                        escaparCsv(nivel.getDefinicionNumeros())));
    }

    private static String escaparCsv(String valor) {
        String contenido = valor == null ? "" : valor;
        String escapado = contenido.replace("\"", "\"\"");
        if (escapado.contains(",") || escapado.contains("\"") || escapado.contains("\n") || escapado.contains("\r")) {
            return "\"" + escapado + "\"";
        }
        return escapado;
    }

    private static boolean esEncabezado(String[] columnas) {
        if (columnas.length == 0) {
            return false;
        }
        String primera = columnas[0].trim().toLowerCase();
        return "descripcion".equals(primera) || "descripción".equals(primera);
    }

    private static String[] parseLineaCsv(String linea) {
        List<String> valores = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean enComillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (enComillas) {
                if (c == '"') {
                    if (i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                        actual.append('"');
                        i++;
                    } else {
                        enComillas = false;
                    }
                } else {
                    actual.append(c);
                }
            } else {
                if (c == '"') {
                    enComillas = true;
                } else if (c == ',') {
                    valores.add(actual.toString().trim());
                    actual.setLength(0);
                } else {
                    actual.append(c);
                }
            }
        }
        valores.add(actual.toString().trim());
        return valores.toArray(new String[0]);
    }

    private static Integer parseEntero(String valor) {
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Boolean parseBooleano(String valor) {
        String normalizado = valor.trim().toUpperCase();
        if ("SI".equals(normalizado)) {
            return Boolean.TRUE;
        }
        if ("NO".equals(normalizado)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static String[] parseOperadores(String valor) {
        return Arrays.stream(valor.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    public static List<Double> parsearDefinicionNumeros(String definicion, boolean permitirVacio) {
        List<Double> valores = new ArrayList<>();
        if (definicion == null) {
            definicion = "";
        }
        String[] partes = definicion.split(";");
        for (String parte : partes) {
            String token = parte.trim();
            if (token.isEmpty()) {
                continue;
            }
            Optional<RangoDatos> rango = intentarParsearRango(token);
            if (rango.isPresent()) {
                RangoDatos datos = rango.get();
                agregarRango(valores, datos.inicio, datos.fin, datos.decimales, token);
            } else {
                try {
                    valores.add(Double.parseDouble(token));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Número o rango inválido: " + token);
                }
            }
        }
        if (!permitirVacio && valores.isEmpty()) {
            throw new IllegalArgumentException("La definición de números no contiene valores válidos.");
        }
        return valores;
    }

    private static Optional<RangoDatos> intentarParsearRango(String token) {
        Matcher matcher = PATRON_RANGO.matcher(token);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        try {
            BigDecimal inicio = new BigDecimal(matcher.group(1));
            BigDecimal fin = new BigDecimal(matcher.group(2));
            if (inicio.compareTo(fin) > 0) {
                BigDecimal tmp = inicio;
                inicio = fin;
                fin = tmp;
            }
            int decimales = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : inferirDecimales(inicio, fin);
            return Optional.of(new RangoDatos(inicio, fin, Math.max(0, decimales)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static void agregarRango(List<Double> valores, BigDecimal inicio, BigDecimal fin, int decimales, String token) {
        if (inicio.compareTo(fin) == 0) {
            valores.add(inicio.doubleValue());
            return;
        }
        BigDecimal paso = determinarPaso(inicio, fin, decimales);
        if (paso.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("No se pudo determinar un paso válido para el rango: " + token);
        }
        BigDecimal actual = inicio;
        int salvaguarda = 0;
        while (actual.compareTo(fin) <= 0 && salvaguarda < 10000) {
            valores.add(actual.doubleValue());
            actual = actual.add(paso);
            salvaguarda++;
        }
        if (salvaguarda >= 10000) {
            throw new IllegalArgumentException("El rango es demasiado amplio: " + token);
        }
        if (Math.abs(valores.get(valores.size() - 1) - fin.doubleValue()) > 1e-9) {
            valores.add(fin.doubleValue());
        }
    }

    private static BigDecimal determinarPaso(BigDecimal inicio, BigDecimal fin, int decimales) {
        BigDecimal paso = decimales > 0 ? BigDecimal.ONE.movePointLeft(decimales) : BigDecimal.ONE;
        BigDecimal diferencia = fin.subtract(inicio).abs();
        if (diferencia.compareTo(paso) < 0) {
            return diferencia;
        }
        return paso;
    }

    private static int inferirDecimales(BigDecimal inicio, BigDecimal fin) {
        return Math.max(obtenerEscala(inicio), obtenerEscala(fin));
    }

    private static int obtenerEscala(BigDecimal valor) {
        int escala = valor.stripTrailingZeros().scale();
        return Math.max(escala, 0);
    }

    private static class RangoDatos {
        private final BigDecimal inicio;
        private final BigDecimal fin;
        private final int decimales;

        private RangoDatos(BigDecimal inicio, BigDecimal fin, int decimales) {
            this.inicio = inicio;
            this.fin = fin;
            this.decimales = decimales;
        }
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
