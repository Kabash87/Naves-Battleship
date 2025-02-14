package resources;

public class Protocol {
    // Mensajes de registro
    public static final String REGISTRATION = "#REG#";
    public static final String REG_OK = "#REG_OK#";

    // Mensajes de tablero y posiciones
    // Ejemplo: "#TAB,10#" para un tablero 10x10
    public static final String BOARD_PREFIX = "#TAB,";
    public static final String BOARD_SUFFIX = "#";

    // Mensaje de posiciones de barcos (se envía la cadena con todas las posiciones)
    // Ejemplo: "#POS%(A,1)(A,2)(A,3)(A,4), ...#"
    public static final String POSITION_PREFIX = "#POS%";
    public static final String POSITION_RIVAL = "RIVAL";

    // Mensaje de inicio de partida
    public static final String START_GAME = "#INICIO#";

    // Mensaje para indicar turno (se incluye el tiempo, por ejemplo: "#turno#30")
    public static final String TURN_PREFIX = "#turno#";

    // Mensaje de tiro que envía el cliente. Ejemplo: "#TIRO(A,5)#"
    public static final String TIRO_PREFIX = "#TIRO";

    // Respuestas del servidor al tiro
    public static final String AGUA = "#AGUA#";
    public static final String TOCADO = "#TOCADO#";
    public static final String HUNDIDO = "#HUNDIDO#";

    // Mensaje para notificar hundimiento a todos
    // Ejemplo: "#BARCO,3,Jugador2#"
    public static final String BARCO = "#BARCO";

    // Mensajes de fin de jugador y ganador
    // Ejemplo: "#FIN#Jugador2#"
    public static final String FIN = "#FIN#";
    // Ejemplo: "#GANADOR#Jugador1#"
    public static final String GANADOR = "#GANADOR#";
}
