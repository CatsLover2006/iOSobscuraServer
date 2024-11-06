package ca.litten.ios_obscura_server.frontend;

public class Templates {
    public static String generateBasicHeader(String title) {
        return "<!DOCTYPE html>\n" +
                "<html class=\"cytyle-flat\"><head><meta charset=\"utf-8\"><title>" + title + "</title><link rel=\"apple-touch-icon\" sizes=\"1024x1024\" href=\"/icon\"><link rel=\"icon\" type=\"image/png\" sizes=\"32x32\" href=\"/icon32\"><link rel=\"icon\" type=\"image/png\" sizes=\"16x16\" href=\"/icon16\"><meta name=\"viewport\" content=\"initial-scale=1.0,width=device-width\"><style>@import url(/getCSS/secure);@import url(/getCSS/insecure);</style></head>";
    }
}
