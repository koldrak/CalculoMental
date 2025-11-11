import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;


public class Generador {
    private static final Pattern PATRON_RANGO = Pattern.compile("\\s*(-?\\d+(?:\\.\\d+)?)[\\s]*-[\\s]*(-?\\d+(?:\\.\\d+)?)(?:\\s*@\\s*(\\d+))?\\s*");
    private static final int MAX_DECIMALES_VISIBLES = 6;
    private static final double EPSILON = 1e-9;
    private static final double EPSILON_ENTERO = 1e-6;

    private boolean permitirDecimales = true;
    private boolean permitirNegativos = true;
    private List<Double> numeros;
    private List<String> simbolos;
    private int cantidadOperaciones =2;
    private boolean permitirParentesis = false;
    private boolean permitirDespejarX = false;
    private int cantidadEjercicios = 7;

    Generador() {
        String ruta = System.getProperty("user.dir") + "/";

        leerConfig(ruta + "config.txt");

        List<Double> numerosLeidos = leerNumeros(ruta + "numeros.txt");
        numeros = ajustarDecimalesSegunConfiguracion(numerosLeidos);
        if (numeros.isEmpty()) {
            mostrarErrorNumerosVacios();
        }

        simbolos = leerSimbolosDesdeConfig(ruta + "simbolos.txt");
        if (simbolos.isEmpty()) {
            mostrarErrorSimbolosVacios();
        }
    }

    public Generador(NivelDificultad nivel) {
        this.permitirDecimales = nivel.isPermitirDecimales();
        this.permitirNegativos = nivel.isPermitirNegativos();
        this.permitirParentesis = nivel.isPermitirParentesis();
        this.permitirDespejarX = nivel.isPermitirDespejarX();
        this.cantidadOperaciones = Math.max(1, nivel.getCantidadOperaciones());
        this.cantidadEjercicios = Math.max(1, nivel.getCantidadEjercicios());

        List<Double> numerosDefinidos = nivel.obtenerNumeros();
        if (numerosDefinidos.isEmpty()) {
            throw new IllegalArgumentException("La definición de números del nivel está vacía.");
        }
        this.numeros = ajustarDecimalesSegunConfiguracion(numerosDefinidos);
        if (this.numeros.isEmpty()) {
            throw new IllegalArgumentException("La configuración de números no produce valores válidos.");
        }

        String[] operadores = nivel.getOperadoresPermitidos();
        this.simbolos = new ArrayList<>(operadores.length);
        for (String operador : operadores) {
            if (!operador.isEmpty()) {
                this.simbolos.add(operador);
            }
        }
        if (this.simbolos.isEmpty()) {
            throw new IllegalArgumentException("Debes definir al menos un operador para el nivel.");
        }
    }

    public List<Ejercicio> generarEjercicios() {
        List<Ejercicio> lista = new ArrayList<>();

        if (!permitirDespejarX) {
            for (int i = 0; i < cantidadEjercicios; i++) {
                lista.add(generarEjercicio());
            }
            return lista;
        }

        int minEjerciciosDespejarX = (int) Math.ceil(cantidadEjercicios / 2.0);
        List<Ejercicio> ejerciciosDespejarX = generarEjerciciosDespejarX(minEjerciciosDespejarX);

        while (ejerciciosDespejarX.size() < minEjerciciosDespejarX) {
            ejerciciosDespejarX.add(generarEjercicioDespejarXSimple());
        }

        lista.addAll(ejerciciosDespejarX);

        while (lista.size() < cantidadEjercicios) {
            lista.add(generarEjercicio());
        }

        Collections.shuffle(lista);
        return lista;
    }

