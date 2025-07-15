public class EjercicioMultiple extends Ejercicio {
    private final String texto;

    public EjercicioMultiple(String expresionOriginal, double resultado) {
        super(0, 0, "", resultado); // no usamos a ni b ni operador
        this.texto = formatearExpresion(expresionOriginal);
        this.resultado = resultado;
    }

    @Override
    public String getEjercicioTexto() {
        return texto.contains("=") ? texto : texto + " = ?";
    }


    @Override
    public String getResultadoTexto() {
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
