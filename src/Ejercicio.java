import java.math.BigDecimal;
import java.math.RoundingMode;

public class Ejercicio {
    private static final int MAX_DECIMALES_VISIBLES = 6;

    public double a, b, resultado;
    public String operador, operadorVisible;

    public Ejercicio(double a, double b, String operador, double resultado) {
        this.a = a;
        this.b = b;
        this.operador = operador;
        this.resultado = resultado;
        switch (operador) {
            case "*": operadorVisible = "x"; break;
            case "/": operadorVisible = ":"; break;
            default: operadorVisible = operador;
        }
    }

    protected String formatearNumero(double n) {
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

    public String getEjercicioTexto() {
        return formatearNumero(a) + " " + operadorVisible + " " + formatearNumero(b) + " = ?";
    }

    public String getResultadoTexto() {
        return formatearNumero(a) + " " + operadorVisible + " " + formatearNumero(b) + " = " + formatearNumero(resultado);
    }
}
