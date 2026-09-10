package cat.copernic.proyectoar;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * Esta clase es el CEREBRO del juego. Es como un niño que aprende a tocar
 * piano. Al principio no sabe cuándo pulsar las teclas, pero con el tiempo va
 * aprendiendo qué acciones le dan puntos y cuáles le quitan.
 */
public class AprendizajeRefuerzo {

    // ZONAS DEL JUEGO (DÓNDE ESTÁ LA TECLA)
    // Estas son las 6 posiciones diferentes donde puede estar una tecla:
    public static final int ZONA_LEJOS_INDICE = 0;        // Tecla MUY ARRIBA del lugar ideal
    public static final int ZONA_CERCA_INDICE = 1;        // Tecla ARRIBA del lugar ideal  
    public static final int ZONA_BIEN_ARRIBA_INDICE = 2;  // Tecla CERCA por ARRIBA del ideal
    public static final int ZONA_PERFECTO_INDICE = 3;     // Tecla en la POSICIÓN PERFECTA 
    public static final int ZONA_BIEN_ABAJO_INDICE = 4;   // Tecla CERCA por ABAJO del ideal
    public static final int ZONA_PASARSE_INDICE = 5;      // Tecla MUY ABAJO (se pasó) 
    public static final int FILAS = 6;                    // Total de zonas (6)

    // ACCIONES POSIBLES (QUÉ PUEDE HACER)
    // Solo puede hacer 2 cosas con cada tecla:
    public static final int ACCION_PRESS_INDICE = 0;      // PULSAR la tecla
    public static final int ACCION_WAIT_INDICE = 1;       // ESPERAR (no pulsar)
    public static final int COLUMNAS = 2;                 // Total de acciones (2)

    // Nombres en texto de las acciones (para entender mejor el código)
    private static final String ACCION_PRESS_STR = "press";  // Pulsar
    private static final String ACCION_WAIT_STR = "wait";    // Esperar

    // PREMIOS Y CASTIGOS (CÓMO APRENDE)
    // Cuando la IA acierta, le damos puntos POSITIVOS}
    private static final float RECOMPENSA_PERFECTO = 100f;        // Pulsó en el momento EXACTO
    private static final float RECOMPENSA_BUENO = 5f;             // Pulsó en un buen momento
    private static final float RECOMPENSA_ESPERA_SEGURA = 2f;     // Esperó cuando era peligroso
    private static final float RECOMPENSA_ESPERA_CALIENTE = 6f;   // Esperó inteligentemente

    // Cuando la IA se equivoca, le damos puntos NEGATIVOS  
    private static final float PENALIZACION_PREMATURA = -8f;      // Pulsó demasiado pronto
    private static final float PENALIZACION_PASARSE = -10f;       // Dejó que la tecla se pasara
    private static final float PENALIZACION_MORTAL = -100f;       // Pulsó una tecla MORTAL (grave)
    private static final float PENALIZACION_FALLO = -15f;         // Fallo general

    // MEMORIA DE LA IA (LO QUE APRENDE)
    /**
     * TABLA DE APRENDIZAJE para teclas NORMALES Es como una hoja de cálculo con
     * 6 filas (zonas) y 2 columnas (acciones) Guarda lo que ha aprendido sobre
     * cuándo pulsar teclas normales
     */
    private final float[][] tablaQ_Normal;

    /**
     * TABLA DE APRENDIZAJE para teclas MORTALES Otra hoja de cálculo igual,
     * pero para teclas mortales Aprende que NUNCA debe pulsar teclas mortales
     */
    private final float[][] tablaQ_Mortal;

    // CONFIGURACIÓN DEL APRENDIZAJE (CÓMO DE RÁPIDO/INTELIGENTE APRENDE)
    private float alpha;   // Velocidad de aprendizaje, mientras mas baja, mas lento aprende
    private float gamma;         // Qué tan importante es el recompensa futura, mientras mas alta, mas valora el futuro
    private float epsilon; // Exploración vs explotación, mientras mas alta, mas explora