    public EjercicioMultiple generarEjercicioUnico() {
        int originalCantidad = this.cantidadEjercicios;
        try {
            this.cantidadEjercicios = 1;
            List<Ejercicio> ejercicios = generarEjercicios();
            if (!ejercicios.isEmpty() && ejercicios.get(0) instanceof EjercicioMultiple) {
                return (EjercicioMultiple) ejercicios.get(0);
            }
            if (!ejercicios.isEmpty()) {
                Ejercicio ejercicio = ejercicios.get(0);
                return new EjercicioMultiple(ejercicio.getEjercicioTexto().replace(" = ?", ""), ejercicio.resultado);
            }
        } finally {
            this.cantidadEjercicios = originalCantidad;
        }
        throw new IllegalStateException("No se pudo generar un ejercicio válido con la configuración actual.");
    }


    private Ejercicio generarEjercicio() {
        Random r = new Random();
        int intentos = 0;

        while (intentos < 1000) {
            intentos++;

            StringBuilder expresion = new StringBuilder();
            double acumulador = numeros.get(r.nextInt(numeros.size()));
            expresion.append(formatearNumero(acumulador));

            boolean valido = true;

            for (int i = 1; i < cantidadOperaciones; i++) {
                String operador = simbolos.get(r.nextInt(simbolos.size()));
                double siguiente = numeros.get(r.nextInt(numeros.size()));

                if (operador.equals("/") && siguiente == 0) {
                    valido = false;
                    break;
                }

                if (operador.equals("/") && !permitirDecimales && acumulador % siguiente != 0) {
                    valido = false;
                    break;
                }

                expresion.append(" ").append(operador).append(" ").append(formatearNumero(siguiente));

                switch (operador) {
                case "/":
                    acumulador = acumulador / siguiente;
                    break;
                case "*":
                    acumulador *= siguiente;
                    break;
                case "+":
                    acumulador += siguiente;
                    break;
                case "-":
                    acumulador -= siguiente;
                    break;
            }

            // Validar acumulador intermedio
            if (!permitirNegativos && acumulador < 0) {
                valido = false;
                break;
            }

            }

            if (valido) {
                String expresionFinal = expresion.toString();
                if (permitirParentesis) {
                    expresionFinal = insertarParentesis(expresionFinal);
                }

                double resultado = evaluarExpresion(expresionFinal);

                if (!permitirDecimales) {
                    if (!esEntero(resultado)) continue;
                    resultado = normalizarEntero(resultado);
                }
                if (!permitirNegativos && resultado < 0) continue;

                return new EjercicioMultiple(expresionFinal, resultado);
            }

        }

        // Fallback con varias divisiones exactas
        double fallbackAcumulador = numeros.get(r.nextInt(numeros.size()));
        if (!permitirDecimales) {
            fallbackAcumulador = seleccionarEnteroDeRespaldo(fallbackAcumulador);
        }

        StringBuilder expr = new StringBuilder(formatearNumero(fallbackAcumulador));

        for (int i = 1; i < cantidadOperaciones; i++) {
            if (!permitirDecimales) {
                long acumuladorEntero = (long) normalizarEntero(fallbackAcumulador);
                long divisorEntero = seleccionarDivisorEntero(acumuladorEntero);
                expr.append(" / ").append(formatearNumero(divisorEntero));
                fallbackAcumulador = acumuladorEntero / (double) divisorEntero;
            } else {
                double divisor = numeros.get(r.nextInt(numeros.size()));
                if (Math.abs(divisor) < EPSILON) divisor = 1;
                expr.append(" / ").append(formatearNumero(divisor));
                fallbackAcumulador = fallbackAcumulador / divisor;
            }
        }

        double resultado = permitirDecimales ? fallbackAcumulador : normalizarEntero(fallbackAcumulador);
        return new EjercicioMultiple(expr.toString(), resultado);
    }
    
