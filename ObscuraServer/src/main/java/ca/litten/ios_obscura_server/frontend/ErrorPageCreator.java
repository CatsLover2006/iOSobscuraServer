package ca.litten.ios_obscura_server.frontend;

public class ErrorPageCreator {
    
    public final String app404;
    
    public final String general404;
    
    public ErrorPageCreator(String headerTag) {
        app404 = "<!DOCTYPE html>\n<html class=\"cytyle-flat\"><head><meta charset=utf-8><title>Nonexistent App</title><link rel=\"apple-touch-icon\" sizes=\"1024x1024\" href=\"/icon\"><link rel=\"icon\" type=\"image/png\" sizes=\"32x32\" href=\"/icon32\"><link rel=\"icon\" type=\"image/png\" sizes=\"16x16\" href=\"/icon16\"><meta name=\"viewport\" content=\"initial-scale=1.0,width=device-width\"><style>@import url(/getCSS)</style>"
            + headerTag + "</head><body class=pinstripe><panel><fieldset><div><div><strong><center>Error 404</center></strong></div></div><div><div>That app doesn't exist. At least, it isn't part of iOS Obscura.</div></div><a href=https://discord.gg/rTJ9zxjMu3><div><div>Have the app?<br>Join the iOS Obscura discord!</div></div></a><div><div><form action=/searchPost><input name=search placeholder=Search style=\"-webkit-appearance:none;border-bottom:1px solid #999\"> <button style=\"float:right;background:0 0\" type=submit><img src=/searchIcon style=height:18px;border-radius:50%></button></form></div></div><a href=javascript:history.back()><div><div>Go Back</div></div></a><a href=\"/\"><div><div>Return to Homepage</div></div></a></fieldset></panel></body></html>";
        general404 = "<!DOCTYPE html>\n<html class=\"cytyle-flat\"><head><meta charset=utf-8><title>Error 404</title><link rel=\"apple-touch-icon\" sizes=\"1024x1024\" href=\"/icon\"><link rel=\"icon\" type=\"image/png\" sizes=\"32x32\" href=\"/icon32\"><link rel=\"icon\" type=\"image/png\" sizes=\"16x16\" href=\"/icon16\"><meta name=\"viewport\" content=\"initial-scale=1.0,width=device-width\"><style>@import url(/getCSS);</style>"
                + headerTag + "</head><body class=pinstripe><panel><fieldset><div><div><strong><center>Error 404</center></strong></div></div><div><div>That location doesn't exist.</div></div><a href=https://discord.gg/rTJ9zxjMu3><div><div>Have any legacy iOS apps?<br>Join the iOS Obscura discord!</div></div></a><div><div><form action=/searchPost><input name=search placeholder=Search style=\"-webkit-appearance:none;border-bottom:1px solid #999\"> <button style=\"float:right;background:0 0\" type=submit><img src=/searchIcon style=height:18px;border-radius:50%></button></form></div></div><a href=javascript:history.back()><div><div>Go Back</div></div></a><a href=\"/\"><div><div>Return to Homepage</div></div></a></fieldset></panel></body></html>";
    }
}