    // CONSTRUCTOR
    /**
     * le decimos cómo queremos que aprenda
     */
    public AprendizajeRefuerzo(float alpha, float gamma, float epsilon) {
        this.alpha = alpha;
        this.gamma = gamma;
        this.epsilon = epsilon;

        // Creamos las tablas vacías (todas las celdas empiezan en 0), no sabe nada al principio
        this.tablaQ_Normal = new float[FILAS][COLUMNAS];
        this.tablaQ_Mortal = new float[FILAS][COLUMNAS];
    }

    // MÉTODOS PRINCIPALES 
    /**
     * Mira una tecla y dice en QUÉ ZONA está Es como medir qué tan cerca está
     * del lugar perfecto para pulsar
     */
    public int obtenerIndiceZona(int y, int umbral, int alturaPantalla, int altoBaldosa) {
        // Calcula la distancia desde la tecla hasta la línea ideal
        float distancia = y - umbral;

        // Clasifica en una de las 6 zonas según la distancia
        if (distancia < -alturaPantalla * 0.35f) {
            return ZONA_LEJOS_INDICE;        // MUY LEJOS por arriba
        } else if (distancia < -alturaPantalla * 0.15f) {
            return ZONA_CERCA_INDICE;        // CERCA pero por arriba  
        } else if (distancia < -alturaPantalla * 0.05f) {
            return ZONA_BIEN_ARRIBA_INDICE;  // BIEN por arriba
        } else if (Math.abs(distancia) <= alturaPantalla * 0.05f) {
            return ZONA_PERFECTO_INDICE;     // PERFECTO 
        } else if (distancia < alturaPantalla * 0.15f) {
            return ZONA_BIEN_ABAJO_INDICE;   // BIEN por abajo
        } else {
            return ZONA_PASARSE_INDICE;      // SE PASÓ 
        }
    }

    /**
     * DECIDE qué hacer con una tecla: PULSAR o ESPERAR A veces prueba cosas
     * nuevas (explora), otras veces hace lo que mejor le ha funcionado
     * (explotación)
     */
    public String elegirAccion(String tipo, int i) {
        // Elige la tabla correcta (normal o mortal)
        float[][] tabla = obtenerTablaQPorTipo(tipo);

        // Mira qué ha aprendido para esta zona:
        float qPress = tabla[i][ACCION_PRESS_INDICE];  // Lo bueno/malo de PULSAR aquí
        float qWait = tabla[i][ACCION_WAIT_INDICE];    // Lo bueno/malo de ESPERAR aquí

        // A veces (según exploración/Epsilon) hace algo ALEATORIO para aprender
        if (Math.random() < epsilon) {
            return Math.random() < 0.5 ? ACCION_PRESS_STR : ACCION_WAIT_STR;
        }

        // Normalmente hace lo que MEJOR le ha funcionado
        if (qPress == qWait) {
            // Si ambas son igual de buenas, elige al azar
            return Math.random() < 0.5 ? ACCION_PRESS_STR : ACCION_WAIT_STR;
        }
        // Elige la acción que le ha dado MEJORES resultados
        return qPress > qWait ? ACCION_PRESS_STR : ACCION_WAIT_STR;
    }

