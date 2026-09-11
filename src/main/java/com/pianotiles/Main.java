package com.pianotiles;

import java.util.ArrayList;

import processing.core.PApplet;

public class Main extends PApplet {

    // Agente de Q-Learning
    AprendizajeRefuerzo agente;
    // Lista de teclas activas en el juego
    ArrayList<Tecla> teclasActivas;

    // Estados del juego
    enum EstadoJuego {
        MENU, JUGANDO
    }
    EstadoJuego estadoActual = EstadoJuego.MENU;

    // Configuración del tablero y teclas
    int columnas = 4;
    int anchoTecla, alturaTecla = 120;
    float velocidad = 5; // Velocidad inicial de caída
    float lineaPulsacion; // Línea horizontal donde se debe pulsar

    // Variables de puntuación y recompensas
    int puntuacion = 0;
    float lastReward = 0;

    // Configuración del área de juego (DONDE CAEN LAS TECLAS)
    int margenIzquierdo = 80;
    int anchoJuego, anchoInfo;

    // Control del spawn de teclas
    int spawnTimer = 0;
    int spawnRate = 35; // Cada 35 frames aparece una tecla

    // Botones del menú
    int btnJugarX, btnJugarY, btnAncho = 200, btnAlto = 60;
    int btnSalirX, btnSalirY;

    public static void main(String[] args) {
        PApplet.main("com.pianotiles.Main");
    }

    // Configuración inicial de la ventana
    public void settings() {
        fullScreen();
    }

    //Setup, ACORDARSE QUE Se ejecuta una vez al inicio
    public void setup() {
        // Centramos los botones del menú
        btnJugarX = width / 2 - btnAncho / 2;
        btnJugarY = height / 2 - 50;
        btnSalirX = width / 2 - btnAncho / 2;
        btnSalirY = height / 2 + 40;

        // División entre zona de juego y panel lateral
        anchoInfo = (int) (width * 0.3f);
        anchoJuego = width - anchoInfo - margenIzquierdo;
        // Tamaño proporcional de cada columna
        anchoTecla = anchoJuego / columnas;
        // Línea donde es mejor pulsar
        lineaPulsacion = height * 0.75f;

        teclasActivas = new ArrayList<>();
        agente = new AprendizajeRefuerzo(0.2f, 0.9f, 0.35f);

        // Si ya hay aprendizaje guardado, se carga
        agente.cargarQTable("qtable.txt");
    }

    //Bucle del juego, se repite constantemente
    public void draw() {
        background(240);
        switch (estadoActual) {
            case MENU:
                dibujarMenu();
                break;
            case JUGANDO:
                dibujarAreaJuego();
                dibujarPanelInfo();
                dibujarBotonMenuJuego();
                break;
        }
    }

    // ---- PANTALLA DE MENÚ
    void dibujarMenu() {
        fill(50);
        textAlign(CENTER, CENTER);
        textSize(48);
        text("PIANO TILES", width / 2, height / 2 - 150);

        //Botón JUGAR
        fill(estaSobreBoton(btnJugarX, btnJugarY, btnAncho, btnAlto) ? color(100, 200, 100) : color(150, 250, 150));
        rect(btnJugarX, btnJugarY, btnAncho, btnAlto, 10);
        fill(0);
        textSize(24);
        text("Jugar", width / 2, btnJugarY + btnAlto / 2);

        //Botón SALIR
        fill(estaSobreBoton(btnSalirX, btnSalirY, btnAncho, btnAlto) ? color(200, 100, 100) : color(250, 150, 150));
        rect(btnSalirX, btnSalirY, btnAncho, btnAlto, 10);
        fill(0);
        text("Salir", width / 2, btnSalirY + btnAlto / 2);
    }