    private String insertarParentesis(String expr) {
        List<String> tokens = new ArrayList<>(Arrays.asList(expr.trim().split(" ")));
        if (tokens.size() < 7) return expr; // muy corto para dos bloques

        List<int[]> bloques = new ArrayList<>();
        Random r = new Random();
        int intentos = 0;

        while (bloques.size() < 2 && intentos < 10) {
            intentos++;

            // Posibles inicios válidos
            List<Integer> posiblesInicios = new ArrayList<>();
            for (int i = 0; i < tokens.size() - 4; i += 2) {
            	if (!tokens.get(i).equalsIgnoreCase("x")
            		    && !tokens.get(i + 2).equalsIgnoreCase("x")
            		    && esNumero(tokens.get(i))
            		    && esOperador(tokens.get(i + 1))
            		    && esNumero(tokens.get(i + 2))) {

                    posiblesInicios.add(i);
                }
            }

            // Evitar los que cubran todo
            List<Integer> internos = new ArrayList<>();
            for (int i : posiblesInicios) {
                if (i > 0 && i < tokens.size() - 4) internos.add(i);
            }
            if (internos.isEmpty()) break;

            int inicio = internos.get(r.nextInt(internos.size()));
            int fin = inicio + 2;

            int maxOps = r.nextInt(1, 4);
            int cuenta = 1;
            while (cuenta < maxOps && fin + 2 < tokens.size()
                    && esOperador(tokens.get(fin + 1))
                    && esNumero(tokens.get(fin + 2))) {
                fin += 2;
                cuenta++;
            }

            // Verificar solapamiento
            boolean solapa = false;
            for (int[] bloque : bloques) {
                if (!(fin < bloque[0] || inicio > bloque[1])) {
                    solapa = true;
                    break;
                }
            }

            if (!solapa) {
                bloques.add(new int[]{inicio, fin});
            }
        }

        // Insertar paréntesis de atrás hacia adelante para no romper índices
        Collections.reverse(bloques);
        for (int[] bloque : bloques) {
            tokens.add(bloque[0], "(");
            tokens.add(bloque[1] + 2, ")");
        }

     // Corregimos secuencias como ") (" o "( )" insertando una operación válida
        for (int i = 1; i < tokens.size() - 1; i++) {
            if (tokens.get(i - 1).equals(")") && tokens.get(i + 1).equals("(") && esOperadorValido(tokens.get(i))) {
                // ok, ya hay operador entre paréntesis
                continue;
            }

            // Si hay paréntesis juntos sin operador, insertamos uno aleatorio
            if (tokens.get(i).equals(")") && tokens.get(i + 1).equals("(")) {
                String operador = simbolos.isEmpty() ? "+" : simbolos.get(new Random().nextInt(simbolos.size()));
                tokens.add(i + 1, operador);
            }

            // Si hay un bloque "( )", lo ignoramos o lo removemos
            if (tokens.get(i - 1).equals("(") && tokens.get(i).equals(")")) {
                tokens.remove(i); // elimina ")"
                tokens.remove(i - 1); // elimina "("
                i -= 2;
            }
        }

        return String.join(" ", tokens);

    }
    private boolean esOperadorValido(String s) {
        return s.equals("+") || s.equals("-") || s.equals("*") || s.equals("/");
    }