    /**
     * Calcula los PUNTOS que gana o pierde por una acción
     */
    public float obtenerRecompensa(String accion, int indiceZona, String tipo) {
        String zonaStr = obtenerEtiquetaZona(indiceZona);

        // Para teclas MORTALES: solo es seguro ESPERAR y si las deja pasar en la zona pasarse, se le recompenza aun mas

        if ("mortal".equals(tipo)) {
            if (zonaStr.equals("pasarse")) {

                return ACCION_WAIT_STR.equals(accion) ? RECOMPENSA_PERFECTO: PENALIZACION_MORTAL;
            }
            return ACCION_WAIT_STR.equals(accion) ? RECOMPENSA_ESPERA_SEGURA : PENALIZACION_MORTAL;
        }

        // Para teclas NORMALES: depende de la zona y la acción
        switch (zonaStr) {
            case "perfecto":
                // En zona PERFECTA: PULSAR = mucho puntos, ESPERAR = perder puntos
                return ACCION_PRESS_STR.equals(accion) ? RECOMPENSA_PERFECTO : PENALIZACION_PREMATURA;
            case "bien_arriba":
                // En zona BUENA ARRIBA: ambas acciones dan puntos
                return ACCION_PRESS_STR.equals(accion) ? RECOMPENSA_BUENO : RECOMPENSA_ESPERA_CALIENTE;
            case "bien_abajo":
                // En zona BUENA ABAJO: PULSAR = puntos, ESPERAR = perder puntos
                return ACCION_PRESS_STR.equals(accion) ? RECOMPENSA_BUENO : PENALIZACION_PASARSE;
            case "cerca":
                // En zona Cerca: ES nejore ESPERAR
                return ACCION_WAIT_STR.equals(accion) ? RECOMPENSA_ESPERA_SEGURA : PENALIZACION_PREMATURA;
            case "lejos":
                // En zona LEJANOS: es mejor ESPERAR
                return ACCION_WAIT_STR.equals(accion) ? RECOMPENSA_ESPERA_SEGURA : PENALIZACION_PREMATURA;
            case "pasarse":
                // Si se PASÓ: siempre pierde puntos
                return PENALIZACION_PASARSE;
            default:
                return 0f; // No pasa nada
        }
    }

    /**
     * ACTUALIZA lo que ha aprendido después de cada acción
     */
    public void actualizarValorQ(String tipo, int i, String accion, float recompensa, int iPrima, String contexto) {
        // 1. Elige la tabla correcta (normal o mortal)
        float[][] tabla = obtenerTablaQPorTipo(tipo);
        int a = ACCION_PRESS_STR.equals(accion) ? ACCION_PRESS_INDICE : ACCION_WAIT_INDICE;

        // 2. Si no es el final, mira qué podría ganar en el futuro
        float maxSiguiente = 0f;
        if (!"terminal".equals(contexto)) {
            maxSiguiente = Math.max(tabla[iPrima][ACCION_PRESS_INDICE], tabla[iPrima][ACCION_WAIT_INDICE]);
        }

        // 3. Mira lo que pensaba antes de esta acción
        float qActual = tabla[i][a];

        // 4. Calcula el NUEVO valor (aprende de la experiencia)
        float tdTarget = recompensa + gamma * maxSiguiente;  // Lo que debería haber pensado
        float tdError = tdTarget - qActual;                      // La diferencia (error)
        float qActualizado = qActual + alpha * tdError; // Corrige su pensamiento

        // 5. Guarda lo aprendido en la memoria
        tabla[i][a] = qActualizado;
    }

