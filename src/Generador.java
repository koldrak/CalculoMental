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

    private boolean permitirDecimales = true;
    private boolean permitirNegativos = true;
    private List<Double> numeros;
    private List<String> simbolos;
    private int cantidadOperaciones =2;
    private boolean permitirParentesis = false;
    private boolean permitirDespejarX = false;

 Generador() {
        String ruta = System.getProperty("user.dir") + "/";
        numeros = leerNumeros(ruta + "numeros.txt");
        if (numeros.isEmpty()) {
            mostrarErrorNumerosVacios();
        }
        simbolos = leerSimbolosDesdeConfig(ruta + "simbolos.txt");
        if (simbolos.isEmpty()) {
            mostrarErrorSimbolosVacios();
        }

        leerConfig(ruta + "config.txt");
    }

 public List<Ejercicio> generarEjercicios() {
	    List<Ejercicio> lista = new ArrayList<>();
	    for (int i = 2; i <= 8; i++) {
	        if (permitirDespejarX) {
	            lista.add(generarEjercicioDespejarX());
	        } else {
	            lista.add(generarEjercicio());
	        }
	    }
	    return lista;
	}


    private Ejercicio generarEjercicio() {
        Random r = new Random();
        int intentos = 0;

        while (intentos < 100) {
            intentos++;

            StringBuilder expresion = new StringBuilder();
            double acumulador = numeros.get(r.nextInt(numeros.size()));
            expresion.append(acumulador);

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

                expresion.append(" ").append(operador).append(" ").append(siguiente);

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

                // 🚫 Si no se permiten decimales y el resultado no es exacto, descartar este ejercicio
                if (!permitirDecimales && resultado != Math.floor(resultado)) continue;

                if (!permitirDecimales) resultado = Math.round(resultado);
                if (!permitirNegativos && resultado < 0) continue;

                return new EjercicioMultiple(expresionFinal, resultado);
            }

        }

        // Fallback con varias divisiones exactas
        double fallbackAcumulador = numeros.get(r.nextInt(numeros.size()));
        StringBuilder expr = new StringBuilder(formatearNumero(fallbackAcumulador));

        for (int i = 1; i < cantidadOperaciones; i++) {
            double divisor = 1;
            if (!permitirDecimales) {
                while (fallbackAcumulador % divisor != 0) {
                    divisor++;
                }
            } else {
                divisor = numeros.get(r.nextInt(numeros.size()));
                if (divisor == 0) divisor = 1;
            }

            expr.append(" / ").append(formatearNumero(divisor));
            fallbackAcumulador = fallbackAcumulador / divisor;
        }

        double resultado = permitirDecimales ? fallbackAcumulador : Math.round(fallbackAcumulador);
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

    private Ejercicio generarEjercicioDespejarX() {
        Random r = new Random();
        int intentos = 0;

        while (intentos < 100) {
            intentos++;

            List<String> tokens = new ArrayList<>();
            int xIndex = -1;

            // Construir expresión base
            for (int i = 0; i < cantidadOperaciones * 2 + 1; i++) {
                if (i % 2 == 0) {
                    // Posición de número
                    String valor;
                    if (xIndex == -1 && r.nextDouble() < 0.4) { // con 40% de probabilidad insertar la incógnita
                        valor = "x";
                        xIndex = i;
                    } else {
                        valor = formatearNumero(getRandomNumero());
                    }
                    tokens.add(valor);
                } else {
                    // Posición de operador
                    tokens.add(simbolos.get(r.nextInt(simbolos.size())));
                }
            }

            if (xIndex == -1) {
                // Asegurar al menos una X
                int index = r.nextInt(cantidadOperaciones + 1) * 2;
                tokens.set(index, "x");
                xIndex = index;
            }

            // Generar expresión en texto
            String expr = String.join(" ", tokens);
            if (permitirParentesis) {
                expr = insertarParentesis(expr);
            }

            // Sustituir la X por una variable temporal para evaluar
            List<String> conX = new ArrayList<>(Arrays.asList(expr.split(" ")));

            // Sustituimos la X por una variable temporal que luego cambiaremos por un número de prueba
            for (int i = 0; i < conX.size(); i++) {
                if (conX.get(i).equalsIgnoreCase("x")) {
                    conX.set(i, "{X}");
                }
            }

         // Probar con distintos valores de X para encontrar uno que cumpla
            double min = numeros.stream().mapToDouble(Double::doubleValue).min().orElse(-10);
            double max = numeros.stream().mapToDouble(Double::doubleValue).max().orElse(10);

            // Tomamos un número entero como resultado objetivo
            double resultadoEsperado = Math.round(min + r.nextDouble() * (max - min));

            for (double candidato = -50; candidato <= 50; candidato += 0.25) {
                // Validar candidato antes de evaluar
                if (!permitirNegativos && candidato < 0) continue;
                if (!permitirDecimales && candidato != Math.floor(candidato)) continue;

                // Reemplazar {X} por el candidato
                List<String> evaluable = new ArrayList<>();
                for (String token : conX) {
                    if (token.equals("{X}")) {
                        evaluable.add(formatearNumero(candidato));
                    } else {
                        evaluable.add(token);
                    }
                }

                String evaluableExpr = String.join(" ", evaluable);
                double resultado = evaluarExpresion(evaluableExpr);

                // Validar resultado antes de comparar
                if (!permitirNegativos && resultado < 0) continue;
                if (!permitirDecimales && resultado != Math.floor(resultado)) continue;

                // Comparar con resultado esperado
                boolean coincide = permitirDecimales
                    ? Math.abs(resultado - resultadoEsperado) < 0.01
                    : Math.round(resultado) == Math.round(resultadoEsperado);

                if (coincide) {
                    long conteoX = expr.chars().filter(c -> c == 'X').count();
                    if (conteoX == 1) {
                        return new EjercicioMultiple(expr + " = " + formatearNumero(resultadoEsperado), candidato);
                    }
                }
            }
        }
        // Si falla tras 100 intentos, generar un ejercicio numérico normal
        return generarEjercicio();
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
