package ca.litten.ios_obscura_server.parser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;

public class ArchiveListDecoder {
    public static String[] getUrlListFromArchiveOrgListing(URL archiveOrgFileListUrl) {
        try {
            LinkedList<String> list = new LinkedList<>();
            String pathMinusFile;
            {
                String len = archiveOrgFileListUrl.toString();
                String[] t = archiveOrgFileListUrl.getPath().split("/");
                pathMinusFile = len.substring(0, len.length() -
                        t[t.length - 1].length());
            }
            HttpURLConnection connection = (HttpURLConnection)archiveOrgFileListUrl.openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("GET");
            connection.connect();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                System.err.println("Not found");
                return new String[]{};
            }
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document document = builder.parse(connection.getInputStream());
            Element root = document.getDocumentElement();
            NodeList nodes = root.getChildNodes();
            Node node;
            for (int i = 0; i < nodes.getLength(); i++) {
                node = nodes.item(i);
                if (node.getAttributes().getNamedItem("name").getNodeValue().toLowerCase().endsWith(".ipa")) {
                    list.add(pathMinusFile + node.getAttributes().getNamedItem("name").getNodeValue());
                } else {
                    NodeList children = node.getChildNodes();
                    for (int j = 0; j < children.getLength(); j++) {
                        if (children.item(j).getNodeName().equals("format") &&
                                children.item(j).getChildNodes().item(0).getNodeValue()
                                        .equals("iOS App Store Package")) {
                            list.add(pathMinusFile + node.getAttributes().getNamedItem("name").getNodeValue());
                        }
                    }
                }
            }
            return list.toArray(new String[]{});
        } catch (Exception e) {
            System.err.println(e);
        }
        return new String[]{};
    }
}
