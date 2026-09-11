package com.pianotiles;
import processing.core.PApplet;
/**
 * Clase que representa una tecla que cae en pantalla dentro del juego.
 * Cada tecla tiene una posición, un tipo (normal o mortal), una velocidad de caída y un estado (activa o no).
 */
public class Tecla {

    // Posición y dimensiones del rectángulo que representa la tecla
    int x, y, width, height;
    // Tipo de tecla, este define su comportamiento y su color al dibujarla
    String tipo; // Puede ser "normal" o "mortal"
    // Indica si la tecla se puede pulsar y puntuar
    boolean activa;

    private PApplet app;  // Referencia al sketch principal de Processing
    private float velocidad; // Velocidad de caída de la tecla
    /**
     * Constructor de la tecla.
     * app       Referencia a PApplet para poder dibujar
     * x         Posición X
     * y         Posición Y
     * width         Ancho
     * heigt         Alto
     * tipo      Tipo de tecla ("normal" o "mortal")
     * velocidad Velocidad de caída
     */
    public Tecla(PApplet app, int x, int y, int width, int height, String tipo, float velocidad) {
        this.app = app;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.tipo = tipo;
        this.velocidad = velocidad;
        // Toda tecla inicia activa al aparecer
        this.activa = true;
    }

    /**
     * Metodo que actualiza la posición de la tecla en cada frame.
     * Hace que la tecla "caiga" sumando su velocidad al eje Y.
     */
    public void actualizar() {
        y += velocidad;
    }
    /**
     * Dibuja la tecla según su tipo y si está activa o no.
     * (height) Altura total de la pantalla (para posicionar la letra en el borde inferior)
     */
    public void mostrar() {
        int colorFill;
        if (tipo.equals("normal")) {
            // Normal activa = azul
            // Normal inactiva = gris 180
            colorFill = activa ? app.color(0, 0, 200) : app.color(180);
        } else if (tipo.equals("mortal")) {
            // Mortal activa = rojo
            // Mortal inactiva = rojo pálido
            colorFill = activa ? app.color(180, 0, 0) : app.color(200, 150, 150);
        } else {
            colorFill = app.color(200);
        }
        app.fill(colorFill);
        app.rect(x, y, width, height);
    }

    // GETTERS Y SETTERS
    //Modifca la velocidad con la que cae la tecla
    public void setVelocidad(float velocidad) {
        this.velocidad = velocidad;
    }

    //Devuelve si la tecla está activa
    public boolean isActiva() {
        return activa;
    }

    //Activa o desactiva una tecla, si esta desactivada no se pulsa de nuevo
    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    //Devuelve la posición Y actual de la tecla.
    public int getY() {
        return y;
    }

    //Devuelve el alto del rectángulo de la tecla
    public int getHeight() {
        return height;
    }

    //Devuelve el tipo de la tecla (normal o mortal).
    public String getTipo() {
        return tipo;
    }
}
