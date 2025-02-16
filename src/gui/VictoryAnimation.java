package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class VictoryAnimation extends JPanel {
	private JLabel victoryLabel;
	private Timer timer;
	private Timer hideTimer;
	private float scale = 0.1f; // Tamaño inicial
	private final float maxScale = 1.0f; // Tamaño final
	private boolean animating = false; // Para controlar si la animación está en curso

	public VictoryAnimation() {
		setOpaque(false);
		setLayout(new GridBagLayout()); // Usamos un layout que nos permita centrar el contenido

		victoryLabel = new JLabel("¡Victoria!");
		victoryLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
		victoryLabel.setForeground(Color.BLACK);
		add(victoryLabel);
		setVisible(false); // Ocultar por defecto

		// Crear el timer para la animación del tamaño
		timer = new Timer(10, new ActionListener() { // Intervalo reducido a 30 ms para mayor fluidez
			@Override
			public void actionPerformed(ActionEvent e) {
				scale += 0.025f; // Incremento más pequeño para suavizar la transición
				int newSize = (int) (10 + scale * 40);
				victoryLabel.setFont(new Font("SansSerif", Font.BOLD, newSize));

				// Ajustar el tamaño de la etiqueta y el panel
				Dimension labelSize = victoryLabel.getPreferredSize();
				setPreferredSize(new Dimension(labelSize.width + 20, labelSize.height + 20)); // Un poco de espacio
																								// adicional
				revalidate(); // Forzar la actualización del tamaño del panel
				repaint(); // Redibujar el panel

				if (scale >= maxScale) {
					timer.stop(); // Detener la animación
					// Iniciar el timer para ocultar el mensaje después de 2 segundos
					hideTimer.start();
				}
			}
		});

		// Crear el timer para ocultar el mensaje después de 2 segundos
		hideTimer = new Timer(2000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false); // Ocultar el mensaje
				victoryLabel.setFont(new Font("SansSerif", Font.BOLD, 10)); // Reiniciar tamaño
				scale = 0.1f; // Reiniciar escala
				hideTimer.stop(); // Detener el timer de ocultar
				animating = false; // Terminar animación
			}
		});
	}

	public synchronized void startAnimation(JFrame frame) {
		if (animating) {
			return; // Si ya está animando, no hacemos nada
		}

		animating = true; // Marcar que estamos en animación
		scale = 0.1f; // Reiniciar escala
		setVisible(true); // Asegurar que el mensaje esté visible

		// Obtener el tamaño de la ventana principal
		int frameWidth = frame.getWidth();
		int frameHeight = frame.getHeight();

		// Calcular el tamaño del panel (70% del ancho de la ventana)
		int panelWidth = (int) (frameWidth * 0.7);
		int panelHeight = 100; // Puedes ajustar esta altura como desees

		// Calcular la posición para centrar el panel
		int xPos = (frameWidth - panelWidth) / 2;
		int yPos = (frameHeight - panelHeight) / 2;

		// Establecer el tamaño y la posición del panel
		setBounds(xPos, yPos, panelWidth, panelHeight);

		// Detener cualquier timer anterior
		if (timer.isRunning()) {
			timer.stop();
		}

		if (hideTimer.isRunning()) {
			hideTimer.stop();
		}

		// Iniciar la animación
		timer.start();
	}
}