    private boolean esNumero(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean esOperador(String s) {
        return s.equals("+") || s.equals("-") || s.equals("*") || s.equals("/");
    }


    private double evaluarExpresion(String expr) {
        try {
            List<String> postfija = infixToPostfix(expr);
            return evaluarPostfija(postfija);
        } catch (Exception e) {
            System.out.println("Error evaluando: " + expr);
            return 0;
        }
    }

    private List<String> infixToPostfix(String expr) {
        String[] tokens = expr.split(" ");
        Stack<String> operadores = new Stack<>();
        List<String> salida = new ArrayList<>();

        Map<String, Integer> prioridad = new HashMap<>();
        prioridad.put("+", 1);
        prioridad.put("-", 1);
        prioridad.put("*", 2);
        prioridad.put("/", 2);

        for (String token : tokens) {
            if (token.matches("-?\\d+(\\.\\d+)?")) {
                salida.add(token);
            } else if (token.equals("(")) {
                operadores.push(token);
            } else if (token.equals(")")) {
                while (!operadores.isEmpty() && !operadores.peek().equals("(")) {
                    salida.add(operadores.pop());
                }
                if (!operadores.isEmpty() && operadores.peek().equals("(")) {
                    operadores.pop(); // eliminar el "("
                }
            } else if (prioridad.containsKey(token)) {
                while (!operadores.isEmpty() && prioridad.containsKey(operadores.peek())
                        && prioridad.get(token) <= prioridad.get(operadores.peek())) {
                    salida.add(operadores.pop());
                }
                operadores.push(token);
            }
        }

        while (!operadores.isEmpty()) {
            salida.add(operadores.pop());
        }

        return salida;
    }


    private double evaluarPostfija(List<String> tokens) {
        Stack<Double> stack = new Stack<>();
        for (String token : tokens) {
            if (token.matches("-?\\d+(\\.\\d+)?")) {
                stack.push(Double.parseDouble(token));
            } else {
                double b = stack.pop();
                double a = stack.pop();
                switch (token) {
                    case "+": stack.push(a + b); break;
                    case "-": stack.push(a - b); break;
                    case "*": stack.push(a * b); break;
                    case "/": stack.push(a / b); break;
                }
            }
        }
        return stack.pop();
    }

    private List<Ejercicio> generarEjerciciosDespejarX(int cantidadNecesaria) {
        List<Ejercicio> ejercicios = new ArrayList<>();
        int intentos = 0;
        int maxIntentos = Math.max(100, cantidadNecesaria * 15);

        while (ejercicios.size() < cantidadNecesaria && intentos < maxIntentos) {
            intentos++;
            Ejercicio ejercicio = generarEjercicioDespejarX();
            if (ejercicio instanceof EjercicioMultiple && ((EjercicioMultiple) ejercicio).esDespejarX()) {
                ejercicios.add(ejercicio);
            }
        }

        return ejercicios;
    }

    private Ejercicio generarEjercicioDespejarX() {
        Random r = new Random();
        int intentos = 0;

        while (intentos < 100) {
            intentos++;

            List<String> tokens = new ArrayList<>();
            boolean tieneX = false;

            // Construir expresión base
            for (int i = 0; i < cantidadOperaciones * 2 + 1; i++) {
                if (i % 2 == 0) {
                    // Posición de número
                    String valor;
                    if (!tieneX && r.nextDouble() < 0.4) { // con 40% de probabilidad insertar la incógnita
                        valor = "x";
                        tieneX = true;
                    } else {
                        valor = formatearNumero(getRandomNumero());
                    }
                    tokens.add(valor);
                } else {
                    // Posición de operador
                    tokens.add(simbolos.get(r.nextInt(simbolos.size())));
                }
            }

            if (!tieneX) {
                // Asegurar al menos una X
                int index = r.nextInt(cantidadOperaciones + 1) * 2;
                tokens.set(index, "x");
                tieneX = true;
            }

            // Generar expresión en texto
            String expr = String.join(" ", tokens);
            if (permitirParentesis) {
                expr = insertarParentesis(expr);
            }

            long conteoX = expr.chars().filter(c -> c == 'x' || c == 'X').count();
            if (conteoX != 1) {
                continue;
            }

            // Sustituir la X por una variable temporal para evaluar
            List<String> conX = new ArrayList<>(Arrays.asList(expr.split(" ")));
            for (int i = 0; i < conX.size(); i++) {
                if (conX.get(i).equalsIgnoreCase("x")) {
                    conX.set(i, "{X}");
                }
            }

            List<Double> candidatos = generarCandidatosParaX();
            Collections.shuffle(candidatos, r);

            for (double candidato : candidatos) {
                double valorCandidato = permitirDecimales ? candidato : normalizarEntero(candidato);
                if (!permitirNegativos && valorCandidato < 0) {
                    continue;
                }
                if (!permitirDecimales && !esEntero(valorCandidato)) {
                    continue;
                }

                List<String> evaluable = new ArrayList<>(conX.size());
                for (String token : conX) {
                    if (token.equals("{X}")) {
                        evaluable.add(formatearNumero(valorCandidato));
                    } else {
                        evaluable.add(token);
                    }
                }

                String evaluableExpr = String.join(" ", evaluable);
                double resultado = evaluarExpresion(evaluableExpr);

                if (!Double.isFinite(resultado)) {
                    continue;
                }
                if (!permitirNegativos && resultado < 0) {
                    continue;
                }
                if (!permitirDecimales && !esEntero(resultado)) {
                    continue;
                }

                double resultadoFinal = permitirDecimales ? resultado : normalizarEntero(resultado);
                double solucion = permitirDecimales ? valorCandidato : normalizarEntero(valorCandidato);

                return new EjercicioMultiple(expr + " = " + formatearNumero(resultadoFinal), solucion, true);
            }
        }

        // Si falla tras 100 intentos, generar un ejercicio de despejar X simple
        return generarEjercicioDespejarXSimple();
    }

    private List<Double> generarCandidatosParaX() {
        Set<Double> candidatos = new LinkedHashSet<>();

        for (double numero : numeros) {
            double ajustado = ajustarNumeroSegunConfiguracion(numero);
            if (!permitirNegativos && ajustado < 0) {
                continue;
            }
            if (!permitirDecimales && !esEntero(ajustado)) {
                continue;
            }
            candidatos.add(ajustado);
        }

        double paso = permitirDecimales ? 0.25 : 1.0;
        for (double candidato = -50; candidato <= 50; candidato += paso) {
            double ajustado = permitirDecimales ? candidato : normalizarEntero(candidato);
            if (!permitirNegativos && ajustado < 0) {
                continue;
            }
            if (!permitirDecimales && !esEntero(ajustado)) {
                continue;
            }
            candidatos.add(ajustado);
        }

        if (candidatos.isEmpty()) {
            candidatos.add(0.0);
        }

        return new ArrayList<>(candidatos);
    }

    private Ejercicio generarEjercicioDespejarXSimple() {
        double solucion = ajustarNumeroSegunConfiguracion(getRandomNumero());
        double constante = ajustarNumeroSegunConfiguracion(getRandomNumero());

        if (!permitirNegativos) {
            solucion = Math.abs(solucion);
            constante = Math.abs(constante);
        }

        double resultado = solucion + constante;
        if (!permitirDecimales) {
            resultado = normalizarEntero(resultado);
        }

        String expresion = "x + " + formatearNumero(constante) + " = " + formatearNumero(resultado);
        return new EjercicioMultiple(expresion, solucion, true);
    }

    private String formatearNumero(double n) {
        BigDecimal bd = new BigDecimal(Double.toString(n));

        if (bd.scale() > MAX_DECIMALES_VISIBLES) {
            bd = bd.setScale(MAX_DECIMALES_VISIBLES, RoundingMode.HALF_UP);
        }

        bd = bd.stripTrailingZeros();

        if (bd.scale() < 0) {
            bd = bd.setScale(0);
        }

        if (bd.compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        }

        return bd.toPlainString();
    }

    private boolean esEntero(double valor) {
        return Math.abs(valor - Math.rint(valor)) < EPSILON_ENTERO;
    }

    private double normalizarEntero(double valor) {
        return Math.rint(valor);
    }

    private double ajustarNumeroSegunConfiguracion(double valor) {
        if (!permitirDecimales) {
            valor = normalizarEntero(valor);
        }
        return valor;
    }

    private double seleccionarEnteroDeRespaldo(double valorInicial) {
        List<Double> enteros = new ArrayList<>();
        for (double numero : numeros) {
            if (esEntero(numero)) {
                enteros.add(normalizarEntero(numero));
            }
        }

        if (!enteros.isEmpty()) {
            return enteros.get(new Random().nextInt(enteros.size()));
        }

        return normalizarEntero(valorInicial);
    }

    private List<Double> ajustarDecimalesSegunConfiguracion(List<Double> originales) {
        if (originales.isEmpty()) {
            return originales;
        }

        if (permitirDecimales) {
            return new ArrayList<>(originales);
        }

        boolean hayDecimales = false;
        List<Double> copia = new ArrayList<>(originales.size());
        for (double valor : originales) {
            copia.add(valor);
            if (!hayDecimales && !esEntero(valor)) {
                hayDecimales = true;
            }
        }

        if (hayDecimales) {
            System.out.println(
                "numeros.txt contiene valores con decimales. Se conservarán, pero las combinaciones que produzcan resultados no enteros se descartarán porque la opción de decimales está deshabilitada."
            );
        }

        return copia;
    }

    private List<Double> leerNumeros(String ruta) {
        List<Double> valores = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(ruta))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty()) {
                    continue;
                }

