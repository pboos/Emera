/*
 * Copyright (C) 2011 Tonchidot Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.pboos.emera.lib;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Config;
import android.util.Log;

public class PictureUploader {
    private static final String TAG = PictureUploader.class.getSimpleName();

    public String uploadPicture(byte[] data) {
        // TODO replace with own picture server
        String imgbur_key = "5bcb1b22c40d6631e328598280903c6d"; // imgur.com

        final List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("key", imgbur_key));
        nameValuePairs.add(new BasicNameValuePair("type", "base64"));

        String result = doFileUploadJson("http://api.imgur.com/2/upload.json?key=" + imgbur_key,
                "image", data);
        JSONObject json;
        try {
            json = new JSONObject(result);
            return json.getJSONObject("upload").getJSONObject("links").getString("original");
        } catch (JSONException e) {
            return null;
        }
    }

    private String doFileUploadJson(String url, String fileParamName, byte[] data) {
        try {
            String boundary = "BOUNDARY" + new Date().getTime() + "BOUNDARY";
            String lineEnd = "\r\n";
            String twoHyphens = "--";

            URL connUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) connUrl.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"" + fileParamName
                    + "\";filename=\"photo.jpg\"" + lineEnd);
            dos.writeBytes(lineEnd);
            dos.write(data, 0, data.length);
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
            dos.flush();
            dos.close();

            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            rd.close();
            return result.toString();
        } catch (IOException e) {
            if (Config.LOGD) {
                Log.d(TAG, "IOException : " + e);
            }
        }
        return null;
    }
}
