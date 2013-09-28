package com.sh1r0.noveldroid;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

public class Ck101Parser extends AbstractParser {
	@Override
	protected String doInBackground(String... htmls) {
		StringBuilder bookData = new StringBuilder();
		BufferedReader reader = null;
		String line;
		int otherTable = 0;
		Pattern p_html = Pattern.compile("<[^>]+>", Pattern.CASE_INSENSITIVE);
		Matcher m_html;

		int stage;
		for (int n = 0; n < htmls.length; n++) {
			stage = 0;
			try {
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(htmls[n]), "UTF-8"));
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
				return null;
			}
			Log.d("Debug", "處理中: " + htmls[n]);
			try {
				while ((line = reader.readLine()) != null) {
					switch (stage) {
						case 0:
							if (line.indexOf("<div class=\"pbody\">") >= 0) {
								stage = 1;
							}
							break;
						case 1:
							if (line.indexOf("<h") >= 0) { // chapter title
								line = Replace.replace(line, " ", "");
								m_html = p_html.matcher(line);
								line = m_html.replaceAll("");
								bookData.append(line);
								bookData.append("\r\n");
							}
							if (line.indexOf("<div class=\"mes\">") >= 0) {
								stage = 2;
							}
							break;
						case 2:
							if (line.indexOf("class=\"postmessage\">") >= 0) {
								stage = 3;
								if (line.indexOf("<i class=\"pstatus\">") >= 0) { // filter out modified time
									line = line.replaceAll("<i class=\"pstatus\">[^<>]+ </i>", "");
								}
								if (line.indexOf("<div class=\"quote\">") >= 0) { // filter out quotes
									otherTable++;
									break;
								}
								line += "\r\n";
								line = Replace.replace(line, "<br/>", "\r\n");
								line = Replace.replace(line, "<br />", "\r\n");
								line = Replace.replace(line, "&nbsp;", "");
								m_html = p_html.matcher(line);
								line = m_html.replaceAll("");
								line = line.replaceAll("^[ \t]+", "");
								bookData.append(line);
							}
							break;
						case 3:
							if (line.indexOf("<div ") >= 0)
								otherTable++;
							if (line.indexOf("</div>") >= 0) {
								if (otherTable > 0) {
									otherTable--;
									break;
								} else {
									stage = 0;
									line += "\r\n";
								}
							}
							if (otherTable == 0) {
								line = Replace.replace(line, "&nbsp;", "");
								line = Replace.replace(line, "<br/>", "\r\n");
								line = Replace.replace(line, "<br />", "\r\n");
								if (line.indexOf("<i class=\"pstatus\">") >= 0) { // filter out modified time
									line = line.replaceAll("<i class=\"pstatus\">[^<>]+ </i>", "");
								}
								m_html = p_html.matcher(line);
								line = m_html.replaceAll("");
								line = line.replaceAll("^[ \t　]+", "");
								if (line.length() > 2)
									line = "　　" + line;
								bookData.append(line);
							}
							break;
						default:
							break;
					}
				}
			} catch (IOException e1) {
				return null;
			}
			bookData.append("\r\n");
			try {
				reader.close();
			} catch (IOException e) {
				return null;
			}
		}

		return bookData.toString();
	}
}