    // ---- ÁREA DEL JUEGO (DONDE CAEN LAS TECLAS)
    void dibujarAreaJuego() {

        translate(margenIzquierdo, 0); // Mover área a la derecha

        // Dibujar columnas
        for (int c = 0; c < columnas; c++) {
            fill(245);
            rect(c * anchoTecla, 0, anchoTecla, height);
        }
        dibujarZonasPuntuacion(); // Dibujar zonas de acierto (ayuda visual, los colores que salen y los textos de las zonas)
        controlarSpawn(); // Crear teclas nuevas
        actualizarYDibujarTeclas(); // Movimiento y Q-Learning
    }

    //Dibuja las zonas donde el agente debe intentar pulsar
    void dibujarZonasPuntuacion() {
        //Las zonas, sus límites y colores
        String[] zonas = {"lejos", "cerca", "bien_arriba", "perfecto", "bien_abajo", "pasarse"};
        float[] limites = {lineaPulsacion - height * 0.35f, lineaPulsacion - height * 0.15f, lineaPulsacion - height * 0.05f, lineaPulsacion + height * 0.05f, lineaPulsacion + height * 0.15f};
        int[] colores = {color(255, 100, 100), color(255, 160, 60), color(255, 220, 0), color(100, 255, 100), color(100, 200, 255), color(180)};

        for (int i = 0; i < zonas.length; i++) { //Bucle para dibujar cada zona
            float y1 = (i == 0) ? -alturaTecla : limites[i - 1]; //Calcula donde empieza y acaba cada zona, la primera es arriba de todo por lo tanto -altoTecla
            float y2 = (i < limites.length) ? limites[i] : height; //La última zona es hasta el final de la pantalla

            fill(colores[i], 60); //Color con transparencia
            rect(0, y1, anchoJuego, y2 - y1);
            //Ponemos el texto de los nombres de la zona
            fill(0);
            textSize(14);
            text(zonas[i], -38, (y1 + y2) / 2);
        }
    }

    // ---- LOGICA DEL JUEGO
    void controlarSpawn() {
        if (++spawnTimer > spawnRate) { //cada frame aumenta el contador en 1, si supera el rate, se crea una nueva tecla
            generarNuevaTecla(); //Nueva tecla
            spawnTimer = 0; //se reinicia el contador
        }
    }
    //Crea una nueva tecla aleatoria (normal 80% / mortal 20%)

    void generarNuevaTecla() {
        int columna = (int) random(columnas);
        int posX = columna * anchoTecla;
        String tipo = random(1) < 0.8f ? "normal" : "mortal"; //Si el numero random es menor que 0.8 es tipo normal pero si no es mortal
        teclasActivas.add(new Tecla(this, posX, -alturaTecla, anchoTecla, alturaTecla, tipo, velocidad));
    }

    //Actualiza todas las teclas activas y aplica Q-Learning cada frame
    void actualizarYDibujarTeclas() {
        // recorremos al revés para evitar errores al eliminar elementos de la lista
        for (int i = teclasActivas.size() - 1; i >= 0; i--) {
            Tecla t = teclasActivas.get(i);
            // mantenemos sincronizada la velocidad de la tecla con el valor global
            t.setVelocidad(velocidad);
            // Si la tecla ya fue presionada antes ya no afecta al aprendizaje
            if (!t.isActiva()) {
                t.actualizar();  // sigue cayendo
                t.mostrar();
                if (t.getY() > height + 50) {
                    teclasActivas.remove(i); // si ha salido se elimina
                }
                continue; // pasamos a la siguiente tecla
            }

            // 1. Obtener el ÍNDICE de zona (no el string)
            int indiceZona = agente.obtenerIndiceZona((int) (t.getY() + t.getHeight()), (int) lineaPulsacion, height, alturaTecla);

            // 2. El agente decide PRESIONAR o ESPERAR
            String accion = agente.elegirAccion(t.getTipo(), indiceZona);

            // 3. Calcular recompensa
            float recompensa = agente.obtenerRecompensa(accion, indiceZona, t.getTipo());

            boolean teclaPulsada = false;

            if (accion.equals("press")) {
                t.setActiva(false);
                teclaPulsada = true;
            }

            lastReward = recompensa;
            puntuacion += recompensa;

            t.actualizar();
            t.mostrar();

            // 4. Obtener NUEVO estado después de moverse
            int nuevoIndiceZona = agente.obtenerIndiceZona((int) (t.getY() + t.getHeight()), (int) lineaPulsacion, height, alturaTecla);

            // 5. Actualizar Q-Table (usa índices directamente)
            agente.actualizarValorQ(t.getTipo(), indiceZona, accion, recompensa, nuevoIndiceZona, teclaPulsada ? "terminal" : "normal");

            if (t.getY() > height + 50) {
                teclasActivas.remove(i);
            }
        }

        // Cada 600 frames (unos aprox 10 segundos), baja la exploración (menos aleatorio, más inteligente)
        if (frameCount % 600 == 0) {
            agente.setEpsilon(agente.getEpsilon() * 0.98f); // Baja epsilon un 2%
        }
    }

