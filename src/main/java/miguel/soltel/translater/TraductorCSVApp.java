package miguel.soltel.translater;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class TraductorCSVApp extends JFrame {

	private File selectedFile;
	private JTextField txtColumns;
	private JComboBox<String> cmbLanguage;
	private JComboBox<Character> cmbDelimiter;
	private JLabel lblFile;

	public TraductorCSVApp() {
		setTitle("Traductor de CSV");
		setSize(400, 300);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		JLabel lblUpload = new JLabel("Sube el archivo CSV:");
		JButton btnUpload = new JButton("Seleccionar archivo");
		lblFile = new JLabel("Archivo no seleccionado");

		btnUpload.addActionListener(this::selectFile);

		JLabel lblColumn = new JLabel("Indica las columnas a traducir (separadas por comas):");
		txtColumns = new JTextField();

		JLabel lblLanguage = new JLabel("Selecciona el idioma de destino:");
		cmbLanguage = new JComboBox<>(new String[] { "Inglés", "Catalán", "Español", "Euskera" });

		JLabel lblDelimiter = new JLabel("Selecciona el delimitador:");
		cmbDelimiter = new JComboBox<>(new Character[] { ',', '|', ';' });

		JButton btnTranslate = new JButton("Traducir");
		btnTranslate.addActionListener(this::translateFile);

		panel.add(lblUpload);
		panel.add(btnUpload);
		panel.add(lblFile);
		panel.add(lblColumn);
		panel.add(txtColumns);
		panel.add(lblLanguage);
		panel.add(cmbLanguage);
		panel.add(lblDelimiter);
		panel.add(cmbDelimiter);
		panel.add(btnTranslate);

		add(panel);
	}

	private void selectFile(ActionEvent e) {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files", "csv"));
		int returnValue = fileChooser.showOpenDialog(this);
		if (returnValue == JFileChooser.APPROVE_OPTION) {
			selectedFile = fileChooser.getSelectedFile();
			lblFile.setText("Archivo seleccionado: " + selectedFile.getName());
		}
	}

	private void translateFile(ActionEvent e) {
		if (selectedFile != null && !txtColumns.getText().isEmpty() && cmbLanguage.getSelectedItem() != null
				&& cmbDelimiter.getSelectedItem() != null) {
			String[] columns = txtColumns.getText().split(",");
			String targetLanguage = (String) cmbLanguage.getSelectedItem();
			char delimiter = (char) cmbDelimiter.getSelectedItem();
			try {
				TraductorCSV traducir = new TraductorCSV();
				traducir.traducirCSV(selectedFile, columns, targetLanguage, delimiter);
				JOptionPane.showMessageDialog(this,
						"El archivo ha sido traducido correctamente y guardado como:\n"
								+ traducir.createTranslatedFileName(selectedFile),
						"Traducción Completa", JOptionPane.INFORMATION_MESSAGE);
			} catch (Exception ex) {
				ex.printStackTrace();
				JOptionPane.showMessageDialog(this, "Hubo un error al traducir el archivo.", "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		} else {
			JOptionPane.showMessageDialog(this,
					"Por favor, selecciona un archivo, columnas, un idioma y un delimitador.", "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			TraductorCSVApp app = new TraductorCSVApp();
			app.setVisible(true);
		});
	}
}
