import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

public class TransformacaoDeDados {

    public static void main(String[] args) {
        System.out.println("Iniciando Sistema...");
        try {
            File pdfFile = downloadPDF("https://www.gov.br/ans/pt-br/acesso-a-informacao/participacao-da-sociedade/atualizacao-do-rol-de-procedimentos/Anexo_I_Rol_2021RN_465.2021_RN627L.2024.pdf");
            String rawText = extractText(pdfFile);
            Files.write(Paths.get("raw_text.txt"), rawText.getBytes());

            List<String[]> tabela = extractTableData(rawText);
            if (tabela.size() <= 1) {
                System.out.println("AVISO: Nenhum dado foi extraído.");
                return;
            }

            String csvPath = "rol_procedimentos_completo.csv";
            saveCSV(tabela, csvPath);
            String zipPath = "Teste_Bryan_Mendes.zip";
            createZIP(csvPath, zipPath);

            Files.deleteIfExists(Paths.get(csvPath));
            Files.deleteIfExists(pdfFile.toPath());

            System.out.println("\nProcesso concluído com sucesso!");
            System.out.println("Total de procedimentos extraídos: " + (tabela.size() - 1));
            System.out.println("Arquivo ZIP gerado: " + zipPath);

        } catch (Exception e) {
            System.err.println("Erro durante o processamento: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String extractText(File pdfFile) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setLineSeparator("\n");
            return stripper.getText(doc);
        }
    }

    private static List<String[]> extractTableData(String rawText) {
        List<String[]> dados = new ArrayList<>();
        dados.add(new String[]{"PROCEDIMENTO", "RN", "VIGÊNCIA", "OD", "AMB", "HCO", "HSO", "REF", "PAC", "DUT", "SUBGRUPO", "GRUPO", "CAPÍTULO"});

        String[] linhas = rawText.split("\n");
        boolean inTable = false;
        StringBuilder procedimentoAtual = new StringBuilder();
        String[] ultimoRegistro = null;

        // Variáveis para controlar a hierarquia
        String capituloAtual = "";
        String grupoAtual = "";
        String subgrupoAtual = "";

        // Padrões para extração
        Pattern rnPattern = Pattern.compile("(\\d+[A-Z]?/\\d{4})");
        Pattern dataPattern = Pattern.compile("(\\d{2}/\\d{2}/\\d{4})");
        Pattern dutPattern = Pattern.compile("DUT\\s*(\\d+)");
        Pattern subgrupoPattern = Pattern.compile("(CONSULTAS, VISITAS HOSPITALARES OU|PROCEDIMENTOS CLÍNICOS AMBULATORIAIS|AVALIAÇÕES/ACOMPANHAMENTOS|MONITORIZAÇÕES|REABILITAÇÃO|TERAPÊUTICA)");

        for (int i = 0; i < linhas.length; i++) {
            String linha = linhas[i].trim();

            // Identifica o início da tabela
            if (linha.contains("PROCEDIMENTO") && linha.contains("VIGÊNCIA") && linha.contains("OD") && linha.contains("AMB")) {
                inTable = true;
                continue;
            }

            if (!inTable) continue;

            // Atualiza hierarquia (CAPÍTULO, GRUPO, SUBGRUPO)
            if (linha.matches("(?i)^CAP[ÍI]TULO\\s+.*")) {
                capituloAtual = linha.replaceAll("(?i)^CAP[ÍI]TULO\\s+", "").trim();
                grupoAtual = "";
                subgrupoAtual = "";
                continue;
            }
            else if (linha.matches("(?i)^GRUPO\\s+.*")) {
                grupoAtual = linha.replaceAll("(?i)^GRUPO\\s+", "").trim();
                subgrupoAtual = "";
                continue;
            }
            else if (subgrupoPattern.matcher(linha).find()) {
                subgrupoAtual = linha.trim();
                // Verifica se a próxima linha é continuação do subgrupo
                if (i + 1 < linhas.length && linhas[i+1].trim().matches("E HOSPITALARES.*")) {
                    subgrupoAtual += " " + linhas[i+1].trim();
                    i++;
                }
                continue;
            }

            // Linha de procedimento (contém códigos como OD, AMB, etc.)
            if (linha.matches(".*\\b(OD|AMB|HCO|HSO|REF|PAC|DUT)\\b.*")) {
                // Se temos um procedimento acumulado, adiciona ao registro anterior
                if (procedimentoAtual.length() > 0 && ultimoRegistro != null) {
                    ultimoRegistro[0] = cleanProcedimento(procedimentoAtual.toString());
                    procedimentoAtual.setLength(0);
                }

                String[] rnVigencia = extractRnVigencia(linha, rnPattern, dataPattern);
                String procedimento = extractProcedimento(linha);
                String dut = extractDUT(linha, dutPattern);

                String[] partes = new String[13];
                partes[0] = procedimento;
                partes[1] = rnVigencia[0].isEmpty() ? "" : "RN" + rnVigencia[0];
                partes[2] = rnVigencia[1];
                // Manter as abreviações originais (OD, AMB, etc.)
                partes[3] = linha.contains("OD") ? "Odontológica" : "";
                partes[4] = linha.contains("AMB") ? "Ambulatorial" : "";
                partes[5] = linha.contains("HCO") ? "HCO" : "";
                partes[6] = linha.contains("HSO") ? "HSO" : "";
                partes[7] = linha.contains("REF") ? "REF" : "";
                partes[8] = linha.contains("PAC") ? "PAC" : "";
                partes[9] = dut;
                partes[10] = subgrupoAtual.isEmpty() ? "CONSULTAS, VISITAS HOSPITALARES OU ACOMPANHAMENTO DE PACIENTES" : subgrupoAtual;
                partes[11] = grupoAtual.isEmpty() ? "PROCEDIMENTOS GERAIS" : grupoAtual;
                partes[12] = capituloAtual.isEmpty() ? "PROCEDIMENTOS GERAIS" : capituloAtual;

                dados.add(partes);
                ultimoRegistro = partes;
                procedimentoAtual = new StringBuilder(procedimento);
            }
            // Continuação do nome do procedimento
            else if (!linha.isEmpty() && ultimoRegistro != null &&
                    !linha.matches("(?i).*(CAP[ÍI]TULO|GRUPO|SUBGRUPO|PROCEDIMENTOS|HOSPITALARES|ACOMPANHAMENTOS|MONITORIZAÇÕES).*")) {
                if (procedimentoAtual.length() == 0) {
                    procedimentoAtual.append(ultimoRegistro[0]);
                }
                procedimentoAtual.append(" ").append(linha);
                ultimoRegistro[0] = cleanProcedimento(procedimentoAtual.toString());
            }
        }
        return dados;
    }

    private static String extractProcedimento(String linha) {
        // Remove RN e vigência
        String procedimento = linha.replaceAll("RN\\d+/\\d{4}\\s+\\d{2}/\\d{2}/\\d{4}", "")
                .replaceAll("\\d+[A-Z]?/\\d{4}\\s+\\d{2}/\\d{2}/\\d{4}", "")
                // Remove todos os códigos e textos técnicos
                .replaceAll("\\b(OD|AMB|HCO|HSO|REF|PAC|DUT\\s*\\d*)\\b.*", "")
                // Remove textos de hierarquia e repetições
                .replaceAll("PROCEDIMENTOS (CL[ÍI]NICOS )?[A-Z]+.*", "")
                .replaceAll("E HOSPITALARES.*", "")
                .replaceAll("AVALIAÇÕES/ACOMPANHAMENTOS.*", "")
                .replaceAll("MONITORIZAÇÕES.*", "")
                // Remove números no final
                .replaceAll("\\s+\\d+$", "")
                .trim();

        return cleanProcedimento(procedimento);
    }

    private static String cleanProcedimento(String procedimento) {
        // Remove qualquer texto que pareça ser do subgrupo
        String[] termosParaRemover = {
                "ACOMPANHAMENTO DE PACIENTES",
                "CONSULTAS, VISITAS HOSPITALARES",
                "PROCEDIMENTOS CLÍNICOS",
                "E HOSPITALARES",
                "AVALIAÇÕES/ACOMPANHAMENTOS",
                "MONITORIZAÇÕES"
        };

        for (String termo : termosParaRemover) {
            if (procedimento.contains(termo)) {
                procedimento = procedimento.substring(0, procedimento.indexOf(termo)).trim();
                break;
            }
        }

        // Remove múltiplos espaços e trim final
        return procedimento.replaceAll("\\s+", " ").trim();
    }

    private static String[] extractRnVigencia(String linha, Pattern rnPattern, Pattern dataPattern) {
        String rn = "";
        String vigencia = "";

        Matcher rnMatcher = rnPattern.matcher(linha);
        if (rnMatcher.find()) {
            rn = rnMatcher.group(1);
        }

        Matcher dataMatcher = dataPattern.matcher(linha);
        if (dataMatcher.find()) {
            vigencia = dataMatcher.group(1);
        }

        return new String[]{rn, vigencia};
    }

    private static String extractDUT(String linha, Pattern pattern) {
        Matcher matcher = pattern.matcher(linha);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static void saveCSV(List<String[]> tabela, String caminho) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(caminho))) {
            writer.write("\uFEFF"); // BOM para UTF-8
            for (String[] linha : tabela) {
                writer.write(String.join(";", linha));
                writer.newLine();
            }
        }
    }

    private static void createZIP(String arquivo, String zipPath) throws IOException {
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipPath));
             FileInputStream fis = new FileInputStream(arquivo)) {
            zipOut.putNextEntry(new ZipEntry(arquivo));
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zipOut.write(buffer, 0, length);
            }
        }
    }

    private static File downloadPDF(String url) throws IOException {
        String nome = "Anexo_I.pdf";
        try (InputStream in = new URL(url).openStream()) {
            Files.copy(in, Paths.get(nome), StandardCopyOption.REPLACE_EXISTING);
        }
        return new File(nome);
    }
}