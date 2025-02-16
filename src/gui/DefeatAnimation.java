package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DefeatAnimation extends JPanel {
	private JLabel defeatLabel;
	private Timer timer;
	private Timer hideTimer;
	private float scale = 0.1f;
	private final float maxScale = 1.0f;
	private boolean animating = false;

	public DefeatAnimation() {
		setOpaque(false);
		setLayout(new GridBagLayout());

		defeatLabel = new JLabel("Derrota");
		defeatLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
		defeatLabel.setForeground(Color.RED);
		add(defeatLabel);
		setVisible(false);

		timer = new Timer(10, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				scale += 0.025f;
				int newSize = (int) (10 + scale * 40);
				defeatLabel.setFont(new Font("SansSerif", Font.BOLD, newSize));

				Dimension labelSize = defeatLabel.getPreferredSize();
				setPreferredSize(new Dimension(labelSize.width + 20, labelSize.height + 20));

				revalidate();
				repaint();

				if (scale >= maxScale) {
					timer.stop();
					hideTimer.start();
				}
			}
		});

		hideTimer = new Timer(2000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
				defeatLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
				scale = 0.1f;
				hideTimer.stop();
				animating = false;
			}
		});
	}

	public synchronized void startAnimation(JFrame frame) {
		if (animating) {
			return;
		}

		animating = true;
		scale = 0.1f;
		setVisible(true);

		int frameWidth = frame.getWidth();
		int frameHeight = frame.getHeight();

		int panelWidth = (int) (frameWidth * 0.7);
		int panelHeight = 100;

		int xPos = (frameWidth - panelWidth) / 2;
		int yPos = (frameHeight - panelHeight) / 2;

		setBounds(xPos, yPos, panelWidth, panelHeight);

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