    // GUARDAR Y CARGAR MEMORIA (NO OLVIDAR LO APRENDIDO) GUARDA todo lo aprendido en un archivo
    public void guardarQTable(String nombreArchivo) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(nombreArchivo))) {
            escribirTabla(writer, tablaQ_Normal);  // Guarda tabla normal
            escribirTabla(writer, tablaQ_Mortal);  // Guarda tabla mortal
            System.out.println("Q-Tables guardadas en " + nombreArchivo);
        } catch (Exception e) {
            System.err.println("Error al guardar las Q-Tables: " + e.getMessage());
        }
    }

    /**
     * CARGA lo que aprendió antes desde un archivo
     */
    public void cargarQTable(String nombreArchivo) {
        try (BufferedReader reader = new BufferedReader(new FileReader(nombreArchivo))) {
            String linea;
            int lineasCargadas = 0;

            // Lee línea por línea del archivo
            while ((linea = reader.readLine()) != null) {
                String[] partes = linea.split(",");
                if (partes.length == 4) {
                    String tipo = partes[0];                    // normal o mortal
                    int fila = Integer.parseInt(partes[1]);     // zona (0-5)
                    int columna = Integer.parseInt(partes[2]);  // acción (0-1)

                    // Arregla el número (cambia coma por punto)
                    String valorStr = partes[3].replace(',', '.');
                    float valor = Float.parseFloat(valorStr);   // valor aprendido

                    // Elige la tabla correcta
                    float[][] tabla = "mortal".equals(tipo) ? tablaQ_Mortal : tablaQ_Normal;

                    // Guarda en la memoria
                    if (fila >= 0 && fila < FILAS && columna >= 0 && columna < COLUMNAS) {
                        tabla[fila][columna] = valor;
                        lineasCargadas++;
                    }
                }
            }
            System.out.println(" Q-Table cargada desde " + nombreArchivo + " - " + lineasCargadas + " valores cargados");
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    // MÉTODOS AUXILIARES 
    /**
     * elige la tabla correcta (normal o mortal)
     */
    private float[][] obtenerTablaQPorTipo(String tipo) {
        return "mortal".equals(tipo) ? tablaQ_Mortal : tablaQ_Normal;
    }

    /**
     * convierte número de zona a nombre (INT a STRING)
     */
    private String obtenerEtiquetaZona(int indiceZona) {
        switch (indiceZona) {
            case ZONA_LEJOS_INDICE:
                return "lejos";
            case ZONA_CERCA_INDICE:
                return "cerca";
            case ZONA_BIEN_ARRIBA_INDICE:
                return "bien_arriba";
            case ZONA_PERFECTO_INDICE:
                return "perfecto";
            case ZONA_BIEN_ABAJO_INDICE:
                return "bien_abajo";
            case ZONA_PASARSE_INDICE:
                return "pasarse";
            default:
                return "pasarse";
        }
    }

    /**
     * decide si una tabla es de teclas normales o mortales
     */
    private String obtenerTipoPorTabla(float[][] tabla) {
        return tabla == tablaQ_Mortal ? "mortal" : "normal";
    }

    /**
     * escribe una tabla completa en el archivo
     */
    private void escribirTabla(PrintWriter writer, float[][] tabla) {
        String tipo = obtenerTipoPorTabla(tabla);

        for (int i = 0; i < FILAS; i++) {
            for (int a = 0; a < COLUMNAS; a++) {
                // Formatea el número (usa punto decimal)
                String valorFormateado = String.format("%.4f", tabla[i][a]).replace(',', '.');
                writer.println(tipo + "," + i + "," + a + "," + valorFormateado);
            }
        }
    }

    /**
     * Castigo por fallar completamente
     */
    public float obtenerPenalizacionFallo() {
        return PENALIZACION_FALLO;
    }

    // GETTERS Y SETTERS
    public float getAlpha() {
        return alpha;
    }

    public float getGamma() {
        return gamma;
    }

    public float getEpsilon() {
        return epsilon;
    }

    /**
     * Cambia la velocidad de aprendizaje (0-1)
     */
    public void setAlpha(float alpha) {
        if (alpha < 0) {
            this.alpha = 0;
        } else if (alpha > 1) {
            this.alpha = 1;
        } else {
            this.alpha = alpha;
        }
    }

    /**
     * Cambia la importancia del futuro (0-1)
     */
    public void setGamma(float gamma) {
        if (gamma < 0) {
            this.gamma = 0;
        } else if (gamma > 1) {
            this.gamma = 1;
        } else {
            this.gamma = gamma;
        }
    }

    /**
     * Cambia cuánto explora vs explota
     */
    public void setEpsilon(float epsilon) {
        if (epsilon < 0) {
            this.epsilon = 0;
        } else if (epsilon > 1) {
            this.epsilon = 1;
        } else {
            this.epsilon = epsilon;
        }
    }

    /**
     * Para que el juego pueda mostrar lo aprendido
     */
    public float[][] getTablaQNormal() {
        return tablaQ_Normal;
    }

    public float[][] getTablaQMortal() {
        return tablaQ_Mortal;
    }
}
