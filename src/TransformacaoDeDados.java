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
    private static final Map<String, String> LEGENDA = Map.of(
            "OD", "Odontológico",
            "AMB", "Ambulatorial"
    );

    public static void main(String[] args) {
        System.out.println("Iniciando Sistema...");
        try {

            //Chama função para baixar PDF
            System.out.println("Baixando arquivo PDF...");
            File pdfFile = DownloadPDF("https://www.gov.br/ans/pt-br/acesso-a-informacao/participacao-da-sociedade/atualizacao-do-rol-de-procedimentos/Anexo_I_Rol_2021RN_465.2021_RN627L.2024.pdf");


            //Chama função para extrair dados
            System.out.println("Extraindo Dados da Tabela...");
            List<String[]> tabela = ExtractTable(pdfFile);


            //Chama função para salvar dados em csv
            System.out.println("Salvando dados em CSV...");
            String csvPath = "rol_procedimentos.csv";
            SaveCSV(tabela, csvPath);


            //Chama função para compactar arquivo
            System.out.println("Compactando arquivo...");
            String zipPath = "Teste_Bryan_Mendes.zip";
            CreateZIP(csvPath, zipPath);


            //Limpeza
            Files.deleteIfExists(Paths.get(csvPath));
            Files.deleteIfExists(pdfFile.toPath());

            System.out.println("Processo concluído! Arquivo ZIP gerado: " + zipPath);
        } catch (Exception e) {
            System.out.println("Erro: " + e.getMessage());
            e.printStackTrace();
        }
    }



    private static List<String[]> ExtractTable(File pdfFile) throws IOException {
        List<String[]> dados = new ArrayList<>();

        dados.add(new String[] {"Código", "Procedimento", "RN", "Vigência", "Tipo", "SubCategoria", "Categoria", "Capitulo"});

        try (PDDocument doc = Loader.loadPDF(pdfFile)) {
            String text = new PDFTextStripper().getText(doc);

            String regex = (
                    "(\\d{4}\\.\\d{2}\\.\\d{2}-\\d)\\s+" +   // Código
                            "([^\\n]+?)\\s+" +               // Procedimento
                            "(RN\\d+)\\s+" +                 // RN
                            "(\\d{2}/\\d{4})\\s+" +          // Vigência
                            "(OD|AMB)\\s+" +                 // Tipo
                            "([^\\n]+?)\\s+" +               // Subcategoria
                            "([^\\n]+?)\\s+" +               // Categoria
                            "(CAPÍTULO\\s+\\d+.+)"           // Capítulo
            );

            Pattern padrao = Pattern.compile(regex);
            Matcher matcher = padrao.matcher(text.replace("\r", ""));

            while (matcher.find()) {
                String[] linha = new String[8];
                linha[0] = matcher.group(1);    // Código
                linha[1] = matcher.group(2).trim(); // Procedimento
                linha[2] = matcher.group(3);    // RN
                linha[3] = matcher.group(4);    // Vigência
                linha[4] = LEGENDA.getOrDefault(matcher.group(5), matcher.group(5)); // Tipo
                linha[5] = matcher.group(6).trim(); // Subcategoria
                linha[6] = matcher.group(7).trim(); // Categoria
                linha[7] = matcher.group(8).trim(); // Capítulo

                dados.add(linha);
            }
        }
        return dados;
    }

    //Função para salvar dados em csv
    private static void SaveCSV(List<String[]> tabela, String caminho) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(caminho))) {
            for (String[] linha : tabela) {
                writer.write(String.join(";", linha) + "\n");
            }
        }
    }

    //Função para compactar arquivo
    private static void CreateZIP(String arquivo, String zipPath) throws IOException {
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

    //Função para baixar PDF
    private static File DownloadPDF(String url) throws IOException {
        String nome = "Anexo_I.pdf";
        try (InputStream in = new URL(url).openStream()) {
            Files.copy(in, Paths.get(nome), StandardCopyOption.REPLACE_EXISTING);
        }
        return new File(nome);
    }
}