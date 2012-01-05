package uk.ac.ebi.gxa.annotator.loader;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.annotator.AnnotationException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * User: nsklyar
 * Date: 20/12/2011
 */
public class URLContentLoader {

    static final private Logger log = LoggerFactory.getLogger(URLContentLoader.class);

    static File getContentAsFile(String url, File file) throws AnnotationException {
        //ToDo: check if url is not file

        HttpClient client = new HttpClient();

        GetMethod method = new GetMethod(url);

        method.setFollowRedirects(true);

        FileOutputStream out = null;
        InputStream in = null;
        try {
            int statusCode = client.executeMethod(method);

            if (statusCode != HttpStatus.SC_OK) {
                throw new AnnotationException("Failed to connect to: " + url + " " + method.getStatusLine());
            }

            final long responseContentLength = method.getResponseContentLength();

            in = method.getResponseBodyAsStream();
            out = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int count;
            int size = 0;
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
                size = size + count;
            }
            out.flush();

            if (size < responseContentLength) {
                log.error("Not all data are loaded actual size {} expected size {}", size, responseContentLength);
                throw new AnnotationException("Failed to download all annotation data from: " + url +
                        " expected size=" + responseContentLength + " actual=" + size + ". Please try again!");
            }
        } catch (HttpException e) {
            throw new AnnotationException("Fatal protocol violation, when reading from " + url, e);
        } catch (IOException e) {
            throw new AnnotationException("Fatal transport error when reading from " + url, e);
        } finally {
            method.releaseConnection();
            try {
                out.close();
            } catch (IOException e) {
                log.error("Cannot close output stream for file " + file.getAbsolutePath());
            }
            try {
                in.close();
            } catch (IOException e) {
                log.error("Cannot close input stream for reading from " + url);
            }
        }
        return file;

    }
}
