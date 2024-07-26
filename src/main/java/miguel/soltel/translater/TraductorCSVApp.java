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
	private JComboBox<String> cmbLanguageSource;
	private JComboBox<String> cmbLanguageTarget;
	private JComboBox<Character> cmbDelimiter;
	private JComboBox<String> cmbFileType;
	private JLabel lblFile;

	public TraductorCSVApp() {
		setTitle("Traductor de CSV o SQL");
		setSize(400, 300);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		JLabel lblUpload = new JLabel("Sube el archivo CSV o SQL:");
		JButton btnUpload = new JButton("Seleccionar archivo");
		lblFile = new JLabel("Archivo no seleccionado");

		btnUpload.addActionListener(this::selectFile);

		JLabel lblFileType = new JLabel("Selecciona el tipo de archivo:");
		cmbFileType = new JComboBox<>(new String[] { "CSV", "SQL" });

		JLabel lblColumn = new JLabel("Indica las columnas/campos a traducir (separadas por comas):");
		txtColumns = new JTextField();

		JLabel lblLanguageSource = new JLabel("Selecciona el idioma de origen:");
		cmbLanguageSource = new JComboBox<>(new String[] { "Español", "Inglés", "Catalán", "Euskera" });

		JLabel lblLanguageTarget = new JLabel("Selecciona el idioma de destino:");
		cmbLanguageTarget = new JComboBox<>(new String[] { "Inglés", "Catalán", "Español", "Euskera" });

		JLabel lblDelimiter = new JLabel("Selecciona el delimitador:");
		cmbDelimiter = new JComboBox<>(new Character[] { ',', '|', ';' });

		JButton btnTranslate = new JButton("Traducir");
		btnTranslate.addActionListener(this::translateFile);

		panel.add(lblUpload);
		panel.add(btnUpload);
		panel.add(lblFile);
		panel.add(lblFileType);
		panel.add(cmbFileType);
		panel.add(lblColumn);
		panel.add(txtColumns);
		panel.add(lblLanguageSource);
		panel.add(cmbLanguageSource);
		panel.add(lblLanguageTarget);
		panel.add(cmbLanguageTarget);
		panel.add(lblDelimiter);
		panel.add(btnTranslate);

		add(panel);
	}

	private void selectFile(ActionEvent e) {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser
				.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV or SQL Files", "csv", "sql"));
		int returnValue = fileChooser.showOpenDialog(this);
		if (returnValue == JFileChooser.APPROVE_OPTION) {
			selectedFile = fileChooser.getSelectedFile();
			lblFile.setText("Archivo seleccionado: " + selectedFile.getName());
		}
	}

	private void translateFile(ActionEvent e) {
		if (selectedFile != null && !txtColumns.getText().isEmpty() && cmbLanguageSource.getSelectedItem() != null
				&& cmbLanguageTarget.getSelectedItem() != null && cmbDelimiter.getSelectedItem() != null
				&& cmbFileType.getSelectedItem() != null) {
			String[] columns = txtColumns.getText().split(",");
			String sourceLanguage = (String) cmbLanguageSource.getSelectedItem();
			String targetLanguage = (String) cmbLanguageTarget.getSelectedItem();
			char delimiter = (char) cmbDelimiter.getSelectedItem();
			String fileType = (String) cmbFileType.getSelectedItem();
			try {
				Traductor traductor = new Traductor();
				if (fileType.equals("CSV")) {
					traductor.traducirCSV(selectedFile, columns, sourceLanguage, targetLanguage, delimiter);
				} else if (fileType.equals("SQL")) {
					traductor.traducirSQL(selectedFile, columns, sourceLanguage, targetLanguage);
				}
				JOptionPane.showMessageDialog(this,
						"El archivo ha sido traducido correctamente y guardado como:\n"
								+ traductor.createTranslatedFileName(selectedFile, fileType),
						"Traducción Completa", JOptionPane.INFORMATION_MESSAGE);
			} catch (Exception ex) {
				ex.printStackTrace();
				JOptionPane.showMessageDialog(this, "Hubo un error al traducir el archivo.", "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		} else {
			JOptionPane.showMessageDialog(this,
					"Por favor, selecciona un archivo, columnas/campos, un idioma y un delimitador.", "Error",
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
