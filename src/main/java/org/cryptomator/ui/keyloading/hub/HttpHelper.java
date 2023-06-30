package org.cryptomator.ui.keyloading.hub;

import com.google.common.io.CharStreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

class HttpHelper {

	public static String readBody(HttpResponse<InputStream> response) throws IOException {
		try (var in = response.body(); var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			return CharStreams.toString(reader);
		}
	}

}
