public class EjercicioMultiple extends Ejercicio {
    private final String texto;
    private final boolean esDespejarX;

    public EjercicioMultiple(String expresionOriginal, double resultado) {
        this(expresionOriginal, resultado, false);
    }

    public EjercicioMultiple(String expresionOriginal, double resultado, boolean esDespejarX) {
        super(0, 0, "", resultado); // no usamos a ni b ni operador
        this.texto = formatearExpresion(expresionOriginal);
        this.resultado = resultado;
        this.esDespejarX = esDespejarX;
    }

    @Override
    public String getEjercicioTexto() {
        return texto.contains("=") ? texto : texto + " = ?";
    }


    @Override
    public String getResultadoTexto() {
        if (esDespejarX) {
            return "X = " + formatearNumero(resultado);
        }
        return texto + " = " + formatearNumero(resultado);
    }

    private String formatearExpresion(String expresion) {
        String[] tokens = expresion.split(" ");
        StringBuilder sb = new StringBuilder();

        for (String token : tokens) {
            try {
                double numero = Double.parseDouble(token.replace("(", "").replace(")", ""));
                String numFormateado = formatearNumero(numero);

                if (token.startsWith("(")) sb.append("(");
                sb.append(numFormateado);
                if (token.endsWith(")")) sb.append(")");
            } catch (NumberFormatException e) {
                switch (token) {
                    case "*": sb.append("x"); break; // multiplicación
                    case "/": sb.append(":"); break; // división
                    case "x": sb.append("X"); break; // incógnita
                    default: sb.append(token);
                }
            }
            sb.append(" ");
        }

        return sb.toString().trim();
    }



}
