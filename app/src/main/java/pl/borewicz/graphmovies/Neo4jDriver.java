package pl.borewicz.graphmovies;

import android.util.Base64;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.json.*;

public class Neo4jDriver {
    private static String mAuthData, mServerAddress;

    private static boolean checkAuth(String serverAddress, String username, String password)
    {
        String url = serverAddress + "/user/" + username;
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            //add reuqest header
            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "application/json; charset=UTF-8");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Authorization", "Basic "
                    + Base64.encodeToString((username + ":" + password).getBytes(), Base64.DEFAULT));

            return con.getResponseCode() == 200;
        }
        catch (Exception e) {
            return false;
        }
    }

    public static boolean InitializeConnection(String serverAddress, String username, String password)
    {
        if (checkAuth(serverAddress, username, password))
        {
            mServerAddress = serverAddress;
            mAuthData = Base64.encodeToString((username + ":" + password).getBytes(), Base64.DEFAULT);
            return true;
        }
        else
            return false;
    }

    public static JsonObject sendPost(String query) throws Exception {

        String url = mServerAddress + "/db/data/cypher";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("Accept", "application/json; charset=UTF-8");
        con.setRequestProperty("Content-Type", "application/json" );
        con.setRequestProperty("Authorization", "Basic " + mAuthData);

        String urlParameters = Json.createObjectBuilder()
                .add("query", query)
                .add("params", Json.createObjectBuilder().build())
                .build()
                .toString();

        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(urlParameters);
        wr.flush();
        wr.close();

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        JsonReader reader = Json.createReader(new StringReader(response.toString()));
        return reader.readObject();
    }

}
