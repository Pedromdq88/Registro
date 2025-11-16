package com.Proyecto.Registro.Service;

import com.Proyecto.Registro.Entity.Registro;
import com.Proyecto.Registro.Repository.RegistroRepository;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.http.HttpCredentialsAdapter; // ⬅️ IMPORTACIÓN CLAVE AGREGADA
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class RegistroService {

    private final RegistroRepository registroRepository;

    @Value("${sheets.spreadsheet-id}")
    private String spreadsheetId;

    @Value("${sheets.credentials-path}")
    private String credentialsPath;

    @Value("${sheets.sheet-name}")
    private String sheetName;

    private Sheets sheetsService;

    public RegistroService(RegistroRepository registroRepository) {
        this.registroRepository = registroRepository;
    }

    // Inicializa la conexión con Google Sheets usando las credenciales JSON
    @PostConstruct
    public void init() throws IOException, GeneralSecurityException {
        // La ruta asume que el archivo JSON está en src/main/resources/
        FileInputStream credentialsStream = new FileInputStream("src/main/resources/" + credentialsPath);

        GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream)
                .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));

        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        sheetsService = new Sheets.Builder(
                httpTransport,
                GsonFactory.getDefaultInstance(),
                // ⬅️ CORRECCIÓN FINAL: Usamos el Adaptador para resolver el error 'cannot be applied'
                new HttpCredentialsAdapter(credentials)
        ).setApplicationName("RegistroApplication").build();
    }

    public Registro guardarRegistro(Registro registro) {
        // 1. Guardar en PostgreSQL
        Registro registroGuardado = registroRepository.save(registro);

        // 2. Guardar en Google Sheets (Excel)
        try {
            agregarFilaASheets(registroGuardado);
        } catch (IOException e) {
            // Manejo de error si Sheets falla, pero la BDD funciona
            System.err.println("Error al escribir en Google Sheets: " + e.getMessage());
        }

        return registroGuardado;
    }

    private void agregarFilaASheets(Registro registro) throws IOException {
        String range = sheetName + "!A:D"; // Rango de columnas a escribir

        List<Object> row = Arrays.asList(
                registro.getId(),
                registro.getNombre(),
                registro.getEmail(),
                registro.getFechaRegistro().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        List<List<Object>> values = Collections.singletonList(row);
        ValueRange body = new ValueRange().setValues(values);

        // Envía la solicitud para agregar la fila al final
        sheetsService.spreadsheets().values()
                .append(spreadsheetId, range, body)
                .setValueInputOption("USER_ENTERED")
                .setInsertDataOption("INSERT_ROWS")
                .execute();

        System.out.println("Fila agregada a Google Sheets con éxito.");
    }
}