                if (linea.contains("-")) {
                    Optional<RangoDatos> rango = intentarParsearRango(linea);
                    if (rango.isPresent()) {
                        RangoDatos datos = rango.get();
                        agregarRango(valores, datos.inicio, datos.fin, datos.decimales, linea);
                        continue;
                    }
                }

                try {
                    valores.add(Double.parseDouble(linea));
                } catch (NumberFormatException ex) {
                    System.out.println("Valor de número inválido encontrado en " + ruta + ": " + linea);
                }
            }
        } catch (IOException e) {
            System.out.println("Error al leer " + ruta + ": " + e.getMessage());
        }
        return valores;
    }

    private Optional<RangoDatos> intentarParsearRango(String linea) {
        String expresion = linea.trim();
        Matcher matcher = PATRON_RANGO.matcher(expresion);
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
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private long seleccionarDivisorEntero(long valor) {
        long absoluto = Math.abs(valor);
        if (absoluto <= 1) {
            return 1;
        }

        for (long divisor = 2; divisor <= absoluto; divisor++) {
            if (absoluto % divisor == 0) {
                return divisor;
            }
        }

        return 1;
    }

    private void agregarRango(List<Double> valores, BigDecimal inicioBD, BigDecimal finBD, int decimales, String lineaOriginal) {

        if (inicioBD.compareTo(finBD) == 0) {
            valores.add(inicioBD.doubleValue());
            return;
        }

        BigDecimal paso = determinarPaso(inicioBD, finBD, decimales);
        if (paso.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("No se pudo determinar paso para el rango: " + lineaOriginal);
            return;
        }

        int sizeAntes = valores.size();
        for (BigDecimal actual = inicioBD; actual.compareTo(finBD) <= 0; actual = actual.add(paso)) {
            valores.add(actual.doubleValue());
            if (actual.add(paso).compareTo(actual) == 0) {
                break; // evita bucle infinito si el paso es demasiado pequeño
            }
        }

        if (valores.size() == sizeAntes || Math.abs(valores.get(valores.size() - 1) - finBD.doubleValue()) > 1e-9) {
            valores.add(finBD.doubleValue());
        }
    }

    private BigDecimal determinarPaso(BigDecimal inicio, BigDecimal fin, int decimales) {
        BigDecimal paso = decimales > 0 ? BigDecimal.ONE.movePointLeft(decimales) : BigDecimal.ONE;
        BigDecimal diferencia = fin.subtract(inicio).abs();
        if (diferencia.compareTo(paso) < 0) {
            return diferencia;
        }
        return paso;
    }

    private int inferirDecimales(BigDecimal inicio, BigDecimal fin) {
        return Math.max(obtenerEscala(inicio), obtenerEscala(fin));
    }

    private int obtenerEscala(BigDecimal valor) {
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
    private List<String> leerSimbolosDesdeConfig(String ruta) {
        List<String> simbolos = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(ruta))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                System.out.println("Línea original leída: [" + linea + "]");
                
                linea = linea.trim().toUpperCase()
                             .replace("Á", "A")
                             .replace("É", "E")
                             .replace("Í", "I")
                             .replace("Ó", "O")
                             .replace("Ú", "U");

                String valor = "";
                if (linea.contains(":")) {
                    valor = linea.substring(linea.indexOf(":") + 1).trim(); // obtiene valor (ej: "SI")
                }

                if (linea.startsWith("SUMA:") && valor.equals("SI")) {
                    simbolos.add("+");
                } else if (linea.startsWith("RESTA:") && valor.equals("SI")) {
                    simbolos.add("-");
                } else if (linea.startsWith("DIVISION:") && valor.equals("SI")) {
                    simbolos.add("/");
                } else if (linea.startsWith("MULTIPLICACION:") && valor.equals("SI")) {
                    simbolos.add("*");
                }
            }
        } catch (IOException e) {
            System.out.println("Error al leer simbolos.txt: " + e.getMessage());
        }

        return simbolos;
    }

    private void leerConfig(String ruta) {
        try (BufferedReader br = new BufferedReader(new FileReader(ruta))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim().toUpperCase()
                             .replace("Á", "A")
                             .replace("É", "E")
                             .replace("Í", "I")
                             .replace("Ó", "O")
                             .replace("Ú", "U");

                String valor = "";
                if (linea.contains(":")) {
                    valor = linea.substring(linea.indexOf(":") + 1).trim(); // Extrae valor
                }

                if (linea.startsWith("DECIMALES:")) {
                    permitirDecimales = valor.equals("SI");
                } else if (linea.startsWith("NUMEROS NEGATIVOS:")) {
                    permitirNegativos = valor.equals("SI");
                } else if (linea.startsWith("OPERACIONES POR EJERCICIO:")) {
                    try {
                        cantidadOperaciones = Math.max(2, Integer.parseInt(valor)); // mínimo 2
                    } catch (NumberFormatException e) {
                        cantidadOperaciones = 2;
                    }
                } else if (linea.startsWith("CANTIDAD EJERCICIOS:")) {
                    try {
                        cantidadEjercicios = Math.max(1, Integer.parseInt(valor));
                    } catch (NumberFormatException e) {
                        cantidadEjercicios = 7;
                    }
                } else if (linea.startsWith("PARENTESIS:")) {
                    permitirParentesis = valor.equals("SI");

                }else if (linea.startsWith("DESPEJAR_X:")) {
                    permitirDespejarX = valor.equals("SI");
                }


            }
        } catch (Exception e) {
            System.out.println("No se encontró config.txt, usando valores por defecto.");
        }
    }

    private void mostrarErrorSimbolosVacios() {
        JOptionPane.showMessageDialog(
            null,
            "⚠ No se ha seleccionado ninguna operación en simbolos.txt.\n\n" +
            "Para que el generador funcione, debes habilitar al menos una de estas líneas en simbolos.txt:\n" +
            "SUMA: SI\nRESTA: SI\nMULTIPLICACIÓN: SI\nDIVISIÓN: SI\n\n" +
            "Corrige el archivo y vuelve a ejecutar el programa.",
            "Error de configuración",
            JOptionPane.ERROR_MESSAGE
        );
        System.exit(0);
    }

    private void mostrarErrorNumerosVacios() {
        JOptionPane.showMessageDialog(
            null,
            "⚠ numeros.txt no contiene valores válidos.\n" +
            "Revisa la configuración y asegúrate de definir números o rangos válidos (por ejemplo, 1-10).",
            "Error de configuración",
            JOptionPane.ERROR_MESSAGE
        );
        System.exit(0);
    }
    private double getRandomNumero() {
        return numeros.get(new Random().nextInt(numeros.size()));
    }

    private double encontrarNumeroMasCercano(double objetivo) {
        double cercano = numeros.get(0);
        double diferenciaMinima = Math.abs(cercano - objetivo);

        for (double num : numeros) {
            double diferencia = Math.abs(num - objetivo);
            if (diferencia < diferenciaMinima) {
                cercano = num;
                diferenciaMinima = diferencia;
            }
        }

        return cercano;
    }
}

