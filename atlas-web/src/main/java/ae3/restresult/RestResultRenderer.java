package ae3.restresult;

import java.io.IOException;

/**
 * REST result formatter interface
 * @author pashky
 */
public interface RestResultRenderer {
    /**
     * Render object into output using specified profile
     * @param object object to render
     * @param where where to append result text
     * @param profile profile to use
     * @throws RenderException if anything goes wrong with formatting
     * @throws IOException if i/o write error occurs
     */
    void render(Object object, Appendable where, final Class profile) throws RenderException, IOException;
}
