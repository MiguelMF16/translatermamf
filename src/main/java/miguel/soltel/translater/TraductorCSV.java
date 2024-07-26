package miguel.soltel.translater;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import com.opencsv.exceptions.CsvException;

public class TraductorCSV {

	private static final String API_URL_TEMPLATE = "https://translate.google.es/translate_a/single?client=gtx&sl=es&tl=%s&dt=t&q=%s";
	private static final Map<String, String> LANGUAGE_CODES;

	static {
		LANGUAGE_CODES = new HashMap<>();
		LANGUAGE_CODES.put("inglés", "en");
		LANGUAGE_CODES.put("catalán", "ca");
		LANGUAGE_CODES.put("español", "es");
		LANGUAGE_CODES.put("euskera", "eu");
	}

	public void traducirCSV(File inputFile, String[] columns, String targetLanguage, char delimiter)
			throws IOException, CsvException {
		String translatedFileName = createTranslatedFileName(inputFile);

		try (CSVReader reader = new CSVReaderBuilder(new FileReader(inputFile))
				.withCSVParser(new CSVParserBuilder().withSeparator(delimiter).build()).build();
				ICSVWriter writer = new CSVWriterBuilder(new FileWriter(translatedFileName)).withSeparator(delimiter)
						.build()) {

			List<String[]> allRows = reader.readAll();
			String[] header = allRows.get(0);
			int[] columnIndexes = Arrays.stream(columns).mapToInt(column -> getColumnIndex(header, column)).toArray();

			writer.writeNext(header);
			for (int i = 1; i < allRows.size(); i++) {
				String[] row = allRows.get(i);
				for (int columnIndex : columnIndexes) {
					String originalText = row[columnIndex];
					try {
						String translatedText = translateText(originalText, targetLanguage);
						row[columnIndex] = translatedText;
					} catch (Exception e) {
						row[columnIndex] = "Error al traducir";
						e.printStackTrace();
					}
				}
				writer.writeNext(row);
			}
		}
	}

	private int getColumnIndex(String[] header, String column) {
		for (int i = 0; i < header.length; i++) {
			if (header[i].equalsIgnoreCase(column)) {
				return i;
			}
		}
		throw new IllegalArgumentException("Columna no encontrada: " + column);
	}

	private String translateText(String text, String targetLanguage) throws IOException {
		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString());
			String url = String.format(API_URL_TEMPLATE, getLanguageCode(targetLanguage), encodedText);

			HttpGet httpGet = new HttpGet(url);
			try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
				int statusCode = response.getCode();
				String responseString = EntityUtils.toString(response.getEntity());

				if (statusCode != 200) {
					throw new IOException("Error en la traducción: " + statusCode);
				}

				ObjectMapper mapper = new ObjectMapper();
				JsonNode rootNode = mapper.readTree(responseString);
				return rootNode.get(0).get(0).get(0).asText();
			} catch (ParseException e) {
				e.printStackTrace();
				throw new IOException("Error en la conexión o en la traducción", e);
			}
		}
	}

	private String getLanguageCode(String language) {
		String code = LANGUAGE_CODES.get(language.toLowerCase());
		if (code == null) {
			throw new IllegalArgumentException("Idioma no soportado: " + language);
		}
		return code;
	}

	public String createTranslatedFileName(File inputFile) {
		String parentDir = inputFile.getParent();
		String fileName = inputFile.getName();
		String fileBaseName = fileName.substring(0, fileName.lastIndexOf('.'));
		String translatedFileName = fileBaseName + "_translated.csv";
		return Paths.get(parentDir, translatedFileName).toString();
	}
}
