import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigFileHelper {
    private static final String CONFIG_PATH = "config.txt";

    public static LinkedHashMap<String, String> leerConfig() {
        LinkedHashMap<String, String> config = new LinkedHashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(CONFIG_PATH))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                if (!linea.contains(":")) {
                    continue;
                }
                String[] partes = linea.split(":", 2);
                String clave = normalizarClave(partes[0]);
                String valor = partes.length > 1 ? partes[1].trim() : "";
                config.put(clave, valor);
            }
        } catch (IOException e) {
            System.out.println("No se pudo leer " + CONFIG_PATH + ". Se utilizarán valores predeterminados.");
        }
        return config;
    }

    public static void guardarConfig(LinkedHashMap<String, String> config) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(CONFIG_PATH))) {
            for (Map.Entry<String, String> entry : config.entrySet()) {
                pw.println(entry.getKey() + ": " + entry.getValue());
            }
        } catch (IOException e) {
            System.out.println("No se pudo guardar " + CONFIG_PATH + ".");
        }
    }

    public static int leerGradoDificultad() {
        LinkedHashMap<String, String> config = leerConfig();
        int grado = parseEntero(config.getOrDefault("GRADO_DIFICULTAD", "0"), 0);
        return NivelDificultad.clampGrado(grado);
    }

    public static void guardarGradoDificultad(int grado) {
        LinkedHashMap<String, String> config = leerConfig();
        int gradoNormalizado = NivelDificultad.clampGrado(grado);
        config.put("GRADO_DIFICULTAD", String.valueOf(gradoNormalizado));
        guardarConfig(config);
    }

    public static int leerCantidadEjercicios(int predeterminado) {
        LinkedHashMap<String, String> config = leerConfig();
        return parseEntero(config.getOrDefault("CANTIDAD EJERCICIOS", String.valueOf(predeterminado)), predeterminado);
    }

    private static String normalizarClave(String clave) {
        return clave.trim().toUpperCase()
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U");
    }

    private static int parseEntero(String valor, int predeterminado) {
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            return predeterminado;
        }
    }
}
