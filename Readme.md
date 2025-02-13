# Naves-Battleship

## Índice
1. [Descripción](#descripción)
2. [Estructura del Proyecto](#estructura-del-proyecto)
3. [Compilación y Ejecución](#compilación-y-ejecución)
4. [TODO](#todo)
5. [Autores](#autores)

## Descripción
Naves-Battleship es un juego de batalla naval multijugador implementado en Java. Los jugadores pueden conectarse a un servidor y jugar entre ellos, disparando para hundir los barcos del oponente hasta que solo quede un jugador activo. El juego cuenta con una interfaz de usuario en Java Swing para una experiencia visual mejorada.

## Estructura del Proyecto
El proyecto está organizado de la siguiente manera:

```
src/
├── client
│   ├── Client.java
│   ├── ClientListener.java
│   ├── ClientMain.java
│   └── ClientReaderThread.java
├── gui
│   └── GUIMain.java
├── resources
│   └── Protocol.java
└── server
    ├── Board.java
    ├── BufferedReaderWrapper.java
    ├── ClientHandler.java
    ├── Coordinate.java
    ├── GameServer.java
    ├── Player.java
    ├── PrintWriterWrapper.java
    ├── ServerMain.java
    ├── Ship.java
    ├── ShotResult.java
    └── ShotResultType.java
```

## Compilación y Ejecución
Para compilar y ejecutar el proyecto, sigue estos pasos en PowerShell:

### 1. Generar la lista de archivos Java
```powershell
Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object { $_.FullName } > sources.txt
```

### 2. Compilar los archivos Java y generar los archivos `.class` en `bin/`
```powershell
javac -d bin $(Get-Content sources.txt)
```

### 3. Ejecutar el servidor
```powershell
java -cp bin server.ServerMain
```

### 4. Ejecutar el cliente
```powershell
java -cp bin client.ClientMain
```

## TODO
- Permitir que el jugador introduzca su nombre.
- Permitir introducir IP y puerto personalizados al ejecutar tanto el servidor como el cliente.
- Implementar un mensaje de victoria visualmente atractivo.
- Agregar un modo oscuro.
- Incluir imágenes o colores diferenciadores para "Tocado", "Hundido" y "Agua".
- Diseñar una interfaz muy detallada para el servidor.

## Autores
- **Francisco Hernández Puertas**
- **Diego Hernández**
- **Pedro Pérez**
- **Antonio Mba Nzang**

