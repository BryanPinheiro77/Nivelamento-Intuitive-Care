import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

public class WebScraping {
    public static void main(String[] args) {
        try{
            String url = "https://www.gov.br/ans/pt-br/acesso-a-informacao/participacao-da-sociedade/atualizacao-do-rol-de-procedimentos";
            System.out.println("Acessando Site...");


            String htmlContent = getHtml(url); //Chama o metodo para pegar o html

            List<String> pdfLinks = extractPdf(htmlContent); //Extrai os links pdf

            if (pdfLinks.isEmpty()){
                System.out.println("Nenhum Pdf encontrado!");
                return;
            }

            List<String> downloadedFiles = downloadPdfs(pdfLinks); //Chama o metodo para fazer download dos pdfs

            createZip(downloadedFiles, "anexos.zip");

            deleteFiles(downloadedFiles); //Chama o metodo de deletar os arquivos baixados apos serem zipados

            System.out.println("Processo finalizado com sucesso!");
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    //Usado para pegar o html da pagina usando HttpURLConnection
    public static String getHtml(String urlString) throws IOException{
        StringBuilder content = new StringBuilder();
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))){
            String line;
            while ((line = reader.readLine()) != null){
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    //Usado para Extrair os links de pdf encontrados na página(Somente Anexo I e II)
    public static List<String> extractPdf(String html) {
        List<String> pdfLinks = new ArrayList<>();
        String baseUrl = "https://www.gov.br";
        Pattern pattern = Pattern.compile("href=\"(.*?)\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(html);

        while (matcher.find()) {
            String link = matcher.group(1);
            if (link.endsWith(".pdf") && (link.contains("Anexo_I") || link.contains("Anexo_II"))) {
                if (!link.startsWith("http")) {
                    link = baseUrl + link;
                }
                pdfLinks.add(link);
            }
        }
        return pdfLinks;
    }



    //Usado para fazer o download dos pdfs encontrados
    public static List<String> downloadPdfs(List<String> pdfLinks){
        List<String> fileNames = new ArrayList<>();

        for (String pdfLink : pdfLinks) {
            try{
                String fileName = pdfLink.substring(pdfLink.lastIndexOf("/") + 1);
                System.out.println("Baixando: " + fileName);

                try (InputStream in = new URL(pdfLink).openStream()) {
                    Files.copy(in, Paths.get(fileName), StandardCopyOption.REPLACE_EXISTING);
                }

                fileNames.add(fileName);
                System.out.println("Download Concluído: " + fileName);
            } catch (IOException e) {
                System.out.println("Erro ao baixar " + pdfLink + ": " + e.getMessage());
            }
        }
        return fileNames;
    }


    //Adiciona os arquivos baixados em um arquivo Zip
    public static void createZip(List<String> files, String zipFileName){
        try(ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFileName))){
            for (String file : files) {
                File fileToZip = new File(file);

                try (FileInputStream inputFile = new FileInputStream(fileToZip)) {
                    ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
                    zipOut.putNextEntry(zipEntry);

                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputFile.read(buffer)) >= 0){
                        zipOut.write(buffer, 0, length);
                    }
                }
            }
            System.out.println("ZIP criado com sucesso!");
        } catch (IOException e) {
            System.out.println("Erro ao criar o arquivo ZIP: " + e.getMessage());
        }
    }

    //Deleta os arquivos baixados após serem zipados
    public static void deleteFiles(List<String> files){
        for (String file : files) {
            try{
                Files.deleteIfExists(Paths.get(file));
                System.out.println("Arquivo: " + file + " Deletado com sucesso!");
            } catch (IOException e){
                System.out.println("Erro ao deletar " + file + ": " + e.getMessage());
            }
        }
    }
}
