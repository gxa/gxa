package uk.ac.ebi.gxa.anatomogram.svgutil;

import org.apache.batik.dom.util.DOMUtilities;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.JPEGTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public enum ImageFormat {
    SVG, JPEG, PNG;

    public void writeSvg(Document svg, OutputStream os) throws IOException, TranscoderException {
        switch (this) {
            case SVG: {
                DOMUtilities.writeDocument(svg, new OutputStreamWriter(os));
                return;
            }
            case JPEG: {
                JPEGTranscoder t = new JPEGTranscoder();
                TranscoderInput input = new TranscoderInput(svg);
                TranscoderOutput output = new TranscoderOutput(os);
                t.transcode(input, output);
                return;
            }
            case PNG: {
                PNGTranscoder t = new PNGTranscoder();
                TranscoderInput input = new TranscoderInput(svg);
                TranscoderOutput output = new TranscoderOutput(os);
                t.transcode(input, output);
                return;
            }
            default:
                throw new IllegalStateException("unknown encoding");
        }
    }
}