    // ---- PANEL DE INFORMACIÓN
    void dibujarPanelInfo() {
        fill(255);
        rect(margenIzquierdo + anchoJuego - 80, 0, anchoInfo, height);

        float panelX = margenIzquierdo + anchoJuego - 50;
        float centerX = panelX + anchoInfo / 2;

        fill(0);
        textAlign(CENTER, CENTER);
        textSize(20);
        text("Q-Learning Control", centerX, 30);

        textSize(15);
        text("Puntuación: " + puntuacion, centerX, 60);
        text("Última recompensa: " + nf(lastReward, 0, 2), centerX, 80);

        // Parámetros editables (NF es una funcion que nos permite convertri numeros a un string correcto)
        text("Alpha: Q (AUMENTAR) / A (DISMINUIR): " + nf(agente.getAlpha(), 0, 2), centerX, 120);
        text("Gamma: W (AUMENTAR) / S (DISMINUIR): " + nf(agente.getGamma(), 0, 2), centerX, 150);
        text("Epsilon: E (AUMENTAR) / D (DISMINUIR): " + nf(agente.getEpsilon(), 0, 4), centerX, 180);
        text("Velocidad: R (AUMENTAR) / F (DISMINUIR): " + nf(velocidad, 0, 2), centerX, 210);
        text("ENTER = Guardar QTable", centerX, 240);

        // Mostrar valores Q de cada estado/acción
        dibujarQTable(panelX);
    }

    // ---- DIBUJA LA Q-TABLE EN EL PANEL LATERAL
    void dibujarQTable(float panelX) {
        fill(0);
        textAlign(CENTER, CENTER);
        text("ESTADO | PULSAR | ESPERAR", panelX + anchoInfo / 2, 300);

        int yOffset = 340;
        String[] tipos = {"normal", "mortal"};
        String[] zonas = {"lejos", "cerca", "bien_arriba", "perfecto", "bien_abajo", "pasarse"};
        int[] indices = {0, 1, 2, 3, 4, 5};

        for (String tipo : tipos) {
            textAlign(LEFT, TOP);
            text(tipo.toUpperCase() + ":", panelX + 10, yOffset);
            yOffset += 15;

            for (int i = 0; i < zonas.length; i++) {
                // Obtener valores Q directamente de los arrays bidimensionales
                float qPress = 0f;
                float qWait = 0f;

                if (tipo.equals("mortal")) {
                    qPress = agente.getTablaQMortal()[indices[i]][0]; // ACCION_PRESS_I = 0
                    qWait = agente.getTablaQMortal()[indices[i]][1];  // ACCION_WAIT_I = 1
                } else {
                    qPress = agente.getTablaQNormal()[indices[i]][0];
                    qWait = agente.getTablaQNormal()[indices[i]][1];
                }

                // Dibujar estado
                fill(0);
                text(zonas[i] + ":", panelX + 10, yOffset);

                // Valor PULSAR
                fill(qPress >= qWait ? color(0, 150, 0) : color(150, 0, 0));
                text(nf(qPress, 0, 2), panelX + anchoInfo / 2 - 10, yOffset);

                // Valor ESPERAR
                fill(qWait >= qPress ? color(0, 150, 0) : color(150, 0, 0));
                text(nf(qWait, 0, 2), panelX + anchoInfo * 0.8f, yOffset);

                fill(0);
                yOffset += 15;
            }
            yOffset += 40;
        }
    }

