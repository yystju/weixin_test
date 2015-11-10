package shi.quan.weixin.uploader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

import java.io.File;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

import java.io.*;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import java.net.HttpURLConnection;

import javax.json.JsonReader;
import javax.json.JsonObject;
import javax.json.Json;
import javax.json.JsonWriter;
import javax.json.JsonObjectBuilder;
import javax.json.JsonArrayBuilder;

import net.codejava.networking.MultipartUtility;
import java.util.List;
import java.io.StringWriter;

public class MAIN {
  private static void debug(Object obj) {
    System.out.println(obj);
  }

  private static void error(Object obj) {
    if(obj instanceof Exception) {
      ((Exception)obj).printStackTrace();
    } else {
      System.out.println(obj);
    }
  }

  private static interface FolderVisitor {
    void visit(int indend, File file);
  }

  private static void visitFolder(int indent, File root, FolderVisitor visitor) {
    if(root != null && root.exists() && root.isDirectory() && visitor != null) {
      File[] filesInDir = root.listFiles();

      for(int i = 0; i < filesInDir.length; ++i) {
        if(filesInDir[i].exists()) {
          if(filesInDir[i].isFile()) {
            visitor.visit(indent, filesInDir[i]);
          } else {
            visitFolder(indent+1, filesInDir[i], visitor);
          }
        }
      }
    }
  }

  private static BufferedImage sharpen(BufferedImage img) {
    float[] sharpKernel = {
      0.0f, -1.0f, 0.0f,
      -1.0f, 5.0f, -1.0f,
      0.0f, -1.0f, 0.0f
    };
    BufferedImageOp sharpen = new ConvolveOp(
      new Kernel(3, 3, sharpKernel),
      ConvolveOp.EDGE_NO_OP, null
    );

    return sharpen.filter(img, null);
  }

