package miguel.soltel.translater;

import java.io.BufferedReader;
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
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import com.opencsv.exceptions.CsvException;

public class Traductor {

	private static final String API_URL_TEMPLATE = "https://translate.google.es/translate_a/single?client=gtx&sl=%s&tl=%s&dt=t&q=%s";
	private static final Map<String, String> LANGUAGE_CODES;

	static {
		LANGUAGE_CODES = new HashMap<>();
		LANGUAGE_CODES.put("inglés", "en");
		LANGUAGE_CODES.put("catalán", "ca");
		LANGUAGE_CODES.put("español", "es");
		LANGUAGE_CODES.put("euskera", "eu");
	}

	public void traducirCSV(File inputFile, String[] columns, String sourceLanguage, String targetLanguage,
			char delimiter) throws IOException, CsvException {
		String translatedFileName = createTranslatedFileName(inputFile, "CSV");

		CSVParser parser = new CSVParserBuilder().withSeparator(delimiter)
				.withQuoteChar(CSVParser.DEFAULT_QUOTE_CHARACTER).withEscapeChar(CSVParser.DEFAULT_ESCAPE_CHARACTER)
				.build();

		try (CSVReader reader = new CSVReaderBuilder(new FileReader(inputFile)).withCSVParser(parser).build();
				FileWriter fileWriter = new FileWriter(translatedFileName);
				CustomCSVWriter writer = new CustomCSVWriter(fileWriter, delimiter)) {

			List<String[]> allRows = reader.readAll();
			String[] header = allRows.get(0);
			int[] columnIndexes = Arrays.stream(columns).mapToInt(column -> getColumnIndex(header, column)).toArray();

			writer.writeNext(header);
			for (int i = 1; i < allRows.size(); i++) {
				String[] row = allRows.get(i);
				for (int columnIndex : columnIndexes) {
					String originalText = row[columnIndex];
					try {
						String translatedText = translateText(originalText, sourceLanguage, targetLanguage);
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

	public void traducirSQL(File inputFile, String[] fields, String sourceLanguage, String targetLanguage)
			throws IOException {
		String translatedFileName = createTranslatedFileName(inputFile, "SQL");

		try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
				FileWriter fileWriter = new FileWriter(translatedFileName)) {
			String line;
			while ((line = reader.readLine()) != null) {
				for (String field : fields) {
					if (line.contains(field + "=")) {
						int startIndex = line.indexOf(field + "=") + field.length() + 2;
						int endIndex = line.indexOf("'", startIndex);
						String originalText = line.substring(startIndex, endIndex);
						try {
							String translatedText = translateText(originalText, sourceLanguage, targetLanguage);
							line = line.substring(0, startIndex) + translatedText + line.substring(endIndex);
						} catch (Exception e) {
							line = line.substring(0, startIndex) + "Error al traducir" + line.substring(endIndex);
							e.printStackTrace();
						}
					}
				}
				fileWriter.write(line + "\n");
			}
		}
	}

	String createTranslatedFileName(File inputFile, String fileType) {
		String parentDir = inputFile.getParent();
		String fileName = inputFile.getName();
		String fileBaseName = fileName.substring(0, fileName.lastIndexOf('.'));
		String translatedFileName = fileBaseName + "_translated." + fileType.toLowerCase();
		return Paths.get(parentDir, translatedFileName).toString();
	}

	private int getColumnIndex(String[] header, String columnName) {
		for (int i = 0; i < header.length; i++) {
			if (header[i].equalsIgnoreCase(columnName)) {
				return i;
			}
		}
		throw new IllegalArgumentException("Columna no encontrada: " + columnName);
	}

	private String translateText(String text, String sourceLanguage, String targetLanguage) throws IOException {
		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString());
			String url = String.format(API_URL_TEMPLATE, getLanguageCode(sourceLanguage),
					getLanguageCode(targetLanguage), encodedText);

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

	private static class CustomCSVWriter implements AutoCloseable {
		private final ICSVWriter writer;

		public CustomCSVWriter(FileWriter fileWriter, char delimiter) {
			this.writer = new CSVWriterBuilder(fileWriter).withSeparator(delimiter)
					.withQuoteChar(CSVWriter.NO_QUOTE_CHARACTER).withEscapeChar(CSVWriter.DEFAULT_ESCAPE_CHARACTER)
					.withLineEnd(CSVWriter.DEFAULT_LINE_END).build();
		}

		public void writeNext(String[] row) {
			writer.writeNext(row, false);
		}

		@Override
		public void close() throws IOException {
			writer.close();
		}
	}
}