    // --- VOLVER AL MENÚ
    void dibujarBotonMenuJuego() {
        int btnW = 120, btnH = 40;
        int btnX = 10, btnY = 10;
        fill(estaSobreBoton(btnX, btnY, btnW, btnH) ? color(200, 200, 250) : color(220, 220, 255));
        rect(btnX, btnY, btnW, btnH, 5);
        fill(0);
        textSize(16);
        textAlign(CENTER, CENTER);
        text("Salir (ESC)", btnX + btnW / 2, btnY + btnH / 2);
    }

    // ---- INPUT DEL USUARIO
    boolean estaSobreBoton(int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w
                && mouseY >= y && mouseY <= y + h;
    }

    public void keyPressed() {
        if (estadoActual == EstadoJuego.JUGANDO) {
            float step = 0.01f;
            float speedStep = 0.5f;

            // CAMBIA LAS MAYÚSCULAS POR MINÚSCULAS:
            if (key == 'q' || key == 'Q') {
                agente.setAlpha(agente.getAlpha() + step);
            }
            if (key == 'a' || key == 'A') {
                agente.setAlpha(agente.getAlpha() - step);
            }
            if (key == 'w' || key == 'W') {
                agente.setGamma(agente.getGamma() + step);
            }
            if (key == 's' || key == 'S') {
                agente.setGamma(agente.getGamma() - step);
            }
            if (key == 'e' || key == 'E') {
                agente.setEpsilon(agente.getEpsilon() + step);
            }
            if (key == 'd' || key == 'D') {
                agente.setEpsilon(agente.getEpsilon() - step);
            }
            if (key == 'r' || key == 'R') {
                velocidad = constrain(velocidad + speedStep, 2, 20);
            }
            if (key == 'f' || key == 'F') {
                velocidad = constrain(velocidad - speedStep, 2, 20);
            }

            // Volver al menú
            if (key == ESC) {
                irAMenu();
            }
        }

        // Guardar Q-Table manualmente con ENTER
        if (key == '\n') {
            agente.guardarQTable("qtable.txt");
            println("Q-Table guardada manualmente.");
        }
    }

    // Manejo de clicks del ratón según el estado actual del juego
    public void mousePressed() {
        // Si estamos en el menú principal...
        if (estadoActual == EstadoJuego.MENU) {
            // Si el jugador hace click en el botón "Jugar" → iniciar la partida
            if (estaSobreBoton(btnJugarX, btnJugarY, btnAncho, btnAlto)) {
                iniciarJuego();
            }// Si hace click en el botón "Salir" → cerrar el juego
            else if (estaSobreBoton(btnSalirX, btnSalirY, btnAncho, btnAlto)) {
                exit();
            }
        } // Si NO estamos en el menú (es decir, estamos jugando o en otro estado)...
        else {
            // Si el jugador hace click en el botón de la esquina (Menú ESC) → volver al menú
            if (estaSobreBoton(10, 10, 120, 40)) {
                irAMenu();
            }
        }
    }

    // ---- ESTADOS DEL JUEGO
    void iniciarJuego() {
        estadoActual = EstadoJuego.JUGANDO; // cambio de estado
        puntuacion = 0; // puntuacion cero
        lastReward = 0; //ultiam recomensa cero
        teclasActivas.clear(); // eliminamos cualquier tecla que hubiera previamente en pantalla
        spawnTimer = 0; //reinciamos el contador de spawn
    }

    void irAMenu() {
        estadoActual = EstadoJuego.MENU; //cambio de estado a Menu
    }
}