  private static String get(String urlStr) {
    try {
      System.setProperty("https.proxyHost", "pxlyon1.srv.volvo.com");
      System.setProperty("https.proxyPort", "8080");
      URL url = new URL(urlStr);
      
      HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
      
      conn.setDoOutput(true);
      conn.setUseCaches(false);
      conn.setRequestMethod("GET");
      
      int responseCode = conn.getResponseCode();
      
      //debug("responseCode := " + responseCode);
      
      String responseLine = new BufferedReader(new InputStreamReader(conn.getInputStream())).readLine();
      
      return responseLine;
    } catch(Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  private static String post(String urlStr, String content, String contentType) {
    try {
      System.setProperty("https.proxyHost", "pxlyon1.srv.volvo.com");
      System.setProperty("https.proxyPort", "8080");
      
      URL url = new URL(urlStr);
      
      HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
      
      conn.setDoOutput(true);
      conn.setUseCaches(false);
      conn.setRequestMethod("POST");

      conn.setRequestProperty("Content-Type", contentType);
      conn.setRequestProperty("Content-Length", "" + Integer.toString(content.length()));

      DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
      wr.writeBytes(content);
      wr.flush ();
      wr.close ();
      
      int responseCode = conn.getResponseCode();
      
      //debug("responseCode := " + responseCode);
      
      String responseLine = new BufferedReader(new InputStreamReader(conn.getInputStream())).readLine();
      
      return responseLine;
    } catch(Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  /**
   * https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=APPID&secret=APPSECRET
   */
  private static String getWeixinToken(String appId, String appsecret) {
    debug("[getWeixinToken]");

    String url = String.format("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s", new Object[]{appId, appsecret});

    //debug("url : " + url);

    String responseStr = get(url);

    //debug("responseStr : " + responseStr);

    JsonReader rdr = Json.createReader(new StringReader(responseStr));

    JsonObject obj = rdr.readObject();

    String token = obj.getString("access_token");

    // int expires_in = obj.getInt("expires_in");

    // debug("token : " + token);
    // debug("expires_in : " + expires_in);

    rdr.close();

    return token;
  }

  /**
   * https://api.weixin.qq.com/cgi-bin/media/upload?access_token=ACCESS_TOKEN&type=TYPE
   */
  private static String uploadWeixinMedia(String token, String type, String description, File file) {
    debug("[uploadWeixinMedia]");

    String url = String.format("https://api.weixin.qq.com/cgi-bin/media/upload?access_token=%s&type=%s", new Object[]{token, type});

    //debug("url : " + url);

    String media_id = null;

    try {
        MultipartUtility multipart = new MultipartUtility(url, "UTF-8");
         
        multipart.addHeaderField("User-Agent", "Weixin Resource Uploader");
        multipart.addHeaderField("Test-Header", "Header-Value");
         
        multipart.addFormField("description", description);
        multipart.addFormField("keywords", "");
         
        multipart.addFilePart("media", file);

        List<String> response = multipart.finish();

        String responseText = String.join("", response);

        JsonReader rdr = Json.createReader(new StringReader(responseText));

        JsonObject obj = rdr.readObject();

        if(!obj.containsKey("errcode")) {
          media_id = obj.getString("media_id");
        } else {
          media_id = null;
          error("WEIXIN ERROR : " + obj.getString("errmsg") + " ("+obj.getInt("errcode")+")");
        }
        rdr.close();
    } catch (IOException ex) {
        error(ex);
    }

    return media_id;
  }

  /**
   * https://api.weixin.qq.com/cgi-bin/media/uploadimg?access_token=ACCESS_TOKEN
   */
  private static String uploadWeixinImage(String token, File file) {
    debug("[uploadWeixinImage]");

    String url = String.format("https://api.weixin.qq.com/cgi-bin/media/uploadimg?access_token=%s", new Object[]{token});

    String ret = null;

    try {
        MultipartUtility multipart = new MultipartUtility(url, "UTF-8");
         
        multipart.addHeaderField("User-Agent", "Weixin Resource Uploader");
        multipart.addHeaderField("Test-Header", "Header-Value");

        multipart.addFormField("keywords", "");
        multipart.addFilePart("media", file);

        List<String> response = multipart.finish();

        String responseText = String.join("", response);

        JsonReader rdr = Json.createReader(new StringReader(responseText));

        JsonObject obj = rdr.readObject();

        ret = obj.getString("url");

        // if(0 == obj.getInt("errcode")) {
        //   ret = obj.getString("url");
        // } else {
        //   ret = null;
        //   error("WEIXIN ERROR : " + obj.getString("errmsg"));
        // }

        rdr.close();
    } catch (IOException ex) {
        error(ex);
    }

    return ret;
  }

  private static class WeixinAriticle {
    public String thumb_media_id; /*MANDTORY*/
    public String author = "";
    public String title;/*MANDTORY*/
    public String content_source_url = "";
    public String content;/*MANDTORY*/
    public String digest = "";
    public String show_cover_pic = "1";

    public WeixinAriticle(String thumb_media_id, String title, String content) {
      this.thumb_media_id = thumb_media_id;
      this.title = title;
      this.content = content;
    }
  }

  /**
   * https://api.weixin.qq.com/cgi-bin/media/uploadnews?access_token=ACCESS_TOKEN
   */
  private static String uploadWeixinNews(String token, WeixinAriticle[] articles) {
    debug("[uploadWeixinNews]");

    String url = String.format("https://api.weixin.qq.com/cgi-bin/media/uploadnews?access_token=%s", new Object[]{token});

    JsonObjectBuilder root = Json.createObjectBuilder();

    JsonArrayBuilder array = Json.createArrayBuilder();

    for(int i = 0; i < articles.length; ++i) {
      array.add(Json.createObjectBuilder()
            .add("thumb_media_id", articles[i].thumb_media_id)
            .add("author", articles[i].author)
            .add("title", articles[i].title)
            .add("content_source_url", articles[i].content_source_url)
            .add("content", articles[i].content)
            .add("digest", articles[i].digest)
            .add("show_cover_pic", articles[i].show_cover_pic));
    }

    root.add("articles", array);

    StringWriter stringWriter = new StringWriter();

    JsonWriter writer = Json.createWriter(stringWriter);

    writer.writeObject(root.build());

    writer.close();

    String content = stringWriter.toString();

    String responseStr = post(url, content, "application/json");

    debug("responseStr : " + responseStr);

    JsonReader rdr = Json.createReader(new StringReader(responseStr));

    JsonObject obj = rdr.readObject();

    String media_id = null;

    if(!obj.containsKey("errcode")) {
      media_id = obj.getString("media_id");
    } else {
      media_id = null;
      error("WEIXIN ERROR : " + obj.getString("errmsg") + " ("+obj.getInt("errcode")+")");
    }

    debug("media_id : " + media_id);

    rdr.close();

    return media_id;
  }

  /**
   * https://api.weixin.qq.com/cgi-bin/message/mass/sendall?access_token=ACCESS_TOKEN
   */
  private static String broadcastNews(String token, String media_id) {
    debug("[broadcastNews]");

    String url = String.format("https://api.weixin.qq.com/cgi-bin/message/mass/sendall?access_token=%s", new Object[]{token});

    JsonObjectBuilder root = Json.createObjectBuilder();

    root.add("filter", Json.createObjectBuilder()
            .add("is_to_all", true));
    root.add("msgtype", "mpnews");
    root.add("mpnews", Json.createObjectBuilder()
            .add("media_id", media_id));

    StringWriter stringWriter = new StringWriter();

    JsonWriter writer = Json.createWriter(stringWriter);

    writer.writeObject(root.build());

    writer.close();

    String content = stringWriter.toString();

    String responseStr = post(url, content, "application/json");

    //debug("responseStr : " + responseStr);

    JsonReader rdr = Json.createReader(new StringReader(responseStr));

    JsonObject obj = rdr.readObject();

    String msg_id = null;

    if(0 == obj.getInt("errcode")) {
      msg_id = obj.getString("msg_id");
    } else {
      msg_id = null;
      error("WEIXIN ERROR : " + obj.getString("errmsg") + " ("+obj.getInt("errcode")+")");
    }

    debug("msg_id : " + msg_id);

    rdr.close();

    return msg_id;
  }

  /**
   * https://api.weixin.qq.com/cgi-bin/message/mass/sendall?access_token=ACCESS_TOKEN
   */
  private static String broadcastText(String token, String text) {
    debug("[broadcastText]");

    String url = String.format("https://api.weixin.qq.com/cgi-bin/message/mass/sendall?access_token=%s", new Object[]{token});

    JsonObjectBuilder root = Json.createObjectBuilder();

    root.add("filter", Json.createObjectBuilder()
            .add("is_to_all", true));
    root.add("msgtype", "text");
    root.add("text", Json.createObjectBuilder()
            .add("content", text));

    StringWriter stringWriter = new StringWriter();

    JsonWriter writer = Json.createWriter(stringWriter);

    writer.writeObject(root.build());

    writer.close();

    String content = stringWriter.toString();

    String responseStr = post(url, content, "application/json");

    //debug("responseStr : " + responseStr);

    JsonReader rdr = Json.createReader(new StringReader(responseStr));

    JsonObject obj = rdr.readObject();

    String msg_id = null;

    if(0 == obj.getInt("errcode")) {
      msg_id = "" + obj.getInt("msg_id");
    } else {
      msg_id = null;
      error("WEIXIN ERROR : " + obj.getString("errmsg") + " ("+obj.getInt("errcode")+")");
    }

    debug("msg_id : " + msg_id);

    rdr.close();

    return msg_id;
  }

  /**
   * https://api.weixin.qq.com/cgi-bin/message/mass/preview?access_token=ACCESS_TOKEN
   */
  private static String previewNews(String token, String openId, String media_id) {
    debug("[broadcastNews]");

    String url = String.format("https://api.weixin.qq.com/cgi-bin/message/mass/preview?access_token=%s", new Object[]{token});

    JsonObjectBuilder root = Json.createObjectBuilder();

    root.add("touser", openId);
    root.add("msgtype", "mpnews");
    root.add("mpnews", Json.createObjectBuilder()
            .add("media_id", media_id));

    StringWriter stringWriter = new StringWriter();

    JsonWriter writer = Json.createWriter(stringWriter);

    writer.writeObject(root.build());

    writer.close();

    String content = stringWriter.toString();

    String responseStr = post(url, content, "application/json");

    debug("responseStr : " + responseStr);

    JsonReader rdr = Json.createReader(new StringReader(responseStr));

    JsonObject obj = rdr.readObject();

    String msg_id = null;

    if(0 == obj.getInt("errcode")) {
      msg_id = obj.getString("msg_id");
    } else {
      msg_id = null;
      error("WEIXIN ERROR : " + obj.getString("errmsg"));
    }

    debug("msg_id : " + msg_id);

    rdr.close();

    return msg_id;
  }

  public static void main(String[] args) {
    debug("[MAIN]");

    if(args.length < 1) {
      error("USAGE: java -jar ResourceLoader <RESOURCE_ROOT>");
      return;
    }

    try {
      // File rootFolder = new File(args[0]);

      // visitFolder(0, rootFolder, new FolderVisitor() {
      //   public void visit(int indent, File file) {
      //     try {
      //       String prefix = "";
      //       for(int i = 0; i < indent; ++i) {
      //         prefix += "\t";
      //       }
      //       debug(prefix + file);

      //       if(file.getName().toUpperCase().endsWith(".JPG")) {//The image file We need to upload.
      //         //Shrink the size.
      //         BufferedImage img = ImageIO.read(file);

      //         img = sharpen(img);

      //         debug("(" + img.getWidth() + "x" + img.getHeight() + ")");

      //         ImageIO.write(img, "jpg", file);
      //       }
      //     } catch(IOException ioe) { // IOE ^_^
      //       error(ioe);
      //     }
      //   }
      // });
      
      //String token = getWeixinToken("wxa9dcbc261ad64167", "d4624c36b6795d1d99dcf0547af5443d");
      String token = getWeixinToken("wx384c7e53c9f5ed13", "d0240e199e1f6e6a5e96018152a877f2");
      debug("token : " + token);
      if(token != null) {
        // String url = uploadWeixinImage(token, new File("./source/img/2.jpg"));
        // debug("url : " + url);

        // String media_id = uploadWeixinMedia(token, "image", "A Test Image", new File("./source/img/2.jpg"));
        // debug("media_id : " + media_id);

        // if(media_id != null) {
        //   WeixinAriticle[] articles = new WeixinAriticle[] {
        //     new WeixinAriticle(media_id, "TEST", "This is a test~")
        //   };

        //   articles[0].content_source_url = url;

        //   String news_media_id = uploadWeixinNews(token, articles);

        //   if(news_media_id != null) {
        //     String msgId = broadcastNews(token, news_media_id);
        //   }
        // }

        broadcastText(token, "Hello");
      }
    } catch (Exception ex) {
      error(ex);
    }
  }
}