public class Ejercicio {
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
        if (n == Math.floor(n)) {
            return String.valueOf((int) n); // si es entero, sin decimales
        } else {
            double redondeado = Math.round(n * 100.0) / 100.0;
            if (redondeado == Math.floor(redondeado)) {
                return String.valueOf((int) redondeado); // por si redondea a entero (ej: 10.00 → 10)
            } else {
                return String.format("%.2f", redondeado); // solo 2 decimales visibles si son necesarios
            }
        }
    }

    public String getEjercicioTexto() {
        return formatearNumero(a) + " " + operadorVisible + " " + formatearNumero(b) + " = ?";
    }

    public String getResultadoTexto() {
        return formatearNumero(a) + " " + operadorVisible + " " + formatearNumero(b) + " = " + formatearNumero(resultado);
    }
}
